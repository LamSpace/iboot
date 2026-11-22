# Spec Delta

## MODIFIED Requirements

### Requirement: RocketMQ 应用入口只接受 TLS

RocketMQ 的应用入口 SHALL 由只接受 TLS 的前置代理提供；明文连接 MUST NOT 获得代理响应。客户端仍不校验服务端证书，MUST NOT 被描述为 mTLS 或零信任链路。

- 前置代理 MUST 只转发 TLS ClientHello；非 TLS 字节 MUST 被拒绝或立即关闭。
- Proxy 原生端口 MUST NOT 直接暴露给应用容器或宿主机；应用 MUST 只通过前置代理访问。
- 客户端 MUST 使用 `enableSsl=true`；官方 5.2.2 客户端硬编码 `InsecureTrustManager`，服务端证书校验 MUST 登记为已知边界。
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

### Requirement: RocketMQ 凭据注入与初始化

RocketMQ 的客户端凭据 SHALL 从环境或密钥注入，初始化 MUST 使用受管运维账号。

- `.env.example` MUST 登记全部新增变量，MUST NOT 提供可直接使用的生产凭据。
- Broker、Proxy 与初始化任务 MUST 从环境生成运行时配置，MUST NOT 把口令写入镜像或仓库。
- 业务服务 MUST 只收到自己的 RocketMQ 凭据。
- 初始化 MUST 幂等：重复执行 MUST NOT 破坏已有 topic、账号或授权。

#### Scenario: 重复初始化

- **WHEN** ACL 初始化任务重复执行
- **THEN** 退出码为 0
- **AND** 账号与授权与首次执行一致

#### Scenario: 服务只收到自己的凭据

- **WHEN** 检查 workflow 与 notify 的运行环境
- **THEN** 各自只有自己的 RocketMQ access key / secret key
- **AND** 不含运维账号凭据

### Requirement: 内置默认凭据不可用

RocketMQ 镜像自带的 `rocketmq2/12345678` 等默认凭据 MUST NOT 能访问启用 ACL 后的 Broker。

- 运维账号 MUST 由环境注入并生成 `tools.yml`。
- 默认凭据访问 MUST 被拒绝。

#### Scenario: 默认 tools 凭据被拒绝

- **WHEN** 使用镜像默认 `tools.yml` 连接 Broker
- **THEN** 认证失败
- **AND** 不能创建 topic、用户或 ACL
