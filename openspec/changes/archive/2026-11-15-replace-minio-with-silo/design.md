# Design

## Context

当前编排固定 `minio/minio:RELEASE.2025-09-07T16-13-09Z`，通过 aityp/SWR 镜像代理重打标签；该镜像已停止维护。应用侧全部使用 `io.minio:minio` Java 客户端，实际调用 `bucketExists`、`makeBucket`、`statObject`、`removeObject`、`listBuckets`、`listObjects`、`putObject`、`getObject` 与预签名 URL，属于标准 S3 操作。文件存储类型与端点、凭证存放在 `sys_config`（`sys.file.storageType`、`sys.file.minio.*`、`sys.minio.*`），监控接口为 `/api/monitor/minio`，权限码为 `minio:query`。

`pgsty/silo` 是 MinIO 的同源社区 fork，保留 S3 API、`MINIO_*` 服务端变量、`/minio/*` 路由与 `.minio.sys` 磁盘格式。它不提供独立的 Java SDK，因此应用继续使用 MinIO 客户端。当前 compose 以 `docker compose -f docker/docker-compose.yml` 启动，项目名为 `docker`，对象存储卷物理名为 `docker_minio_data`。

## Goals / Non-Goals

**Goals:**

- 把对象存储运行期切换到 Docker Hub 上持续维护的 `pgsty/silo`，并保持 S3/MinIO API、桶、对象键与磁盘格式不变。
- 完成项目范围内的 MinIO → Silo 命名统一：编排、环境变量、`sys_config`、Java 类型、监控接口/权限、前端视图、CloudEvent 与文档。
- 用一次性迁移保留既有 `sys_config`、菜单/权限、备份记录与 Docker 卷数据。
- 用接口基线登记与回归脚本证明这次破坏性重命名是受控的，而不是静默漂移。

**Non-Goals:**

- 不更换存储协议或对象键布局，不迁移桶内数据，不改桶名。
- 不更换 `io.minio:minio` 客户端库，不引入 AWS SDK。
- 不修改开发凭证（`minioadmin`/`minioadmin`）与 Silo 服务端 `MINIO_ROOT_*` 变量；Silo 官方仅承诺 `MINIO_*` 契约。
- 不重命名历史 OpenSpec 归档、`docs/spike` 或已生成的 `iboot-portal/dist` 产物。

## Decisions

### 1. 选择 Silo 而不是 RustFS / SeaweedFS / Garage

Silo 与现有代码、配置和磁盘格式同源，切换成本最低；RustFS 虽是 Apache-2.0 且活跃，但健康检查是 `/health`、环境变量是 `RUSTFS_*`，监控与配置需要额外改动；SeaweedFS 需要 master/volume/filer/s3 多进程编排；Garage 是 S3 子集且缺少 IAM/lifecycle/versioning。选择 Silo 的主要代价是它仍是社区 fork、许可证仍为 AGPL-3.0，与当前 MinIO 一致，没有许可证回退。

### 2. 固定标准镜像标签，直接拉取 Docker Hub

使用 `pgsty/silo:RELEASE.2026-11-12T00-00-00Z`（当前最新 `RELEASE.*`），不使用 `latest`。选择标准镜像而不是 `-distroless`，因为初始化容器需要 `/bin/sh` 与 `mcli`。拉取脚本删除 aityp/SWR 的 `pull`/`tag`/`rmi` 逻辑，直接 `docker pull docker.io/pgsty/silo:<tag>`。失败语义保持：标签缺失即非零退出，不回退到其它来源。

### 3. 继续使用 MinIO Java 客户端，只重命名应用类型

Silo 没有独立 SDK，且其协议与 MinIO 客户端兼容。保留 `io.minio:minio` 依赖，把 `MinioProperties`、`MinioFileStorageStrategy`、`MinioKnowledgeStorage`、`MinioAdminService`、`MinioMonitorController` 改为 `Silo*`。替代方案是改用 AWS S3 SDK，但会引入签名、分页与预签名差异，远超本次目标。

### 4. 全量重命名，但保留数据面契约

重命名范围：

- 编排：服务 `minio` → `silo`，容器 `iboot-minio` → `iboot-silo`，初始化容器 `minio-init` → `silo-init`，`thanos` 的 endpoint `minio:9000` → `silo:9000`。
- 配置：`IBOOT_AI_MINIO_*` → `IBOOT_AI_SILO_*`；`sys.file.minio.*` → `sys.file.silo.*`；`sys.minio.*` → `sys.silo.*`；`sys.file.storageType` 的 `minio` → `silo`。
- Java：上述类型与字段/方法（`isMinioEnabled` → `isSiloEnabled` 等）。
- 监控：`/api/monitor/minio` → `/api/monitor/silo`，`minioEnabled` → `siloEnabled`，`minio:query` → `silo:query`，菜单权限 `monitor:minio` → `monitor:silo`，菜单路径 `/monitor/minio-monitor` → `/monitor/silo-monitor`，CloudEvent `com.iboot.monitor.minio.info` → `com.iboot.monitor.silo.info`。
- 前端：`MinioMonitorView` → `SiloMonitorView`，路由 `minio-monitor` → `silo-monitor`，API 客户端与 `monitor.minio.*` → `monitor.silo.*`。

保留的数据面契约：

- 桶名 `iboot-files`、`iboot-ai`、`thanos` 不变。
- 开发凭证 `minioadmin`/`minioadmin` 与 `MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD` 不变；后者是 Silo 官方文档承诺的服务端兼容变量。
- `/minio/health/live` 继续作为健康检查路径；Silo 保留该路由。监控代码只改类名/方法名，不改 URL。
- `io.minio` 包名不变。

### 5. 卷逻辑名改名，物理名保留

compose 卷改为：

```yaml
volumes:
  silo_data:
    name: ${IBOOT_SILO_DATA_VOLUME:-docker_minio_data}
```

这样逻辑名与代码一致，同时继续挂载既有 `docker_minio_data`，不产生新的空卷。若部署使用不同 Compose 项目名，可通过 `IBOOT_SILO_DATA_VOLUME` 覆盖。替代方案是新建 `docker_silo_data` 并复制数据，但那需要停机与额外磁盘，且当前机器上没有现存卷，收益不足。

### 6. 数据库迁移使用新增 Flyway V7，不改 V1

V1 是已应用的基线，修改会触发 Flyway 校验和失败。新增 `V7__rename_minio_to_silo.sql`，按 `config_key`/`id` 更新，保证可重复执行且不依赖行 ID 之外的隐式状态：

- `sys_config`：`sys.file.minio.*` → `sys.file.silo.*`；`sys.minio.*` → `sys.silo.*`；`sys.file.storageType` 值 `minio` → `silo`；配置名称/说明中的 `MinIO` → `Silo`。
- `sys_menu`：菜单 706 的名称、路径、组件与 `monitor:silo`；菜单 7061 的名称与 `silo:query`。
- `db_backup_record`：历史 `storage_type='minio'` → `'silo'`。

V7 在 V3 修复种子编码之后执行，中文名称可直接用 UTF-8 写入；迁移后需抽查 `sys_config` 与 `sys_menu` 文本是否正常。回滚 SQL 为反向 `REPLACE` 与按 ID 恢复，保留在迁移说明中。

### 7. 健康检查优先使用 Silo 官方命令，实施时验证严格性

Silo 文档推荐 `silo healthcheck ready`，但明确提示它可能在存储或 IAM 完成加载前返回 0。实施时先启动容器验证该命令，再尝试 `mcli ready local`（取决于镜像是否预置 `local` 别名）。最终选择能反映 S3 可用性的最严格命令；若两者都不够严格，则使用 `silo healthcheck ready` 并依赖应用的启动重试（`restart: unless-stopped`）兜底。初始化容器统一使用 `mcli`，不使用旧镜像的 `/usr/bin/mc`。

### 8. 破坏性接口变更按既有契约登记

`/api/monitor/minio` → `/api/monitor/silo`、`minioEnabled` → `siloEnabled` 是外部可见变更。按 `api-contract-regression` 的允许差异机制：

- 更新 `scripts/regress/routes.txt` 的路径映射。
- 更新 `scripts/regress/migrations.txt` 的 `/api/monitor/minio` 登记为 `/api/monitor/silo`。
- 重新导出 `scripts/regress/golden/monitor.json`，并在变更说明中登记旧路径、新路径与理由。
- 同步更新 `scripts/regress/golden/file.json` 中关于预签名 URL 的 MinIO 描述。

不存在其它服务迁移，因此不需要新增 `migrations.txt` 行；这次是同一服务内的重命名。

## Risks / Trade-offs

- [Silo 是社区 fork，长期维护不确定] → 固定 `RELEASE.*` 标签，保留 MinIO 兼容磁盘格式；若需再次迁移，可继续复用同一数据卷。
- [`silo healthcheck ready` 可能过早返回成功] → 实施时对比 `mcli ready local`；不达标时依赖应用启动重试，并在 tasks 中记录验证结果。
- [卷物理名与 Compose 项目名绑定] → 用 `IBOOT_SILO_DATA_VOLUME` 覆盖；部署前用 `docker volume ls` 确认实际卷名。
- [回滚镜像可能不可得] → `minio/minio` 已从 Docker Hub 删除；保留本机已有的 MinIO 镜像作为回滚路径，或在变更前导出镜像。
- [数据库迁移不可逆] → 迁移前备份 `sys_config`、`sys_menu`、`db_backup_record`；V7 只做键/值/文本替换，反向 SQL 简单可验证。
- [全量重命名面广，易漏改] → 完成后全仓搜索 `minio|MinIO|MINIO`（排除归档、产物与历史 spike），逐项确认。
- [旧值 `minio` 被拒绝会导致未迁移实例启动失败] → 这是有意的失败关闭；V7 随部署自动执行，错误信息会指出非法值。
- [本机 Docker Hub 拉取不稳定] → 拉取脚本保持固定标签与明确失败；必要时预先拉取或使用本机缓存，不改变来源。

## Migration Plan

1. 备份 `iboot` 库中的 `sys_config`、`sys_menu`、`db_backup_record`，以及对象存储卷；如需要回滚，先在本机保留 MinIO 镜像。
2. 合并代码与 `V7` 迁移，更新根目录 `.env` 中的 `IBOOT_AI_SILO_*`（若存在）。
3. 启动 `db-init`，确认 V7 执行成功且 `sys_config`、菜单、备份记录已改名。
4. 以既有卷 `docker_minio_data` 启动 `silo` 服务；确认桶、对象、预签名 URL、控制台与 `/minio/health/live` 正常。
5. 执行存储契约测试、监控接口回归、备份触发与恢复演练，确认 `storageType=silo`。
6. 回滚：停止栈，恢复数据库备份，把卷映射与镜像切回 MinIO；若 MinIO 镜像已不存在，只能依赖本机保留的镜像或前滚修复。

## Open Questions

- `mcli` 在标准镜像中的确切路径与 `mcli ready local` 是否可用，需在实施时用实际容器验证；这不改变方案，只决定最终健康检查命令。
- Silo 是否接受 `SILO_ROOT_USER` 等 `SILO_*` 服务端变量，官方文档未承诺；默认继续使用 `MINIO_*`，若实测支持可在后续变更中收紧。
