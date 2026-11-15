# Design

## Context

- 存储提供方在服务启动期由 `sys.file.storageType` 选定，`iboot-file`、`iboot-job`、`iboot-system` 各自持有一份 `FileStorageConfig` 与 `SiloProperties`，并在 Bean 创建时即连接对象存储；`iboot-monitor` 的 `SiloProperties` 只用于监控查询。
- 宿主机开发与 Docker 全量运行共用同一个 MySQL 实例，`sys_config` 只有一份，无法为两种运行方式存入不同的 endpoint。
- `openspec/specs/file-storage/spec.md` 已约定对象存储下载必须 302 到预签名 URL，因此不能用“全部流量走应用代理”来回避公网地址问题。
- Flyway 会校验已执行迁移的校验和，不能直接修改 `V1__baseline.sql` 的种子值来改变默认值。
- `sys_config` 的 `update_time` 在管理端修改时会更新时间；种子行保持 `NULL`，可用于区分“从未改过的默认值”和“被显式改过的值”。

## Goals / Non-Goals

**Goals:**

- 全新数据库和 Docker 全量部署默认使用 Silo，且不需要手工改参数。
- 宿主机开发保持 `localhost:9000` 可达；Docker 内服务端使用 `silo:9000`；浏览器预签名地址使用宿主机或公网地址。
- 部署差异与密钥通过环境变量注入，为云上部署留出配置入口。
- 不改变现有 `sys_config` 参数的可见性与管理方式。

**Non-Goals:**

- 不迁移已经写入本地磁盘的历史文件。
- 不引入对象存储的高可用、分片、生命周期策略。
- 不替换 `oss` 提供方，也不把 Silo 凭据改成只允许环境变量。
- 不改动 Grafana/Tempo 或本次存储变更之外的可观测性配置。

## Decisions

### 决策 1：环境变量优先，`sys_config` 兜底

`SiloProperties` 与 `FileStorageConfig` 先读取环境变量，值为空时再读 `sys_config`。变量命名沿用模块前缀：

- `IBOOT_FILE_STORAGE_TYPE`
- `IBOOT_FILE_SILO_ENDPOINT`
- `IBOOT_FILE_SILO_PUBLIC_ENDPOINT`
- `IBOOT_FILE_SILO_ACCESS_KEY`
- `IBOOT_FILE_SILO_SECRET_KEY`
- `IBOOT_FILE_SILO_BUCKET`

Docker 的 `x-app-env` 显式设置类型与内网 endpoint，因此 `.env` 中的宿主机默认值不会污染容器（Compose 的 `environment` 优先级高于 `env_file`）。

**替代方案**：

- 只改数据库：无法同时适配宿主机与容器。
- 只读环境变量：破坏现有参数配置页，且大量非部署参数不适合放进 env。
- 为每个运行方式建独立数据库：改动更大，且与项目现有单库开发约定冲突。

### 决策 2：迁移无条件把 `local` 切到 `silo`

新增迁移执行：

```sql
UPDATE `sys_config`
SET `config_value` = 'silo'
WHERE `config_key` = 'sys.file.storageType'
  AND `config_value` = 'local';
```

**放弃的方案**：最初打算用 `update_time IS NULL` 只改“从未被编辑过”的种子行。实际验证发现 V3 修复种子编码、V7 改 MinIO 命名都会更新这些行，而 `update_time` 带 `ON UPDATE CURRENT_TIMESTAMP`，迁移执行完后所有种子行的 `update_time` 都不再为空，条件永远不成立。`update_by` 也没有被管理端更新路径写入，无法区分人工编辑。因此选择无条件切换；需要保留本地存储的环境用 `IBOOT_FILE_STORAGE_TYPE=local` 覆盖，或在升级后把配置改回 `local`。

**替代方案**：

- 新增专用“已编辑”标记列或标记表：改动面大，且无法追溯本次之前的历史编辑。
- 修改 `V1__baseline.sql`：已执行迁移的环境会因校验和不一致而失败。

### 决策 3：读写客户端与预签名客户端分离

`SiloFileStorageStrategy` 用内网 endpoint 建一个客户端负责 `putObject`、`statObject`、`getObject`、`removeObject`、`bucketExists`；另建一个只用于 `getPresignedObjectUrl` 的客户端，endpoint 取 `publicEndpoint`。两者地址相同时复用同一个客户端。

SigV4 预签名把 host 纳入签名，不能简单把内网 URL 的 host 替换成公网地址，否则签名失效。客户端显式指定 region（默认 `us-east-1`，可用 `IBOOT_FILE_SILO_REGION` 覆盖），避免预签名前向浏览器侧地址发起 bucket 区域查询——容器内无法访问 `localhost:9000`，这一步必须避免。

**替代方案**：

- 字符串替换 URL host：签名校验失败。
- 所有下载经文件服务代理：与 `file-storage` spec 的 302 预签名约定冲突，并增加服务端带宽与内存压力。
- 让容器通过 `host.docker.internal` 访问宿主机端口：需要额外 hosts 配置，且浏览器侧不可移植。

### 决策 4：用 `@Value` 字段注入覆盖值，不改构造签名

四个 `SiloProperties` 的构造器已被单测直接调用。通过 `@Value("${iboot.file.silo.endpoint:}")` 注入覆盖字段，在字段为空时回退到原有读取逻辑；测试中手工构造实例时字段为 `null`，行为不变。Spring 的 `SystemEnvironmentPropertySource` 会把 `iboot.file.silo.endpoint` 解析到 `IBOOT_FILE_SILO_ENDPOINT`。

**替代方案**：

- 给构造器加参数：需要同步修改所有单测。
- 抽到共享内核统一配置：四份重复实现本就是历史遗留，本次不做跨模块重构。

### 决策 5：补齐启动依赖

`iboot-system` 与 `iboot-job` 在启动期会实例化存储策略并连接 Silo，Compose 中补 `silo: condition: service_healthy`，避免镜像启动顺序导致的连接失败。宿主机开发由 `scripts/dev-init.sh` 在启动后端前等待 Silo 健康，无需额外改动。

## Risks / Trade-offs

- [切换后历史本地文件不可读] → 文档明确说明；需要保留旧文件时在切换前导出，或把存储类型改回 `local` 后重启。
- [参数配置页显示 DB 中的端点，而容器实际使用 env 覆盖值] → 文档说明 env 优先；启动日志打印最终生效的存储类型与内网/公网 endpoint，便于排障。
- [环境变量拼写错误会静默回退到 DB] → 对非空覆盖值做合法性校验；非法存储类型直接阻止启动并打印原值。
- [公网 endpoint 配置成内网地址导致浏览器下载失败] → 部署检查清单要求公网地址可从浏览器访问；预签名 URL 的 host 由独立客户端保证一致。
- [生产环境仍把凭据放在 `sys_config`] → 本次提供 env 覆盖入口，文档建议生产只用环境变量注入，不把密钥写入数据库。

## Migration Plan

1. 发布后端与 Compose 变更，执行新增迁移。
2. 重启 `iboot-file`、`iboot-system`、`iboot-job`，使存储策略重新选择。
3. 验证：上传文件落到 `iboot-files` 桶；`GET /api/file/download/{id}` 302 到公网 endpoint；宿主机进程与容器内服务都能正常读写。
4. 回滚：设置 `IBOOT_FILE_STORAGE_TYPE=local` 或把 `sys.file.storageType` 改回 `local`，重启上述三个服务。迁移本身不回滚，但配置切换足以恢复旧行为。

## Open Questions

无。
