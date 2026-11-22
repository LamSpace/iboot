# Proposal

## Why

B4.3 为 MySQL、Redis、Nacos 与 Silo 加上了账号认证，但九个业务服务仍共用同一组凭据：
`iboot_app`（MySQL/Redis）、`iboot`（Nacos）与 Silo 根账号。中间件侧的审计、慢查询与连接列表只能看到
一个身份，无法区分是哪个服务在读写；服务间的 mTLS 身份到数据面就断开了。现在拆分账号，让每个服务在
中间件层也拥有可区分、可单独吊销的身份。

## What Changes

- **MySQL**：按服务创建 `iboot_system`、`iboot_workflow`、`iboot_file`、`iboot_notify`、`iboot_job`、
  `iboot_monitor`、`iboot_auth`、`iboot_ai`、`iboot_gateway`、`iboot_ops` 账号，权限保持现有边界
  （`iboot` 库 DML，无 DDL）；`iboot_app` 只保留给显式兼容工具，不再注入业务服务。
- **Redis**：按服务创建命名 ACL 用户，权限与现有 `iboot_app` 等价；服务、健康检查与回归脚本改用各自账号。
- **Nacos**：`nacos-init` 为每个服务创建独立账号并绑定 `iboot_role`；发现与配置拉取使用各自账号。
- **Silo**：为 `iboot-file` 与 `iboot-ai` 创建桶级最小权限账号；根账号只用于初始化、桶管理与运维。
- **BREAKING**：业务容器与宿主机进程不再收到共享 MySQL/Redis/Nacos/Silo 凭据；部署方必须提供
  每服务口令变量，缺失时启动失败。
- **回归**：新增每服务身份矩阵，断言各中间件连接使用不同账号；负向断言共享账号与错误凭据被拒绝。
- 同步更新 `.env.example`、compose、`dev-init.sh`、初始化脚本、配置默认值与 TLS/部署文档。

## Capabilities

### New Capabilities

无。本变更只收窄已有能力的行为边界。

### Modified Capabilities

- `middleware-auth`：MySQL、Redis、Silo、Nacos 从“共享账号认证”升级为“每服务独立身份”，
  并把身份矩阵纳入回归。

## Impact

- 配置：根 `.env`、`.env.example`、`docker/docker-compose.yml`、各服务 `application.yml`。
- 脚本：`scripts/mysql-users-init.sh`、`scripts/nacos-init.sh`、`scripts/dev-init.sh`、
  `scripts/regress/middleware-auth.sh`、新增 `scripts/regress/middleware-identity.sh`。
- 运行时：MySQL/Redis/Nacos/Silo 账号集合；九个业务服务与运维控制台。
- 文档：`docs/27`、`docs/23`、`docs/08`、`docker/README.md`、`README.md`。
- CI：`.github/workflows/regress.yml` 的每服务凭据注入。
