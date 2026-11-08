# Proposal

## Why

ROADMAP「阶段 2 收尾」中，TraceID 贯穿、故障隔离、Nacos 配置热更新与前端零改动四项已达标，但 **Sentinel 业务接入仍停留在 2.0 门禁的 Spike**：网关与业务服务没有任何限流、熔断规则，`api-gateway` 规范里的「入口限流」与「上游不可用快速失败」目前主要靠连接/响应超时兜底，机制不可观测、不可调整。Spike 同时留下明确缺口——**规则持久化到 Nacos 尚未验证**。

现在补齐这件事，才能把「单服务故障不雪崩」从原则变成可操作、可观测、可验收的机制，并为阶段 3 的弹性与容量治理准备统一入口。

## What Changes

- 网关接入 Sentinel Gateway 适配：按**路由**配置 QPS 限流；被限流请求返回可辨识的 `429` JSON，响应体与既有网关错误契约一致；规则调整无需重启。
- `iboot-workflow` 对 `iboot-notify` 的通知登记调用接入**熔断降级**：下游持续失败或慢调用时快速失败，保留既有「通知失败不阻断审批」语义。
- `iboot-notify` 的内部通知端点接入**入口流控**，避免调用方重试风暴压垮通知服务。
- 规则以 **Nacos 为权威来源**，三个接入点启动时从 Nacos Datasource 加载并热更新；本地开发提供幂等脚本写入基线规则。
- **Sentinel Dashboard 为必选组件**：随 `docker/docker-compose.yml` 常驻启动（不使用 profile），`scripts/dev-init.sh` 启动并等待就绪，`--status` 中可见；本地开发关闭登录，生产必须开启认证并由密钥管理注入账号密码。
- Dashboard 上的临时规则修改**不作为持久化来源**：重启或重新拉取 Nacos 规则后回到基线，避免出现「界面改了但仓库/Nacos 没有」的隐性状态；持久化变更统一写 Nacos。
- Sentinel 的 pass / block / exception / RT 指标接入 Micrometer，进入既有 Prometheus 抓取与 Grafana 面板。
- 更新 `docs/23`、`docs/11`、`README` 与 `ROADMAP`，把 Sentinel 从「未接入」移入能力索引，并登记 Dashboard 地址、认证方式与规则维护流程。

## Capabilities

### New Capabilities

- `service-resilience`: 跨服务调用与入口的流控、熔断降级、规则来源与热更新、Sentinel Dashboard 的生命周期与访问边界，以及治理指标的对外契约。

### Modified Capabilities

- `api-gateway`: 「入口限流」从原则性要求细化为路由级、运行期可调整、响应可辨识的行为契约；补充被限流时的失败响应约定。

## Impact

- 构建：`iboot-gateway`、`iboot-workflow`、`iboot-notify` 增加 Sentinel 依赖；视实现需要新增一个非部署的 Sentinel 共享模块（承载 Micrometer 指标扩展），父 POM 的 `<modules>` 相应调整。
- 配置：三个接入点的 `application*.yml`、Nacos 规则 data-id 与 `SENTINEL_GROUP`、`.env.example` 的 Dashboard 账号密码占位。
- 运行时：`docker/docker-compose.yml` 新增常驻 `sentinel-dashboard` 服务；`scripts/dev-init.sh`、`scripts/pull-docker-images.sh`、`scripts/docker-deploy.sh`、Prometheus 抓取配置同步更新；新增规则初始化脚本。
- 可观测性：新增 Sentinel 指标与 Grafana 查询，Dashboard 与 Prometheus 的分工写入文档。
- 不涉及数据库结构、不做前端接口变更；门户不新增 Sentinel 入口。
- 资源：本地常驻进程 +1（Dashboard），计入 ROADMAP R4；Dashboard 仅用于治理观察与临时演练，不替代 Prometheus/Grafana、ELK 或 Tempo。
- 风险：Sentinel 1.8.9 的 Gateway 适配在 Spring Cloud Gateway 5 / Boot 4 上的可用性、Dashboard 容器到宿主机开发进程的反向连通性、Nacos 规则持久化的首次落地，均需在实施中先行验证。
