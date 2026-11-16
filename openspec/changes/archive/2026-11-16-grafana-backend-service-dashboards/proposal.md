# Proposal

## Why

后端已从单体拆分为 10 个可部署模块，但 Grafana 看板仍停留在拆分前的假设上：应用指标要么抓不到、要么抓不全，导致「JVM (Micrometer)」「Spring Boot 3.x Statistics」等看板整块 No Data；同时没有一块能横向看到全部后端模块的看板。用户在实际环境中已经观察到这一现象，需要让监控看板与当前监控栈、当前模块划分保持一致。

目前已确认的具体原因（来自运行中的环境）：

- Prometheus 只抓取 6 个服务，且目标写成 compose 服务名；后端在宿主机运行时全部 DOWN，全量容器运行时 auth/file/job/monitor 也从未被采集。
- `iboot-auth`、`iboot-file`、`iboot-job` 缺少 `micrometer-registry-prometheus`，`/actuator/prometheus` 直接 500；登录指标已迁到 auth，业务看板的登录面板因此必然 No Data。
- 只有 system / ai / ops 配置了 `application` 标签，其余模块的指标无法按服务区分。
- 「Spring Boot 3.x Statistics」所有面板指向不存在的 datasource uid；「Spring Boot Observability」的日志面板依赖未部署的 Loki；「Spring Boot Cache (3.x)」依赖从未上报的 `cache_gets_total`。
- 自定义业务/缓存看板引用了未上报的 `cache_hits_total`、`cache_misses_total`，登录指标也因 auth 未暴露而缺失。

## What Changes

- **统一后端指标契约**：为 auth / file / job 补齐 `micrometer-registry-prometheus` 依赖；为全部 10 个模块补齐 `application` 与 `namespace` 指标标签，值取自 `spring.application.name`。
- **Prometheus 抓取覆盖全部模块与两种运行方式**：统一通过 `host.docker.internal:<port>` 抓取 10 个服务的 `/actuator/prometheus`，宿主机开发与 compose 全量部署都能采到数据；保留既有 job 名，避免告警规则失效。
- **新增一块「iBoot 后端服务总览」Grafana 看板**：以服务为维度展示存活状态、请求量、错误率、P95 延迟、JVM 堆/GC/线程、CPU/进程与日志级别事件；支持按服务多选，并可下钻到社区 JVM 看板。
- **按「有数据才保留」清理社区看板**：删除无数据来源的 Spring Boot 3.x Statistics 与 Spring Boot Cache；保留 JVM (Micrometer)、Spring Boot Observability、Spring Boot Executors 及 MySQL/Redis/Elasticsearch/Node 中间件看板。
- **修复保留下来的看板**：Spring Boot Observability 的两块 Loki 日志面板改用现有 Elasticsearch 数据源查询 `iboot-logs-*`；修正 Tempo 数据源 service map 指向的 Prometheus uid。
- **补齐自定义看板的指标来源**：缓存看板增加命中/未命中计数器或改用现有命中率指标；AI 看板在启动时注册固定标签的计数器，避免首次使用前整块 No Data；业务看板在 auth 暴露指标后恢复登录面板。
- **文档更新**：监控组件说明、看板目录、宿主机与容器两种模式下的采集方式。

## Capabilities

### New Capabilities

- `grafana-dashboards`: 规定 Grafana 数据源与看板的供给契约：每个预置看板必须有当前监控栈可提供的数据来源；后端服务总览看板必须覆盖全部可部署模块；新增模块时看板与采集配置的扩展方式；无数据看板的清理规则。

### Modified Capabilities

- `service-runtime`: 增加「运行期指标暴露」要求：每个可部署服务都暴露 Prometheus 指标端点，`application` 标签等于应用名，指标端点可用性不因缺少依赖而退化为 404/500。

## Impact

- 后端：`iboot-auth`、`iboot-file`、`iboot-job` 的 `pom.xml`；全部 10 个模块的 `application.yml`（`management.metrics.tags`）；`iboot-system` 的业务/缓存指标；`iboot-ai` 的指标注册时机。
- 监控栈：`docker/scripts/prometheus/prometheus.yml`；`docker/scripts/grafana/provisioning/datasources/*.yaml`；`docker/scripts/grafana/provisioning/dashboards/vendor/*.json` 与 `custom/*.json`。
- 部署与运行：不影响应用启动参数与端口；宿主机模式依赖 compose 中已存在的 `host.docker.internal` 映射。
- 告警：保持既有 job 名与指标名不变，不修改告警规则。
- 文档：`docs/` 中监控、快速开始与部署相关章节。
- 测试：Prometheus targets 全 UP、两种运行方式下看板有数据、看板 JSON 可被 Grafana 正常加载、删除项不再出现。
