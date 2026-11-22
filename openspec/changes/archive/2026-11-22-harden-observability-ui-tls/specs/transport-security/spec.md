# Spec Delta

## ADDED Requirements

### Requirement: 可观测查询与控制台只提供 TLS

Grafana、Prometheus、Alertmanager、Thanos Query 与 Kibana 的 HTTP 入口 SHALL 只提供 TLS；明文请求 MUST NOT 获得页面或查询结果。

- 各组件 MUST 加载本地叶子证书，并通过本地 CA 校验上游连接。
- Grafana 数据源、Thanos sidecar 到 Prometheus、Prometheus 到 Alertmanager MUST 使用 `https://`。
- 数据库中登记的控制台地址 MUST 使用 `https://`。
- Sentinel Dashboard 的明文回环入口 MUST 作为已登记边界，不得计入 TLS 覆盖。

#### Scenario: 控制台 HTTPS 可用

- **WHEN** 使用本地 CA 访问五个控制台的 HTTPS 地址
- **THEN** 页面或健康端点返回成功
- **AND** 证书通过本地 CA 校验

#### Scenario: 明文请求被拒绝

- **WHEN** 客户端以 `http://` 访问任一控制台端口
- **THEN** 请求失败或重定向到 HTTPS
- **AND** 不返回页面或查询数据

#### Scenario: 数据源与告警链路加密

- **WHEN** Grafana 查询 Prometheus/Thanos，或 Prometheus 向 Alertmanager 投递告警
- **THEN** 连接使用 HTTPS
- **AND** 服务端证书通过本地 CA 校验

#### Scenario: 明文允许清单收尾

- **WHEN** 执行明文默认地址审计
- **THEN** 仅剩 CLI 回环回调这一条例外
- **AND** 新增控制台明文地址会导致审计失败
