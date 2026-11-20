# Spec Delta

## ADDED Requirements

### Requirement: 数据库连接使用 TLS

数据库迁移执行者与所有服务 SHALL 通过 TLS 连接 MySQL，服务端 MUST 拒绝明文连接。

- MySQL MUST 启用 TLS 并设置 `require_secure_transport=ON`。
- 迁移执行者与服务 MUST 使用相同的 TLS 连接参数与本地 CA。
- 客户端 MUST 校验服务端证书；MUST NOT 使用 `useSSL=false` 或关闭校验。
- 数据库健康检查 MUST 能区分 TLS 连接失败与数据库不可用。

#### Scenario: 迁移在 TLS 下完成

- **WHEN** 执行 `iboot-db-init`
- **THEN** 迁移连接使用 TLS
- **AND** 迁移完成后服务连接同样使用 TLS

#### Scenario: 明文连接被拒绝

- **WHEN** 客户端以 `sslMode=DISABLED` 连接 MySQL
- **THEN** 服务端拒绝连接
- **AND** 错误信息表明禁止不安全传输

#### Scenario: 错误 CA 导致失败

- **WHEN** 客户端使用不受信任的 CA 连接 MySQL
- **THEN** 连接失败
- **AND** 不会回退为明文
