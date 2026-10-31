# Proposal

## Why

`add-microservice-foundation` 的验收项 8.4（`gateway → admin → ai` 一条链路）没有达成。排查结论是**网关对经路由转发的 `/api/**` 请求不产生服务端 span**，只有 `/actuator/**` 有：

- Tempo 中 `service.name=iboot-gateway` 的 span 只有 `http get /actuator/health` 与 `http get /actuator/info`；
- 追踪、观测、OTLP 导出、网关观测的自动配置全部 matched，导出通路也正常（actuator span 能到达）；
- 关闭/开启 `spring.cloud.gateway.server.webflux.metrics.enabled` 均无变化，`local` 与 `nacos` 两种 profile 行为一致。

后果是：拆分后一次用户请求在追踪系统里会被切成若干条互不相干的链路（admin、ai 各自成根），**"先看一条 trace 定位问题"这个拆分最核心的排障能力实际上不存在**，而阶段 2 底座已经把"链路贯穿"写进了验收判据。

同一会话中尝试过两种修法，均未成功，因此需要独立立项把问题查清并验证到位：

1. `ObservationDocumentation.observation(...)` 创建标准服务端 span → 运行期抛 `NullPointerException: You have not provided a default convention`，反编译确认传参顺序正确；
2. `Observation.createNotStarted(...)` → 请求正常返回，但 Tempo 中仍无网关 span。

## What Changes

- **网关为所有非 actuator 请求创建服务端 span**，并保证下游服务沿用同一 TraceID。实现方式在 design 中给出结论（框架能力 / 显式过滤器 / 显式 Tracer），本提案不预设。
- **建立可靠的验证手段**：不依赖时间窗查询（本次排查被系统时钟跳变严重干扰），改为"从日志取 TraceId → 按 ID 查链路"与"用 `SimpleObservationRegistry` 做单元断言"两条腿。
- **补充可重复的验证脚本**，把"三服务同链路"变成一条命令的结果，而不是手工翻 Tempo。
- **不再以 actuator 的 span 作为网关可观测性的证据**：actuator 有 span 不代表性（本次就是被它误导，一度认为 `gateway → admin` 正常，实际那条链路来自 Spring Boot Admin 的探活）。

明确不做（非目标）：

- **不引入服务网格或第三方 agent**：问题在应用侧观测，不在基础设施。
- **不改下游服务**：admin 与 ai 已经能正确产生与接收 span，问题只在网关这一跳。
- **不为追踪改造网关的路由与转发逻辑**：只加观测，不改转发语义。

## Capabilities

### New Capabilities

- `gateway-tracing`: 网关入口的追踪行为——每个转发请求产生服务端 span、向下游传递上下文、不与 actuator 观测重复、可整体关闭，以及可重复的验证方式。

### Modified Capabilities

（无。`add-microservice-foundation` 的 `distributed-tracing` 能力尚未归档为主规格，本变更不与其冲突；归档顺序上应先完成该变更，或在归档时合并两者的要求。）

## Impact

**代码**

- `iboot-gateway`：新增入口观测组件（形式待定），可能需要调整 `GatewayErrorHandler` 的日志以保留完整堆栈（已在排查中修改，尚未提交）。

**验证**

- 新增按 TraceId 查链路的脚本，纳入 `scripts/regress/`。
- 网关测试新增"观测确实被创建"的单元断言，把"过滤器没跑"与"span 没导出"分开。

**风险**

- 网关是唯一入口，观测组件出错会直接打断所有请求（尝试一就把全部请求变成了 500）。因此必须有单元测试覆盖，并且在任何时刻都能用配置关掉。
- 本机时钟不稳定，所有基于时间窗的验证结论都不作数，需在验证方式上规避。
