# Design

## Context

动机见 `proposal.md`。当前相关事实：

- `ApprovalApplicationService.submit` 是 `@Transactional`，在事务内调用 `NotificationPort.enqueue`；`HttpNotificationPort` 用 `TransactionSynchronization.afterCommit` 把登记推迟到提交后，在虚拟线程里做 3 次指数退避，失败只记 `notification_trigger_failed` 与日志。
- `iboot-notify` 的 `sys_notification_delivery` 已经是投递侧 outbox：唯一键 `uk_event_channel_recipient` + `INSERT IGNORE` 保证幂等，`selectDispatchableIdsByEventId` 只取 `PENDING`/`FAILED`，终态不会被重复调度。
- 数据边界：所有服务共用 `iboot` 库，每张表只有一个所有者；workflow 的业务表前缀为 `biz_`，DDL 统一由 `iboot-db-init` 的 Flyway 迁移执行。
- 本地中间件由 `docker/docker-compose.yml` 承载；应用默认以宿主机进程运行，容器内 Proxy 返回的容器内网地址宿主机不可达。
- 版本基线：Boot 4.0.3、Spring Framework 7.0.5、JDK 25.0.3；RocketMQ 尚未进入版本矩阵。

**Spike 结论（2026-11-08，一次性环境，未进仓库）**

- `rocketmq-client-java:5.2.2` 在 Boot 4.0.3 + JDK 25 上可编译、启动并完成生产者/消费者往返；`enableVirtualThreads(true)` 可用。
- 该客户端把 gRPC 与 Guava shade 进自身 jar；唯一未 shade 的传递依赖是 `protobuf-java:4.32.0`（其生成代码的运行时依赖）。Boot 4 BOM 不管理该版本，实测无冲突。
- `ClientConfiguration` 的 SSL 默认是 **true**；本地明文 Proxy 必须显式 `enableSsl(false)`。
- Proxy 的 gRPC 端口是 **8081**（不是 8080）；Docker 场景必须设置 `useEndpointPortFromRequest=true`，否则路由返回 `172.18.0.x:8081`，宿主机不可达。
- 消费者启动时 topic 不存在会直接启动失败；必须先建 topic，或让消费者后台重试。
- `rocketmq-spring-boot-starter` / `rocketmq-v5-client-spring-boot-starter` 2.3.6 的父 POM 仍是 Boot 2.7.18 / Spring 5.3 / Java 8，不可用于本项目。

## Goals / Non-Goals

**Goals:**

- 审批提交与通知事件持久化原子化；broker 或通知服务停机时审批照常成功，恢复后自动补投。
- 重复发布、重复消费不产生重复投递；失败可重试、可进入死信、可按事件 ID 检索。
- 业务服务启动不依赖 RocketMQ 健康状态。
- 本地与容器化两种运行方式共用同一份客户端配置语义。

**Non-Goals:**

- 不引入 RocketMQ Dashboard；死信与积压先通过 `mqadmin`、指标与日志检索。
- 不把通知域之外的事件（审批通过/驳回、消息公告）迁到 MQ。
- 不替换 `sys_notification_delivery` 的投递重试；本次只解决「登记动作不丢」。
- 不做 Seata、Kafka 或 SkyWalking；不改前端与 API 契约。

## Decisions

### D1：生产方 outbox，而不是事务消息或提交后直发

提交后直发无法满足「broker 停机时审批不受影响、恢复后自动补投」：`send` 在 broker 不可用时直接失败，事件就丢了。RocketMQ 事务消息把半消息与本地事务绑定，但半消息发送本身要求 broker 在线，同样无法在 broker 停机时保持审批可用。

选择本地 outbox：审批事务内写 `biz_notification_outbox`，事务提交即代表事件已持久化；中继独立重试发布。代价是多一张表、一个中继与清理/死信运维，但换来的是与 broker 可用性彻底解耦。备选「直接发 + 进程内重试」被否，原因是它只是把当前的丢失窗口换了个位置。

### D2：使用官方 gRPC 客户端手写 Bean，不用 Spring Boot Starter

`rocketmq-client-java:5.2.2` 无 Spring 依赖，直接声明为普通依赖即可；Starter 停留在 Boot 2.7 基线，不能为它回退 Boot 4（违反路线图原则 6）。

- 生产者与消费者都不做成 `@Bean` 的构造期强依赖：`RocketMqNotificationPublisher` 懒创建 `Producer`，失败后关闭并按下次调用重建；`RocketMqConsumerLifecycle` 实现 `SmartLifecycle`，在虚拟线程里后台重试创建 `PushConsumer`。这样 broker 未就绪不会拖垮服务启动。
- 客户端配置：`endpoints` 来自 `IBOOT_ROCKETMQ_ENDPOINTS`（宿主机默认 `localhost:18081`，容器内默认 `rocketmq-proxy:8081`）；`enableSsl(false)` 由 `iboot.rocketmq.ssl-enabled` 控制，默认 false；`enableVirtualThreads(true)`。
- 版本固定：服务端 `apache/rocketmq:5.3.1`（spike 已验证），客户端 `5.2.2`。升服务端或客户端都必须重跑 spike 与集成测试。

### D3：outbox 表与中继状态机

表 `biz_notification_outbox`（workflow 所有）：

| 列 | 说明 |
|---|---|
| `id` | 自增主键 |
| `event_id` | 业务事件 ID，唯一键；审批待办为 `approval-todo-<processInstanceId>` |
| `event_type` | 事件类型，与消息 tag 一致 |
| `payload` | 通知命令 JSON（eventId/eventType/templateCode/recipientId/params） |
| `status` | `PENDING` / `SENDING` / `SENT` / `DEAD` |
| `attempts` / `max_attempts` | 已尝试次数与上限（默认 10） |
| `next_retry_time` | 指数退避的下次可投递时间 |
| `last_error` | 脱敏后的最后一次错误，截断到 1000 字符 |
| `message_id` | broker 返回的 messageId，便于按事件追查 |
| `create_time` / `update_time` | 时间戳 |

索引：`uk_event_id(event_id)`、`idx_status_retry(status, next_retry_time)`。

状态机与并发：

1. 中继在一个短事务里用 `SELECT ... FOR UPDATE SKIP LOCKED` 取一批 `PENDING`（且 `next_retry_time` 已到），改成 `SENDING` 后立即提交，**不在持锁期间做网络 I/O**。
2. 事务外逐条发送；成功更新 `SENT` 并写 `message_id`；失败把 `attempts+1`、按指数退避写 `next_retry_time` 并回到 `PENDING`，达到上限则 `DEAD`。
3. 中继启动时与每轮开始前把超过租约（默认 5 分钟）的 `SENDING` 重置为 `PENDING`；崩溃恢复后最多产生一次重复发布，由消费侧幂等吸收。
4. 多实例同时轮询时，`SKIP LOCKED` 保证同一条记录只被一个实例领取。

中继本身是 workflow 内的基础设施轮询器（`SmartLifecycle` + 虚拟线程 + 固定间隔），不接入 Quartz，也不承载业务定时任务；`iboot-job` 的「定时职责」边界不变。

### D4：消息契约与消费语义

- 主题 `iboot-notification`，消息键 `eventId`，tag = 事件类型（当前只有 `APPROVAL_TODO`）。
- 消息体复用现有 HTTP 合同的字段名：`eventId`、`eventType`、`templateCode`、`recipientId`、`params`。不新增共享模块；生产方与消费方各自持有 DTO，字段名由本设计固定。
- `iboot-notify` 用 `PushConsumer`，消费者组 `iboot-notify-consumer`；监听器把消息体映射为 `NotificationCommand`，调用现有 `NotificationApplicationService.enqueue`。
- 任何异常返回 `ConsumeResult.FAILURE`，交给 RocketMQ 重试；超过最大接收次数进入 `%DLQ%iboot-notify-consumer`。不在本次做「永久错误直接 ack」的分类——模板停用、接收人不存在也会走完有限次重试后进入死信，保证事件可查、可人工重放；若后续噪声明显，再单独加分类。
- 幂等由两层保证：登记侧 `uk_event_channel_recipient` + `INSERT IGNORE`；调度侧 `selectDispatchableIdsByEventId` 只取 `PENDING`/`FAILED`，终态记录不会被重复发送。

### D5：滚动升级与兼容层

发布顺序（先扩展、后切换）：

1. 执行 `V4__workflow_notification_outbox.sql` 建表（只加表与索引）。
2. 先发布 `iboot-notify`：新增消费者，同时保留 `/internal/notifications` 端点与入口流控。
3. 再发布 `iboot-workflow`：outbox 写入 + 中继生效，删除 `HttpNotificationPort`。
4. 全部 workflow 实例升级完成后，观察一个发布周期；后续变更再删除内部端点与其 Sentinel 规则。

这样新旧实例混跑时：新 workflow → 旧 notify 只会把消息留在 broker，待 notify 升级后消费；旧 workflow → 新 notify 仍走保留的 HTTP 端点，不会丢通知。

回滚：先设 `iboot.workflow.outbox.relay.enabled=false` 暂停中继并排空 `PENDING`，再回滚 workflow；notify 消费者可保留，继续消费已发布消息。数据库表保留，不做收缩迁移。未排空的事件在回滚后需要人工重放，文档必须写明。

### D6：可观测性

- workflow：`notification_outbox_pending`、`notification_outbox_sent_total`、`notification_outbox_retry_total`、`notification_outbox_dead_total`；日志带 `eventId`、`attempts`、`messageId`。
- notify：`notification_consume_success_total`、`notification_consume_failure_total`，标签 `application=iboot-notify`；失败日志带 `eventId` 与消息 ID。
- 死信检索：`mqadmin queryMsgByKey -n <namesrv> -t %DLQ%iboot-notify-consumer -k <eventId>`；积压与死信数量由上述指标与 `mqadmin` 共同确认。

## Risks / Trade-offs

- **[客户端 5.2.2 与服务端 5.3.1 协议漂移]** → 两端版本同时固定进 ROADMAP；集成测试覆盖生产/消费往返、虚拟线程与重复投递；升级任一端必须重跑 spike。
- **[Docker 内 Proxy 返回不可达地址]** → compose 挂载 `useEndpointPortFromRequest=true`；宿主机映射 `18081:8081`，容器内应用使用 `rocketmq-proxy:8081`；文档分别给出两种 endpoints。
- **[中继 SENDING 租约期间崩溃]** → 启动与每轮重置超租约记录；最多重复发布一次，消费侧幂等吸收。
- **[outbox 无限增长]** → `SENT` 记录保留期（默认 7 天）由清理任务删除；`DEAD` 不自动删除，需人工处理后清理。清理任务与指标同批实现。
- **[中继轮询增加数据库压力]** → 默认 1 秒间隔、批量 20、`idx_status_retry` 覆盖查询；按需调大间隔，且不影响审批线程。
- **[通知服务启动时 topic 不存在]** → `rocketmq-init` 一次性建 topic 与消费者组；消费者仍以后台重试兜底，服务启动不失败。
- **[本机内存 +3 进程]** → 计入 R4；Broker/Proxy 堆上限 512MB/256MB，NameServer 256MB；不引入 Dashboard。

## Migration Plan

见 D5。补充验证顺序：建表 → 起 RocketMQ 栈与 topic → 发布 notify → 发布 workflow → 验证 broker 停机/恢复、重复消息、消费失败进死信、审批事务回滚不落 outbox → 更新 ROADMAP 与 docs/23。
