# transport-security 增量规格

## MODIFIED Requirements

### Requirement: RocketMQ 应用入口只接受 TLS

RocketMQ 的应用入口 SHALL 由只接受 TLS 的前置代理提供；明文连接 MUST NOT 获得代理响应。

- 前置代理 MUST 只转发 TLS ClientHello；非 TLS 字节 MUST 被拒绝或立即关闭。
- Proxy 原生端口 MUST NOT 直接暴露给应用容器或宿主机；应用 MUST 只通过前置代理访问。
- 客户端 MUST 使用 `enableSsl=true`，并按“RocketMQ 客户端校验服务端证书”要求校验服务端。
- Broker 与 Proxy MUST 启用认证与授权；未携带凭据或凭据错误的客户端 MUST NOT 生产或消费消息。
- 每个应用 MUST 使用独立账号；生产方 MUST 只能发布其业务 topic，消费方 MUST 只能订阅其消费组与 topic。
- 运维账号 MUST 只用于 topic/ACL 初始化与排障，MUST NOT 注入业务服务。
- ACL 元数据 MUST 持久化到独立数据卷，容器重建后账号与授权 MUST 仍然有效。

#### Scenario: TLS 握手成功

- **WHEN** 使用本地 CA 对 RocketMQ 前置代理的 gRPC 端口执行 TLS 握手
- **THEN** 握手成功且证书链可被本地 CA 验证

#### Scenario: 加密客户端可投递通知

- **WHEN** workflow 以 `enableSsl=true` 且携带自身凭据向 `rocketmq-tls:8081` 发布通知事件
- **THEN** 事件投递成功且消费端可处理

#### Scenario: 明文连接被拒绝

- **WHEN** 客户端以明文连接 RocketMQ 前置代理端口
- **THEN** 连接被关闭或不返回代理响应
- **AND** 回归不得把该结果计为 TLS 通过

#### Scenario: 应用无法直连 Proxy

- **WHEN** 业务服务容器尝试直接连接 `rocketmq-proxy:8081`
- **THEN** 解析或连接失败
- **AND** 业务服务仍能通过 `rocketmq-tls:8081` 完成投递与消费

#### Scenario: 无凭据客户端被拒绝

- **WHEN** 客户端不携带 access key / secret key 连接 Proxy 或 Broker
- **THEN** 认证失败
- **AND** 不产生可消费的消息

#### Scenario: 生产方只能发布自己的 topic

- **WHEN** `iboot_workflow` 向 `iboot-notification` 发布消息
- **THEN** 发送成功
- **AND** 向其它 topic 发布被拒绝

#### Scenario: 消费方只能订阅自己的 topic 与消费组

- **WHEN** `iboot_notify` 订阅 `iboot-notification` 并使用自己的消费组
- **THEN** 订阅与消费成功
- **AND** 以生产身份发布消息被拒绝

#### Scenario: 错误凭据被拒绝

- **WHEN** 客户端使用错误的 access key 或 secret key
- **THEN** 认证失败
- **AND** 不返回消息内容

## ADDED Requirements

### Requirement: RocketMQ 客户端校验服务端证书

RocketMQ Java 客户端 SHALL 校验 Proxy 服务端证书链与主机名，MUST NOT 使用信任所有证书的实现。

- 客户端 MUST 使用本地 CA 信任库（或部署方显式配置的信任库）初始化 `TrustManagerFactory`。
- 信任库缺失、损坏或不可读时，客户端 MUST 失败关闭，MUST NOT 回退到信任所有证书。
- 服务端证书 MUST 覆盖客户端实际连接的 `rocketmq-tls` 主机名；主机名不匹配时握手 MUST 失败。
- 兼容层 MUST 只修补客户端 5.2.2 的信任管理器，MUST NOT 关闭 gRPC 的 HTTPS 主机名校验。
- 回归 MUST 证明正确 CA 可投递、错误 CA 被拒绝，并检查兼容层被打包到 `BOOT-INF/classes`。
- 客户端升级到已修复的版本后，兼容层 MUST 被删除。

#### Scenario: 正确 CA 可投递

- **WHEN** workflow 使用包含本地 CA 的信任库连接 `rocketmq-tls:8081`
- **THEN** TLS 握手完成证书链与主机名校验
- **AND** 消息投递成功

#### Scenario: 错误 CA 被拒绝

- **WHEN** 客户端使用不含本地 CA 的信任库连接同一入口
- **THEN** TLS 握手失败
- **AND** 生产或消费请求不成功

#### Scenario: 主机名不匹配被拒绝

- **WHEN** 客户端连接的主机名不在服务端证书 SAN 中
- **THEN** gRPC 的 HTTPS 主机名校验失败

#### Scenario: 信任库不可用时失败关闭

- **WHEN** 显式配置的信任库路径不存在或无法加载
- **THEN** 客户端初始化失败
- **AND** 不建立到 Proxy 的连接

#### Scenario: 兼容层随 fat jar 生效

- **WHEN** 解包 workflow 或 notify 的 Spring Boot fat jar
- **THEN** `BOOT-INF/classes` 中存在兼容层类
- **AND** 客户端运行时优先加载该类而不是内置的不安全实现
