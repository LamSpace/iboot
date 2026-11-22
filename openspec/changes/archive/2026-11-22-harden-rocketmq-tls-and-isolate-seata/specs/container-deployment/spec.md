# Spec Delta

## REMOVED Requirements

### Requirement: RocketMQ Proxy 的加密通道编排

**Reason**: B5.3 把 RocketMQ 应用入口改为只接受 TLS 的前置代理，并迁入专用 `mq` 网络；原“TLS 可选、同端口接受明文”的编排不再成立。

**Migration**: 编排新增 `rocketmq-tls` 前置代理；RocketMQ 组件只加入 `mq`，应用端点改为 `rocketmq-tls:8081`；宿主机只发布 `127.0.0.1:18081` 的 TLS 入口。

## ADDED Requirements

### Requirement: RocketMQ TLS 前置与 mq 网络编排

编排 SHALL 为 RocketMQ 提供只接受 TLS 的前置代理，并把 NameServer、Broker、Proxy 与初始化任务隔离在专用 `mq` 网络。

- 前置代理 MUST 使用 `ssl_preread` 只转发 TLS ClientHello；非 TLS 连接 MUST 被关闭。
- 前置代理 MUST 同时加入 `app` 与 `mq`；RocketMQ NameServer、Broker、Proxy 与初始化任务 MUST 只加入 `mq`。
- 应用 MUST 连接 `rocketmq-tls:8081`；宿主机 MUST 只发布前置代理的 `127.0.0.1:18081`，MUST NOT 发布 Proxy 的明文端口。
- 业务服务 MUST NOT 依赖 RocketMQ 启动；Proxy 不可用时审批提交与通知事件持久化 MUST 仍然成功。
- 文档 MUST 保留“客户端不校验服务端证书、无 ACL”的边界，MUST NOT 宣称 mTLS 或零信任。

#### Scenario: TLS 前置代理启动

- **WHEN** 启动核心编排
- **THEN** `rocketmq-tls` 监听 8081 并通过本地证书完成 TLS 握手
- **AND** RocketMQ Proxy 仍保持健康

#### Scenario: 明文被前置代理拒绝

- **WHEN** 客户端以明文连接 `localhost:18081`
- **THEN** 连接被关闭或不返回代理响应
- **AND** TLS 客户端仍可完成握手

#### Scenario: 应用只走 TLS 入口

- **WHEN** workflow 或 notify 发送/消费通知事件
- **THEN** 连接发往 `rocketmq-tls:8081`
- **AND** 事件投递与消费成功

#### Scenario: Proxy 不可用不阻塞审批

- **WHEN** RocketMQ Proxy 停止运行
- **THEN** 审批提交成功
- **AND** 通知事件保留在 outbox 中等待补投

### Requirement: Seata 网络隔离编排

编排 SHALL 为 Seata 建立专用 `tx` 网络，使控制面只对 `iboot-workflow` 与 `iboot-system` 可达。

- `seata-server` MUST 只加入 `tx`；MUST NOT 加入 `data`。
- `iboot-workflow` 与 `iboot-system` MUST 加入 `tx`；其余服务 MUST NOT 加入 `tx`。
- Seata 的 8091 MUST 继续只绑定宿主机回环地址，MUST NOT 新增跨网卡发布。
- 编排 MUST NOT 为 Seata 伪造原生 TLS/认证；文档 MUST 把其登记为显式例外。

#### Scenario: 事务参与方可解析 Seata

- **WHEN** `iboot-workflow` 或 `iboot-system` 连接 `seata-server:8091`
- **THEN** 连接成功
- **AND** Seata AT/TCC 回归通过

#### Scenario: 其它服务无法解析 Seata

- **WHEN** 非 `tx` 网络服务尝试解析或连接 `seata-server:8091`
- **THEN** 解析或连接失败
- **AND** 该结果由暴露面回归覆盖

#### Scenario: Seata 端口仍只回环发布

- **WHEN** 检查宿主机端口映射
- **THEN** 8091 只绑定 `127.0.0.1`
- **AND** 其它主机无法连接

## MODIFIED Requirements

### Requirement: 宿主机端口按暴露等级绑定

部署 SHALL 只把门户的 HTTP/HTTPS 端口绑定到全部网卡，其余发布到宿主机的端口 MUST 只绑定回环地址；不需要宿主机访问的端口 MUST NOT 发布。

- 门户的 80 与 443 MUST 绑定 `0.0.0.0`，作为唯一跨网卡入口。
- 业务服务、中间件、控制台与可观测 UI 的宿主端口 MUST 绑定 `127.0.0.1`。
- RocketMQ MUST 只发布 TLS 前置代理的 `127.0.0.1:18081`；Proxy 的原生 8080/8081 MUST NOT 发布。
- Logstash、SkyWalking Banyandb、Thanos 组件、各 exporter 与 SkyWalking OAP 的内部端口 MUST NOT 发布到宿主机。
- 宿主机开发 MUST 继续通过 `localhost` 访问其所需的中间件与服务端口；MUST NOT 依赖机器 LAN 地址。

#### Scenario: 只有门户对其它主机可见

- **WHEN** 从同一网段的另一台主机访问本机端口
- **THEN** 只有 80 与 443 可连接
- **AND** MySQL、Redis、Elasticsearch、Nacos、Kafka 与服务端口均不可连接

#### Scenario: 宿主机开发仍可访问中间件

- **WHEN** 宿主机进程通过 `localhost` 连接 MySQL、Redis、Silo、Elasticsearch、Nacos、Seata、Kafka 或 RocketMQ TLS 入口
- **THEN** 连接成功
- **AND** 无需修改现有地址或新增启动参数

#### Scenario: 内部端口不再发布

- **WHEN** 检查编排容器的端口映射
- **THEN** Logstash、SkyWalking Banyandb、Thanos 组件与 exporter 没有宿主机映射
- **AND** Prometheus 在编排网络内完成抓取

#### Scenario: RocketMQ 只发布 TLS 入口

- **WHEN** 检查 RocketMQ 的宿主机端口
- **THEN** 只有 `127.0.0.1:18081` 映射到 `rocketmq-tls`
- **AND** 18080 与 Proxy 的 8081 不再映射

### Requirement: 容器网络按角色分段

编排 SHALL 把容器划分到 `edge`、`app`、`data`、`obs`、`mq` 与 `tx` 网络，每个服务 MUST 只加入其职责所需的最少网络。

- 门户 MUST 只加入 `edge`，并 MUST NOT 加入承载数据面的网络。
- 网关 MUST 加入 `edge` 与 `app`；需要注册中心或配置中心的组件 MUST 同时加入 `data`。
- 业务服务 MUST 加入 `app`，并按实际依赖加入 `data`；`iboot-workflow` 与 `iboot-system` 另须加入 `tx`。
- MySQL、Redis、Silo、Elasticsearch、Nacos、Kafka 与 Tempo MUST 只加入 `data`。
- RocketMQ NameServer、Broker、Proxy 与初始化任务 MUST 只加入 `mq`；`rocketmq-tls` MUST 加入 `app` 与 `mq`。
- Seata MUST 只加入 `tx`；其它服务 MUST NOT 加入 `tx`。
- 可观测组件 MUST 按抓取或存储关系加入 `obs`，需要访问数据面时再加入 `data`。

#### Scenario: 门户不能直达数据面

- **WHEN** 从门户容器尝试连接 MySQL、Redis、Elasticsearch 或 Nacos
- **THEN** 连接失败
- **AND** 门户仍能通过 `edge` 访问网关、系统服务与运维控制台

#### Scenario: 业务服务可访问其依赖

- **WHEN** 业务服务连接 MySQL、Redis、Nacos、Silo 或其调用的其它服务
- **THEN** 连接成功
- **AND** 不因网络分段出现解析或超时错误

#### Scenario: 可观测组件仍能抓取与存储

- **WHEN** Prometheus 抓取业务服务与 exporter，Grafana 查询 Elasticsearch 与 Prometheus
- **THEN** 抓取与查询成功
- **AND** Thanos 与 Tempo 仍可读写 Silo

#### Scenario: 非事务服务不能直达 Seata

- **WHEN** `iboot-file` 等非 `tx` 服务尝试连接 `seata-server:8091`
- **THEN** 解析或连接失败
- **AND** `iboot-workflow` 与 `iboot-system` 仍可连接

#### Scenario: 应用不能直连 RocketMQ Proxy

- **WHEN** `iboot-file` 尝试连接 `rocketmq-proxy:8081`
- **THEN** 解析或连接失败
- **AND** 它仍可通过 `rocketmq-tls:8081` 使用 TLS 入口

### Requirement: 暴露面回归门禁

回归 SHALL 检查宿主机端口绑定、非回环地址可达性、容器网络连通矩阵与 mq/tx 隔离，任一不符合 MUST 导致失败。

- 检查 MUST 断言只有门户 80/443 绑定 `0.0.0.0`，其余已发布端口绑定回环地址。
- 检查 MUST 断言通过宿主机非回环地址访问内部端口失败。
- 检查 MUST 在容器运行时执行门户到数据面的负向连通性，以及业务服务到依赖的正向连通性。
- 检查 MUST 验证非 `tx` 服务无法访问 Seata、非 `mq` 服务无法直连 RocketMQ Proxy，且 TLS 入口可用。
- 检查 MUST 验证 RocketMQ 18081 明文连接失败、TLS 握手成功。
- 容器不可用时 MUST 明确跳过容器矩阵，但仍执行端口绑定检查。
- 检查 MUST 接入全量回归与 CI，不得只作为手工步骤。

#### Scenario: 内部端口意外公开

- **WHEN** 任一内部端口绑定到 `0.0.0.0`
- **THEN** 暴露面检查失败并指出该端口
- **AND** 环境不得被判定为可验收

#### Scenario: 网络分段被破坏

- **WHEN** 门户容器能够连接 MySQL、Redis、Elasticsearch 或 Nacos
- **THEN** 网络矩阵检查失败并指出可达的目标
- **AND** 不会只告警后继续

#### Scenario: mq/tx 隔离被破坏

- **WHEN** 非 `tx` 服务能连接 Seata，或非 `mq` 服务能直连 RocketMQ Proxy
- **THEN** 暴露面检查失败并指出越界的源与目标
- **AND** 环境不得被判定为可验收

#### Scenario: 容器未运行

- **WHEN** 宿主机开发模式只运行了部分容器
- **THEN** 端口绑定检查仍然执行
- **AND** 容器网络矩阵被明确标记为跳过

### Requirement: RocketMQ 开发栈与启动隔离

本地编排 SHALL 提供 RocketMQ NameServer、Broker、Proxy、TLS 前置代理与一次性 topic 初始化。TLS 前置代理 MUST 向宿主机提供可达的 gRPC 端点；topic 与消费者组 MUST 在消费者首次订阅前存在或可自动创建。RocketMQ MUST NOT 作为任何业务服务的启动依赖：其不可用 MUST NOT 阻止服务启动、审批提交或通知事件的持久化。

#### Scenario: 干净环境启动

- **WHEN** 从干净环境启动核心编排
- **THEN** NameServer、Broker、Proxy 与 TLS 前置代理达到健康状态
- **AND** 通知 topic 与消费者组已创建
- **AND** 业务服务全部启动

#### Scenario: broker 未就绪不阻塞业务启动

- **WHEN** RocketMQ broker 尚未就绪或启动失败
- **THEN** 业务服务仍然启动并处理请求
- **AND** 审批提交成功，通知事件保留在 outbox 中等待补投

#### Scenario: 宿主机应用连接 Proxy

- **WHEN** 应用以宿主机进程方式运行并按映射端口连接 TLS 前置代理
- **THEN** 前置代理返回的路由端点可从宿主机访问
- **AND** 生产者与消费者可完成收发

#### Scenario: topic 尚未创建时消费者不拖垮服务

- **WHEN** 消费者启动时通知 topic 尚不存在
- **THEN** 消费者在后台重试订阅
- **AND** 通知服务本身保持启动并可处理其它请求
