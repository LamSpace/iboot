# Tasks

## 1. 凭据契约与注入

- [x] 1.1 在 `.env.example` 登记 `MYSQL_APP_PASSWORD`、`MYSQL_EXPORTER_PASSWORD`、`REDIS_PASSWORD`、`SILO_ROOT_USER`、`SILO_ROOT_PASSWORD`、`NACOS_USERNAME`、`NACOS_PASSWORD`、`NACOS_ADMIN_PASSWORD`；验证：变量均为空占位或非生产默认值，文档说明生产必须用密钥管理注入。
- [x] 1.2 在 compose 中为上述变量加 `${VAR:?}` 守卫，并移除可用的中间件默认口令；验证：不设置变量时 `docker compose config` 立即失败并指出变量名，设置后通过。
- [x] 1.3 在 `docs/08-部署指南.md` 的环境变量表补充新变量与用途；验证：表中每一项与 `.env.example` 一致。

## 2. MySQL 最小权限账号

- [x] 2.1 新增 `scripts/mysql-users-init.sh` 与 compose 一次性服务 `mysql-users-init`，幂等创建/更新 `iboot_app` 与 `iboot_exporter`；验证：连续执行两次均成功，账号只创建一次。
- [x] 2.2 让 `db-init` 与业务服务依赖账号初始化完成，宿主机 `dev-init.sh` 在迁移前执行同一脚本；验证：干净与已有数据卷启动均成功。
- [x] 2.3 八个业务服务改用 `iboot_app`，mysqld-exporter 改用 `iboot_exporter`；验证：业务增删改查成功，`CREATE TABLE`/`GRANT` 被拒绝，exporter 写入被拒绝。
- [x] 2.4 更新 `internal-tls.sh` 的 MySQL 检查与 `docs/27`、`docker/README.md` 的账号说明；验证：检查使用受管账号，错误口令用例失败。

## 3. Redis 认证

- [x] 3.1 Redis 以 `--requirepass` 启动，口令来自 `REDIS_PASSWORD`；服务、健康检查、exporter 与回归携带认证；验证：无认证与错误口令被拒绝，正确口令 `PING` 成功。
- [x] 3.2 更新 `docs/27` 与 `docker/README.md` 的 Redis 连接说明；验证：文档命令带认证且可原样执行。

## 4. Silo 受管凭据

- [x] 4.1 Silo 根凭据改为 `.env` 注入，移除 `minioadmin`；`silo-init`、健康检查、Thanos 与业务服务使用同一组受管凭据；验证：`minioadmin` 失败，受管凭据可列举 bucket 并读写对象。
- [x] 4.2 更新 `docs/27`、`docs/08` 与 `docker/README.md` 的 Silo 凭据说明；验证：文档不再出现可用的 `minioadmin` 默认值。

## 5. Nacos 认证

- [x] 5.1 实测 Nacos 3.3 的初始管理员机制、登录接口与创建用户接口，并在 `design.md` 记录最终接口与首次启动处理；验证：在临时容器中完成一次登录与用户创建。
- [x] 5.2 新增 `nacos-init` 一次性任务：轮换管理员口令、创建 `iboot` 服务账号并授予所需角色；验证：重复执行幂等，匿名管理请求失败。
- [x] 5.3 启用 `NACOS_AUTH_ENABLE` 与 `NACOS_AUTH_ADMIN_ENABLE`，八个服务配置 discovery/config 用户名口令；验证：所有服务完成注册与配置拉取，匿名注册被拒绝。
- [x] 5.4 `sentinel-init.sh` 先登录获取令牌再写入；验证：无凭据时失败，有凭据时写入并回读一致。
- [x] 5.5 本地控制台鉴权保持关闭、生产覆盖文件开启；更新 `docs/23` 与 `docker/README.md` 的鉴权边界；验证：本地内嵌可用，生产配置检查要求开启。

## 6. 认证回归门禁

- [x] 6.1 新增 `scripts/regress/middleware-auth.sh`，覆盖 MySQL 越权、Redis 未认证、Silo 默认凭据、Nacos 匿名请求；验证：正向通过、每个负向用例按预期失败。
- [x] 6.2 将脚本接入 `scripts/regress/regress.sh`（内部 TLS 之后、暴露面之前）；验证：全量回归输出包含「中间件认证」步骤，失败时总退出码非零。

## 7. 集成验证

- [x] 7.1 宿主机模式执行完整回归；验证：中间件认证、内部 TLS、暴露面、冒烟与权限矩阵全部通过。
- [x] 7.2 Docker 全量模式执行完整回归；验证：认证、TLS、暴露面与网络矩阵通过，服务全部注册；Docker 备份用例仍为既有预期失败。
- [x] 7.3 回滚演练：恢复上一版 compose/脚本与旧 `.env` 后启动；验证：服务使用旧凭据启动，数据卷与数据不变。

## Workflow follow-up

- 同步规格并归档本变更。
- 提交功能分支、创建 PR、合并后触发一次全栈回归。
