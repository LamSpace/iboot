# Proposal

## Why

MySQL 仍保留 `root@'%'`，且 `db-init`、`mysql-users-init`、`mysql-grants-init` 都以 root 连接。任何能访问
编排网络的进程或泄露的初始化凭据都直接获得完整数据库管理权限，与已经完成的工作负载身份和表级最小权限不匹配。

## What Changes

- 首次初始化时创建两个专用账号：
  - `iboot_migrator`：仅 `iboot` 库的 DDL/DML，用于 Flyway 迁移；
  - `iboot_provisioner`：账号与授权管理所需的最小动态权限，用于每服务账号和表级授权初始化。
- 移除 `MYSQL_ROOT_HOST="%"` 并删除 `root@'%'`；root 只在容器内 `localhost` 可用，不再从编排网络或宿主机端口登录。
- `db-init` 改用 `iboot_migrator`；`mysql-users-init` 与 `mysql-grants-init` 改用 `iboot_provisioner`。
- 新增 `MYSQL_MIGRATOR_PASSWORD`、`MYSQL_PROVISIONER_PASSWORD` 环境变量与缺失启动失败守卫。
- **BREAKING**：通过 TCP 从非 localhost 使用 root 的脚本或运维流程必须改用迁移/管理账号。
- 回归新增：远程 root 被拒绝、迁移账号可 DDL 但不能管理账号、管理账号可建账号但不能使用 root 权限、本地 root 仍可完成首次引导。
- 同步更新 `.env.example`、CI、compose、初始化脚本、README 与 docs/。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `middleware-auth`：MySQL 管理面从“root 可远程使用”收窄为“root 仅本地引导，迁移与账号管理各有专用最小权限账号”。

## Impact

- 配置：`.env`、`.env.example`、`docker/docker-compose.yml`、CI `regress.yml`。
- 脚本：新增 `docker/mysql-init-accounts.sh`；修改 `scripts/mysql-users-init.sh`、
  `scripts/mysql-grants-init.sh`、`scripts/dev-init.sh`、`scripts/regress/*`。
- 运行时：MySQL 账号集合；`db-init`、`mysql-users-init`、`mysql-grants-init` 一次性容器。
- 文档：`README.md`、`docs/05`、`docs/08`、`docs/23`、`docs/27`、`docker/README.md`。
