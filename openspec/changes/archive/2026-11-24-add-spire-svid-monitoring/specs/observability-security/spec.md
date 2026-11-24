# Spec Delta

## ADDED Requirements

### Requirement: SPIRE 与 SVID 监控告警

环境 SHALL 监控 SPIRE Server/Agent 可用性、SVID 刷新结果与 SVID 有效期，并把安全类告警
路由到独立 receiver。

- Prometheus MUST 通过 blackbox 探测 `spire-server` 与 `spire-agent` 的 `/ready` 端点。
- SVID 刷新调度 MUST 暴露最近成功时间、退出码与最小剩余有效期指标，并被 Prometheus 采集。
- 告警规则 MUST 覆盖 SPIRE Server/Agent 不可用、SVID 刷新失败、刷新停滞与 SVID 7 天内到期。
- TLS/SVID/SPIRE 告警 MUST 带 `team="security"` 标签，Alertmanager MUST 将其路由到
  `security-webhook` receiver。
- 回归 MUST 校验规则与 Alertmanager 配置可解析，并通过故障注入确认失败告警可见。

#### Scenario: SPIRE 控制面不可用

- **WHEN** SPIRE Server 或 Agent 的 `/ready` 探测连续失败
- **THEN** 对应 `SpireServerUnhealthy`/`SpireAgentUnhealthy` 告警进入 pending/firing
- **AND** 告警被路由到安全 receiver

#### Scenario: SVID 刷新失败

- **WHEN** 调度写入 `iboot_svid_refresh_last_exit_code=1`
- **THEN** Prometheus 采集到该指标
- **AND** `SvidRefreshFailed` 告警进入 pending/firing

#### Scenario: 长时间未成功刷新

- **WHEN** 最近成功时间早于 3 天
- **THEN** `SvidRefreshStale` 告警进入 pending/firing

#### Scenario: SVID 即将过期

- **WHEN** 十个服务 SVID 的最小剩余有效期低于 7 天
- **THEN** `SvidCertificateExpiringSoon` 告警触发
