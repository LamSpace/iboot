# Tasks

## 1. 凭据与账号初始化

- [x] 1.1 在 `.env.example` 登记 `MIDDLEWARE_<SERVICE>_PASSWORD`（十个服务）与 `SILO_FILE_*`、`SILO_JOB_*`、`SILO_AI_*`；verification: `grep` 能列出全部变量且无真实口令。
- [x] 1.2 重写 `scripts/mysql-users-init.sh`：按服务创建 `iboot_<service>` 账号，保持 `iboot` 库 DML 边界，移除共享 `iboot_app`；verification: 在运行中的 MySQL 执行脚本后查询 `mysql.user`，八个账号存在且 `iboot_app` 不存在。
- [x] 1.3 扩展 `scripts/nacos-init.sh`：为十个服务幂等创建账号并绑定 `iboot_role`，保留 `iboot` 运维账号；verification: 脚本重复执行返回成功，逐账号登录成功。
- [x] 1.4 扩展 Silo 初始化：预创建 `thanos`/`iboot-files`/`iboot-ai` 桶，创建文件、备份、AI 三个服务账号并附加桶级策略；verification: `mcli admin user list` 与跨桶访问失败用例。

## 2. 服务端凭据注入

- [x] 2.1 更新 `docker/docker-compose.yml`：`x-app-env` 清空所有每服务口令，八个数据服务与两个控制面服务分别注入自己的 MySQL/Redis/Nacos 身份，文件与 AI 注入各自 Silo 账号；verification: `docker compose config` 渲染后逐个容器只含自身凭据。
- [x] 2.2 更新 `scripts/dev-init.sh`：按服务注入身份并用 `env -u` 移除其它服务口令；补充必需变量校验；verification: 启动后读取 `/proc/<pid>/environ`，只出现本服务凭据。
- [x] 2.3 更新各服务 `application.yml` 的默认账号名与 `.env`/CI 环境；verification: `mvn -B -DskipTests install` 通过，宿主机启动无共享账号回退。

## 3. 回归与文档

- [x] 3.1 重写 `scripts/regress/middleware-auth.sh` 的正向用例为每服务账号，并新增身份矩阵：MySQL processlist、Redis CLIENT LIST、Nacos 逐账号登录、Silo 用户列表；verification: 本地 Docker 全栈运行脚本全部通过。
- [x] 3.2 新增负向用例：共享账号不可用、跨服务口令登录失败、Silo 跨桶读取失败、进程环境不含他人凭据；verification: 人为把某服务改回共享账号时脚本失败。
- [x] 3.3 同步 `docs/27`、`docs/23`、`docs/08`、`docker/README.md` 与 `ROADMAP.md`：登记账号矩阵、注入边界与剩余风险；verification: 文档中的账号名与脚本一致。
- [x] 3.4 运行全量回归；verification: `scripts/regress/regress.sh` 在 Docker 全栈通过（备份用例按既有边界跳过）。

## Workflow follow-up

- 归档 OpenSpec 变更并同步 `middleware-auth` 主规格。
- 触发 GitHub Actions `full-regress` 全栈回归并确认通过。
- 新建 `feat/split-middleware-service-identities` 分支，提交、推送、创建 PR 并合并到 `master`。
