# Tasks

## 1. 变量与配置

- [x] 1.1 在 `.env.example`、根 `.env`、CI 登记 26 个 `MYSQL_*`/`REDIS_*`/`NACOS_*` 变量；verification: `grep` 能列出全部变量，本地与 CI 的值互不相同。
- [x] 1.2 更新 compose：`mysql-users-init`、Redis ACL、`nacos-init` 与十个服务块分别使用组件变量；`x-app-env` 清空全部组件变量与旧共享变量；verification: `docker compose config` 渲染后每服务只含自己的三个组件口令。
- [x] 1.3 更新 `dev-init.sh` 的必需变量、按服务注入与 `env -u` 清理；verification: 启动后 `/proc/<pid>/environ` 只含本服务的三个组件口令。

## 2. 初始化脚本

- [x] 2.1 `mysql-users-init.sh` 改用 `MYSQL_<SERVICE>_PASSWORD`；verification: 重复执行创建八个账号成功。
- [x] 2.2 `nacos-init.sh` 改用 `NACOS_<SERVICE>_PASSWORD`；verification: 十个服务账号逐一登录成功。
- [x] 2.3 Redis ACL 命令使用 `REDIS_<SERVICE>_PASSWORD`；verification: 八个服务账号 PING 成功。

## 3. 回归

- [x] 3.1 改造 `scripts/regress/middleware-auth.sh`：按组件读取口令，断言三类口令不同、跨组件认证失败、进程环境只含自己的组件口令；verification: 脚本逐项 PASS。
- [x] 3.2 更新 `scripts/regress/middleware-least-privilege.sh` 的 MySQL 口令来源；verification: 表级/键级最小权限检查通过。
- [x] 3.3 运行全量回归；verification: `scripts/regress/regress.sh` 通过。

## 4. 文档

- [x] 4.1 更新 `.env.example`、CI、`docs/05`、`docs/08`、`docs/09`、`docs/12`、`docs/22`、`docs/23`、`docs/27`、`docker/README.md` 的变量表与示例；verification: 变量名与 compose/脚本一致，仓库内无旧的 `MIDDLEWARE_*` 有效引用。
- [x] 4.2 从 `docs/27` §21.4 移除“三类中间件复用同一口令”的残余风险；§29 未单列该项，无额外条目需要删除。verification: 文档只剩“按组件分别轮换”的操作说明。

## 5. Workflow follow-up

- [x] 归档 OpenSpec 变更并同步 `middleware-auth` 主规格。
- [x] 新建 `feat/split-middleware-credential-domains` 分支，提交、推送、创建 PR；PR CI 通过后合并到 `master`。
- [x] 在 GitHub 触发 `full-regress` 并确认通过（或记录不可抗因素）。
