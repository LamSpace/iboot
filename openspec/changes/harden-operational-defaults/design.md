# Design

## Context

现状与约束见 `proposal.md`。补充三个影响设计的事实：

- 共享文件 `iboot-kernel/src/main/resources/iboot-tls.yml` 给所有服务注入 `${ELASTICSEARCH_USERNAME:elastic}`；`iboot-ai`、`iboot-notify`、`iboot-monitor` 都通过它连接 ES。
- ES 里各服务用途不同：`iboot-ai` 读写 `iboot-kb-chunk-*`，`iboot-notify` 读写 `iboot-content-*`，`iboot-monitor` 只读 `iboot-logs-*`，Logstash 写 `iboot-logs-*`，Grafana 只读 `iboot-logs-*`。
- Neo4j 的 `NEO4J_AUTH` 只在空数据卷首次启动时生效；已有卷不会因环境变量变化而改口令。

## Goals / Non-Goals

**Goals:**

- 业务路径上不再出现 `elastic` 超级用户口令。
- Grafana 匿名访问只读，管理员凭据来自环境。
- 证书过期在 30 天和 7 天两个窗口产生告警，且探针失效可见。
- 明文默认地址的静态审计可重复执行，例外显式登记。

**Non-Goals:**

- 不做 Tempo OTLP/HTTP 的 TLS 化（已知边界，进入允许清单，留待单独变更）。
- 不做服务间 mTLS、RocketMQ 强制 TLS、Seata 认证、Kafka SASL、Redis ACL。
- 不引入 Vault 等外部密钥系统；继续使用 `.env` + 编排环境变量。
- 不改变 Nacos 控制台本地免登录的既有边界。

## Decisions

### 1. Elasticsearch 按用途建账号，一次性任务初始化

新增 `scripts/es-users-init.sh`，由 `es-users-init` 一次性容器在 ES 健康后执行，幂等创建角色与用户：

| 账号 | 用途 | 索引权限 | 集群权限 |
|---|---|---|---|
| `iboot_ai` | AI 知识库读写 | `iboot-kb-chunk-*`: `read`、`write`、`create_index`、`delete`、`view_index_metadata` | `monitor` |
| `iboot_notify` | 通知内容读写 | `iboot-content-*`: 同上 | `monitor` |
| `iboot_monitor` | 运行日志查询 | `iboot-logs-*`: `read`、`view_index_metadata` | `monitor` |
| `iboot_logstash` | 日志写入 | `iboot-logs-*`: `create_index`、`index`、`write`、`view_index_metadata` | `monitor`、`manage_index_templates` |
| `iboot_grafana` | Grafana 日志数据源 | `iboot-logs-*`: `read`、`view_index_metadata` | `monitor` |
| `iboot_exporter` | 指标导出 | `*`: `monitor` | `monitor` |

- 账号口令分别由 `ES_AI_PASSWORD`、`ES_NOTIFY_PASSWORD`、`ES_MONITOR_PASSWORD`、`ES_LOGSTASH_PASSWORD`、`ES_GRAFANA_PASSWORD`、`ES_EXPORTER_PASSWORD` 注入；`elastic` 口令只出现在 `es-users-init`、ES 健康检查与运维脚本。
- 编排在 `x-app-env` 按服务覆盖 `ELASTICSEARCH_USERNAME/PASSWORD`；共享 `iboot-tls.yml` 的默认值从 `elastic` 改为空，避免配置层再提供超级用户回退。
- MySQL 初始化已经采用同一模式（一次性任务 + 非空守卫），这里复用该模式，不新增抽象。

**备选**：给所有服务一个共享读写账号。被否决，因为 AI/通知/监控的索引集合不同，共享账号会把最小权限退化为“一个账号能碰全部业务索引”。

### 2. Neo4j 单一受管口令，已有卷需显式迁移

- 编排改为 `NEO4J_AUTH=neo4j/${NEO4J_PASSWORD:?NEO4J_PASSWORD 必须在 .env 中设置}`；`.env.example` 只保留变量名与 `iboot-dev-neo4j` 这类明显本地占位值。
- `IBOOT_AI_GRAPH_PASSWORD` 由 `NEO4J_PASSWORD` 派生，避免同一口令维护两份。
- 已有 `neo4j_data` 卷保留旧口令，环境变量不会改它。迁移时先停 Neo4j，用 `neo4j-admin dbms set-initial-password` 离线改口令，或删除 graph profile 的数据卷重建；图数据可由业务重新同步，故重建可接受，但必须在文档中写明。

**备选**：社区版建应用级用户。被否决：Neo4j Community 的用户/角色管理能力有限，声称按服务身份隔离会超出实际能力。

### 3. Grafana 匿名 Viewer + 环境注入管理员口令

- 保留 `GF_AUTH_ANONYMOUS_ENABLED=true` 以维持门户内嵌看板，但把 `GF_AUTH_ANONYMOUS_ORG_ROLE` 从 `Admin` 改为 `Viewer`。
- `GF_SECURITY_ADMIN_PASSWORD` 改为 `${GRAFANA_ADMIN_PASSWORD:?...}`；`docker-compose.prod.yml` 不额外覆盖即继承该守卫。
- ES 数据源账号从 `elastic` 改为 `iboot_grafana`。
- Sentinel 本地继续 `auth.enabled=false`；`docker-compose.prod.yml` 已有登录覆盖，保持不变，只在文档与回归中确认。

**风险点**：Grafana 13 的 Explore 对 Viewer 是否可用需要在实施时验证；若匿名 Viewer 打不开 Tempo Explore，退路是给匿名组织授予数据源查询权限但仍不授予 Admin，或在门户侧改用固定只读服务账号，不得回退到匿名 Admin。

### 4. 证书探针用 blackbox exporter，告警放在 Prometheus

- 新增 `prom/blackbox-exporter`（固定版本）常驻容器，加入 `app` 与 `obs` 网络，不发布宿主端口。
- 探针配置用本地 CA 校验 `https://iboot-system:8080/actuator/health`（宿主机模式为 `host.docker.internal:8080`）。所有服务当前共用同一张叶子证书，因此一个探针即可覆盖到期时间；B5 拆分证书后按新证书追加目标。
- 新增 `alerts/tls-alerts.yml`：
  - `TlsCertificateExpiringSoon`：`(probe_ssl_earliest_cert_expiry - time()) / 86400 < 30`，warning。
  - `TlsCertificateExpiringCritical`：同上 `< 7`，critical。
  - `TlsCertificateProbeFailed`：`probe_success == 0`，warning。
- 保留 `tls-init.sh --check` 与既有回归检查，作为部署前的快速失败。

**备选**：自写脚本解析证书并推送到 Pushgateway。被否决：多一个自维护组件，且 blackbox 已覆盖 TLS 握手失败这类真实故障。

### 5. 明文审计用静态扫描 + 显式允许清单

- 新增 `scripts/regress/plaintext-audit.sh`，扫描：
  - `iboot-*/src/main/resources/application*.yml`、`iboot-*/src/main/resources/iboot-*.yml`
  - `docker/scripts/grafana/provisioning/datasources/*.yaml`
- 命中 `http://` 即失败，除非匹配 `scripts/regress/plaintext-allowlist.txt` 中的条目；每条允许项带用途与去向注释。
- 首批允许项：Tempo OTLP 默认地址、Grafana Tempo 数据源、CLI 回环回调。`IBOOT_AI_SILO_ENDPOINT` 的默认值改为 `https://localhost:9000`，不进入允许清单。
- 扫描接入 `regress.sh`，与 TLS 负向检查并列执行，不替代后者。

## Risks / Trade-offs

- **ES 角色权限过窄导致启动或写入失败** → 初始化后立即跑一轮写读回归；角色权限按上表逐项验证，不用 `all` 兜底。
- **Grafana 匿名 Viewer 影响 Explore** → 实施时打开 Tempo 页面实测；不可用时按决策 3 的退路处理，不回退 Admin。
- **已有 Neo4j 卷口令不变** → 文档与迁移脚本明确“改环境变量不改旧卷”，提供离线改口令或重建路径。
- **静态扫描误报** → 允许清单只允许缩小；新增例外在评审中说明原因，避免扫描变成摆设。
- **blackbox 单探针在 B5 拆分证书后失效** → 规格已要求新证书纳入同一套探针；B5 设计时同步扩展目标列表。

## Migration Plan

1. 更新 `.env.example` 与本地 `.env`，补齐 ES 各账号口令、Grafana 管理员口令、Neo4j 新口令。
2. 执行 `scripts/tls-init.sh` 确认证书有效，再重建 ES 相关容器，运行 `es-users-init`。
3. 重建 Grafana、Neo4j（必要时先离线改口令）与业务服务；门户逐一验证看板、Tempo、运行日志。
4. 启动 blackbox exporter，确认 Prometheus 出现 `probe_ssl_earliest_cert_expiry`，并手工把阈值改到未来时间验证告警能触发后还原。
5. 回滚：恢复上一版 compose、`.env` 与 Grafana 数据源；ES 旧账号 `elastic` 仍保留，回滚不需要改 ES 数据。Neo4j 回滚需保留旧卷口令或一并回滚环境变量。

## Open Questions

无。
