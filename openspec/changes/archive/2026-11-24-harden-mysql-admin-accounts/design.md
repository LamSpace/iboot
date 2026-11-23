# Design

## Context

当前 MySQL 官方镜像通过 `MYSQL_ROOT_HOST: "%"` 同时创建 `root@'localhost'` 与 `root@'%'`；`db-init`、
`mysql-users-init`、`mysql-grants-init` 三个一次性容器都以 root 连接。root 同时承担建表、创建账号与
表级授权三种职责，任一路径泄露都会得到完整数据库管理权限。

本次变更把职责拆成两个专用账号，并把 root 收回到容器内回环引导路径。

## Goals / Non-Goals

**Goals:**

- 编排网络与宿主机端口不再存在可登录的 root。
- 迁移账号只能改 `iboot` 库结构，不能管理账号。
- 账号初始化只能在业务权限范围内创建/修改服务账号与授权，不能取得全局管理能力。
- 第一次初始化和已有数据卷都可幂等地创建这两个账号。
- 回归能直接证明上述边界，而不是只检查配置。

**Non-Goals:**

- 不改变业务服务账号与表级授权矩阵。
- 不引入外部密钥管理；口令仍由根 `.env` 注入。
- 不改变 MySQL 的 TLS 配置与端口绑定。
- 不把 `MYSQL_ROOT_PASSWORD` 从 MySQL 容器移除：本地首次引导与容器内健康检查仍需要它。

## Decisions

### 1. 两个专用账号，而不是一个

- `iboot_migrator`：`ALL PRIVILEGES ON iboot.*`，只用于 Flyway 建表/改表。
- `iboot_provisioner`：`CREATE USER ON *.*`、`RELOAD ON *.*`、`PROCESS`、`REPLICATION CLIENT`、
  `SELECT ON performance_schema.*`，以及 `SELECT, INSERT, UPDATE, DELETE ON iboot.* WITH GRANT OPTION`。
  用于创建/更新服务账号、授予表级权限、创建 exporter；不具备 `iboot` 库的 DDL，也不能取得
  `SUPER`、`FILE`、`SHUTDOWN`、`CREATE ROLE` 等全局能力，或操作 `iboot` 之外的业务库。
  这比把 root 交给初始化容器显著收窄，同时覆盖现有授权矩阵需要的全部权限。

替代方案是一个账号同时承担迁移与账号管理；放弃，因为账号管理凭据泄露仍可改表，迁移凭据泄露也可建账号，
失去了职责分离。

### 2. 首次引导放在 MySQL 容器内，通过 local socket 以 root 执行

新增 `docker/mysql-init-accounts.sh`，挂载到 `/docker-entrypoint-initdb.d/`，仅在空数据卷首次启动时执行：

1. 删除 `root@'%'`，并补一个 `root@'127.0.0.1'`（`skip_name_resolve=ON` 时 TCP 回环不会匹配 `localhost`）；
2. 创建/更新 `iboot_migrator` 与 `iboot_provisioner`；
3. 只授予上述权限；
4. `FLUSH PRIVILEGES`。

脚本同时挂载到 `/scripts/mysql-init-accounts.sh`，已有数据卷可在容器内手工执行一次，完成同样的引导。
脚本使用 `MYSQL_MIGRATOR_PASSWORD`、`MYSQL_PROVISIONER_PASSWORD`，不读取 root 口令以外的凭据。

### 3. 三条初始化链路各用各的身份

| 容器 | 账号 | 原因 |
|---|---|---|
| `db-init` | `iboot_migrator` | 只做 Flyway 建表/改表 |
| `mysql-users-init` | `iboot_provisioner` | 创建/更新服务与 exporter 账号 |
| `mysql-grants-init` | `iboot_provisioner` | 撤销旧授权并按矩阵授予表级权限 |

compose 不再向这三个容器注入 `MYSQL_ROOT_PASSWORD`；`MYSQL_ROOT_PASSWORD` 只保留在 `mysql` 服务本身。

### 4. 凭据守卫与命名

- 新增 `MYSQL_MIGRATOR_PASSWORD`、`MYSQL_PROVISIONER_PASSWORD`，`.env.example` 给出开发默认值。
- `dev-init.sh`、compose 的非空守卫、CI 环境同时补齐；缺失时在拉起容器前失败。
- `mysql-users-init.sh` 与 `mysql-grants-init.sh` 改用 `MYSQL_ADMIN_USER` / `MYSQL_ADMIN_PASSWORD`
  （默认 `iboot_provisioner`），不再接受 root 口令。
- `db-init` 使用现有 `MYSQL_USERNAME` / `MYSQL_PASSWORD`，compose 注入迁移账号。

### 5. 回归从权限边界取证

新增/扩展 `scripts/regress/middleware-auth.sh`：

- 容器内 localhost 以 root 连接成功（首次引导仍可用）；
- 容器非回环地址以 root 连接失败（等价于编排网络）；
- `iboot_migrator` 可 `CREATE TABLE`，但 `CREATE USER` / `GRANT` 失败；
- `iboot_provisioner` 可创建临时账号并授予表权限，但 `GRANT ALL ON *.*`、`FILE`、`SHUTDOWN` 失败；
- 三个初始化容器环境不含 `MYSQL_ROOT_PASSWORD`。

## Risks / Trade-offs

- [`iboot_provisioner` 权限仍较高] → 限定在 `iboot` 库与账号管理，不能取得全局管理能力；口令只在初始化容器中出现。
- [已有数据卷不会自动运行 initdb 脚本] → 文档要求手工执行一次容器内引导；本地按“清空数据卷重建”处理。
- [首次引导脚本依赖容器内回环 root] → 这是镜像初始化模型；root 仍不能从网络登录，风险限定在容器回环。
- [测试或工具直接使用 root] → 更新文档与示例；回归用 localhost root 做取证，不改业务路径。

## Migration Plan

1. 在 `.env` 补齐迁移与管理账号口令。
2. 本地执行 `dev-init.sh --purge` 后重建，验证首次引导；已有环境在 MySQL 容器内手工执行一次引导脚本。
3. 重建 `db-init`、`mysql-users-init`、`mysql-grants-init`，确认迁移与账号初始化成功。
4. 运行 `middleware-auth.sh` 与全量回归。
5. 回滚：恢复旧的初始化容器账号与 `MYSQL_ROOT_HOST`；本次创建的专用账号可保留，不影响旧路径。

## Open Questions

无。
