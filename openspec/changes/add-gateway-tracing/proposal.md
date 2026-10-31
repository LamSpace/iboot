# Proposal

## Why

`add-microservice-foundation` 的验收项 8.4（`gateway → 下游 → 下游` 一条链路）原判定为未达成，理由是"网关对经路由转发的 `/api/**` 请求不产生服务端 span"。**该判定是错的**，本变更实施前已用实测推翻：

- 对 `GET /api/user/list` 连发 40 次，Tempo 中 40 条 trace 的根 span 都属于 `iboot-gateway`；
- 任取一条，父子关系完整：`iboot-gateway http get`（服务端，根）→ `iboot-gateway HTTP GET`（客户端，带 route.id）→ `iboot-admin http get` → 管理端安全过滤链 span，同一 TraceID；
- 先前"查不到"的直接原因是**查询时间窗的边界**：请求时间戳 `1793416911.58` 落在查询窗口 `end=1793416911` 之外。这正是本变更设计里已经识别过的风险——时间窗不可信。

真正缺的不是埋点，而是**不依赖时间窗的验证手段**：

- 没有 `X-Trace-Id` 响应头，取一条请求的 TraceID 只能翻日志或反复查追踪后端；
- 没有按 TraceID 查链路的脚本，"三服务是否同链路"只能人工翻界面，而人工翻界面正是上次误判的来源；
- 追踪采样默认值 0.1，90% 的请求不会出现在追踪后端，进一步放大了"看不到就是没有"的错觉。

另外记录一个排查副产品：`ObservationDocumentation.observation(convention, null, supplier, registry)` 抛出的 `NullPointerException: You have not provided a default convention`，根因是把第二个参数（默认约定）传成了 `null`——该方法内部对该参数做了 `requireNonNull`。这是调用方式问题，不是库缺陷，因此也不需要自行实现观测组件。

## What Changes

- **网关在响应头回显 TraceID**（`X-Trace-Id`），使脚本与人都能直接拿到标识。
- **新增 `scripts/regress/trace-chain.sh <traceId>`**：按标识查追踪后端，输出涉及的服务并判断是否包含期望服务，不依赖时间窗。
- **补充单元断言**：用 `SimpleObservationRegistry` 断言"经网关的请求会产生服务端观测"，把"未创建"与"未导出"分开，防止未来回归。
- **三服务追踪采样默认值由 0.1 调整为 1.0**，保证验证与日常排障能看到完整链路；压测时用 `IBOOT_TRACING_SAMPLE` 调低。
- **修正追踪总开关**：`IBOOT_TRACING_ENABLED` 原先映射到 `management.tracing.enabled`，该属性在 Boot 4 已失效（设为 `false` 后追踪照常导出），改映射到 `management.tracing.export.enabled`。
- **订正文档与验收记录**：说明网关 span 的存在性、按 TraceID 查链路的方法，以及本次误判的成因。

明确不做（非目标）：

- **不自行实现入口观测组件**：框架的 `ServerHttpObservationFilter` 已为转发请求创建服务端 span，重复创建会产生同级 span，污染父子结构与耗时统计。
- **不改造网关 span 的属性**：路径已在高基数键 `http.url` 上；低基数键 `uri` 为 `UNKNOWN`，只影响 span 名称的可读性，不影响链路贯通，本次不动。
- **不引入服务网格或第三方 agent**，不改下游服务，不改路由与转发语义。

## Capabilities

### New Capabilities

- `gateway-tracing`: 网关入口的追踪行为——转发请求产生服务端 span、向下游传递上下文、不与 actuator 观测重复、可整体关闭，以及不依赖时间窗的可复现验证方式。

### Modified Capabilities

（无。`add-microservice-foundation` 的 `distributed-tracing` 能力尚未归档为主规格，本变更不与其冲突；归档顺序上应先完成该变更，或在归档时合并两者的要求。）

## Impact

**代码**

- `iboot-gateway`：新增响应头过滤器与相同职责的单元测试；不改路由与转发逻辑。
- `iboot-admin` / `iboot-ai` / `iboot-gateway`：追踪采样默认值调整。

**验证**

- 新增 `scripts/regress/trace-chain.sh`。
- 网关注入现有 `GatewayTracingTest`，覆盖"服务端观测被创建"与"响应头回显 TraceID"。

**风险**

- 新增过滤器位于请求主链路，出错会打断所有请求。因此它只做"读当前 span 的 traceId + 写响应头"，异常全部兜底，且追踪关闭时不注册任何行为。
- 采样调到 1.0 会增加追踪后端写入量，仅适合开发与验收环境；生产按需通过环境变量调低。
