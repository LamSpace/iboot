# Design

## Context

B4.2 已把中间件端口收敛到回环地址并启用 TLS，但认证仍是空白：MySQL 业务连接使用 `root/root`，Redis 无口令，Silo 使用 `minioadmin`，Nacos 的客户端与管理端鉴权关闭。现有编排、宿主机开发脚本与回归都依赖这些默认值。行为契约见 `specs/`，动机见 `proposal.md`。

约束：

- 宿主机开发（`dev-init.sh`）与 Docker 全量部署必须使用同一套账号与凭据。
- 一次性初始化必须能在已有数据卷上重复执行，不能重置数据或已有账号。
- 认证不能破坏现有 TLS、服务发现、日志与可观测链路。

## Goals / Non-Goals

**Goals:**

- 业务服务与 exporter 不再使用 MySQL root；Redis 必须认证；Silo 无默认凭据；Nacos 注册/配置与管理接口必须认证。
- 凭据只从 `.env` 或密钥管理注入，缺失时启动前失败。
- 认证负向用例进入全量回归。

**Non-Goals:**

- 不引入每服务独立身份或客户端证书（B5）；Redis 使用单一受管口令而非每服务 ACL 用户。
- 不处理 ES 应用账号最小权限、Sentinel 控制台登录、Kafka SASL、RocketMQ ACL 与 Seata 认证。
- 不改变 B4.2 的端口绑定与网络分段。

## Decisions

### 1. 账号矩阵

| 组件 | 账号 | 用途 | 权限 |
|---|---|---|---|
| MySQL | `root` | 仅账号初始化与迁移 | 管理权限；不注入业务服务 |
| MySQL | `iboot_app` | 八个业务服务 | `iboot` 库 `SELECT/INSERT/UPDATE/DELETE` |
| MySQL | `iboot_exporter` | mysqld-exporter | `PROCESS`、`REPLICATION CLIENT`、`performance_schema` 只读 |
| Redis | 默认用户 | 全部服务 | 正确口令后可用；口令由 `REDIS_PASSWORD` 注入 |
| Silo | 根账号 | 服务、初始化、Thanos | 由 `SILO_ROOT_USER/PASSWORD` 注入；移除默认值 |
| Nacos | `iboot` | 全部服务与管理脚本 | 注册、发现、配置读写；控制台账号另由本地例外处理 |

不采用每服务账号：当前没有工作负载身份，给每个服务发不同口令只会增加分发成本，仍无法证明调用方身份；每服务身份留给 B5。

### 2. MySQL 用户初始化：独立一次性任务

新增 `mysql-users-init` 一次性服务，执行 `scripts/mysql-users-init.sh`：

- 以 root 连接 MySQL，使用 `CREATE USER IF NOT EXISTS` + `ALTER USER` 同步口令，再 `GRANT`。
- 业务账号只授予 `iboot.*` 的 DML；exporter 账号只授予监控所需权限。
- 任务幂等：已有账号只更新口令与授权，不删除数据。
- Docker 全量：`db-init` 依赖该任务成功退出；宿主机开发：`dev-init.sh` 在 `db-init` 前用 `docker compose run --rm mysql-users-init` 执行同一脚本。

不采用在 `db-init` Java 中拼 DDL：账号初始化是编排职责，放在独立一次性任务中更容易在两种模式下复用，也避免把凭据带进迁移代码。

### 3. Redis：`requirepass` + `REDISCLI_AUTH`

Redis 以 `--requirepass` 启动，口令来自 `.env`。健康检查、exporter 与回归使用 `REDISCLI_AUTH` 环境变量，避免口令出现在命令行。

业务服务已有 `spring.data.redis.password` 占位，只需在 compose 与开发环境注入 `REDIS_PASSWORD`。不启用 ACL 用户：当前所有服务共享同一身份，ACL 的收益要等 B5 的每服务身份。

### 4. Silo：根凭据受管，默认值移除

`MINIO_ROOT_USER/PASSWORD` 改为 `.env` 必填变量；`silo-init` 与健康检查使用同一组变量。业务服务通过 `IBOOT_*_SILO_ACCESS_KEY/SECRET_KEY` 注入同一组凭据；不再在 `application.yml` 保留 `minioadmin` 默认值。Thanos 的 bucket 配置改从环境读取。

### 5. Nacos：客户端/管理端鉴权，控制台本地例外

- 启用 `NACOS_AUTH_ENABLE=true` 与 `NACOS_AUTH_ADMIN_ENABLE=true`。
- 新增 `nacos-init` 一次性任务：
  1. 使用镜像初始管理员完成首次登录；
  2. 修改管理员口令为 `.env` 注入值；
  3. 创建 `iboot` 服务账号并授予所需角色；
  4. 后续启动只做幂等校验。
- 所有服务在 `spring.cloud.nacos.discovery` 与 `spring.cloud.nacos.config` 下配置 `username/password`。
- `sentinel-init.sh` 先登录获取 `accessToken`，再调用 v3 admin 写入；无令牌时失败。
- `NACOS_AUTH_CONSOLE_ENABLE` 在本地 compose 保持 `false` 以支持门户内嵌；`docker-compose.prod.yml` 必须设为 `true`。该例外写入文档，不能宣称本地控制台已鉴权。

实测确认的 Nacos 3.3 初始化接口（已在临时容器与本地栈验证）：

- `POST /nacos/v3/auth/user/admin`，参数 `password`：首次设置内置 `nacos` 管理员口令；
  已初始化时返回 `code=409`，脚本继续登录。
- `POST /nacos/v3/auth/user/login`，参数 `username`、`password`：返回 `accessToken`。
- `POST /nacos/v3/auth/user`，参数 `username`、`password`：创建服务账号；
  已存在时返回 500，改用 `PUT /nacos/v3/auth/user` + `username`、`newPassword` 同步口令。
- `POST /nacos/v3/auth/role`，参数 `role`、`username`：创建并绑定角色；已存在时忽略。
- `POST /nacos/v3/auth/permission`，参数 `role`、`resource=*`、`action=rw`：授权；幂等。
- Nacos 客户端的 HTTP 登录必须使用 `https://` 方案，`serverAddr` 设为
  `https://<host>:8848`；gRPC 仍按 8848→9848 偏移并复用现有 TLS 系统属性。

首次启动窗口由 `nacos-init` 在服务注册前关闭：镜像不提供环境变量直接设置初始口令，
因此管理员初始化与口令轮换是同一个一次性任务；非本地部署仍必须通过密钥管理注入口令。

### 6. 凭据注入与缺失失败

`.env.example` 新增：

```text
MYSQL_APP_PASSWORD=
MYSQL_EXPORTER_PASSWORD=
REDIS_PASSWORD=
SILO_ROOT_USER=
SILO_ROOT_PASSWORD=
NACOS_USERNAME=iboot
NACOS_PASSWORD=
NACOS_ADMIN_PASSWORD=
```

compose 对这些变量使用 `${VAR:?}`；应用配置不再保留可用的默认口令。宿主机开发由 `dev-init.sh` 在启动前校验同样变量，缺失直接失败。

### 7. 回归

新增 `scripts/regress/middleware-auth.sh`：

- MySQL：业务账号可 DML、不可 DDL；exporter 不可写；错误口令失败。
- Redis：无认证与错误口令失败；正确口令 `PING` 成功。
- Silo：`minioadmin` 失败；受管凭据可访问健康端点。
- Nacos：匿名注册/配置写入失败；受管账号注册成功；管理脚本无令牌失败。

脚本接入 `regress.sh`，放在内部 TLS 之后、暴露面之前。CI 的全栈任务使用同一套 `.env` 注入值。

## Risks / Trade-offs

- **首次启动默认管理员窗口** → `nacos-init` 在服务注册前完成轮换；文档登记该窗口，非本地部署必须使用密钥管理注入。
- **已有数据卷上的账号** → 初始化脚本使用 `CREATE USER IF NOT EXISTS` + `ALTER USER`，只同步口令与授权，不重建数据。
- **多个脚本硬编码默认口令** → 先全局搜索并逐处替换；回归负向用例专门检查默认值。
- **根账号仍可远程连接** → root 只用于初始化与迁移，不注入业务服务；后续可改为专用迁移账号并关闭远程 root，作为独立变更。
- **Silo/Thanos 凭据变更导致历史数据不可读** → 根凭据只影响认证，不影响卷内对象；启动后先验证 bucket 列举与对象读取。
- **回滚** → 恢复上一版 compose 与 `.env` 后重启；不删除数据卷。

## Migration Plan

1. 在 `.env` 补齐新变量，`.env.example` 登记占位。
2. 先落地 MySQL/Redis/Silo，验证宿主机与 Docker 两种模式。
3. 再落地 Nacos 认证与账号初始化，验证服务注册与 `sentinel-init`。
4. 新增认证回归并接入全量回归。
5. 更新文档后做完整验证与回滚演练。

## Open Questions

无阻塞性未知；Nacos 初始管理员接口在实施首任务中实测，若与预期不同，只调整初始化脚本，不改变账号矩阵与客户端方案。
