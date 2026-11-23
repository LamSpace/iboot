# Design

## Context

每服务账号已经拆分，但口令变量仍是 `MIDDLEWARE_<SERVICE>_PASSWORD`，同一个值同时写入
`MYSQL_PASSWORD`、`REDIS_PASSWORD` 与 `NACOS_PASSWORD`。这使三个组件无法独立轮换，任一组件
泄露都会扩大到另外两个组件。

## Goals / Non-Goals

**Goals:**

- MySQL、Redis、Nacos 的口令来源和值完全独立。
- 缺失任一口令时启动前失败，不回退到共享变量。
- 回归能从值、认证结果和进程环境三个角度证明独立性。

**Non-Goals:**

- 不改变每服务账号名与权限矩阵。
- 不引入外部密钥管理；仍由根 `.env` 或 CI 环境注入。
- 不改变 TLS 与客户端证书配置。
- 不为 Silo、Kafka、RocketMQ 引入新的口令域；它们已按用途拆分。

## Decisions

### 1. 变量矩阵

| 组件 | 数据服务（8） | gateway / ops |
|---|---|---|
| MySQL | `MYSQL_<SERVICE>_PASSWORD` | 无 |
| Redis | `REDIS_<SERVICE>_PASSWORD` | 无 |
| Nacos | `NACOS_<SERVICE>_PASSWORD` | `NACOS_<SERVICE>_PASSWORD` |

服务名中的 `-` 转 `_`，与账号名保持一致。

### 2. 不再保留共享变量的回退

`MIDDLEWARE_<SERVICE>_PASSWORD` 从 `.env.example`、CI 与启动路径移除；compose 的 `x-app-env`
继续显式清空它，防止旧 `.env` 把它带进容器。缺失新变量时 compose 的 `${VAR:?}` 守卫直接失败。

替代方案是“新变量缺省回退到 `MIDDLEWARE_*`”；放弃，因为回退会让“独立”只在显式覆盖时成立，
回归无法区分真实独立与偶然相同。

### 3. 注入路径拆分为三条

- compose：`mysql-users-init` 只接收 `MYSQL_*`；Redis 命令只接收 `REDIS_*`；`nacos-init` 只接收 `NACOS_*`；
  各业务服务只注入自己的三个组件变量。
- 宿主机：`dev-init.sh` 按服务分别注入 `MYSQL_PASSWORD=${MYSQL_<SERVICE>_PASSWORD}`、
  `REDIS_PASSWORD=${REDIS_<SERVICE>_PASSWORD}`、`NACOS_PASSWORD=${NACOS_<SERVICE>_PASSWORD}`，
  并用 `env -u` 移除全部其它组件变量。
- 回归：从 `.env` 读取三个变量，分别执行认证。

### 4. 本地与 CI 使用不同值

`.env.example` 只登记变量名；根 `.env` 与 CI 为每个服务、每个组件设置不同值。这样本地回归即可
发现“误用另一个组件口令仍能通过”的回归。

### 5. 回归设计

- 值断言：同一服务的 `MYSQL_*`、`REDIS_*`、`NACOS_*` 两两不相等。
- 认证断言：MySQL 口令登录 Redis/Nacos 失败；Redis 口令登录 MySQL 失败。
- 进程断言：数据服务的环境变量中三个口令分别等于对应变量；gateway/ops 只有 Nacos 口令。
- 缺失断言：compose 配置阶段缺少任一变量即失败。

## Risks / Trade-offs

- [变量从 10 个增加到 26 个] → `.env.example` 与文档集中登记；本地与 CI 使用可读的统一前缀。
- [旧 `.env` 仍含 `MIDDLEWARE_*`] → 不再读取，compose 显式清空；迁移文档要求删除旧变量。
- [轮换需要同时更新服务与初始化脚本] → 每服务三个变量独立，按组件轮换时只改一个变量并重建对应组件连接。
- [CI 环境变量过长] → 使用清晰的命名与分组注释；不引入脚本生成，保持 CI 可审计。

## Migration Plan

1. 在根 `.env` 与 CI 增加 26 个变量；本地使用不同开发值。
2. 更新 compose、初始化脚本与 `dev-init.sh`，重建中间件与后端。
3. 运行账号初始化与表级授权，再运行全量回归。
4. 删除旧 `MIDDLEWARE_*` 变量；确认缺失时启动失败。
5. 回滚：恢复 compose 与脚本中的共享变量映射，并重新注入旧变量。

## Open Questions

无。
