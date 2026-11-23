# Tasks

## 1. 凭据与首次引导

- [x] 1.1 在 `.env.example`、根 `.env`、CI 与 `dev-init.sh` 登记 `MYSQL_MIGRATOR_PASSWORD`、`MYSQL_PROVISIONER_PASSWORD`；verification: `grep` 能列出变量，缺失时 `dev-init.sh` 在启动容器前失败。
- [x] 1.2 新增 `docker/mysql-init-accounts.sh`：以 local socket root 删除 `root@'%'`，创建/更新迁移与管理账号并授予约定权限；verification: 空数据卷启动后 `mysql.user` 只有 `root@localhost`，两个账号存在且权限与设计一致。
- [x] 1.3 compose 把 `MYSQL_ROOT_HOST` 收为 `localhost`，并把引导脚本同时挂载到 initdb 与 `/scripts`；verification: `docker compose config` 渲染 `localhost`，`mysql.user` 不含 `root@'%'`，容器内可手工重跑引导脚本。

## 2. 初始化链路切换

- [x] 2.1 `db-init` 改用 `iboot_migrator`；verification: 空数据卷上 Flyway 迁移成功，进程环境不含 root 口令。
- [x] 2.2 `mysql-users-init.sh` 与 `mysql-grants-init.sh` 改用 `MYSQL_ADMIN_USER`/`MYSQL_ADMIN_PASSWORD`（默认 `iboot_provisioner`）；verification: 可重复执行，八服务账号与 exporter 账号、表级授权均正确。
- [x] 2.3 三个一次性容器不再收到 `MYSQL_ROOT_PASSWORD`；verification: `docker inspect` 三个容器的环境不含 root 口令。

## 3. 回归

- [x] 3.1 扩展 `scripts/regress/middleware-auth.sh`：localhost root 成功、非回环 root 失败、迁移账号 DDL 成功但账号管理失败、管理账号可建账号但全局管理失败；verification: 脚本逐项 PASS。
- [x] 3.2 断言三个初始化容器环境不含 root 口令；verification: 人为注入时脚本失败。
- [x] 3.3 运行 `middleware-auth.sh` 与 `scripts/regress/regress.sh`；verification: 全量回归通过。

## 4. 文档

- [x] 4.1 更新 `README.md`、`docs/05`、`docs/08`、`docs/23`、`docs/27`、`docker/README.md`：账号矩阵、首次引导、既有数据卷迁移、root 本地边界；verification: 文档中的账号名/变量与 compose、脚本一致。
- [x] 4.2 更新 `docs/27` §13.1/§13.2/§13.5、`docs/23` 与 `docker/README.md`：登记迁移/管理账号矩阵、首次引导与 root 回环边界；原“root 可从编排网络连接”的边界已移除。verification: 文档中的账号名与 compose、脚本一致。

## 5. Workflow follow-up

- [x] 归档 OpenSpec 变更并同步 `middleware-auth` 主规格。
- [ ] 新建 `feat/harden-mysql-admin-accounts` 分支，提交、推送、创建 PR；PR CI 通过后合并到 `master`。
- [ ] 在 GitHub 触发 `full-regress` 并确认通过（或记录不可抗因素）。
