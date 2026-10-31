# Proposal

## Why

登录是安全边界上变更最频繁、最值得隔离的一段代码，而它目前仍是 `iboot-admin` 的一部分：管理端一旦因为备份、任务调度或某个业务改动需要重启，所有新登录都会一起中断。

`iboot-file` 已经验证了「绞杀者 + 契约不变」的拆分方法，现在轮到依赖最少、边界最清楚的下一个单元。这一步只做**物理拆分**，不动协议、不动前端、不动契约。

## What Changes

- 新增 `iboot-auth` 模块（独立 JVM，端口 8083），承载 `/api/auth/login`、`/api/auth/logout`、`/api/auth/register`。
- 登录、登出、注册的实现从 `iboot-admin` 搬到新服务：`AuthController`、`AuthApplicationService`、`LoginLogApplicationService.recordLoginLog`、三个 DTO、登录指标。
- **`/api/auth/scope` 不搬**：它返回的是数据权限范围（部门 / 角色 / 用户），属于系统域，随将来的 `iboot-system` 迁移；管理端的数据权限拦截器本来就在本地解析同一份结果。
- 网关新增三条精确路由；`scripts/regress/migrations.txt` 登记 `/api/auth/login`、`/api/auth/logout`、`/api/auth/register` 三条迁移。
- 过渡期 `iboot-auth` 直连 `iboot_admin` 库读写（会话仍在共享 Redis），不新建 schema。
- 管理端删除旧实现与随之失效的白名单配置、指标计数器。
- **不变**：登录响应结构、令牌格式、Redis 会话键、前端登录页与路由守卫、权限与数据范围语义。

## Capabilities

### New Capabilities

- `auth-service`: 认证作为独立可部署单元的契约——登录/登出/注册的对外行为、共享会话的唯一事实来源、认证服务不可用时的降级语义。

### Modified Capabilities

（无。对外接口的路径、参数与响应结构均未改变；路径守恒由既有 `api-contract-regression` 能力覆盖。）

## Impact

- **新增模块**：`iboot-auth`（pom、启动类、CloudEvents 信封、API 版本配置、鉴权白名单、Nacos 注册与追踪）。
- **管理端**：删除 `AuthController` 的登录/登出/注册方法、`AuthApplicationService` 对应实现、`LoginLogApplicationService.recordLoginLog`、登录与登出指标计数器、三条白名单配置。
- **网关**：`application-local.yml` 与 `application-nacos.yml` 各加一条 auth 路由。
- **脚本**：`scripts/dev-init.sh` 增加服务与启动顺序；`scripts/regress/{routes.txt,migrations.txt,golden/*}` 更新。
- **文档**：`ROADMAP.md`、`doc/03`、`doc/05`、`doc/06`、`doc/09`、`doc/10`、`doc/13`、`doc/22`、`doc/23`。
- **已知债**：过渡期 `iboot-auth` 写 `iboot_admin.sys_user`（最后登录信息、注册）与 `sys_login_log`，退出条件见 design。
