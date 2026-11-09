# Spec Delta

## ADDED Requirements

### Requirement: Kafka 日志缓冲栈与启动隔离

本地编排 SHALL 提供 KRaft 模式的单进程 Kafka，作为集中日志链路的缓冲层。Kafka MUST NOT 作为任何服务的启动依赖：其不可用 MUST NOT 阻止服务启动或请求处理。

- Kafka MUST 以 KRaft 模式运行，MUST NOT 部署 ZooKeeper。
- Kafka MUST 使用编排网络服务名对外提供地址，并 MUST 额外提供宿主机可达的监听地址，供宿主机进程与运维工具连接。
- Kafka 数据 MUST 使用命名卷持久化，普通停止或重启 MUST NOT 删除。
- Kafka 的容器内存上限 MUST 明确配置，使本机资源占用可预期。
- 健康检查 MUST 反映 broker 能否接受生产与消费，而不是仅检查容器进程存在。
- 集中日志系统与消息队列任一不可用，MUST NOT 阻止业务服务启动。

#### Scenario: 干净环境启动

- **WHEN** 从干净环境启动核心编排
- **THEN** Kafka 达到健康状态，日志 topic 初始化完成
- **AND** 业务服务全部启动并通过健康检查

#### Scenario: broker 未就绪不阻塞业务启动

- **WHEN** Kafka 尚未就绪或启动失败
- **THEN** 业务服务仍然启动并处理请求
- **AND** 控制台与文件日志保持可用

#### Scenario: 宿主机进程连接 Kafka

- **WHEN** 应用或运维工具以宿主机进程方式按映射端口连接 Kafka
- **THEN** 返回的路由地址可从宿主机访问
- **AND** 生产者与消费者可完成收发

#### Scenario: 健康检查反映真实可用性

- **WHEN** broker 进程存在但无法接受生产请求
- **THEN** 健康检查不将其判定为健康

### Requirement: 日志 topic 的初始化与保留

编排 SHALL 以一次性任务幂等创建日志 topic，并为其设定短保留期，使消息队列只承担缓冲而不承担长期存储。

- 初始化任务 MUST 在服务投递日志之前完成，重复执行 MUST 成功退出且不重建已有 topic。
- 日志 topic MUST 固定分区数与副本数，MUST NOT 依赖自动创建 topic。
- 日志 topic 的保留期 MUST 为 24 小时；超出保留期的记录 MUST 被清理。
- 集中日志系统的保留期 MUST 独立配置，MUST NOT 与消息队列保留期绑定。
- 消费组 MUST 在重启后从已提交位点继续消费，无需重建 topic。

#### Scenario: 重复执行初始化

- **WHEN** 在已存在日志 topic 的环境中再次执行初始化任务
- **THEN** 任务以 0 退出
- **AND** topic 的分区数与保留期保持不变

#### Scenario: 保留期到期清理

- **WHEN** 日志在 topic 中存放超过 24 小时
- **THEN** 超出保留期的记录被清理
- **AND** 集中日志系统中的历史记录不受影响

#### Scenario: 消费者重启后续消费

- **WHEN** 日志消费进程重启
- **THEN** 从已提交位点继续消费
- **AND** 无需重建 topic 或重新初始化
