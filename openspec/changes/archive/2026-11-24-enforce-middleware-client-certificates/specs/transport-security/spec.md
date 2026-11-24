# Spec Delta

## ADDED Requirements

### Requirement: MySQL 与 Redis 使用客户端证书 mTLS

MySQL 与 Redis SHALL 要求业务客户端出示由本地 CA 签发的工作负载证书；MUST NOT 只依赖口令或 ACL。

- MySQL 服务账号 MUST 绑定 `REQUIRE SUBJECT '/CN=iboot-<service>'`；客户端 MUST 使用该服务的 PKCS12 keystore。
- 迁移账号与 exporter MAY 继续使用服务端 TLS + 口令；它们 MUST NOT 使用业务服务客户端证书。
- Redis MUST 开启 `tls-auth-clients yes`；数据服务、exporter 与回归客户端 MUST 出示本地 CA 签发的证书。
- Silo 与 Nacos HTTP API 不支持按工作负载的客户端证书认证，MUST 保持服务端 TLS + 账号/令牌，并登记为显式例外。
- 回归 MUST 覆盖无证书被拒绝、正确证书成功、其它服务证书不能冒充。

#### Scenario: MySQL 无客户端证书被拒绝

- **WHEN** 业务账号使用正确口令但不带客户端证书连接 MySQL
- **THEN** 认证失败
- **AND** 不建立可用会话

#### Scenario: MySQL 正确证书可连接

- **WHEN** 服务使用自己的 PKCS12 keystore 与账号连接 MySQL
- **THEN** 连接成功
- **AND** 只能使用自己的账号

#### Scenario: MySQL 其它服务证书不能冒充

- **WHEN** 使用服务 A 的证书连接服务 B 的账号
- **THEN** 认证失败
- **AND** 不返回业务数据

#### Scenario: Redis 无客户端证书被拒绝

- **WHEN** 客户端使用正确 ACL 口令但不带客户端证书连接 Redis
- **THEN** TLS 握手或认证失败
- **AND** 不执行任何命令

#### Scenario: Redis 正确证书可连接

- **WHEN** 数据服务或 exporter 使用自己的客户端证书与 ACL 账号连接 Redis
- **THEN** `PING` 返回 `PONG`
- **AND** 业务读写正常

#### Scenario: Silo 与 Nacos HTTP 仍是显式例外

- **WHEN** 检查 Silo 或 Nacos HTTP 客户端的身份方式
- **THEN** 仍为服务端 TLS + 账号/令牌
- **AND** 文档与回归不得宣称这两条链路是客户端证书 mTLS
