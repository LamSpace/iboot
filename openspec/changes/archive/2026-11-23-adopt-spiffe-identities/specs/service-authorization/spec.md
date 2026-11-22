# Spec Delta

## MODIFIED Requirements

### Requirement: 调用方身份由客户端证书 CN 确定

服务间请求的调用方身份 SHALL 来自已完成双向 TLS 的客户端证书，MUST NOT 信任请求头或代理注入的标识。

- 身份 MUST 优先取证书 URI SAN 中的 SPIFFE ID；信任域 MUST 为 `iboot.local`。
- SPIFFE ID 的路径 MUST 区分 `service/<name>` 与 `client/<name>`，映射结果 MUST 与既有调用方名称一致。
- 证书没有 SPIFFE ID 时 MAY 回退到 Subject CN，以兼容旧证书；回退 MUST 记录为兼容路径。
- 没有证书或身份无法解析时 MUST 按未授权处理。
- 授权规则 MUST 继续使用稳定的服务名，MUST NOT 绑定证书序列号或过期时间。

#### Scenario: 优先使用 SPIFFE ID

- **WHEN** 客户端证书同时包含 CN `iboot-file` 与 SPIFFE ID `spiffe://iboot.local/service/iboot-system`
- **THEN** 调用方身份被解析为 `iboot-system`
- **AND** 以 `iboot-system` 的规则判定授权

#### Scenario: 旧证书回退到 CN

- **WHEN** 客户端证书没有 URI SAN 但 CN 为 `iboot-gateway`
- **THEN** 调用方身份为 `iboot-gateway`

#### Scenario: 未知信任域被拒绝

- **WHEN** 证书 URI SAN 为 `spiffe://other.local/service/iboot-system`
- **THEN** 不把该 URI 当作有效身份
- **AND** 必要时回退到 CN

#### Scenario: 服务证书识别为服务名

- **WHEN** 网关以带 SPIFFE ID 的 `iboot-gateway` 证书访问系统服务
- **THEN** 过滤器识别调用方为 `iboot-gateway`

#### Scenario: 无法识别时按匿名处理

- **WHEN** 客户端证书既没有可解析的 SPIFFE ID，也缺少可解析的 CN
- **THEN** 调用方身份为空
- **AND** 命中授权规则的路径按未授权处理
