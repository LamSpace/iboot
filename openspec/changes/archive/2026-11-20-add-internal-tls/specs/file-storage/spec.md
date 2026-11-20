# Spec Delta

## ADDED Requirements

### Requirement: Silo 访问使用 HTTPS

Silo 存储提供者 SHALL 通过 HTTPS 提供内部读写与预签名下载，明文请求 MUST NOT 返回对象内容。

- 内部 endpoint 与公开 endpoint MUST 使用 `https://`。
- 预签名 URL MUST 以 HTTPS 公开地址签发，主机名必须可被浏览器用本地 CA 校验。
- 服务端 MUST 加载本地证书；客户端 MUST 使用本地 CA 校验。
- HTTP 请求 MUST 被拒绝或被重定向到 HTTPS，MUST NOT 返回对象内容。

#### Scenario: 内部读写使用 HTTPS

- **WHEN** 文件服务或系统服务通过 Silo 保存或读取对象
- **THEN** 请求使用 HTTPS 内部 endpoint
- **AND** 服务端证书通过本地 CA 校验

#### Scenario: 预签名 URL 使用 HTTPS

- **WHEN** 客户端请求对象下载地址
- **THEN** 返回的预签名 URL 以 `https://` 开头
- **AND** 浏览器使用本地 CA 可打开该 URL

#### Scenario: 明文请求不返回内容

- **WHEN** 客户端以 `http://` 访问 Silo
- **THEN** 请求被拒绝或重定向
- **AND** 不返回对象内容
