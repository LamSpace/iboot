# Spec Delta

## ADDED Requirements

### Requirement: 内部契约快照使用 HTTPS

契约快照与内部回归 SHALL 通过 HTTPS 访问各服务，并使用本地 CA 校验证书。

- `SVC_*` 直连地址 MUST 使用 HTTPS。
- 快照与回归脚本中的 HTTPS 请求 MUST 使用 `SVC_CACERT`（或共用的 `REG_CACERT`）指向的本地 CA。
- 去掉 CA 或改用明文时，检查 MUST 失败并指出证书或协议错误。
- 内部 TLS 检查 MUST 接入全量回归与 CI。

#### Scenario: 快照经 HTTPS 生成

- **WHEN** 执行契约快照
- **THEN** 每个服务通过 HTTPS 导出 OpenAPI
- **AND** 请求使用本地 CA 校验通过

#### Scenario: 缺少内部 CA 失败

- **WHEN** 快照请求未携带本地 CA
- **THEN** 检查失败并指出证书校验错误

#### Scenario: 明文服务端口失败

- **WHEN** 某个服务仍以明文提供端口
- **THEN** 内部 TLS 检查失败并指出该端口
