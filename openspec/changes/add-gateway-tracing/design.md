# Design

## Context

动机见 `proposal.md` 的 Why。这里只记录影响做法的实测事实。

**已确认的事实**

- Tempo 中 `service.name=iboot-gateway` 的 span 只有 `/actuator/health` 与 `/actuator/info`，经路由转发的 `/api/**` 请求没有服务端 span。
- 追踪、观测、OTLP 导出、网关观测四类自动配置全部 matched；导出通路正常（actuator span 能到达 Tempo）。
- 关闭 `metrics.enabled`、改用 `local` profile、重新打包重启，行为均不变。
- 下游 admin 与 ai 能正常产生、接收 span（`admin → ai` 存在真实链路）。

**两次失败尝试**

| 尝试 | 结果 |
|---|---|
| `ObservationDocumentation.observation(convention, null, supplier, registry)` | 运行期 `NullPointerException: You have not provided a default convention`；反编译确认传入的第一个参数是 `new DefaultServerRequestObservationConvention()`，字节码顺序正确 |
| `Observation.createNotStarted("http.server.requests", supplier, registry)` | 请求返回 200，但追踪后端仍无网关 span |

**环境干扰**

本机系统时钟在排查期间两次大幅跳变（跨天、跨 9 小时）。追踪后端按时间窗检索，任何"窗口内没有数据"的结论都不可信。这直接决定了本变更必须先把验证手段做可靠。

## Goals / Non-Goals

**Goals:**

- 让网关成为链路的真实起点：入口 span 存在、上下文传得下去、下游不另起链路。
- 把"链路是否贯通"变成一条可重复执行的命令，不依赖人工翻追踪界面。
- 观测组件任何情况下都不能影响转发。

**Non-Goals:**

- 不排查 Micrometer / Spring Cloud Gateway 的上游缺陷，只求本项目内可用且可解释。
- 不做采样策略调优、不做 span 属性规范化治理。
- 不改动路由、限流、CORS 等既有网关行为。

## Decisions

### D1. 先建验证手段，再动实现

顺序固定为：

1. 单元层：在测试里用 `SimpleObservationRegistry` 断言"过滤器执行后产生了观测"，把**未执行**与**未导出**分开；
2. 集成层：一条脚本从网关日志取 TraceID，按 ID 查询追踪后端，输出三个服务是否同链路；
3. 才进入实现调整。

**为什么**：本次排查最大的损耗不是写代码，而是**无法判断"没有 span"到底是没创建还是没导出**。先把观测点做出来，后面每一步都有确定信号。

**备选**：先写实现再验证——已经证明会在时间窗不可靠时陷入反复试错。

### D2. 实现路线：先查工厂方法异常，再决定 API

按顺序尝试，前一步不成立才进入下一步：

1. **查清 `observation(...)` 的 NPE**：比对 Micrometer 1.16.3 与 Spring Framework 7 的 API 变更，确认是调用约定问题还是库缺陷；能修则用标准 API（span 名称与属性最规范，下游兼容性最好）。
2. **改用 `Observation.createNotStarted`**：能创建观测但需要自行补齐 span 名称与关键属性；本次实测未被观测到，需要先用 D1 的单元断言确认过滤器确实执行，再判断是否导出问题。
3. **退到显式 `Tracer`**：直接创建服务端 span，并用 Reactor 上下文传播。可控性最高，但需要自己处理异步上下文，容易再次踩到 spike 里"手工客户端不传递上下文"的坑。

**判断依据**：只要 D1 的单元断言能证明观测被创建、集成脚本能证明链路贯通，就是可接受方案；实现越贴近框架标准越好。

### D3. 只对非运维路径创建 span

`/actuator/**` 已有独立观测，跳过。判定按路径前缀，不引入更复杂的匹配规则。

**为什么**：重复 span 会污染链路的父子结构与耗时统计，也会让"网关耗时"这类后续指标失真。

### D4. 观测失败必须被隔离

观测创建与结束都包在异常兜底里：观测出错只记日志，请求照常转发。

**为什么**：网关是唯一入口，尝试一已经用 500 证明了这里的失败半径。这条不是防御性编程的洁癖，是这次事故的直接教训。

### D5. 复用既有开关

沿用 `management.tracing.enabled`（环境变量 `IBOOT_TRACING_ENABLED`），不新增专用开关。

**为什么**：网关的 span 属于追踪能力的一部分，用户关闭追踪时不应该还需要记住第二个开关。

## Risks / Trade-offs

| 风险 | 处置 |
|---|---|
| 观测组件把全部请求打成 500（已发生过一次） | 实现必须带单元测试；上线前用脚本回归全部 `/api/**` 冒烟 |
| 显式创建 span 与框架的迭代器语义冲突（`doFinally` 在异步线程执行） | 优先用框架 API；退到 Tracer 时在跨越线程处显式传上下文 |
| 时钟不可靠导致验证结论失真 | 验证只按 TraceID 查询；时间窗只用于人工浏览 |
| 网关 span 缺少标准属性（无 convention 时） | 若最终走 `createNotStarted`，自行补齐方法与路径属性，并记录取舍 |

## Migration Plan

1. 先提交验证手段（脚本 + 单元断言），此时行为不变；
2. 再提交实现，开关默认开启，出问题用 `IBOOT_TRACING_ENABLED=false` 立即降级；
3. 回滚：实现与验证分属不同提交，直接回退实现提交即可，转发行为从未被修改。

## Open Questions

- `ObservationDocumentation.observation(...)` 的 NPE 是调用约定问题还是库自身缺陷——实现第一步据此决定用哪条 API，不影响本设计的其余部分。
