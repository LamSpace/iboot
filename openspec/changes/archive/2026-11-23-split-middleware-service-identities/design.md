# Design

## Context

See `proposal.md` - Why. 当前实现集中在三处：

- `scripts/mysql-users-init.sh` 只创建共享 `iboot_app` 与 `iboot_exporter`。
- Redis 的 ACL 用户硬编码在 `docker/docker-compose.yml` 的命令行里，只有 `iboot_app` 与 `iboot_exporter`。
- `scripts/nacos-init.sh` 只创建共享 `iboot`；Silo 的文件与 AI 服务都用根账号。
- 应用侧统一读取 `MYSQL_USERNAME`/`MYSQL_PASSWORD`、`REDIS_USERNAME`/`REDIS_PASSWORD`、`NACOS_USERNAME`/`NACOS_PASSWORD`。

宿主机开发（`dev-init.sh`）与容器部署（compose）是两套注入路径，任何拆分都必须在两边同时落地，
否则回归会在另一种模式下悄悄回退到共享账号。

## Goals / Non-Goals

**Goals:**

- 每个服务在 MySQL、Redis、Nacos 上拥有可区分的身份；文件与 AI 在 Silo 上拥有桶级账号。
- 共享账号不再进入业务进程；缺失凭据时启动失败，而不是回退。
- 回归能直接从中间件侧观察身份，并检出回退。

**Non-Goals:**

- 不实现表级或键级的权限隔离：MySQL 仍保持现有 DML 边界，Redis 仍允许业务键空间。
  这是身份拆分，不是授权模型重做。
- 不拆分 Kafka 日志账号：日志链路已按用途（生产/消费/运维）区分，且九服务共享同一日志 topic。
- 不改变 Neo4j 单账号边界：只有 AI 服务使用图数据库。
- 不引入外部密钥管理系统；仍使用根 `.env` 注入。

## Decisions

### 1. 账号命名：`iboot_<service>`，服务名中的 `-` 转 `_`

`iboot-system` → `iboot_system`，`iboot-workflow` → `iboot_workflow`。MySQL、Redis、Nacos 共用同一命名，
便于在日志、`CLIENT LIST`、连接列表和 Nacos 用户列表之间交叉核对。

替代方案是每个中间件独立命名（`mysql_system`、`redis_system`）；放弃，因为排查时需要额外映射，
而身份语义并不因此更强。

### 2. 每服务一个口令，三类中间件共用，允许按组件覆盖

`.env` 以 `MYSQL_SYSTEM_PASSWORD` 为锚点；`REDIS_SYSTEM_PASSWORD`、`NACOS_SYSTEM_PASSWORD`
缺省回退到同一个值。生产环境可以设置独立值覆盖，但本地开发不需要维护 30 个变量。

替代方案是每个组件独立变量；放弃，因为变量数量会淹没 `.env`，且本变更的验收点是身份而不是口令轮换。
风险在“Risks”登记。

### 3. MySQL 保持 DML 边界，不按表授权

所有服务共用 `iboot` schema，表级只读关系已被 §11.12 接受为长期契约。若在这里按表授权，
需要把列级消费清单变成 GRANT 规则，任何新增读取都会变成发布阻塞。本变更只替换“谁在连接”，
不改变“能读写什么”。

替代方案是同时落地表级授权；放弃，因为它会把身份拆分与权限模型重做耦合在一起，回归面过大。

### 4. Nacos 保留 `iboot` 作为运维账号，业务服务不再使用

`iboot` 继续供 `sentinel-init.sh` 等管理脚本使用，避免把管理权限塞进任一业务身份。
每个业务服务使用自己的账号，绑定同一 `iboot_role`（`resource=*`、`action=rw`），
因此权限等价但身份可区分。

替代方案是删除 `iboot` 并让脚本借用某个服务账号；放弃，因为那会让一个业务身份持有管理脚本的凭据。

### 5. Silo 使用桶级策略，根账号只留在初始化路径

`iboot_file` 只能访问 `iboot-files` 桶，`iboot_ai` 只能访问 `iboot-ai` 桶。策略包含
`ListBucket`、`GetBucketLocation`、对象读写删除与分片上传所需动作；`silo-init` 预创建两个桶。
根账号继续用于 `silo-init`、Thanos 与运维工具。

替代方案是继续共用根账号；放弃，因为它无法证明文件与 AI 的边界，也无法在泄露时单独吊销。

### 6. 注入路径：compose 显式覆盖，宿主机进程显式裁剪

- compose：`x-app-env` 把所有每服务口令清空，各服务块只设置自己的三个身份。
  `env_file: ../.env` 会把整份 `.env` 带进容器，因此必须显式清空，不能只依赖“不读取”。
- 宿主机：`dev-init.sh` 在启动每个进程时，为该服务注入自己的三组身份，并用 `env -u`
  移除其它服务的口令变量。

### 7. 回归从中间件侧取证，而不是只看配置

- MySQL：`information_schema.processlist` 按 `USER` 聚合，断言八个服务的账号都出现且没有共享账号。
- Redis：`CLIENT LIST` 解析 `user=`，断言各服务 ACL 用户出现且没有共享用户。
- Nacos：逐账号登录并读一次配置，断言账号可用；共享账号仍可用于脚本。
- Silo：`mcli admin user list` 断言两个服务账号存在，并用各自账号尝试跨桶读取，必须失败。
- 容器/进程：compose 模式检查容器环境，宿主机模式检查 `/proc/<pid>/environ`，断言只拿到自己的凭据。

## Risks / Trade-offs

- [口令在 MySQL/Redis/Nacos 间复用] → 本地开发接受；生产通过 `REDIS_*_PASSWORD`/`NACOS_*_PASSWORD` 覆盖，
  文档明确写出。
- [服务以固定账号运行，账号名泄露不构成认证绕过] → 口令仍是唯一凭据；身份拆分用于审计与吊销，
  不宣称抗凭据泄露。
- [遗漏某个服务的环境裁剪] → 回归同时检查中间件侧身份与进程环境；任一服务回退到共享账号即失败。
- [Silo 桶名或权限动作不全] → 预创建桶并用真实上传/下载/预签名冒烟覆盖；跨桶负向用例兜底。
- [宿主机模式凭据裁剪影响调试] → `.env` 仍保留全部变量；只有 `dev-init.sh` 拉起的进程被裁剪，
  手工 `mvn spring-boot:run` 需自行注入对应服务凭据，文档写明。

## Migration Plan

1. 先在 `.env` 补齐每服务口令与 Silo 服务账号口令；旧 `MYSQL_APP_PASSWORD` 可保留但不再使用。
2. 执行 `mysql-users-init`、`nacos-init`、Silo 初始化，创建新账号；旧共享账号在服务切换完成后删除。
3. 更新 compose 与 `dev-init.sh`，重建九个服务与运维控制台。
4. 运行 `middleware-auth.sh` 与全量回归；确认身份矩阵通过。
5. 回滚：把服务环境变量指回旧共享账号并重建；新账号可保留，不影响旧路径。

## Open Questions

无。
