# Proposal

## Why

阶段 0 与阶段 1 已把功能补齐，`iboot-admin` 现在的体量是 433 个 Java 文件、25 个 Controller，阶段 2 要按限界上下文拆成 8 个服务。但拆分前的三个前提都不存在：portal 直连 `8080` / `8081`，没有统一入口；没有服务注册与配置中心；没有贯穿全链路的 TraceID。同时现有 119 个测试里只有 4 个 `@SpringBootTest`，没有接口契约快照与权限矩阵回归——**以这样的底子开拆，拆坏了既发现不了，也排查不了**。

现在具备开工条件：`iboot-ai` 已经是独立进程 + 独立 schema（`iboot_ai`）+ 独立 Flyway，并已用 `PermissionScopeClient` / `AdminApiClient` 以调用者 JWT 跨服务访问 admin，拆分方法论有现成样板；Spring Cloud Alibaba `2025.1.0.0` 已发布，其 BOM 钉住 nacos-client 3.1.1 / sentinel 1.8.9 / seata 2.5.0 / rocketmq 5.3.1；对应镜像已全部拉取到本地并写入 `scripts/pull-docker-images.sh`。

本变更只交付**拆分底座与回归基线**，不拆任何业务服务。

## What Changes

- **引入 Nacos 3.1.1**：注册中心 + 配置中心。配置接入统一使用 `spring.config.import`，不引入 `bootstrap.yml`（R9）。`docker/docker-compose.yml` 新增 `nacos` 服务，置于 `platform` profile，控制台宿主机端口映射到 `8849` 以避开 `iboot-admin` 的 `8080`（Nacos 3.x 控制台已与主端口分离且去掉 `/nacos` 前缀）。
- **新增 `iboot-gateway` 模块**：Spring Cloud Gateway，承接 `/api/**` 与 `/api/ai/**` 两条路由，作为统一 CORS、限流入口与后续鉴权前置的落点。`iboot-ai` 现有的 `/api/ai/**` 路径约定保持不变。
- **不新建 `iboot-common`**：本变更里唯一的潜在消费者是网关，而网关的错误响应格式是它自己的职责，抽公共模块属于为想象中的复用付房租。共享内核推迟到 `add-auth-center`——那时 admin、ai、auth 三者才真正需要同一套 `Result` / `BusinessException` / resource-server 配置。
- **`iboot-admin` 与 `iboot-ai` 注册进 Nacos**：两者继续独立启动，服务名分别为 `iboot-admin` 与 `iboot-ai`；网关按 `lb://` 转发。
- **链路追踪**：两个服务接入 Micrometer Tracing + OTLP 导出，新增 `tempo` 服务（`grafana/tempo:2.10.8`），由既有 Grafana 作为查询界面。`docker-compose.yml` 当前没有任何 tracing backend，这是拆分后排障的硬前提（R10）。
- **回归基线工具**：新增 `scripts/regress/`，在单体状态下捕获并固化接口快照（Springdoc 导出）、路由覆盖检查、关键链路冒烟、角色 × 端点权限矩阵。后续每次服务外迁都用它 diff。
- **2.0 门禁 Spike**：两个最小 Boot 4.0.3 应用验证「互相发现 + 配置热更新 + Sentinel 限流生效」与「跨两跳同一条 TraceID」，结论沉淀为 `doc/spike/microservice-2.0-spike-report.md`。
- **portal 仅调整代理目标**：`vite.config.ts` 与 `nginx.conf` 的 target 改指网关，**不改任何业务代码**。验收判据「前端零改动」在这一步建立常驻证据。

明确不做（非目标）：

- **不拆任何业务服务**：`file` / `notify` / `job` / `monitor` / `workflow` / `system` 的外迁各独立成 change，本变更只交付底座。
- **不做认证中心**：`iboot-auth` 与 OAuth2 授权码在 `add-auth-center` 中交付；本变更期间 admin 继续自行签发与校验 JWT，网关不做鉴权拦截，只做转发。
- **不引入 Seata / RocketMQ**：分布式事务与业务事件等到有真实跨服务写与削峰需求时再引入（2.4）。
- **不改任何 `/api/**` 路径、不改响应结构**：这是「前端零改动」与回归快照零 diff 的前提。
- **不上 OpenTelemetry Collector**：应用直连 Tempo 的 OTLP 端口即可；Collector 镜像已备好，等需要分流指标/日志时再启用。

## Capabilities

### New Capabilities

- `service-discovery`: 服务注册与发现、配置中心接入与热更新、去除 `bootstrap.yml` 旧式接入的约束。
- `api-gateway`: 统一入口与路由表、CORS、限流入口、对 SSE 长连接与 `?t=token` 鉴权约定的透传约束。
- `distributed-tracing`: 跨服务的 TraceID 传播与导出、追踪后端就位、排障可用性判据。
- `api-contract-regression`: 接口快照零漂移检查、路由覆盖检查、关键链路冒烟与权限矩阵回归。

### Modified Capabilities

（无。既有 `ai-chat` / `notification` / `knowledge-graph` 等能力的行为要求不变：网关只做转发，不改路径、不改响应结构、不改 SSE 语义。portal 的调用路径在源码层面完全不变。）

## Impact

**构建**

- 根 `pom.xml` 新增 `iboot-gateway` module，并在 `dependencyManagement` 引入 `spring-cloud-dependencies` 与 `spring-cloud-alibaba-dependencies:2025.1.0.0`。
- `iboot-admin` / `iboot-ai` 新增 nacos-discovery、nacos-config、micrometer-tracing、OTLP exporter 依赖。
- 明确锁定 Boot `4.0.x`，不随 Spring Cloud 2025.1.2+ 升级到 Boot 4.1（SCA 适配仍在进行）。

**运行时**

- `docker/docker-compose.yml` 新增 `nacos`、`tempo`，置于 `platform` profile；需要同步更新 `scripts/pull-docker-images.sh`（已完成）与本机端口规划。
- `iboot-admin` 启动依赖 Nacos 可用；需要提供「Nacos 不可用时仍可启动」的降级路径，避免本地开发被中间件阻塞。

**前端**

- 仅 `iboot-portal/vite.config.ts` 与 `iboot-portal/nginx.conf` 的代理目标地址变化，无 Vue / TS 代码改动。

**测试与质量**

- 新增 `scripts/regress/`；CI/本地验证顺序变为「单元 → 集成 → 契约快照 → 路由覆盖 → 冒烟」。
- 现有 119 个测试必须在改造前后全绿；本变更不触碰 admin / ai 的业务代码，测试文件预期零改动。

**风险**

- SCA `2025.1.0.0` 是首个支持 Boot 4.0.x 的版本线，starter 自动配置与 Boot 4 条件注解可能存在未覆盖组合——这正是 2.0-A 门禁先行的原因，门禁不过则不进入 2.1。
- 本机内存：新增 Nacos（0.5–1G）+ Tempo（0.3G），叠加既有 19 个中间件容器；靠 `platform` profile 与容器内存上限控制（R4）。
