# Tasks

## 1. 后端指标契约

- [x] 1.1 为 `iboot-auth`、`iboot-file`、`iboot-job` 的 `pom.xml` 增加 `micrometer-registry-prometheus` 依赖，并验证 `mvn -s /home/lam/repo/settings.xml -DskipTests package` 构建通过
- [x] 1.2 在 auth / file / gateway / job / monitor / notify / workflow 的 `application.yml` 增加 `management.metrics.tags.application`（值为各模块 `spring.application.name`）与 `management.metrics.tags.namespace: default`，并验证每个模块启动后 `/actuator/prometheus` 返回 200
- [x] 1.3 重建并重启 10 个后端模块，验证每个端口的 `jvm_info` 都带 `application` 标签且值等于模块名
- [x] 1.4 更新 `docs/11-可观测性设计.md` 与 `docs/10-后端配置详解.md` 的指标契约说明（端点、标签、新增模块要求），并验证文档中的命令与当前实现一致

## 2. Prometheus 采集配置

- [x] 2.1 将 `docker/scripts/prometheus/prometheus.yml` 的应用目标改为 10 个 `host.docker.internal:<port>` 目标，保留既有 job 名与 `metrics_path`，并用 `promtool check config` 验证语法
- [x] 2.2 重载 Prometheus 后验证 10 个应用目标全部 UP，且 `jvm_info`、`http_server_requests_seconds_count` 能按 application 查询到样本
- [x] 2.3 在 `docs/11-可观测性设计.md` 记录宿主机/容器双模式采集方式与 `host.docker.internal` 前置条件，并按文档步骤复核宿主机模式

## 3. 后端服务总览看板

- [x] 3.1 新增 `docker/scripts/grafana/provisioning/dashboards/custom/iboot-backend-services-dashboard.json`（uid `iboot-backend-services`），包含服务多选变量、健康状态、请求量、错误率、P95/P99、JVM、CPU/进程、日志事件与 Druid 连接池面板，并验证 Grafana 能加载且变量可选
- [x] 3.2 在运行环境中验证总览看板列出全部 10 个模块、每个面板有数据，并验证下钻链接能带 `application` 打开 JVM (Micrometer) 看板
- [x] 3.3 在 `docs/11-可观测性设计.md` 增加看板目录与新增模块的接入步骤，并按文档步骤模拟新增一个服务变量取值

## 4. 看板清理与数据源修复

- [x] 4.1 删除 `19004_rev1_fixed.json`（Spring Boot 3.x Statistics）与 `21303_rev1_fixed.json`（Spring Boot Cache），验证 Grafana 刷新后两者不再出现在看板列表
- [x] 4.2 新增 `provisioning/datasources/elasticsearch.yaml`（uid `iboot-elasticsearch`，索引 `iboot-logs-*`，时间字段 `@timestamp`），验证 Grafana 数据源健康检查通过
- [x] 4.3 将 `17175_rev2_fixed.json` 的两块 Loki 日志面板改为 Elasticsearch 查询并按 `app.keyword` 过滤服务，验证面板能查到当前服务的日志
- [x] 4.4 为 Prometheus 数据源显式声明 uid `PBFA97CFB590B2093`，修正 Tempo 数据源的 `serviceMap.datasourceUid`，验证 4701/17175 面板与 Tempo service map 引用均可解析
- [x] 4.5 在 `BusinessMetricsService` 增加 `cache_hits_total`、`cache_misses_total` 计数器，验证缓存看板的命中/未命中面板有数据，命中率面板保持可用
- [x] 4.6 为 `AiMetrics` 中标签集合可枚举的核心计数器在启动时预注册，验证 AI 指标看板在无流量时显示 0 而非 No Data
- [x] 4.7 在 `docs/11-可观测性设计.md` 记录看板保留/删除清单与理由，并验证文档与 provisioning 目录一致

## 5. 双模式集成验证

- [x] 5.1 在宿主机开发模式（`scripts/dev-init.sh --frontend`）下验证 10 个采集目标 UP、总览看板与保留下来的看板均有数据、删除的看板不出现
- [x] 5.2 在 compose 全量模式下重复 5.1 的验证，确认两种模式数据语义一致
- [x] 5.3 执行 `mvn -s /home/lam/repo/settings.xml -DskipTests package` 与 `cnpm run build`（`iboot-portal`），确认本次后端改动未破坏构建

## Workflow follow-up

- 变更验收通过后按项目流程归档该 OpenSpec change。
