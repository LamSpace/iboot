# Spec Delta

## ADDED Requirements

### Requirement: 叶子证书携带 SPIFFE 身份

本地 CA 签发的叶子证书 SHALL 在 URI SAN 中包含 SPIFFE ID，供服务间授权使用。

- 服务证书 MUST 包含 `spiffe://iboot.local/service/<service>`。
- 探针客户端证书 MUST 包含 `spiffe://iboot.local/client/<name>`。
- SPIFFE ID 的 `<service>`/`<name>` MUST 与证书 CN 一致。
- 证书检查 MUST 校验 URI SAN 存在且信任域正确。

#### Scenario: 服务证书带 SPIFFE ID

- **WHEN** 检查 `docker/certs/services/iboot-system.pem`
- **THEN** URI SAN 包含 `spiffe://iboot.local/service/iboot-system`

#### Scenario: 探针证书带 SPIFFE ID

- **WHEN** 检查 `docker/certs/clients/prometheus.pem`
- **THEN** URI SAN 包含 `spiffe://iboot.local/client/prometheus`

#### Scenario: 缺失 SPIFFE ID 时检查失败

- **WHEN** 证书没有 SPIFFE URI SAN
- **THEN** 证书检查失败并指出证书路径
