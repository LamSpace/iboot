# Design

## Context

动机见 `proposal.md`。当前相关事实：

- `iboot-gateway` 是 WebFlux 服务，路由按 profile 分别写在 `application-local.yml` 与 `application-nacos.yml`，错误响应由 `GatewayErrorHandler` 统一为 `{code,message,path}`。
- 服务间调用统一使用 `@LoadBalanced RestClient`，不走网关；`iboot-workflow` 的 `HttpNotificationPort` 已实现「事务提交后异步投递 + 指数退避 + 失败不阻断主流程」。
- Nacos 3.1.1 通过 `spring.config.import: optional:nacos:...` 接入；2.0 门禁 Spike 已验证 v1 配置写入接口 `/nacos/v1/cs/configs` 可用，但**规则持久化到 Nacos 未验证**。
- 版本基线：Boot 4.0.3、Spring Cloud 2025.1.0、SCA 2025.1.0.0、Sentinel 1.8.9（由 SCA BOM 管理）。Spike 已验证 Sentinel 1.8.9 的核心限流可用。
- 本地已有 `bladex/sentinel-dashboard:1.8.9` 镜像，容器内监听 `8858`；默认 `auth.username/password=sentinel/sentinel`，`-Dauth.enabled=false` 可关闭登录。
- SCA 的 `spring-cloud-starter-alibaba-sentinel` 不提供 Micrometer 绑定；Sentinel core 提供 `MetricExtension` 扩展点，可用于把治理指标桥接到 Micrometer。
- R4 已登记本机内存压力，新增常驻进程必须计入。

## Goals / Non-Goals

**Goals:**

- 网关按路由限流，超限返回与现有错误契约一致的 `429` JSON。
- `iboot-workflow → iboot-notify` 的通知登记具备熔断降级，保持「通知失败不阻断审批」。
- `iboot-notify` 的内部通知端点具备入口流控。
- 规则以 Nacos 为权威来源，运行期热更新；本地可一键写入基线规则。
- Sentinel Dashboard 作为本地开发必选组件常驻，生产必须认证且不经公网网关。
- 治理指标进入 Prometheus/Grafana，且不依赖 Dashboard 在线。

**Non-Goals:**

- 不把 Sentinel 铺到全部九个业务服务；本变更只落网关与两个代表服务。
- 不引入 Seata、RocketMQ、Kafka 或 SkyWalking；不改变阶段 2 收尾的其它判据。
- 不替换 Prometheus/Grafana、ELK 或 Tempo；Dashboard 只做单资源实时观察与规则查看。
- 不改造 Sentinel Dashboard 源码，不实现「Dashboard 编辑直接写回 Nacos」。
- 不做前端/门户入口、数据库结构或阶段 3 的 Helm/HPA 变更。
- 不做全链路压测与容量规划；限流阈值只给出可演示的基线。

## Decisions

### D1：选择 Sentinel，而不是 Resilience4j 或自研熔断

Sentinel 1.8.9 已由 SCA BOM 锁定，2.0 门禁 Spike 已实测可用，且同时提供网关适配、Nacos 规则源与 Dashboard。Resilience4j 与 Spring Cloud CircuitBreaker 更贴近 Spring 原生抽象，但没有现成的规则中心与治理控制台，接入后会形成第二套治理模型；自研熔断则重复 Sentinel 已成熟的滑动窗口与半开逻辑。

代价是 Sentinel 的 WebFlux/Gateway 适配需要先验证对 Spring Cloud Gateway 5 的兼容性（见风险与任务 1）。

### D2：Nacos 是规则权威来源，Dashboard 只做观察与临时演练

三个接入点各自配置 Sentinel Nacos Datasource，启动即加载，Nacos 配置变更即时刷新：

| 服务 | data-id | rule-type |
|---|---|---|
| `iboot-gateway` | `iboot-gateway-flow-rules` | `flow` |
| `iboot-workflow` | `iboot-workflow-degrade-rules` | `degrade` |
| `iboot-notify` | `iboot-notify-flow-rules` | `flow` |

统一使用 group `SENTINEL_GROUP`、类型 `json`。新增 `scripts/sentinel-init.sh`，通过 Nacos v1 配置写入接口幂等写入 `docker/sentinel/rules/*.json`，由 `dev-init.sh` 在 Nacos 就绪后调用。

Sentinel Dashboard 1.8.9 官方不提供 Nacos 规则持久化，改造它需要维护一份 Dashboard 源码分支，成本与收益不成比例。因此本变更明确：**Dashboard 上的规则修改只作用于运行期内存，重启或 Nacos 规则刷新后回到基线**；需要长期生效的修改写 Nacos。文档必须把这个边界写清楚，避免出现「界面改了但仓库没有」的隐性状态。

备选：以 Dashboard 内存规则为准——被否，重启即丢失，不满足「规则持久化」；自建带 Nacos 推送的 Dashboard——被否，超出本变更范围，且第三方镜像无法保证可维护性。

### D3：网关使用 Sentinel Gateway 适配，路由 ID 即资源名

`iboot-gateway` 增加 `spring-cloud-starter-alibaba-sentinel` 与 `spring-cloud-alibaba-sentinel-gateway`（均由 SCA BOM 管版本）。SCA 自动配置会注册 `SentinelGatewayFilter`，该过滤器把当前 **路由 ID** 作为 Sentinel 资源名；因此规则直接使用现有路由 ID（`workflow`、`notify` 等），不需要额外定义 API Group。

- 规则：**标准 `flow` 规则**（`resource` = 路由 ID，`grade=1` QPS），由 Sentinel 的 `FlowSlot` 校验；基线只对 `workflow` 与 `notify` 两条路由生效，其它路由暂不限制，避免误伤。
- **为什么不用 `gw-flow`**：实现时验证发现，SCA Sentinel Gateway 1.8.9 的 `GatewayFlowRule` 会被转换成内部参数规则，依赖 `ParameterMetricStorage` 的指标生命周期；在网关的异步 entry 路径上指标在两次请求之间被清理，规则无法稳定触发。标准 `flow` 规则以同一路由 ID 为资源，行为稳定，且在 Dashboard 的「流控规则」页可见；路由级语义不变。
- 限流响应：自定义 `BlockRequestHandler`，输出 `{"code":429,"message":"请求过于频繁","path":"..."}`，与 `GatewayErrorHandler` 的字段和 `Content-Type` 对齐。不复用 `GatewayErrorHandler`——它处理的是异常链，Sentinel 的阻塞走回调，不复用更直接。
- 不把 `/api/push/**` 的 SSE 路由纳入限流基线：长连接重连频率低，误限流的代价高于收益；后续如需要单独加规则。

备选：用 `sentinel-spring-webflux-adapter` 按 URL 限流——被否，无法表达「按路由」的阈值，且与 `api-gateway` 的路由模型脱节。`gw-flow` 规则经实测不可靠，已在上面说明并改用标准 `flow` 规则。

### D4：业务服务按「调用方熔断 + 提供方入口流控」分工

**调用方（`iboot-workflow`）**：在 `HttpNotificationPort` 的异步投递入口用编程式 `SphU.entry("iboot-workflow:notify-enqueue")` 包住整个「重试直到耗尽」的过程。

- 选编程式而非 `@SentinelResource`：`send` 由线程池回调触发且属于类内调用，Spring AOP 代理无法拦截自调用；为一个注解拆出新 Bean 会把现有重试逻辑切碎。编程式入口边界明确，改动局限在一个方法。
- 重试耗尽后在外层 catch 中调用 `Tracer.trace` 记录异常，驱动异常比例熔断；随后仍按原语义只记日志与指标，不向审批主流程抛出。
- 调用方 `RestClient` 显式设置连接 2 秒、读取 5 秒超时，避免下游不可达时异步投递线程长时间占用，保证熔断能在有限时间内看到失败。
- 熔断打开时直接走降级分支：递增现有 `notification_trigger_failed` 计数并记录 eventId，不再向下游发请求。
- 基线规则：异常比例 `0.5`、`minRequestAmount=5`、`statIntervalMs=10000`、`timeWindow=10`，即 10 秒窗口内至少 5 次请求、异常比例过半时熔断 10 秒，随后自动半开。

**提供方（`iboot-notify`）**：内部通知端点 `/internal/notifications` 使用 Sentinel Web MVC 的 URL 资源做入口流控，而不是在 Controller 上加注解。原因是该端点由 `@LoadBalanced RestClient` 按 URL 调用，URL 资源与调用方视角一致，且不需要改 Controller 方法签名。

- 基线规则：QPS 50、快速失败。
- 自定义 `BlockExceptionHandler`，返回项目统一的 `Result` JSON（`code=429`），避免默认纯文本响应与前端/调用方的 JSON 解析约定冲突。
- 通知按 `eventId` 幂等（现有实现），因此调用方重试与提供方限流不会产生重复投递；实施时用现有幂等测试确认。

### D5：Dashboard 必选，随默认开发栈启动

`docker/docker-compose.yml` 新增无 profile 的 `sentinel-dashboard`：

- 镜像 `bladex/sentinel-dashboard:1.8.9`（`pull-docker-images.sh` 已有），容器名 `iboot-sentinel-dashboard`，端口 `8858:8858`，`restart: unless-stopped`。
- `JAVA_TOOL_OPTIONS` 限制堆并控制认证：本地默认 `-Xms128m -Xmx256m -Dauth.enabled=false`；非本地部署模板必须改为 `-Dauth.enabled=true` 并注入 `SENTINEL_DASHBOARD_USER` / `SENTINEL_DASHBOARD_PASSWORD`。
- 增加 `extra_hosts: host.docker.internal:host-gateway`，为「Dashboard 容器 → 宿主机开发进程」的反向探测留出兜底；正常路径仍使用客户端自动上报的地址。
- `scripts/dev-init.sh` 在中间件阶段启动并等待 `http://localhost:8858/` 就绪；未就绪即判定开发环境未就绪（必选语义）。`--status` 通过 compose ps 显示；就绪输出追加 Dashboard 地址。
- 网关不新增 Dashboard 路由，门户不嵌入；只允许本机直连或内网访问。

`docker-deploy.sh` 不按 profile 过滤，因此全栈部署会自动包含 Dashboard；`pull-docker-images.sh` 已包含该镜像，无需新增拉取逻辑。

### D6：指标经 `MetricExtension` 桥接 Micrometer，不依赖 Dashboard

新增非部署模块 `iboot-sentinel`，只承载治理指标桥接：

- 实现 `com.alibaba.csp.sentinel.metric.extension.MetricExtension`，在 `@PostConstruct` 中调用 `MetricExtensionProvider.addMetricExtension(...)` 注册（该接口是全局扩展点，不要求 SPI 文件）。
- 指标映射：
  - `sentinel.pass` / `sentinel.block` / `sentinel.exception` → Counter，标签 `resource`；`block` 额外带 `rule`（flow/degrade/system/authority/param）。
  - `sentinel.rt` → Timer，标签 `resource`。
  - `sentinel.threads` → Gauge（每资源 AtomicInteger）。
  - 统一附加 `application=${spring.application.name}`，与现有自定义指标的标签习惯一致。
- 自动配置 `@ConditionalOnClass({MetricExtension, MeterRegistry})` + `@ConditionalOnMissingBean`，避免在未接入 Sentinel 的服务中激活；重复注册时先检查 `MetricExtensionProvider` 列表，避免测试多上下文重复计数。
- `iboot-kernel` 与 `iboot-gateway` 依赖 `iboot-sentinel`；`sentinel-core` 以 `provided` 方式编译，避免把 Sentinel 传递给未接入的服务。这样只有真正引入 SCA Sentinel 的三个服务会装载扩展。
- 资源名保持有界：只用路由 ID、内部端点 URL、固定的调用资源名，不用用户 ID 或参数做标签；指标用于 Prometheus 聚合与告警，Dashboard 继续承担单资源实时曲线。

备选：在三个服务里各写一份指标代码——被否，重复且容易漂移；直接改 Sentinel Dashboard 的数据源——被否，Prometheus 无法抓取 Dashboard 的内部存储。

### D7：Dashboard 与 Prometheus 的职责边界

- Dashboard：按资源查看实时 QPS、通过/阻塞、RT 曲线，查看当前规则，做临时演练；不是告警与长期存储。
- Prometheus/Grafana：跨实例聚合、告警、与既有 HTTP/JVM 指标同屏；不依赖 Dashboard 存活。
- 文档 `docs/11` 与 `docs/23` 分别补一段职责说明，避免把 Dashboard 当成第二个 Grafana。

### D8：回滚与降级

- 规则回滚：删除 Nacos 中对应 data-id 或写回空数组，服务秒级回到无规则；无需重启。
- 代码回滚：撤销三个服务的 Sentinel 依赖与入口代码，Dashboard 一并移出 compose；不涉及数据库迁移。
- 运行时降级：Dashboard 停机不影响业务与规则；Nacos 不可用时服务按无规则启动，业务不因治理组件启动失败。
- 配置中心不可用时 Sentinel 的 Nacos Datasource 会持续告警；不影响启动，恢复后自动加载。

## Risks / Trade-offs

- **Sentinel 1.8.9 Gateway 适配 × Spring Cloud Gateway 5** → 已验证：`SentinelGatewayFilter` 能注册并把路由 ID 作为资源；但 `gw-flow` 规则转换出的参数规则在异步 entry 路径上指标生命周期不稳定，因此改用标准 `flow` 规则（资源仍是路由 ID）。不为此升级 SCA/Boot，版本矩阵仍以 ROADMAP §4 为准。
- **Dashboard 容器访问宿主机开发进程** → 客户端自动上报的地址通常是宿主机 LAN IP；若不可达，用 `client-ip=host.docker.internal` + compose 的 `extra_hosts` 兜底；全栈 compose 场景下客户端在容器内，地址天然可达。
- **Dashboard 编辑不持久化** → 文档明确「Nacos 是权威、界面改动仅演练」；验收只以 Nacos 变更后的行为为准。
- **限流误伤业务** → 基线只覆盖 `workflow`、`notify` 两条路由与内部通知端点，阈值取保守值；验收时用 Nacos 下调阈值触发，不用生产基线压测。
- **通知端点被限流后调用方重试放大** → 调用方已有指数退避与最大次数，且新增熔断会在连续失败后快速失败；通知按 eventId 幂等，重复投递风险由现有实现兜底。
- **指标基数** → 资源名限定为路由 ID / 固定端点 / 固定调用名；禁止把用户、租户或请求参数放入标签。
- **R4 内存 +1** → Dashboard 堆上限 256MB；它不参与业务启动依赖，可在极端内存压力下单独停止（业务与指标不受影响），但默认开发栈必须启动。
- **重复注册 MetricExtension** → 注册前检查 provider 列表；自动配置使用 `@ConditionalOnMissingBean`，测试上下文隔离。

## Migration Plan

1. **兼容性验证**：在 `iboot-gateway` 临时接入 SCA Sentinel Gateway 模块，确认 Boot 4 / SCG 5 下路由资源可被 Sentinel 识别、Dashboard 可见客户端；确认 Dashboard 容器能反向访问宿主机进程。
2. **规则底座**：新增 `iboot-sentinel` 指标模块、三个服务的依赖与 Nacos Datasource 配置；新增规则 JSON 与 `scripts/sentinel-init.sh`；启动后确认规则已加载、Dashboard 显示资源。
3. **网关限流**：实现路由级 `flow` 规则与自定义 `BlockRequestHandler`；用 Nacos 下调阈值验证 `429` 响应与单路由隔离。
4. **业务治理**：`iboot-workflow` 接入编程式熔断入口；`iboot-notify` 接入 URL 流控与 `BlockExceptionHandler`；停掉 notify 验证审批仍成功、熔断打开后快速降级、恢复后半开。
5. **Dashboard 必选**：compose 增加无 profile 服务，`dev-init.sh` 启动/等待/状态输出；验证停 Dashboard 后业务与指标不受影响。
6. **观测与文档**：Prometheus 抓取与 Grafana 查询、`docs/11`、`docs/23`、`README`、`ROADMAP` 同步；把 Sentinel 从「未接入」移入能力索引。
7. **回归**：`mvn -pl iboot-gateway,iboot-workflow,iboot-notify test`；启动全栈冒烟；确认前端零改动。
8. **回滚**：Nacos 规则置空 → 撤销代码与 compose 服务；无数据库变更，回滚不影响业务数据。

## Open Questions

- 基线阈值（网关两条路由、通知内部端点）只用于演示与验收，最终取值在实施时按本机吞吐调整；不改变本设计的行为契约。
- Dashboard 健康检查可用的工具（`curl` / `bash /dev/tcp`）取决于镜像内容，实施时按镜像实际能力选择；不影响「必选且等待就绪」的要求。
