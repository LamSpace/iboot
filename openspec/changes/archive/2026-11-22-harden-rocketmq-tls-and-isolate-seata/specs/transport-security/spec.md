# Spec Delta

## REMOVED Requirements

### Requirement: RocketMQ 只提供加密通道

**Reason**: B5.3 把 RocketMQ 的应用入口收敛为只接受 TLS 的前置代理，原“同端口仍接受明文”的行为不再成立。

**Migration**: 应用统一连接 `rocketmq-tls:8081`（宿主机 `localhost:18081`）并保持 `enableSsl=true`；直接连接 `rocketmq-proxy:8081` 或使用明文的客户端必须升级。

## ADDED Requirements

### Requirement: RocketMQ 应用入口只接受 TLS

RocketMQ 的应用入口 SHALL 由只接受 TLS 的前置代理提供；明文连接 MUST NOT 获得代理响应。客户端仍不校验服务端证书，MUST NOT 被描述为 mTLS 或零信任链路。

- 前置代理 MUST 只转发 TLS ClientHello；非 TLS 字节 MUST 被拒绝或立即关闭。
- Proxy 原生端口 MUST NOT 直接暴露给应用容器或宿主机；应用 MUST 只通过前置代理访问。
- 客户端 MUST 使用 `enableSsl=true`；官方 5.2.2 客户端硬编码 `InsecureTrustManager`，服务端证书校验 MUST 登记为已知边界。
- RocketMQ ACL 与客户端认证 MUST 留待后续变更；本批 MUST NOT 宣称已具备调用方身份。

#### Scenario: TLS 握手成功

- **WHEN** 使用本地 CA 对 RocketMQ 前置代理的 gRPC 端口执行 TLS 握手
- **THEN** 握手成功且证书链可被本地 CA 验证

#### Scenario: 加密客户端可投递通知

- **WHEN** workflow 以 `enableSsl=true` 向 `rocketmq-tls:8081` 发布通知事件
- **THEN** 事件投递成功且消费端可处理

#### Scenario: 明文连接被拒绝

- **WHEN** 客户端以明文连接 RocketMQ 前置代理端口
- **THEN** 连接被关闭或不返回代理响应
- **AND** 回归不得把该结果计为 TLS 通过

#### Scenario: 应用无法直连 Proxy

- **WHEN** 业务服务容器尝试直接连接 `rocketmq-proxy:8081`
- **THEN** 解析或连接失败
- **AND** 业务服务仍能通过 `rocketmq-tls:8081` 完成投递与消费

### Requirement: Seata 是显式例外并受网络隔离

Seata 2.6.0 没有原生传输层 TLS/认证，SHALL 被登记为显式例外；其控制面 MUST 只允许 `iboot-workflow` 与 `iboot-system` 访问，MUST NOT 被纳入 mTLS/零信任宣称。

- Seata Server MUST 只加入专用 `tx` 网络；MUST NOT 加入通用 `data` 网络。
- 只有 `iboot-workflow` 与 `iboot-system` MUST 加入 `tx`；其余服务 MUST NOT 解析或连接 `seata-server:8091`。
- 文档 MUST 登记：无原生 TLS/认证、专用网络隔离、回环发布与业务侧 mTLS/内部令牌补偿、残余风险与复审触发条件。
- 本批 MUST NOT 引入手工 TLS 终止器来宣称端到端加密。

#### Scenario: 只有事务参与方可访问 Seata

- **WHEN** `iboot-workflow` 或 `iboot-system` 连接 `seata-server:8091`
- **THEN** 连接成功
- **AND** AT/TCC 验收用例保持通过

#### Scenario: 非事务服务无法访问 Seata

- **WHEN** 其它业务服务容器尝试连接 `seata-server:8091`
- **THEN** 解析或连接失败
- **AND** 暴露面回归明确报告该隔离结果

#### Scenario: 例外被明确登记

- **WHEN** 阅读 TLS 与零信任操作指南
- **THEN** Seata 被标注为“无原生 TLS/认证的显式例外”
- **AND** 不计入 mTLS 覆盖率或零信任能力

## MODIFIED Requirements

### Requirement: 编排发布的内部端口仅绑定回环地址

Docker 编排发布到宿主机的内部端口 MUST 只绑定回环地址，MUST NOT 对宿主机其它网络接口提供监听；宿主机开发模式下由开发者直接启动的进程不在本条范围内。

- 业务服务、运维控制台、MySQL、Redis、Silo、Elasticsearch、Nacos、Seata、Kafka、RocketMQ TLS 入口、Neo4j 与可观测 UI 的容器发布端口 MUST 绑定 `127.0.0.1`。
- 通过宿主机非回环地址发起连接 MUST 失败，即使目标端口在回环地址上可用。
- 公开入口 MUST 只有门户的 80 与 443；新增任何全接口绑定 MUST 被视为安全回归。
- 宿主机开发 MUST 使用 `localhost` 或 `127.0.0.1` 访问内部端口，MUST NOT 使用机器 LAN 地址。

#### Scenario: 非回环地址无法访问内部端口

- **WHEN** 使用宿主机的非回环 IP 连接 MySQL、Redis、Elasticsearch 或 Nacos 的已发布端口
- **THEN** 连接失败
- **AND** 使用 `localhost` 的同一连接成功

#### Scenario: 内部端口不监听全部网卡

- **WHEN** 检查编排的容器端口映射
- **THEN** 内部端口只出现在回环地址上
- **AND** 只有门户 80/443 监听 `0.0.0.0`

#### Scenario: 宿主机开发不依赖 LAN 地址

- **WHEN** 按文档以宿主机进程方式启动并连接中间件
- **THEN** 所有地址均为 `localhost` 或 `127.0.0.1`
- **AND** 不出现指向本机 LAN IP 的配置

### Requirement: 日志与检索面 TLS 有负向门禁

回归 SHALL 对 Elasticsearch、Kafka、日志管道与 RocketMQ 执行 TLS 负向检查，明文、缺少 CA 或错误凭据 MUST 导致失败。

- 检查 MUST 覆盖 ES 的明文、无 CA、错误凭据，Kafka 的明文与无 truststore，以及 Logstash 两端连接的证书校验。
- RocketMQ 明文连接 MUST 被拒绝并计为负向用例；MUST NOT 再以“明文仍可连接”作为边界放行。
- 检查 MUST 接入全量回归与 CI，不得只作为手工步骤。
- 任一检查失败 MUST 阻止把环境判定为可验收。

#### Scenario: 明文 ES 被检出

- **WHEN** Elasticsearch 仍接受明文请求
- **THEN** 日志与检索面 TLS 检查失败并指出该端口

#### Scenario: 明文 Kafka 被检出

- **WHEN** Kafka 任一监听仍接受 PLAINTEXT
- **THEN** 检查失败并指出该监听

#### Scenario: 全部 TLS 检查通过

- **WHEN** 在启用日志与检索面 TLS 的全栈环境执行回归
- **THEN** 正向链路与负向用例全部通过

#### Scenario: 明文 RocketMQ 被检出

- **WHEN** RocketMQ TLS 入口仍接受明文连接
- **THEN** 日志与检索面 TLS 检查失败并指出该端口
