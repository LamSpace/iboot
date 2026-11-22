# Proposal

## Why

可观测组件仍以便于访问为由使用 HTTP：Grafana 3000、Prometheus 9090、Alertmanager 9093、Thanos Query 19192、Kibana 5601 都只在回环地址监听，但查询结果、看板会话与管理员凭据在明文链路上传输。Grafana 到 Prometheus/Thanos 的两条明文数据源链路也是 `plaintext-allowlist.txt` 里最后一批未处理的内部地址。

## What Changes

- Prometheus、Alertmanager、Thanos Query、Grafana、Kibana 的 HTTP 服务加载本地叶子证书，只提供 HTTPS；明文请求不再获得响应。
- Grafana 的 Prometheus/Thanos 数据源改为 `https://` 并用本地 CA 校验；Thanos sidecar 查询 Prometheus、Prometheus 通知 Alertmanager 同样改用 HTTPS。
- 数据库 `sys_config` 中的 Prometheus/Grafana/Kibana/Alertmanager/Thanos 控制台地址迁移为 `https://`。
- 回归新增可观测控制台 TLS 正向与明文负向检查；`plaintext-allowlist.txt` 收缩到只剩 CLI 回环回调。
- **不改动**：Sentinel Dashboard 仍为回环 HTTP（未挂载证书，作为已登记边界）；exporter 抓取端点仍为 HTTP；不启用客户端证书（B5）。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`: 新增“可观测查询与控制台只提供 TLS”的要求，并将明文允许清单收缩到 CLI 回环回调。

## Impact

- 编排与配置：`docker/docker-compose.yml`、Prometheus/Alertmanager/Thanos 的 Web TLS 配置、`grafana.ini`、Grafana 数据源、Kibana 环境变量。
- 数据库：新增迁移更新 `sys_config` 的五个控制台 URL。
- 脚本：`scripts/regress/tls-cert-monitor.sh`、新增 `observability-tls.sh`、`regress.sh`、`plaintext-allowlist.txt`。
- 文档：`docs/08`、`docs/11`、`docs/27`、`docker/README.md`。
