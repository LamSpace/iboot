# Spec Delta

## ADDED Requirements

### Requirement: Tempo 接收与查询链路使用 TLS

Tempo 的 OTLP/HTTP、OTLP/gRPC 接收端口与 HTTP 查询端口 SHALL 只接受 TLS；明文上报或查询 MUST NOT 获得成功响应。

- 应用上报地址与 Grafana 数据源 MUST 使用 `https://`，并通过本地 CA 校验服务端证书。
- 客户端缺少 CA、使用错误 CA 或跳过证书校验时 MUST 失败，MUST NOT 回退到明文。
- Tempo 内部 gRPC 服务端口 MUST NOT 发布到宿主机，其明文边界 MUST 在文档中登记。

#### Scenario: 加密上报与查询成功

- **WHEN** 服务通过 `https://tempo:4318/v1/traces` 上报 span，且 Grafana 通过 `https://tempo:3200` 查询
- **THEN** 两条请求均成功
- **AND** 服务端证书通过本地 CA 校验

#### Scenario: 明文上报被拒绝

- **WHEN** 客户端以 `http://` 访问 4318 或 3200
- **THEN** 请求不获得成功响应

#### Scenario: 缺少 CA 时握手失败

- **WHEN** 客户端使用空信任库访问 Tempo 的 TLS 端口
- **THEN** TLS 证书校验失败
- **AND** 回归判定为失败
