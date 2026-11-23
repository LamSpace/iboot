# Proposal

## Why

B5.5 已把中间件账号拆成每服务身份，但一个服务在 MySQL、Redis、Nacos 上仍复用同一个
`MIDDLEWARE_<SERVICE>_PASSWORD`。任一组件泄露或需要单独轮换时，另外两个组件会被一起影响，
与“每服务、每组件独立凭据”的零信任目标不一致；`docs/27` §21.4 已将该风险登记为残余边界。

## What Changes

- 新增 26 个凭据变量：
  - `MYSQL_<SERVICE>_PASSWORD`：八个数据服务；
  - `REDIS_<SERVICE>_PASSWORD`：八个数据服务；
  - `NACOS_<SERVICE>_PASSWORD`：十个服务（含 gateway/ops）。
- compose、`dev-init.sh`、`mysql-users-init.sh`、`nacos-init.sh`、Redis ACL 命令、CI 与回归全部改用
  组件专属变量；`MIDDLEWARE_<SERVICE>_PASSWORD` 不再作为有效注入源。
- **BREAKING**：部署方必须提供 26 个新变量；缺失时在拉起容器前失败。
- 回归断言：同一服务的三类口令互不相同；用 MySQL 口令登录 Redis/Nacos 失败，反之亦然；
  服务进程只持有本服务的三个组件口令。
- 同步更新 `.env.example`、CI、README、docs/05、docs/08、docs/23、docs/27 与 `docker/README.md`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `middleware-auth`：新增“中间件组件凭据独立”要求，收窄原“每服务一个口令跨组件复用”的允许范围。

## Impact

- 配置：根 `.env`、`.env.example`、`docker/docker-compose.yml`、CI `regress.yml`。
- 脚本：`scripts/dev-init.sh`、`scripts/mysql-users-init.sh`、`scripts/nacos-init.sh`、
  `scripts/regress/middleware-auth.sh`、`scripts/regress/middleware-least-privilege.sh`。
- 运行时：MySQL/Redis/Nacos 账号口令来源；八个数据服务与 gateway/ops。
- 文档：`README.md`、`docs/05`、`docs/08`、`docs/23`、`docs/27`、`docker/README.md`。
