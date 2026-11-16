# Design

## Context

- 监控栈：Prometheus（`docker/scripts/prometheus/prometheus.yml`）+ MySQL/Redis/ES/Node exporter + Thanos + Tempo + Grafana 13.2.6；日志经 Kafka → Logstash → Elasticsearch，Kibana 查询。
- 看板以声明式文件供给：`docker/scripts/grafana/provisioning/dashboards/vendor`（社区看板）与 `custom`（iBoot 自有看板），Grafana 每 30 秒重新加载；数据源同样由 provisioning 文件供给。
- 后端有两种运行方式：`scripts/dev-init.sh` 在宿主机以进程运行；`docker compose` 全量运行。compose 中 10 个后端模块都发布宿主端口，Prometheus 容器已配置 `host.docker.internal:host-gateway`。
- 当前指标状态（实测）：只有 system/ai/ops 带 `application` 标签；auth/file/job 的 `/actuator/prometheus` 返回 500；notify/workflow/monitor/gateway 有指标端点但无 `application` 标签；Prometheus 只配置了 6 个应用目标，且在宿主机模式下全部 DOWN。
- 指标命名由 Micrometer 决定，点号转下划线，例如 `iboot.ai.chat.requests` → `iboot_ai_chat_requests_total`；既有告警与看板依赖这些名字，不能重命名。

## Goals / Non-Goals

**Goals:**

- 两种运行方式下，Prometheus 都能抓到全部 10 个模块，且每个模块的指标可按统一标签区分。
- 一块看板覆盖全部后端模块，并能在新增模块时自动出现。
- 每个保留的看板都有真实数据来源；没有修复路径的看板被删除。
- 日志面板复用现有 ES 日志链路，不再依赖未部署的 Loki。

**Non-Goals:**

- 不修改告警规则语义，不新增告警。
- 不迁移社区看板到新面板类型，不重做中间件看板。
- 不为每个模块生成独立看板文件。
- 不引入 Loki、Prometheus Agent、远程写入或新的采集代理。
- 不改变业务接口与数据库结构。

## Decisions

### 1. 统一用 `host.docker.internal:<port>` 抓取应用指标

每个模块保留独立 job（`iboot-system`、`iboot-auth` 等），目标统一写 `host.docker.internal:<port>`，`metrics_path` 为 `/actuator/prometheus`，`instance` 重写为稳定的模块名端口。

- 理由：compose 已为全部 10 个模块发布宿主机端口，且 Prometheus 容器已具备 `host.docker.internal` 映射；同一份配置在宿主机模式与容器模式都成立，不需要维护两套目标，也不会产生重复序列。
- 备选：仅保留 compose 服务名 —— 宿主机模式全部 DOWN，且缺 4 个模块；备选：同时配置服务名与宿主目标 —— 两种模式各有一半目标 DOWN，`up` 与重复序列会污染告警和看板；备选：Prometheus 使用 `network_mode: host` —— 依赖 Linux，破坏容器内 exporter 解析，且与项目跨平台部署目标冲突。
- job 名保持不变，既有告警（`up{job="iboot-system"}`、`up{job="iboot-ai"}`）无需改动。

### 2. 指标依赖与标签按模块补齐，不做共享自动配置

- 在 `iboot-auth`、`iboot-file`、`iboot-job` 的 `pom.xml` 增加 `micrometer-registry-prometheus`。
- 在缺失的模块（auth/file/gateway/job/monitor/notify/workflow）`application.yml` 增加：
  - `management.metrics.tags.application: <模块名>`
  - `management.metrics.tags.namespace: default`
- 理由：各模块已各自维护 `management` 配置块，按现有风格追加标签的改动面最小；把注册表依赖放进 `iboot-kernel` 会让 db-init、logging 等非服务模块也获得指标端点，并改变既有依赖图。
- `iboot-system` 既有的 `MetricsConfig` 与 `application.yml` 标签值一致，保持原样，不在本次变更中删除。

### 3. 新增一块参数化「后端服务总览」看板

新增 `custom/iboot-backend-services-dashboard.json`，uid 固定为 `iboot-backend-services`：

- 变量 `service`：`label_values(jvm_info, application)`，多选 + 全选，默认全部；`instance` 可选。
- 行与面板：
  - 服务状态：`up{job=~"iboot-.*"}` 按 job 展示，配合 `process_uptime_seconds` 与 `jvm_info` 的版本标签。
  - 请求量：`sum by (application) (rate(http_server_requests_seconds_count[5m]))`。
  - 错误率：5xx 占比，按 application 聚合。
  - 延迟：P95/P99，`histogram_quantile` + `http_server_requests_seconds_bucket`。
  - JVM：堆/非堆、GC 暂停、线程数、类加载。
  - CPU/进程：`process_cpu_usage`、`system_cpu_usage`、打开文件数。
  - 日志事件：`logback_events_total` 按级别。
  - 连接池：Druid 指标（`db_connection_*`），仅 `iboot-system` 有数据，面板标题注明适用范围。
- 下钻：面板链接指向 JVM (Micrometer) 看板并带入 `application` 变量。
- 理由：一个参数化看板即可覆盖任意数量模块，新增模块只依赖统一的 `application` 标签；为每个模块复制 JSON 会产生 10 份需要同步维护的资产，且模块增减都要改文件。

### 4. 社区看板按「有数据才保留」处置

| 看板 | 处置 | 依据 |
| --- | --- | --- |
| JVM (Micrometer) 4701 | 保留 | 数据链路修复后可用，作为单服务下钻 |
| Spring Boot Observability 17175 | 保留并改日志面板 | 指标面板数据源正确；两块日志面板改用 ES |
| Spring Boot Executors 20806 | 保留 | 实测存在 `executor_*` 指标 |
| Spring Boot 3.x Statistics 19004 | 删除 | 全部面板指向不存在的 uid `W0lFOlOVk`，且与 4701/17175 重叠 |
| Spring Boot Cache (3.x) 21303 | 删除 | 依赖从未上报的 `cache_gets_total` |
| MySQL / Redis / Elasticsearch / Node Exporter | 保留 | exporter 目标均为 UP，指标可查 |

- 理由：用户明确要求无数据看板直接删除；保留重复且损坏的看板会持续误导。
- 备选：修复 19004 的 uid 后保留 —— 与 4701/17175 高度重叠，维护成本大于收益。

### 5. 日志面板改接 Elasticsearch

- 新增 `provisioning/datasources/elasticsearch.yaml`：type `elasticsearch`，url `http://elasticsearch:9200`，uid `iboot-elasticsearch`，索引 `iboot-logs-*`，时间字段 `@timestamp`，消息字段 `message`，级别字段 `level`。
- 17175 的两块 Loki 面板改为 ES 数据源 + 日志面板类型，查询按 `app.keyword` 过滤服务。
- 理由：日志已经在 ES 中，运行日志页面也走同一条链路；Loki 不在 compose 中，继续保留只会产生 No Data。
- 备选：删除日志面板 —— 用户选择复用现有日志链路。

### 6. 自定义看板的指标缺口就地补齐

- 登录指标：auth 暴露 Prometheus 后，`user_login_success_total`、`user_login_failed_total`、`user_online_count` 恢复，业务看板无需改查询。
- 缓存命中/未命中：在既有 `BusinessMetricsService` 记录命中/未命中的方法里增加 `cache_hits_total`、`cache_misses_total` 计数器，保持看板查询不变；命中率面板继续使用 `cache_hit_rate_percent`。
- AI 指标：对标签集合可枚举的核心计数器（对话结果、token 类型、错误原因、智能体结果）在构造时预先注册，使无流量时也返回 0 而不是 No Data；工具名、模型等高基数标签保持按需注册。
- 理由：这些看板是 iBoot 自有资产，修数据源比删看板更符合「结合现有监控手段」。

### 7. 数据源 uid 显式声明

- 给 Prometheus 数据源显式声明 `uid: PBFA97CFB590B2093`（Grafana 当前为同名数据源生成的 uid），避免升级/重建后 uid 漂移导致引用失效。
- 修正 Tempo 数据源的 `serviceMap.datasourceUid` 为上述 Prometheus uid。
- 备选：把所有看板改为数据源变量 —— 改动全部 JSON、回归面大，且社区看板升级时会被覆盖。

## Risks / Trade-offs

- [宿主机模式依赖 `host.docker.internal` 映射] → compose 已配置 `extra_hosts`；在文档中登记该前置条件，缺失时表现为目标 DOWN 而非静默空数据。
- [宿主机进程与容器同时运行会争用端口] → 两种模式本就互斥，不属于本次变更；验证脚本分别执行。
- [Grafana 13 对旧版 Angular 面板的兼容性] → 4701/14057/14191 仍使用旧面板类型，当前可渲染；不迁移，后续升级 Grafana 时再单独评估。
- [显式 uid 与既有自动 uid 冲突] → uid 取当前实际值，仅为固化；Grafana 对同名同 uid 数据源按更新处理。
- [删除 19004/21303 造成用户预期变化] → 在监控文档与看板目录说明中记录删除原因。
- [AI 计数器预注册增加少量序列] → 仅限可枚举标签，基数可控。
- [文件供给的看板会在 30 秒内覆盖 UI 修改] → 在文档中强调看板修改必须落到 JSON 文件。

## Migration Plan

1. 后端：补依赖与标签 → `mvn -s /home/lam/repo/settings.xml -DskipTests package` → 重启受影响服务 → 验证每个端口的 `/actuator/prometheus` 返回 200 且带 `application` 标签。
2. 采集：更新 `prometheus.yml` → 调用 Prometheus `/-/reload` → 确认 10 个应用目标 UP。
3. Grafana：新增 ES 数据源与总览看板，修改 17175 日志面板，修正 Tempo 数据源；删除 19004、21303 文件 → 等待 provisioning 刷新 → 逐块检查看板有数据。
4. 双模式验证：先宿主机模式，再 `docker compose` 全量模式，确认目标与看板一致。
5. 回滚：还原后端依赖/标签并重建、还原 Prometheus 配置并 reload、还原 Grafana provisioning 文件；数据卷无需变更。

## Open Questions

无。
