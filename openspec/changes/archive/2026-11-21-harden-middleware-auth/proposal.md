# Proposal

## Why

B4.2 已把中间件收敛到回环地址并完成 TLS，但访问控制仍停留在网络边界：业务服务用 MySQL `root/root`、Redis 无口令、Silo 使用 `minioadmin`、Nacos 客户端与管理端鉴权关闭。任何能在本机或编排网络内发起连接的一方，都能直接读写数据、修改注册配置。B4.3 为这些链路补上账号认证与最小权限，使「能连上」不再等于「能操作」。

## What Changes

- **MySQL 最小权限**：新增 `iboot_app`（业务库 DML）与 `iboot_exporter`（监控只读）账号；业务服务与 exporter 不再使用 root。root 仅由一次性初始化/迁移使用，密码从 `.env` 注入。
- **Redis 认证**：启用 `requirepass`，密码从 `.env` 注入；服务、健康检查、exporter 与回归脚本全部带认证；未认证连接被拒绝。
- **Silo 凭据**：根凭据改为 `.env` 注入，移除 `minioadmin/minioadmin` 默认值；服务、`silo-init`、Thanos 与健康检查使用同一组凭据。
- **Nacos 认证**：启用客户端与服务端鉴权（`NACOS_AUTH_ENABLE`）与管理端鉴权（`NACOS_AUTH_ADMIN_ENABLE`）；使用一次性初始化任务创建/轮换专用服务账号，所有服务与 `sentinel-init.sh` 使用该账号。控制台鉴权在本地保持关闭以支持门户内嵌，非本地必须开启；该边界写入文档。
- **凭据注入**：新增变量在 `.env.example` 中登记；compose 使用 `:?` 守卫，缺失时在启动前失败，不提供可用的默认口令。
- **负向门禁**：新增/扩展回归，覆盖错误或缺失凭据时 MySQL、Redis、Silo、Nacos 必须拒绝。
- **文档**：更新 TLS/零信任指南、部署指南、微服务底座指南与 `docker/README.md` 的账号矩阵、初始化顺序与排障。

**BREAKING**：现有 `.env` 必须补齐新凭据变量；依赖 `root/root`、无口令 Redis、`minioadmin` 或匿名 Nacos 的脚本/外部工具将失效。

## Capabilities

### New Capabilities

- `middleware-auth`：定义 MySQL、Redis、Silo 与 Nacos 的账号认证、最小权限、凭据注入与负向门禁。

### Modified Capabilities

- `container-deployment`：新增账号初始化顺序、凭据注入与健康检查认证要求。
- `service-discovery`：Nacos 注册与配置拉取必须使用账号认证，匿名请求被拒绝。

## Impact

- `docker/docker-compose.yml`、`docker/docker-compose.prod.yml`：账号环境变量、一次性初始化服务、健康检查。
- `docker/init.sql`、`iboot-db-init`、`scripts/dev-init.sh`、`scripts/sentinel-init.sh`、`scripts/regress/*`。
- `iboot-*/src/main/resources/application.yml`：MySQL/Redis/Silo/Nacos 客户端凭据。
- `.env.example`、`docs/08`、`docs/23`、`docs/27`、`docker/README.md`。
- 不包含：ES 应用账号最小权限、Sentinel 控制台登录、Kafka SASL、RocketMQ ACL、Seata 认证与每服务工作负载身份（B5）。
