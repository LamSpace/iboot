# Proposal

## Why

SPIRE SVID 已成为宿主机与 Compose 全量模式的默认工作负载身份，但监控只覆盖叶子证书到期：
SPIRE Server/Agent 不可用、SVID 刷新失败或长时间未成功都没有告警。A1 已写出刷新状态指标，
需要接入 Prometheus/Alertmanager 并在故障时可见；docs/28 将该项登记为 A4。

## What Changes

- `blackbox.yml` 新增 `http_2xx` 模块；Prometheus 新增 `spire-health` job，探测
  `spire-server:8080/ready` 与 `spire-agent:8080/ready`；SPIRE 容器加入 `obs` 网络。
- node-exporter 启用文本文件采集（`/certs/runtime`），暴露 `svid-refresh-schedule.sh` 写入的
  `iboot_svid_refresh_*` 与 `iboot_svid_cert_min_remaining_seconds`。
- `tls-alerts.yml` 新增 `svid-alerts` 组：SPIRE Server/Agent 不可用、SVID 刷新失败/停滞、
  SVID 7 天内到期；原 TLS 告警补 `team="security"`。
- Alertmanager 新增 `team="security"` 子路由与 `security-webhook` receiver（本地默认复用门户 webhook）。
- 新增回归 `scripts/regress/spire-svid-monitor.sh`（7a）：规则/配置校验、健康探针、指标、
  故障注入与路由断言。
- 更新 `docs/11` §11.16、`docs/27` §28、`docs/28`、`scripts/README.md`；docs/28 移除 A4。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `observability-security`：SPIRE/SVID 健康与刷新状态必须纳入 Prometheus 告警并路由到安全渠道。

## Impact

- 配置：`docker/docker-compose.yml`（SPIRE 网络、node-exporter、Alertmanager）、
  `docker/scripts/blackbox/blackbox.yml`、Prometheus 两份配置、`tls-alerts.yml`、`alertmanager.yml`。
- 脚本：新增 `scripts/regress/spire-svid-monitor.sh`、`scripts/regress/regress.sh`。
- 文档：`docs/11`、`docs/27`、`docs/28`、`scripts/README.md`。
