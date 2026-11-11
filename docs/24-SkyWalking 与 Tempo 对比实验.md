# 24-SkyWalking 与 Tempo 对比实验

> 阶段 2 收尾的辅助实验，不替换 Tempo、不改变任何既有追踪契约。
> 操作入口见 [docs/11 §11.13](<./11-可观测性设计.md>)，排障见 [docs/23 §9.7](<./23-微服务底座操作指南.md>)。

## 24.1 实验目标

在同一批服务、同一批流量上并行运行两套追踪后端，比较：

- **agent 字节码增强的零侵入自动埋点**（SkyWalking Java Agent）与**现有 SDK 埋点**（`spring-boot-starter-opentelemetry`）；
- 对启动时间、常驻内存、吞吐的实际影响；
- 服务拓扑发现、方法级慢调用剖析、跨服务链路还原能力；
- 两者与 Spring Boot 4 / Spring Framework 7 / 虚拟线程的兼容性。

## 24.2 组件与版本

| 组件 | 版本 | 说明 |
|---|---|---|
| OAP | `apache/skywalking-oap-server:11.0.0` | 镜像站可获取的官方配套版本 |
| Horizon UI | `apache/skywalking-ui:horizon-1.0.0` | 11.x 起 UI 使用独立版本号 |
| BanyanDB | `apache/skywalking-banyandb:0.11.0` | OAP 11 只接受 server API `0.11` |
| Java Agent | `apache-skywalking-java-agent-9.8.0` | 9.8.0 才修复 Spring Framework 7 / Boot 4 入口 span 问题 |
| Tempo | `grafana/tempo:2.10.8` | 既有主链路，未改动 |
| 应用侧 | `spring-boot-starter-opentelemetry` | 既有 SDK 埋点，未改动 |

## 24.3 启动与验收

```bash
# 1. 镜像与 Agent
scripts/pull-docker-images.sh          # 已包含三个 SkyWalking 镜像
scripts/apm/fetch-agent.sh             # 下载 9.8.0 并装配 Spring 6/7 插件

# 2. 辅助栈
docker compose -f docker/docker-compose.yml --profile apm up -d --wait

# 3. 九个业务服务（宿主机）挂 Agent
IBOOT_SKYWALKING_ENABLED=true scripts/dev-init.sh

# 4. 双后端链路验收
scripts/apm/verify.sh

# 5. 影响度量（会重启 iboot-system 多轮）
scripts/apm/measure.sh
```

## 24.4 双后端验收结果

`scripts/apm/verify.sh` 以一次经网关的 `/api/user/profile` 请求为样本：

- Tempo 侧：按响应头 `X-Trace-Id` 查询，链路包含 `iboot-gateway` 与 `iboot-system`，共 10 个 span。
- SkyWalking 侧：通过 OAP GraphQL `findService → findEndpoint → queryTraces` 查询同一入口端点的链路，服务集合同样为 `iboot-gateway, iboot-system`。
- 两侧均能定位到具体接口与各段耗时；脚本以非零退出码报告缺失的服务。

结论：**同一请求在两侧都能还原为完整链路**，满足实验判据。

## 24.5 影响度量

### 方法

- 样本：`iboot-system`（依赖最多、启动最慢的服务）。
- 对照组：不挂 Agent；实验组：挂 Agent，全量采样（`sample_n_per_3_secs=-1`），代表开销上界。
- 启动时间：从 `java` 进程拉起到 `/actuator/health` 返回 200，每组 3 次，共两轮。
- 常驻内存：启动后稳定 10 秒，记录 `ps` RSS 与 `jcmd GC.heap_info` 的已用堆。
- 吞吐：经网关请求 `/api/user/profile`，并发 8，每并发 100 次（共 800 次），统计完成请求数与成功数。
- 环境：MySQL/Redis/Nacos/Seata/Tempo 与 OAP/BanyanDB 运行，其余中间件与业务服务停止，避免内存争用影响数字。

### 两轮原始数据

**第一轮**

| 组别 | 轮次 | 启动(ms) | RSS(MB) | 堆使用(MB) | 吞吐(请求/秒) | 成功/总数 |
|---|---|---|---|---|---|---|
| no-agent | 1 | 16020 | 688 | 215 | 68.6 | 794/800 |
| no-agent | 2 | 16204 | 666 | 192 | 72.2 | 792/800 |
| no-agent | 3 | 15461 | 676 | 137 | 53.2 | 782/800 |
| agent | 1 | 28953 | 1146 | 388 | 46.9 | 750/800 |
| agent | 2 | 29484 | 1157 | 145 | 50.7 | 731/800 |
| agent | 3 | 30370 | 1117 | 197 | 45.4 | 772/800 |

**第二轮**

| 组别 | 轮次 | 启动(ms) | RSS(MB) | 堆使用(MB) | 吞吐(请求/秒) | 成功/总数 |
|---|---|---|---|---|---|---|
| no-agent | 1 | 18696 | 676 | 123 | 61.9 | 788/800 |
| no-agent | 2 | 15041 | 665 | 168 | 57.1 | 792/800 |
| no-agent | 3 | 15532 | 687 | 189 | 60.4 | 795/800 |
| agent | 1 | 31170 | 1147 | 324 | 48.8 | 786/800 |
| agent | 2 | 31750 | 1161 | 368 | 51.7 | 766/800 |
| agent | 3 | 29002 | 1150 | 374 | 56.0 | 764/800 |

### 汇总（两轮共 6 个样本的中位数）

| 指标 | 不挂 Agent | 挂 Agent | 变化 |
|---|---|---|---|
| 启动到健康 | 15.8 s | 29.9 s | **+14.2 s（+90%）** |
| 常驻 RSS | 676 MB | 1 149 MB | **+473 MB（+70%）** |
| 吞吐（中位数） | 61.2 请求/秒 | 49.8 请求/秒 | **−18.6%** |
| 请求成功率 | 98.8% | 95.2% | −3.6 个百分点 |

说明：

- 堆使用量在两组、各轮之间波动很大（123–388 MB），不适合作为结论依据；RSS 稳定得多，且包含 Agent 自身的类加载、缓冲区与线程开销。
- 吞吐的轮间波动明显（不挂 Agent 53.2–72.2，挂 Agent 45.4–56.0），因此只把中位数作为量级参考，不对单次数字下结论。
- 负载中出现少量非 200（主要是 401，非 5xx），并发下两组的成功率差异真实存在，但本次没有进一步归因到会话并发或 Agent 排队；不影响「Agent 有可测开销」的判断。
- 按九个业务服务估算，仅 RSS 一项就会增加约 **4.3 GB**；再叠加 OAP/UI/BanyanDB 三个常驻容器（OAP 已压到 1 GB 上限）。这是本地保留 SkyWalking 的主要成本。

## 24.6 兼容性与观察结果

这部分是本实验最有价值的产出，按严重程度排列：

1. **Spring 6/7 服务端插件默认不加载**。9.8.0 把 `apm-springmvc-annotation-6.x`、`apm-spring-webflux-6.x`、`apm-spring-cloud-gateway-4.x` 放在 `optional-plugins/`，不装配时只能看到 Agent 自身导出器调用 Tempo 的 span，没有任何业务入口链路。`fetch-agent.sh` 已按项目技术栈启用这三个插件。
2. **Lettuce 插件与当前 Reactor 不兼容**。当前 Lettuce/Reactor 已移除 `Mono#subscriberContext`，`RedisReactiveCommandsInstrumentationV5` 的 witness 不成立，但 `TracedCommand` 的 complete/cancel 拦截器仍被调用，持续抛 `NullPointerException`（实测 5 分钟内写入约 2MB Agent 日志）。已把 `apm-lettuce-*` 移入 `expired-plugins/`，代价是失去 Redis 客户端 span。
3. **导出器自身请求被埋点**。Agent 会把 OTel exporter 向 Tempo 的 POST 也记成 span（`/v1/traces`、`Async/v1/traces`、`Callback/onResponse`）。这是两套埋点共存时的噪声，不是业务链路；验收脚本按 `endpointId` 过滤排除。
4. **BanyanDB 的查询模型与 ES 不同**。`queryBasicTracesByName` 在 BanyanDB 上直接报错，必须使用 `queryTraces(serviceId, endpointId)`；时间窗格式是 `yyyy-MM-dd HHmm`（分钟精度）。
5. **方法级剖析的边界**。Agent 自动覆盖框架层（WebFlux/MVC、JDBC、HTTP Client 等）；业务 Service 方法不在默认埋点范围内，需要 `apm-toolkit-trace` + `@Trace` 才会有方法级 span。SDK 侧则可以通过 Micrometer Observation 在业务代码里显式埋点。两者不是同一个层次的「方法级」。

## 24.7 选型结论

**本次不替换 Tempo，SkyWalking 保持为可选的辅助对比栈。** 依据分三层：

1. **功能层面两者都能完成跨服务链路还原**。同一请求在 Tempo 与 SkyWalking 两侧都能还原出 `iboot-gateway → iboot-system` 的完整链路，验收脚本可重复执行。这是 SkyWalking 的主要价值：agent 零侵入、自动发现服务拓扑、不需要在每个服务里写埋点。
2. **兼容性成本高于预期，且集中在 Spring Boot 4 / Spring 7 这条线上**。Spring 6/7 服务端插件默认不加载，必须手动从 `optional-plugins` 装配；Lettuce 插件与当前 Reactor 不兼容并持续抛 NPE；OTel exporter 自身的出站请求会被 agent 埋点，形成噪声。这些问题不是配置错误，而是 agent 对最新框架的适配尚未完全跟上，每次升级 Spring 都要重新验证插件矩阵。
3. **既有 Tempo + OTel SDK 已经覆盖项目当前需求**，且与 Grafana、Prometheus、ELK 在同一观测面内；替换它需要证明净收益，而本次已确认的收益（零侵入、拓扑）不足以抵消额外的 3 个常驻组件、插件兼容性维护和双套埋点的噪声。

**两者各自适合的场景：**

| 场景 | 更适合 |
|---|---|
| 不想改代码、快速接入存量系统，需要自动拓扑 | SkyWalking agent |
| 需要业务方法级埋点、与现有 Micrometer 指标同一套观测模型 | OTel SDK（当前方案） |
| 需要把 agent 与 SDK 埋点做横向比较、验证框架兼容性 | 两套并行（本实验） |
| 本地资源紧张、希望少维护有状态组件 | Tempo（单容器） |
| 生产级 APM 需要拓扑、告警、服务列表一体化 | 云上 ARMS 等托管方案（阶段 3 再评估） |

**定量结论支持上述判断**：以 `iboot-system` 为样本，Agent 使启动到健康从 15.8 s 增至 29.9 s（+90%），常驻 RSS 从 676 MB 增至 1 149 MB（+70%），吞吐中位数从 61.2 降至 49.8 请求/秒（−18.6%），成功率从 98.8% 降到 95.2%。按九个业务服务估算，仅 RSS 就多约 4.3 GB，尚未计入 OAP/UI/BanyanDB 三个容器。对一个本地 30 GB 内存的开发机来说，这个代价与「Tempo 已经够用」的现值不匹配。

因此最终结论是：**Tempo 保持主链路，SkyWalking 保留为可选、可整体摘除的对比栈，不转正。** 如果未来出现「需要零侵入拓扑发现」的明确需求，再按本实验的数据重新评估；届时优先考虑阶段 3 的云上托管 APM（ARMS 等），而不是在本地长期维护 OAP + BanyanDB。

## 24.8 下线与保留

本次实验不自动替换 Tempo。无论结论如何：

- 关闭 `IBOOT_SKYWALKING_ENABLED` 后重启服务即回到纯 Tempo；实测全栈十个服务不带 Agent 启动，119 秒全部健康，进程命令中没有 `-javaagent`，也没有任何 SkyWalking 连接尝试；
- `docker compose -f docker/docker-compose.yml --profile apm down -v` 删除辅助栈与存储卷；
- 若决定长期保留 SkyWalking，另起变更把辅助栈转正并更新能力索引，不在本实验内自动扩大范围。
