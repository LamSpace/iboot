# Design

## Context

动机见 `proposal.md`。当前相关事实：

- 九个业务服务（auth、gateway、system、workflow、notify、file、monitor、job、ai）均引入 `spring-boot-starter-opentelemetry`，在各服务 `application.yml` 的 `management.opentelemetry.tracing.export.otlp.endpoint` 指向 Tempo（`http://localhost:4318/v1/traces`），采样率默认 `1.0`；网关另有 `TraceIdResponseFilter` 回显 `X-Trace-Id`。
- 这是 **SDK 埋点**（Micrometer Observation → OTel SDK），不是 Java Agent；日志中的 `traceId` 来自该 SDK 写入的 MDC。
- 服务在宿主机由 `scripts/dev-init.sh` 以 `java -jar` 启动（第 286 行），中间件由 `docker/docker-compose.yml` 提供；现有 profile 为 `build`、`observability`、`graph`。
- Tempo 镜像为 `grafana/tempo:2.10.8`，已由 `scripts/pull-docker-images.sh` 从华为 SWR 镜像站拉取并 retag。
- `docker.aityp.com`（渡渡鸟镜像同步站）提供的镜像实际走 `swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/...`，与本项目拉取脚本的写法完全一致。
- 经实测，镜像站上可获取的配套版本是 **OAP 11.0.0 + Horizon UI 1.0.0 + BanyanDB 0.11.0**；11.1.0 组合尚未同步（需登录提交，匿名提交已关闭）。三者是官方 Helm 5.0.0 的配套组合，OAP 与 BanyanDB 的 server API 精确锁定在 `0.11`。
- SkyWalking Java Agent **9.8.0** 才修复 Spring Framework 7 / Boot 4 下 `HttpHeaders` 不再实现 `MultiValueMap` 导致的入口 span 缺失；更早版本在 WebFlux / Spring Cloud Gateway 上建不出入口链路。
- 本机没有 `wrk` / `ab` / `hey` / `k6` 等压测工具；Docker Hub 直连不可用。

## Goals / Non-Goals

**Goals:**

- 在不改业务代码、不改依赖、不改 OTLP 配置的前提下，让同一批服务同时向 Tempo 与 SkyWalking 上报。
- 辅助栈默认不启动，启用后可用一条命令起停，并能整体删除。
- 同一请求在两侧都能还原为完整调用链，验证方法可脚本化重复执行。
- 用可复现的方法量化 Agent 对启动时间、常驻内存与吞吐的影响，产出一页选型结论。

**Non-Goals:**

- 不替换 Tempo，不调整既有采样率、OTLP 地址或 `X-Trace-Id` 行为。
- 不为业务方法添加 `@Trace` 或 `apm-toolkit` 依赖；方法级剖析只使用 Agent 对框架层（MVC/WebFlux、JDBC、HTTP Client、Redis 等）的自动埋点。
- 不把 SkyWalking 接入 compose 的业务服务容器（阶段 3 容器化后再评估）；本次只覆盖宿主机 `dev-init.sh` 启动路径。
- 不引入 Elasticsearch 作为 SkyWalking 存储，不引入 OTel Collector。

## Decisions

### D1：版本组合取镜像站可获取的 11.0.0 三件套

| 组件 | 版本 | 理由 |
|---|---|---|
| OAP | `apache/skywalking-oap-server:11.0.0` | 镜像站可获取；11.x 起支持 admin 端口与 Horizon UI |
| UI | `apache/skywalking-ui:horizon-1.0.0` | 11.x 不再发布 `skywalking-ui:11.x` 标签，Horizon 独立版本号；1.0.0 与 OAP 11.0.0 是官方配套 |
| 存储 | `apache/skywalking-banyandb:0.11.0` | OAP 11 只接受 BanyanDB server API `0.11`，0.10 会拒绝启动；0.11.0 在镜像站可用 |
| Agent | `apache-skywalking-java-agent-9.8.0.tgz` | 9.8.0 才支持 Spring Framework 7 / Boot 4 |

替代方案：等镜像站同步 11.1.0（需登录提交，时限不可控）；或从 Docker Hub 直拉（本机不可用）。ROADMAP §4 的 11.1.0 固定值随之改为 11.0.0，并在决策记录中说明「镜像站可获取的官方配套组合优先于补丁版本」。JDK 25 与虚拟线程支持从 OAP 10.4.0 起就有，11.0.0 不损失实验所需能力。

### D2：镜像沿用既有拉取脚本，不引入新机制

在 `scripts/pull-docker-images.sh` 的 Tempo 段之后新增三条，保持 `docker pull <SWR> → docker tag → docker rmi <SWR>` 的既有模式：

```
apache/skywalking-banyandb:0.11.0
apache/skywalking-oap-server:11.0.0
apache/skywalking-ui:horizon-1.0.0
```

镜像站路径已验证存在；脚本重复执行不会失败（retag 与 rmi 均幂等）。Agent 不是镜像，单独由 `scripts/apm/fetch-agent.sh` 从 Apache 归档站下载，校验 SHA-512 后解压到 `docker/skywalking/agent/`，该目录加入 `.gitignore`。

### D3：Agent 以启动参数注入，开关默认关闭

`scripts/dev-init.sh` 增加两个环境变量：

- `IBOOT_SKYWALKING_ENABLED`（默认 `false`）
- `IBOOT_SKYWALKING_AGENT`（默认 `docker/skywalking/agent/skywalking-agent.jar`）
- `IBOOT_SKYWALKING_OAP`（默认 `127.0.0.1:11800`）

开关打开时，九个业务服务的启动命令在 `-jar` 前追加：

```
-javaagent:$IBOOT_SKYWALKING_AGENT
-Dskywalking.agent.service_name=<服务名>
-Dskywalking.collector.backend_service=$IBOOT_SKYWALKING_OAP
-Dskywalking.agent.sample_n_per_3_secs=-1
```

`sample_n_per_3_secs=-1` 表示全量采样，保证验收期链路完整；这也代表 Agent 开销的上界，选型结论中需注明。`iboot-db-init` 与 `iboot-ops` 不挂 Agent：前者是一次性任务，后者不参与 Tempo 链路，挂上会破坏「同一批服务」的对照。开关关闭时启动命令与现状完全一致。

**Agent 插件装配（实测补充）**：9.8.0 把 Spring 6/7 服务端插件放在 `optional-plugins/` 而不是 `plugins/`，默认不加载。`fetch-agent.sh` 在解压后（以及 Agent 已存在时）把以下三个插件移入 `plugins/`：

- `apm-springmvc-annotation-6.x-plugin-9.8.0.jar`：Spring MVC 服务
- `apm-spring-webflux-6.x-plugin-9.8.0.jar`：WebFlux 服务
- `apm-spring-cloud-gateway-4.x-plugin-9.8.0.jar`：网关（SCG 5.x 复用 4.x 插件）

不装配时只能看到 Agent 自身导出器调用 Tempo 的 span，看不到任何业务入口链路。同时把 `apm-lettuce-5.x-6.4.x` 与 `apm-lettuce-6.5.x` 移到 `expired-plugins/`：当前 Lettuce/Reactor 已移除 `Mono#subscriberContext`，插件 witness 不成立却仍拦截 `TracedCommand`，会持续产生 NPE 噪声。代价是失去 Redis 客户端 span，选型结论中记录为兼容性缺口。

替代方案：写独立的包装脚本重启服务（与 `dev-init.sh` 的健康等待、PID 管理重复）；或把 Agent 加进所有服务的 `JAVA_TOOL_OPTIONS`（会误伤 Maven 与测试进程）。均否。

### D4：辅助栈作为 `apm` profile，端口与内存显式约束

在 `docker/docker-compose.yml` 新增三个服务，`profiles: ["apm"]`，均不加入任何 `depends_on`：

| 服务 | 端口（宿主） | 关键配置 |
|---|---|---|
| `skywalking-banyandb` | `17913:17913` | `command: standalone --stream-root-path ... --measure-root-path ...`，命名卷 `skywalking_banyandb_data` |
| `skywalking-oap` | `11800:11800`、`12800:12800`、`17128:17128` | `SW_STORAGE=banyandb`、`SW_STORAGE_BANYANDB_TARGETS=skywalking-banyandb:17912`、`JAVA_OPTS=-Xms512m -Xmx1024m` |
| `skywalking-ui` | `18082:8081` | 挂载 `docker/skywalking/horizon.yaml:/app/horizon.yaml:ro` |

- 8080 已被 `iboot-system` 占用，UI 改用 18082。
- OAP 官方默认堆 2GB，本地压到 512m–1GB；R4 记录的「+3 进程」不变，但内存上限写入文档。
- 健康检查：BanyanDB 用 `nc -z 127.0.0.1 17912`；OAP 镜像内没有 curl/wget，用 bash `/dev/tcp` 探 12800；UI 用 HTTP 探 8081。
- OAP 11 起 admin 端口（17128）为必需，Horizon 的写接口依赖它，必须映射。
- `horizon.yaml` 随仓库提供（内容来自官方模板，仅改 OAP 地址为 `http://skywalking-oap:12800` / `http://skywalking-oap:17128`），不运行时下载。默认账号 `skywalking/skywalking` 仅限本机开发，文档标注不得暴露到内网以外。

### D5：双后端链路用 OAP GraphQL 关联，不引入代码改动

Tempo 侧用网关 `X-Trace-Id` 直接查询。SkyWalking 侧没有等价的响应头，且不引入 `apm-toolkit`，因此验证脚本按以下步骤关联同一次请求：

1. 发起请求前记录时间戳与目标接口；
2. 请求后调用 OAP GraphQL（`http://localhost:12800/graphql`）：
   - `findService(serviceName: "iboot-gateway")` 取 serviceId；
   - `findEndpoint(keyword: "/api/user/profile", serviceId, duration)` 取入口端点 id；
   - `queryTraces(condition: {serviceId, endpointId, queryDuration, ...})` 取该端点在脚本生成的固定时间窗内的链路；
   - 从返回的 `spans` 直接汇总 `serviceCode`，不需要再调 `queryTrace`。
3. 断言 services 覆盖网关及其下游预期服务，并与 Tempo 链路的服务集合比对。

**BanyanDB 的约束**：`queryBasicTracesByName` 在 BanyanDB 存储上直接抛 `BanyanDB Trace Model changed, please use queryTraces`，必须按 serviceId + endpointId 查。`Duration` 的 start/end 是 `String!`，格式为 `yyyy-MM-dd HHmm`（分钟精度），写秒会报 malformed。

**已知噪声**：Agent 会把 OTel exporter 向 Tempo 的导出请求也埋点，表现为 `/v1/traces`、`Async/v1/traces`、`Callback/onResponse` 端点。按 endpointId 过滤可排除；这类 span 不影响业务链路判定，但会作为「agent 与既有 SDK 共存」的观察结果写入结论。若后续需要更精确的关联，可再把 Agent 的可选日志工具包打开（`%tid`），但本次不作为前置。

替代方案：在网关加几行代码回显 SkyWalking trace id——需要改业务代码，且会削弱「零侵入」的对照意义；否。

### D6：影响度量采用同一套固定负载

- **启动时间**：选 `iboot-system`（依赖最多、启动最慢）为样本，分别在不挂/挂 Agent 下各启动 3 次，记录从进程拉起到 `/actuator/health` 返回 200 的时间，取中位数。
- **常驻内存**：服务稳定 60 秒并完成一轮预热请求后，用 `ps -o rss=` 记录 RSS，3 次取中位数；同时记录 `jcmd GC.heap_info` 的堆使用作为辅助。
- **吞吐**：同一接口、同一并发度（8）、同一时长（30 秒），用 `xargs -P` 驱动 `curl` 循环，统计完成请求数与失败数；两组使用完全相同的命令。本机没有专业压测工具，绝对数值只用于同机对照，不对外声称容量。
- 所有原始数据、命令与样本数写入选型结论文档，保证他人可复现。

### D7：实验产出与路线图收口

- 新增 `docs/24-SkyWalking 与 Tempo 对比实验.md`：架构图、端口、启停命令、验收步骤、度量方法与原始数据、选型结论（各自适合的场景与代价）。
- `docs/11-可观测性设计.md` 增加「辅助 APM 对比栈」小节并链接到 docs/24，明确 Tempo 仍是主链路。
- `ROADMAP.md`：§4 版本矩阵改为 11.0.0 组合；§3 阶段 2 收尾在实验完成后标记该项已完成；§7.7 保持「实验结束若收益不足则下线」的既有结论。
- 是否下线 SkyWalking 由结论决定：若保留，另起变更把辅助栈转正；本次不自动替换 Tempo。

## Risks / Trade-offs

- [Agent 与 OTel SDK 同 JVM 产生噪声或上下文串扰] → 两者在不同层、各自传播 `sw8` 与 `traceparent`，理论上互不干扰；实验中检查 SkyWalking 是否把 OTel exporter 的出站请求记成 span，必要时在 `agent.config` 排除对应插件，并把观察结果写入结论。
- [OAP 11.0.0 与路线图固定的 11.1.0 不一致] → 记录偏离原因；镜像站同步 11.1.0 后可另起变更升级，不影响实验判据。
- [OAP 默认 2GB 堆抬高本机内存] → 显式设为 512m–1GB，并按需启停；R4 已计入 +3 进程。
- [全量采样放大 Agent 开销] → 明确这是上界；如需常规负载数据，可在结论中附一组默认采样（3 条/3 秒）的对照。
- [GraphQL 关联受时间窗口影响] → 脚本固定窗口并在请求前后各留余量；失败时输出 Tempo 与 SkyWalking 两侧的候选链路，便于定位。
- [Agent 对某些框架组合覆盖不全] → 实验本身就是验证这一点。已确认 9.8.0 的 Spring 6/7 服务端插件默认在 `optional-plugins`，由 `fetch-agent.sh` 装配；Lettuce 插件与当前 Reactor 不兼容，停用后记录为缺口。其他缺失的链路段按验收失败处理，并在结论中记录插件覆盖情况。
- [默认 UI 账号弱口令] → 仅映射到本机端口，不暴露公网；文档标注禁止在生产或共享网络使用。

## Migration Plan

1. 拉取三个镜像并下载 Agent；不启动任何服务，现有环境不受影响。
2. 以 `IBOOT_SKYWALKING_ENABLED=false` 验证 `dev-init.sh` 行为与改动前一致。
3. 启动 `apm` profile，以开关打开重启九个服务，执行双后端验收与度量。
4. 回滚：关闭开关重启服务即可回到纯 Tempo；`docker compose --profile apm down -v` 删除辅助栈与存储卷，业务数据与 Tempo 卷不受影响。

## Open Questions

（无阻塞项。Agent 是否需要排除个别插件、默认采样对照是否纳入结论，均在实验过程中按观察结果决定，不改变规格与任务拆分。）
