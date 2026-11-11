# Proposal

## Why

ROADMAP 阶段 2 收尾只剩一项辅助实验：用 SkyWalking（OAP + Horizon UI + Java Agent）与既有 Tempo 并行观测同一批服务、同一批流量，补齐「agent 字节码增强的零侵入埋点」与「现有 SDK 埋点」的横向对比。实验要回答的不是「哪个能装」，而是两者在启动开销、常驻内存、吞吐影响、拓扑发现与方法级慢调用剖析上的实际差异，并产出一页选型结论。

这是**辅助对比实验**，不是迁移：不替换 Tempo、不改变阶段 2 的任何验收判据；实验结束后 SkyWalking 必须能被整体摘除，业务与既有 Tempo 链路不受影响。

## What Changes

- 本地编排新增 `apm` profile，提供三件套：`apache/skywalking-banyandb:0.11.0`、`apache/skywalking-oap-server:11.0.0`、`apache/skywalking-ui:horizon-1.0.0`。三者是官方 Helm 5.0.0 的配套组合；OAP 与 BanyanDB 的 server API 版本精确锁定在 `0.11`。
- 镜像统一从渡渡鸟镜像同步站（`docker.aityp.com`）给出的华为 SWR 中转地址拉取，沿用 `scripts/pull-docker-images.sh` 现有的 pull → retag → rmi 模式，不引入新的拉取机制。
- Java Agent 使用 `apache-skywalking-java-agent-9.8.0.tgz`（9.8.0 才修复 Spring Framework 7 / Boot 4 的入口 span 缺失问题），从 Apache 归档站下载到本地目录并加入 `.gitignore`；它不是 Docker 镜像。
- `scripts/dev-init.sh` 在显式开关打开时给九个业务服务追加 `-javaagent`；开关默认关闭，普通开发启动行为不变。不改任何业务 Java 代码、不改 `pom.xml`、不改各服务现有的 OTLP 配置。
- 新增实验操作与度量资产：启动/停止辅助栈、验证同一请求在 Tempo 与 SkyWalking 两侧都能还原、记录 agent 对启动时间/常驻内存/吞吐的影响、停止 SkyWalking 后验证业务与 Tempo 链路不受影响。
- 文档与路线图：`docs/11-可观测性设计.md` 增加实验章节；新增一页选型结论文档；ROADMAP §4 的 SkyWalking 版本改为 11.0.0（镜像站可获取的配套版本），§7.7 保留「实验结束可下线」的结论。

## Capabilities

### New Capabilities

- `apm-comparison`: 可选、可整体摘除的第二套 APM 观测能力。它定义辅助栈的编排与资源边界、agent 的注入方式、同一请求在双后端的可还原性、对既有追踪链路的中立性，以及实验产出与下线要求。

### Modified Capabilities

（无。既有 `distributed-tracing` 与 `gateway-tracing` 的契约不变：Tempo 仍是主链路，OTLP 配置、采样与 `X-Trace-Id` 行为均不调整。）

## Impact

- 编排：`docker/docker-compose.yml` 新增 `apm` profile 下的 BanyanDB、OAP、Horizon UI 三个服务与 `horizon.yaml` 挂载；三个服务均不加入任何业务服务的 `depends_on`。
- 脚本：`scripts/pull-docker-images.sh` 新增三个镜像；新增 agent 获取与实验启停/验证脚本；`scripts/dev-init.sh` 增加开关控制的 `-javaagent` 注入。
- 配置：`.env.example` 增加 agent 开关与 OAP 地址占位；`.gitignore` 忽略本地 agent 目录。
- 文档：`docs/11-可观测性设计.md`、新增选型结论文档、`README.md` 中间件清单、`ROADMAP.md`（§3 阶段 2 收尾、§4 版本矩阵、§5 R4）。
- 不变：任何业务 Java 代码、`pom.xml`、数据库结构、HTTP API、前端代码、现有 Tempo/OTLP 配置与 `scripts/regress/trace-chain.sh` 的既有行为。
- 资源：本地常驻进程 +3（OAP、UI、BanyanDB），已计入 R4；OAP 堆上限需显式压低，不使用官方默认的 2GB。
