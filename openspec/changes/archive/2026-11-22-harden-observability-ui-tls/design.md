# Design

## Context

五个可观测组件都监听宿主机回环地址，但当前只提供 HTTP。共享叶子证书包含 `localhost`、`prometheus`、`alertmanager`、`grafana`、`thanos-query`、`kibana` 等服务名 SAN；各容器已具备挂载 `/certs` 的路径约定。Grafana 数据源、Thanos sidecar 与 Prometheus 告警配置目前都指向 HTTP 地址。

## Goals / Non-Goals

**Goals:**

- 五个控制台的 HTTP 入口只提供 TLS，明文不再返回页面或查询结果。
- Grafana→Prometheus/Thanos、Thanos sidecar→Prometheus、Prometheus→Alertmanager 的链路补齐 TLS 与本地 CA 校验。
- 数据库中的控制台地址迁移为 HTTPS，门户嵌入不产生混合内容。

**Non-Goals:**

- 不启用客户端证书认证（mTLS），仍为服务端 TLS；工作负载身份属于 B5。
- 不改变 exporter 抓取端点、Thanos gRPC 端点与 Sentinel Dashboard 的回环 HTTP 入口。
- 不调整证书生成脚本。

## Decisions

### Prometheus/Alertmanager 使用 `--web.config.file`

两个组件都支持 Prometheus Exporter Toolkit 格式的 Web 配置：

```yaml
tls_server_config:
  cert_file: /certs/portal.pem
  key_file: /certs/portal-key.pem
```

新增 `docker/scripts/prometheus/web.yml` 与 `docker/scripts/alertmanager/web.yml`，分别通过 `--web.config.file` 加载。Prometheus 的自身抓取与 Alertmanager 目标改为 `https://`，并配置 `tls_config.ca_file: /certs/rootCA.pem`。

### Thanos Query 使用 `--http.config`

Thanos 的 `--http.config` 使用同一 TLS 服务端配置格式。只对 Query 的 HTTP 查询端口启用 TLS；Query/Sidecar/Store 之间的 gRPC 端口不发布，保持内部明文，登记为边界。Sidecar 查询 Prometheus 改为 `--prometheus.url=https://prometheus:9090`，并新增 `--prometheus.http-client-file` 指向含 `ca_file` 的客户端配置。

### Grafana 原生 HTTPS 与数据源 CA

`grafana.ini` 的 `[server]` 增加 `protocol = https`、`cert_file`、`cert_key`，`GF_SERVER_ROOT_URL` 改为 `https://localhost:3000`。Grafana 数据源沿用 Elasticsearch 的写法：`jsonData.tlsAuthWithCACert: true` 与 `secureJsonData.tlsCACert`。

### Kibana 服务端 SSL

Kibana 通过 `SERVER_SSL_ENABLED=true`、`SERVER_SSL_CERTIFICATE`、`SERVER_SSL_KEY` 启用 HTTPS；到 Elasticsearch 的连接保持现有 HTTPS + CA。

### 控制台地址由迁移更新

新增 `V16__observability_https_urls.sql`，把 `sys.prometheus.monitor.url`、`sys.grafana.monitor.url`、`sys.kibana.monitor.url`、`sys.alertmanager.monitor.url`、`sys.thanos.query.url` 更新为 `https://`。门户视图读取同一批配置键，无需改动前端代码。

## Risks / Trade-offs

- **Prometheus 自身抓取改为 HTTPS 后 target 证书校验失败** → target 指向 `localhost:9090` 并配置本地 CA；容器内证书 SAN 包含 `localhost`。
- **Thanos sidecar 无法连接 Prometheus** → 显式提供 `--prometheus.http-client.config` 与 CA；启动后检查 sidecar 日志与 Query 数据。
- **Grafana 已有数据卷保留旧 root_url** → `GF_SERVER_ROOT_URL` 覆盖 ini；重建容器后验证登录页与 iframe。
- **Kibana 启动时间较长** → 回归为 HTTPS 检查设置重试，不把启动窗口判为失败。
- **Sentinel 仍为 HTTP** → 在文档与规格中登记为回环边界，不宣称全控制台 TLS。

## Migration Plan

1. 更新 Compose、Web TLS 配置、Grafana/Kibana 配置与数据源。
2. 新增并执行数据库迁移，更新控制台 URL。
3. 重建可观测组件与门户；确认 Grafana/Kibana 嵌入无混合内容。
4. 回归：五个控制台 HTTPS、明文拒绝、Grafana 数据源健康、Thanos sidecar 正常、明文审计仅剩 CLI。
5. 回滚：恢复旧 Compose/配置与迁移前的 URL，重建相关容器；不存在运行时明文回退开关。

## Open Questions

无。
