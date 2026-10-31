# Design

## Context

动机见 `proposal.md` 的 Why。这里只记录影响做法的实测事实。

**已确认的事实（本机实测，2026-10-31）**

- 网关为经路由转发的 `/api/**` 请求**创建并导出**服务端 span：40 次 `GET /api/user/list` 对应 40 条以 `iboot-gateway` 为根的 trace。
- 链路父子关系正确：`iboot-gateway http get`（SERVER，根）→ `iboot-gateway HTTP GET`（CLIENT，带 `spring.cloud.gateway.route.id`）→ `iboot-admin http get`（SERVER）→ 管理端安全过滤链 span。
- 网关服务端 span 的属性：`method=GET`、`http.url=/api/user/list`、`status=…`、`uri=UNKNOWN`。
- 三个服务的追踪采样都是 `IBOOT_TRACING_SAMPLE=1.0`（运行时环境变量），而配置默认值是 `0.1`。
- 既有总开关 `IBOOT_TRACING_ENABLED` 实际上从未生效：它映射到 `management.tracing.enabled`，而 Boot 4 读的是 `management.tracing.export.enabled`。设为 `false` 后网关仍然导出 span，只是不再有警告，从现象上完全看不出来。

**先前误判的成因**

| 现象 | 结论 |
|---|---|
| Tempo 中查不到网关 span | 查询时间窗边界：请求时间戳 `1793416911.58` 落在 `end=1793416911` 之外。不依赖时间窗的验证正是本变更要补的能力 |
| 采样默认 0.1 | 90% 的根 span 不会进入追踪后端，会强化"看不到就是没有"的错觉 |

**两次失败尝试的结论**

| 尝试 | 结论 |
|---|---|
| `ObservationDocumentation.observation(convention, null, supplier, registry)` → `NullPointerException: You have not provided a default convention` | 调用方式问题：反编译确认方法对第二个参数（默认约定）做了 `Objects.requireNonNull`。传 `null` 必然失败，与参数顺序、泛型擦除无关 |
| `Observation.createNotStarted(name, supplier, registry)` → 追踪后端无网关 span | 在采样 0.1 下，手工创建的观测大概率未被采样；且框架本已提供观测，无需手工创建。不再深究 |

## Goals / Non-Goals

**Goals:**

- 让"一次请求是否形成完整链路"成为一个可重复执行、不依赖时间窗的命令。
- 让 TraceID 可以从响应头直接取得，不再依赖日志检索或追踪后端的时间窗查询。
- 用单元断言守住"网关会为转发请求创建服务端观测"这一事实，防止框架升级后静默回退。

**Non-Goals:**

- 不自行实现入口观测组件（框架已提供，重复创建会产生同级 span）。
- 不改网关 span 的属性规范：`uri=UNKNOWN` 只影响 span 名称可读性，不影响链路贯通。
- 不改动路由、限流、CORS 等既有网关行为。
- 不做采样算法与采样策略的调优，只调整默认值。

## Decisions

### D1. 先把验证手段做成可复现的，再谈其它

顺序固定为：

1. 单元层：`GatewayTracingTest` 用 `SimpleObservationRegistry` 断言"转发请求产生了服务端观测"，把**未执行**与**未导出**分开；
2. 运行时：网关在响应头回显 `X-Trace-Id`，让标识可直接取得；
3. 集成层：`trace-chain.sh <traceId>` 按标识查询追踪后端，输出服务集合并判断是否包含期望服务。

**为什么**：本次误判的全部损耗都来自"看不见"——先是时间窗，后是采样率。先把信号做成确定的，后续每一步结论才有依据。

### D2. 不实现自定义观测组件

框架的 `ServerHttpObservationFilter` 已经为转发请求创建服务端 span，并已通过实测证明导出正常。再实现一个同职责的组件会：

- 让同一个请求出现两条同级服务端 span；
- 污染父子结构与网关耗时统计。

因此本变更**不新增**任何观测创建逻辑，只用测试守住既有行为。

### D3. `X-Trace-Id` 用 `WebFilter` 实现，从反应式上下文取观测

- 注册为 `WebFilter`，`getOrder()` 取 `Ordered.HIGHEST_PRECEDENCE`，使跨域预检这类会直接应答的请求也带上响应头。
- **不用 `Tracer.currentSpan()`**：WebFlux 的服务端观测由 `HttpWebHandlerAdapter` 创建后放进反应式上下文，并未以线程局部作用域暴露，实测在过滤器里取当前 span 得到 `null`。改为从 `ObservationThreadLocalAccessor.KEY` 取出观测，再从 `TracingObservationHandler.TracingContext` 取 span 标识。
- TraceID 在过滤器执行时取出并捕获，再交给 `response.beforeCommit(...)` 写入；不在回调里读上下文，避免跨线程上下文丢失。
- 写入过程整体包在异常兜底里，任何失败只记 debug 日志，不影响响应。
- 回显受 `management.tracing.export.enabled` 控制：关闭追踪时不回显一个查不到的标识。

### D4. 采样默认值改为 1.0

`management.tracing.sampling.probability` 的默认值从 `0.1` 改为 `1.0`（环境变量 `IBOOT_TRACING_SAMPLE` 仍可覆盖）。

**为什么**：0.1 会让"链路是否贯通"的验证变成概率事件，也是本次误判的放大因素。开发与验收环境优先保证可观测性，压测时用环境变量调低。

### D5. 复用既有开关，但要改到 Boot 4 的属性名

沿用环境变量 `IBOOT_TRACING_ENABLED`，不新增专用开关。但属性名必须换：

- Boot 4 中 `management.tracing.enabled` 已不起作用——实测设成 `false` 后追踪照常导出，且没有任何警告；
- 生效的是 `management.tracing.export.enabled`（`OnEnabledTracingExportCondition` 读取的属性）。

关闭追踪后：不产生 span，也不回显 `X-Trace-Id`，转发行为不变。

## Risks / Trade-offs

| 风险 | 处置 |
|---|---|
| 响应头过滤器出错打断全部请求 | 只做"读 traceId + 写头"，异常全部兜底；无 `Tracer` 时为空操作；由单元测试覆盖 |
| `beforeCommit` 阶段响应已提交导致写头失败 | TraceID 在过滤器入口捕获，回调只做写头；`beforeCommit` 在提交前执行，失败被兜底 |
| 采样 1.0 增加追踪后端写入量 | 仅开发/验收默认；生产用 `IBOOT_TRACING_SAMPLE` 调低 |
| 追踪后端不可用时 `trace-chain.sh` 无法判定 | 脚本区分"查询失败"与"没有该链路"，前者以错误码退出并打印原因 |

## Migration Plan

1. 先提交验证手段（脚本 + 单元断言 + 响应头），此时只增加一个只读响应头；
2. 再提交采样默认值调整；
3. 回滚：响应头过滤器可整体移除，追踪与转发行为不受影响；采样默认值改回即可。

## Open Questions

（无。原先的两个疑问——`observation(...)` 的 NPE 与"转发请求是否产生 span"——均已由实测与反编译解答。）
