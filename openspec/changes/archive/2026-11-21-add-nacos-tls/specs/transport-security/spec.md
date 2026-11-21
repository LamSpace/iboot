# Spec Delta

## ADDED Requirements

### Requirement: Nacos 注册与配置链路使用 TLS

Nacos 的 HTTP API、控制台与 gRPC 注册/配置通道 MUST 只接受 TLS；gRPC MUST 要求客户端证书，明文或无证书请求 MUST NOT 获得业务响应。

- Nacos MUST 为 8848 与控制台端口加载 PKCS12 keystore，并为 9848 启用 gRPC TLS。
- 客户端 MUST 使用本地 CA 校验证书与主机名，并提供客户端证书；MUST NOT 使用 `trustAll=true`。
- 健康检查与运维脚本 MUST 使用 `--cacert`；MUST NOT 使用 `-k` 或跳过校验。
- Nacos 证书路径 MUST 带 `file:` 前缀；客户端 TLS 属性 MUST 在 JVM 启动前注入。

#### Scenario: Nacos HTTPS API 可用

- **WHEN** 使用本地 CA 访问 Nacos 的 8848 或控制台端口
- **THEN** 返回 200
- **AND** 证书通过本地 CA 校验

#### Scenario: Nacos 明文请求失败

- **WHEN** 客户端以 `http://` 访问 Nacos 的 8848 或控制台端口
- **THEN** 请求失败且不返回业务数据

#### Scenario: gRPC TLS 握手成功

- **WHEN** 使用本地 CA 对 9848 执行 TLS 握手
- **THEN** 握手成功且 `Verify return code` 为 0

#### Scenario: 客户端缺少证书时失败

- **WHEN** Nacos 客户端未配置 `trustCollectionChainPath`、客户端证书或使用错误 CA
- **THEN** 注册与配置拉取失败
