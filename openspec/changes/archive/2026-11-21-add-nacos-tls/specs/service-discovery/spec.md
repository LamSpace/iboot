# Spec Delta

## ADDED Requirements

### Requirement: 服务注册与配置拉取使用 TLS

服务 SHALL 通过 TLS 完成 Nacos 注册、实例发现与配置拉取，MUST NOT 依赖明文注册中心。

- 每个服务 MUST 在启动参数中启用 Nacos client gRPC TLS、指向本地 CA，并提供客户端证书。
- 服务 MUST 能完成注册、获取实例列表与拉取配置；缺少 TLS 参数时 MUST 失败而不是回退明文。
- 网关与负载均衡 MUST 继续根据实例元数据选择 HTTPS 业务地址。

#### Scenario: 服务注册成功

- **WHEN** 服务以 Nacos TLS 参数启动
- **THEN** 实例在 Nacos 中可见
- **AND** 其它服务可以解析并调用该实例

#### Scenario: 缺少客户端 TLS 参数失败

- **WHEN** 服务未启用 Nacos client TLS 或未提供客户端证书
- **THEN** 注册或配置拉取失败
- **AND** 不回退到明文连接

#### Scenario: 配置拉取成功

- **WHEN** 服务通过 TLS 拉取 `iboot-*.yml`
- **THEN** 配置内容与 Nacos 中的值一致
