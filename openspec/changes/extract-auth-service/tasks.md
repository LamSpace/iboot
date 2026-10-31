# Tasks

## 1. 模块与骨架

- [x] 1.1 新建 `iboot-auth` 模块并加入父 POM（依赖：kernel、web、validation、actuator、druid、mysql、jdbc、springdoc、nacos、opentelemetry）
- [x] 1.2 编写 `AuthApplication` 与 `application.yml`：数据源指向 `iboot_admin`、Redis、Nacos 注册与配置、OpenTelemetry 追踪
- [x] 1.3 补齐三项静默契约：CloudEvents 信封（`basePackages = com.iboot.auth.interfaces`）、`ApiVersionConfig`、鉴权白名单
- [x] 1.4 验证：只启动中间件 + 认证服务时 `/actuator/health` 为 UP，Nacos 可见，`/v3/api-docs` 可导出

## 2. 认证实现搬迁

- [x] 2.1 `AuthDirectory`（JdbcTemplate）：查询用户（含角色 ID）、角色编码、菜单权限、系统参数；写入最后登录信息、注册用户、登录日志
- [x] 2.2 从管理端搬运 `AuthApplicationService` 的 login / logout / register，保留登录失败锁定、独占会话、会话超时与注册开关语义
- [x] 2.3 搬运 `LoginRequest`、`RegisterRequest`、`LoginResponse` 与 `AuthController`（仅三个接口）
- [x] 2.4 新增 `LoginMetrics`，以同名指标上报 `user_login_success` / `user_login_failed` 计数器与 `user_online_count` 量表
- [x] 2.5 单测：登录成功、密码错误计数、账号禁用、达到阈值锁定、独占会话踢旧会话、注册开关关闭、用户名重复、无令牌登出幂等

## 3. 接入网关与本地环境

- [x] 3.1 网关 `application-local.yml` 与 `application-nacos.yml` 增加 `/api/auth/login|logout|register` 精确路由，置于兜底路由之前
- [x] 3.2 `scripts/regress/routes.txt` 增加三条路由规则
- [x] 3.3 `scripts/dev-init.sh` 增加 `iboot-auth:8083`，排在管理端健康之后
- [x] 3.4 验证：经网关登录成功，`trace-chain.sh --expect iboot-gateway,iboot-auth` 通过

## 4. 删除管理端旧实现

- [x] 4.1 删除 `AuthController` 的 login / logout / register 与相关私有方法，只保留 `scope`
- [x] 4.2 删除 `AuthApplicationService` 中登录、登出、注册及私有辅助方法，只保留 `getCurrentUserScope`
- [x] 4.3 删除 `LoginLogApplicationService.recordLoginLog` 与随之下线的导入
- [x] 4.4 删除 `BusinessMetricsService` 中登录成功/失败/登出计数器（本变更造成的孤儿），同步清理调用点
- [x] 4.5 清除管理端白名单中的三条认证路径，更新 `SecurityWhitelistBindingTest`
- [x] 4.6 验证：`mvn -s /home/lam/repo/settings.xml test` 全绿（kernel 16 / file 5 / auth 11 / admin 247 / ai 223 / gateway 10）

## 5. 回归与文档

- [x] 5.1 `scripts/regress/migrations.txt` 增加三条登记并推进到 `done`
- [x] 5.2 刷新 `golden/admin.json` 与 `golden/auth.json`，其余服务的基线 MUST 零漂移
- [x] 5.3 `scripts/regress/regress.sh` 全绿（契约、路径守恒、路由覆盖、冒烟、权限矩阵）
- [x] 5.4 故障隔离验证：停止认证服务后已签发令牌仍可访问业务服务；新登录返回明确降级错误
- [x] 5.5 文档更新：`ROADMAP.md`（状态 + OAuth2/OIDC 待办）、`doc/23`（§11.2 / §11.5 / §11.6 / §12.1）、`doc/03`、`doc/09`、`doc/05`、`doc/06`（`doc/10`、`doc/13`、`doc/22` 中关于 `scope` 的描述仍准确，未改）
