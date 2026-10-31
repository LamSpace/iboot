# Design

## Context

认证目前的形态（`iboot-admin`）：

```
AuthController  POST /api/auth/login|logout|register, GET /api/auth/scope
AuthApplicationService
  ├─ UserRepository / RoleRepository / MenuRepository   -> iboot_admin.*
  ├─ ConfigApplicationService (sys_config)              -> iboot_admin.*
  ├─ LoginLogApplicationService (sys_login_log, @Async) -> iboot_admin.*
  ├─ UserDataScopeResolver (dept/role/user)             -> iboot_admin.*
  └─ BusinessMetricsService (user_login_success/failed)
RedisTemplate：login_tokens:<token> / online_token:<userId> / login_retry:<username>
```

关键既有事实（来自 `iboot-kernel` 的 `JwtAuthenticationFilter` 类注释）：**会话的唯一事实来源是共享 Redis，过滤器只读取与滑动续期，不签发也不解析令牌内容**。因此认证服务只负责「签发票据 + 写会话」，与资源服务器天然解耦。

## Goals / Non-Goals

**Goals**

- 把登录/登出/注册搬到独立进程，对外行为逐字节不变。
- 保持「改回网关路由即可回滚」。
- 把认证服务不可用从「全站不可用」降级为「无法登录」。

**Non-Goals**

- 不引入 OAuth2 / OIDC / Spring Authorization Server（另起变更）。
- 不搬 `/api/auth/scope`。
- 不新建 `iboot_auth` schema。
- 不改前端任何文件。

## Decisions

### D1 只搬 login / logout / register，不搬 scope

`/api/auth/scope` 返回的是部门可见范围与角色权限，属于系统域；`iboot-file`、`iboot-ai` 的 `scope-url` 指向管理端，管理端自身的 `DataScopeInterceptor` 也在本地解析同一份结果。搬到认证服务会让同一份解析逻辑存在两处，并让两个服务改配置。

代价：过渡期内 `/api/auth/*` 由两个服务分担。网关按精确路径拆分，`routes.txt` 与文档必须写明。

退出条件：`iboot-system` 拆分时，scope 随系统域迁走。**不要把它迁给认证服务。**

### D2 过渡期直连 `iboot_admin`，不建 schema

认证服务的数据源直接指向 `iboot_admin`，因此 SQL 使用未限定表名。理由：

- 认证的全部持久化数据本来就属于系统域，贸然建 `iboot_auth` 只会把 `sys_login_log` 搬来搬去；
- 新建 schema 会引入「管理端日志页反向跨库读认证服务」的问题，比写方向倒挂更难解释。

登记的临时写操作（退出条件：`iboot-system` 拆分时改为系统服务内部接口）：

| 写操作 | 表 | 退出条件 |
|---|---|---|
| 登录成功更新最后登录 IP/时间 | `sys_user` | 改为系统服务内部接口 |
| 注册插入用户 | `sys_user` | 同上 |
| 登录日志写入 | `sys_login_log` | 归属随日志域迁移 |

### D3 持久化用 JdbcTemplate，不引入 MyBatis

认证服务访问的全部是其他服务的表，不存在本服务领域模型；用一个小型 `AuthDirectory`（`JdbcTemplate`）承载六条查询与三条写入，比引入 MyBatis、PO、XML 与仓储转换更小。这与 `iboot-file` 中 `SysConfigReader` 读取共享库的做法一致。

### D4 登录日志同步写，失败只记录不抛出

管理端原实现是 `@Async`。认证服务不引入执行器；改为同步插入，并用 `try/catch` 兜底——登录日志失败不得影响登录结果。行为差异仅在于日志落库时机，对调用方不可见。

### D5 指标只有登录相关搬走

`user_login_success` / `user_login_failed` / `user_logout` 三个计数器随登录逻辑迁移到认证服务；管理端 `BusinessMetricsService` 中对应字段与方法删除（本变更造成的孤儿）。指标名称保持不变，Grafana 面板与告警不用改查询语句，但数据源会变成认证服务的实例。

### D6 三个静默契约必须照抄

`iboot-file` 拆分时踩过的三项，认证服务同样会踩：

1. 会话 Redis 序列化：依赖内核 `KernelRedisAutoConfiguration` 提供的默认模板，不自建。
2. CloudEvents 信封：`CloudEventResponseBodyAdvice` 的 `basePackages` 必须是 `com.iboot.auth.interfaces`。
3. API 版本：控制器上的 `version = "1"` 需要 `ApiVersionConfig` 提供 `ApiVersionStrategy`。

另加认证特有的第四项：`jwt.secret` 必须与其余服务逐字一致，否则表现为「登录成功但全站 401」。

### D7 服务拓扑

```
portal
  |
  v
iboot-gateway :8088
  ├─ /api/auth/login|logout|register  -> iboot-auth  :8083
  ├─ /api/file/**                     -> iboot-file  :8082
  ├─ /api/ai/**                       -> iboot-ai    :8081
  └─ /api/**（含 /api/auth/scope）     -> iboot-admin :8080
```

### D8 启动顺序

认证服务依赖管理端 Flyway 建出的共享表，因此 `dev-init.sh` 中必须排在管理端健康之后，与 `iboot-file`、`iboot-ai` 相同。

## Risks / Trade-offs

| 风险 | 处置 |
|---|---|
| `jwt.secret` 不一致导致全站 401 | 配置照抄管理端；冒烟覆盖「登录后访问用户信息」 |
| 双写 `sys_user`，管理端改表结构时认证 SQL 静默失效 | 登记为临时状态；契约快照 + 冒烟 + 权限矩阵覆盖 |
| 登录指标读数从 admin 实例迁移到 auth 实例 | 指标名不变，面板查询不变；验证 `/actuator/prometheus` 有对应计数 |
| `/api/auth/*` 双归属造成认知负担 | 文档与 `routes.txt` 注释写明，并登记退出条件 |
| 注册是跨服务写 | 本变更不改其语义；`iboot-system` 拆分时改为内部接口 |

## Migration Plan

1. 新建并启动认证服务 → 验证独立可用。
2. 网关加路由（管理端旧实现仍在，形成绞杀者过渡期）→ 经网关回归全绿。
3. 删除管理端旧实现 → 路径守恒登记改为 `done`。
4. 回滚：网关删掉 auth 路由并把三条路径交回 `/api/**`，恢复管理端旧实现（`git revert`）；表与 Redis 键从未改变，数据无需回迁。
