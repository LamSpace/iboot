# Spec Delta

## ADDED Requirements

### Requirement: RocketMQ 开发栈与启动隔离

本地编排 SHALL 提供 RocketMQ NameServer、Broker、Proxy 与一次性 topic 初始化。Proxy MUST 向宿主机可达的 gRPC 端点返回路由；topic 与消费者组 MUST 在消费者首次订阅前存在或可自动创建。RocketMQ MUST NOT 作为任何业务服务的启动依赖：其不可用 MUST NOT 阻止服务启动、审批提交或通知事件的持久化。

#### Scenario: 干净环境启动

- **WHEN** 从干净环境启动核心编排
- **THEN** NameServer、Broker 与 Proxy 达到健康状态
- **AND** 通知 topic 与消费者组已创建
- **AND** 业务服务全部启动

#### Scenario: broker 未就绪不阻塞业务启动

- **WHEN** RocketMQ broker 尚未就绪或启动失败
- **THEN** 业务服务仍然启动并处理请求
- **AND** 审批提交成功，通知事件保留在 outbox 中等待补投

#### Scenario: 宿主机应用连接 Proxy

- **WHEN** 应用以宿主机进程方式运行并按映射端口连接 Proxy
- **THEN** Proxy 返回的路由端点可从宿主机访问
- **AND** 生产者与消费者可完成收发

#### Scenario: topic 尚未创建时消费者不拖垮服务

- **WHEN** 消费者启动时通知 topic 尚不存在
- **THEN** 消费者在后台重试订阅
- **AND** 通知服务本身保持启动并可处理其它请求
