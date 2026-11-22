# observability-security Specification

## Purpose
定义 Prometheus 与 exporter 之间的采集链路安全：指标端点只接受 TLS，调用方必须出示受管客户端证书，
明文或匿名抓取必须失败。

## Requirements

### Requirement: 指标端点只接受 TLS 与受管客户端

MySQL、Redis、Node 与 Elasticsearch exporter 的指标端点 SHALL 只接受 TLS，并要求客户端证书。

- 每个 exporter MUST 使用独立的服务端证书，证书 SAN MUST 包含其在编排中的服务名。
- exporter MUST 校验证书链；未携带客户端证书或证书不受本地 CA 信任的请求 MUST 被拒绝。
- 明文 HTTP 请求 MUST NOT 返回指标。
- Prometheus MUST 使用自己的客户端证书与本地 CA 抓取全部 exporter。
- 证书与私钥 MUST 从只读挂载注入，MUST NOT 写入镜像。

#### Scenario: Prometheus 携带证书抓取成功

- **WHEN** Prometheus 使用 `prometheus` 客户端证书抓取任一 exporter
- **THEN** TLS 握手成功
- **AND** 返回指标文本

#### Scenario: 无客户端证书被拒绝

- **WHEN** 客户端只校验服务端证书但不提供客户端证书
- **THEN** 请求被拒绝
- **AND** 不返回指标

#### Scenario: 明文被拒绝

- **WHEN** 客户端对 exporter 端口发起明文 HTTP
- **THEN** 不返回指标

#### Scenario: 错误 CA 被拒绝

- **WHEN** 客户端使用不受信任的 CA 校验 exporter 证书
- **THEN** TLS 校验失败

### Requirement: 采集链路有负向回归

回归 SHALL 对每个 exporter 执行正向与负向检查，结果 MUST 接入全量回归与 CI。

- 正向 MUST 覆盖 Prometheus 抓取成功或等价客户端证书请求。
- 负向 MUST 覆盖明文、无客户端证书与错误 CA。
- 任一负向用例返回指标 MUST 阻止环境被判定为可验收。

#### Scenario: 每个 exporter 都有正反向用例

- **WHEN** 执行 exporter mTLS 回归
- **THEN** 四个 exporter 的携带证书请求成功
- **AND** 明文、无客户端证书与错误 CA 请求失败

#### Scenario: 负向结果阻止验收

- **WHEN** 任一 exporter 在无客户端证书时仍返回指标
- **THEN** 回归失败并指出该 exporter
- **AND** 环境不得判定为通过
