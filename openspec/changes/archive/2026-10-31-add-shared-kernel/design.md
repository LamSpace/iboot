# Design

## Context

见 `proposal.md - Why`。这里只记录影响「怎么搬」的现状事实：

- `iboot-admin` 与 `iboot-ai` 是两套独立的 Servlet 应用；ai 已经复制了一份 admin 的安全实现（683 行），两份逻辑高度相似但**白名单不同**（admin 含 `/api/auth/login`、`/api/config/public`、`/api/i18n/**`、Swagger；ai 只有自己的一组路径）。
- 两份过滤器都读同一份 Redis 会话：`login_tokens:{token}` 与 `online_tokens:{userId}`，滑动续期。这是事实上的资源服务器契约。
- 401 响应体不同：admin 用 `Result.error(401, ...)`，ai 手写 `{"code":401,"message":"..."}`。
- 操作日志链路是 `@Log` → `LogAspect` → `OperateLogApplicationService` → `OperateLogRepository` → `OperateLogMapper`，直写 `sys_operate_log`。`OperateLogApplicationService` 同时被 `LogController`（查询）与 `DashboardApplicationService`（统计）使用。
- 所有候选类都在 `com.iboot.admin` 包下，由 `@SpringBootApplication` 的默认扫描发现。

## Goals / Non-Goals

**Goals**

- 让一个新服务「加一个依赖」就同时具备：统一响应信封、异常处理、JWT 资源服务器鉴权、当前用户读取、操作日志。
- admin 与 ai 共用同一份实现，行为与今天一致（除已登记的 401 响应体差异）。
- 内核不依赖任何业务模块，依赖方向单向：`admin / ai / 未来服务 → kernel`。

**Non-Goals**

- 不做服务模板/脚手架（archetype），不生成代码。
- 不引入数据权限、缓存、SSE、存储 SPI。
- 不改变 Redis 会话格式与 JWT 签发逻辑。

## Decisions

### D1. 一个 `iboot-kernel` 模块，内部三个包

`com.iboot.kernel.web` / `.security` / `.logging`，而不是三个 Maven 模块。

**为什么**：三者的依赖链是线性的（logging → security → web），拆成三个 pom 只会多出三份版本与三处发布；而且当前没有任何服务只想要其中一层。

**备选**：`iboot-kernel-web` / `-security` / `-logging` 独立发布——等出现「只要 web、不要 logging」的真实服务再拆，成本是加两个 pom。

### D2. 包名迁移到 `com.iboot.kernel.*`，不保留旧包名

**为什么**：把 `com.iboot.admin.*` 的类放进共享模块，会让所有引用方误以为内核属于 admin，这个误导会一直存在。机械改名的成本是一次性的，编译错误会把所有遗漏点暴露出来。

**备选**：内核沿用 `com.iboot.admin.*` 包名——admin 侧零 import 改动（约 200 处），但包名与模块名长期不一致。

### D3. 用自动配置注册 Bean，不靠 `@ComponentScan("com.iboot")`

内核提供 `KernelAutoConfiguration`，用 `@Import` 引入过滤器、异常处理器、切面等组件，并在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 登记。宿主服务只需加依赖。

**为什么**：宿主启动类在 `com.iboot.admin` / `com.iboot.ai` 下，默认扫描不到 `com.iboot.kernel`。放弃注册的后果不是报错，而是**过滤器不生效、接口照样返回 200**——这是静默的安全失效，必须在结构上排除。

**备选**：各宿主加 `@ComponentScan({"com.iboot.admin", "com.iboot.kernel"})`——能用，但每个新服务都要记得加，漏了同样静默失效。

### D4. 白名单 = 内核默认值 + 宿主覆盖

`iboot.security.whitelist` 为路径列表配置项；内核只提供与业务无关的默认值（Swagger、`/actuator/**`、`/error`）。admin 与 ai 把各自**现有的完整列表**写进自己的 `application.yml`，行为逐条保持。

**为什么**：白名单是服务边界的一部分，不是通用能力。把它硬编码进内核，要么泄漏 admin 的路径给 ai，要么迫使两个服务共用一份过宽的白名单。

**约束**：迁移时必须逐条对照现有常量数组，不允许「顺手精简」。

### D5. 行为对齐以 admin 为准

- 签名无效 / 会话缺失 → 不建立认证，交由 Spring Security 决定响应（与 admin 一致）。
- 过滤器内异常 → 401，响应体为 `Result.error(401, "认证失败，请重新登录")`。
- 滑动续期同时刷新 `login_tokens:{token}` 与 `online_tokens:{userId}`。

ai 的 401 响应体由它自己的安全配置写出，**保持不变**；只有「过滤器访问 Redis 抛异常」这一分支改用内核的 `Result` 信封。此外，内核的全局异常处理器在 ai 中通过 `iboot.kernel.exception-handler.enabled=false` 关闭：ai 已有 `AiExceptionHandler`，若再叠加内核处理器，参数校验与未预期异常的响应结构会被改变，契约快照随之漂移（实测会新增 `ResultVoid` 与 400 响应定义）。关闭后 ai 的 OpenAPI 与基线完全一致。

**为什么允许这个开关**：内核的目标是统一能力，不是强制替换每个服务的既有契约。需要保持原契约的服务可以显式关闭，之后再单独评估是否切换。

### D6. 操作日志只搬「写路径」，查询侧留在管理端

内核包含：`@Log` 注解、`BusinessTypeEnum`、`OperateLog` 模型、`LogAspect`、只做 insert 的 `OperateLogWriteMapper` 与 recorder。
管理端保留：`OperateLogRepository` / `OperateLogMapper` / PO / `OperateLogApplicationService` 的查询方法。

**为什么**：查询侧的 `OperateLogMapper.selectPage` 带 `@DataScope`，整体搬入内核会让内核携带一个在多数服务里静默失效的数据权限注解——这正是 Non-Goals 要避免的。写路径不涉及数据权限，可以安全共享。

**为什么仍然直写**：D10 的终态是「各服务发 CloudEvent → MQ → monitor 落库」，但 MQ 属于路线图 2.4，现在引入等于为一个还没到的需求改造日志链路。直写同一张表是过渡态，行为零变化。

**退出条件**：`iboot-monitor` 拆分时改为事件投递，并删除内核里的直写实现。

**代价**：insert 语句有内核与管理端两份定义（管理端那份随 `recordOperateLog` 一并删除后只剩内核一份，见迁移计划）。

### D7. 测试策略

- admin 的现有测试只做 import 级调整（包名迁移的必然结果）；唯一的非 import 改动是 `ApprovalControllerTest` 补一行 `@Import(GlobalExceptionHandler.class)`——`@WebMvcTest` 的切片不再能扫描到迁出包名下的 advice。所有断言未改。
- ai 的 security 类与 admin 并非逐字相同（`JwtTokenUtil` 只验签、`SecurityUtils` 多两个方法且未登录抛 `IllegalStateException`、过滤器为三参构造），删除副本必然要动 5 个安全相关测试：过滤器测试迁入内核，4 个控制器测试只改 import。合并后的 `SecurityUtils` 以 admin 语义为准（未登录抛 `BusinessException`）。
- 内核新增单元测试，覆盖过滤器语义：白名单放行、无 token 放行、有效 token 建立上下文、非法 token、会话缺失、Redis 异常 → 401。
- 增加一个内核自动配置测试：宿主只加依赖时，过滤器与异常处理器确实被注册（防止 D3 的静默失效回归）。

## Risks / Trade-offs

| 风险 | 处置 |
|---|---|
| 组件扫描/自动配置漏注册 → 鉴权静默失效 | D3 自动配置 + 专门的注册测试 + 冒烟里断言未登录访问返回 401 |
| 迁移遗漏导致包名引用断裂 | 编译错误是显式的；分三批（web → security → logging）提交，每批都能独立编译 |
| `GlobalExceptionHandler` 依赖 `I18nUtil`，内核被迫吸收 i18n | 一并搬 `I18nUtil`（74 行，仅依赖 `MessageSource`），不搬 i18n 的语言包与 Controller |
| 内核逐渐变成「什么都往里塞」的分布式单体 | 本变更明确四项不抽；后续每加一项须在对应 change 里说明为什么必须共享 |
| ai 与原实现的行为差异被忽略 | 401 响应体差异登记在案；其余语义逐条对照后写进测试 |

## Migration Plan

分三批，每批独立可编译、可回退（单次 revert 即可，无数据变更）：

1. **web**：建模块与父 POM 聚合 → 搬 `Result` / `PageResult` / `BusinessException` / `GlobalExceptionHandler` / `I18nUtil` → 改 import → `mvn verify`。
2. **security**：搬 JWT 过滤器、`JwtTokenUtil`、`SecurityUtils`、白名单（改配置项）、`JwtSecurityConfig`、`Constants` → ai 删除副本 → `mvn verify`。
3. **logging**：搬 `@Log` 与整条操作日志链路 → `mvn verify` → 全量回归。

回退：任一批失败直接 revert；由于没有数据库与协议变化，回退后行为与改造前完全一致。

## Open Questions

- 内核是否在服务数量稳定后升级为「新服务模板」（BOM / archetype）——等拆到第 3、4 个服务时再评估，不影响本次任务拆分。
