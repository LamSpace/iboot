# Spec Delta

## ADDED Requirements

### Requirement: 每个可部署服务使用独立服务端证书

十个可部署服务 SHALL 各自加载独立的服务端证书；客户端 MUST 能用服务名校验证正在连接的服务。

- 每个服务证书的 SAN MUST 包含该服务名，并包含 `localhost`、`127.0.0.1`、`::1` 以支持宿主机调试。
- 服务证书 MUST 由同一套本地 CA 签发，使现有信任链无需变更。
- 服务端 MUST NOT 继续使用共享门户证书提供业务端口。
- 客户端证书认证 MUST NOT 在本变更中强制启用；每服务客户端身份留待后续变更。

#### Scenario: 客户端按服务名校验证成功

- **WHEN** 客户端使用本地 CA 连接 `https://iboot-system:8080` 并校验主机名
- **THEN** 证书校验通过
- **AND** 返回的证书 SAN 包含 `iboot-system`

#### Scenario: 服务证书不可互换

- **WHEN** 比较任意两个服务的证书指纹
- **THEN** 两者不相同
- **AND** 每个证书的 SAN 只包含自身服务名与回环地址

#### Scenario: 端口仍接受 TLS 且拒绝明文

- **WHEN** 回归检查 8080–8089 的端口
- **THEN** TLS 1.2 握手与健康检查通过
- **AND** 明文请求不获得业务响应
