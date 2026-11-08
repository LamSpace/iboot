# Proposal

## Why

审批 → 通知的登记目前走 `HttpNotificationPort`：事务提交后异步发内部 HTTP，进程内最多重试 3 次，耗尽后只记日志与指标，事件永久丢失。通知服务宕机、模板停用、接收人不存在都只能在事后从日志里发现；阶段 2 收尾的 RocketMQ 项因此一直悬空。

`rocketmq-client-java` 5.2.2 已在 Boot 4.0.3 / JDK 25 上完成 spike：gRPC 生产者与消费者可收发、虚拟线程可用、无 Spring BOM 冲突。现在具备把这条链路换成「本地 outbox + RocketMQ + 幂等消费」的条件。

## What Changes

- **workflow 产出侧 outbox**：`NotificationPort` 从「HTTP 登记」改为「同事务写入 `biz_notification_outbox`」。审批提交、流程启动、申请落库与 outbox 事件在同一事务内提交；事务回滚时事件一起回滚。
- **后台中继**：workflow 内新增基础设施轮询器，按 `FOR UPDATE SKIP LOCKED` 抢占 PENDING 记录并投递到 RocketMQ；成功标记 `SENT`，失败按指数退避重排，超过上限标记 `DEAD`。broker 停机时审批不受影响，恢复后自动补投。
- **notify 消费侧**：新增 RocketMQ PushConsumer，消费审批待办事件并复用 `NotificationApplicationService.enqueue`。消息键为 `eventId`；重复投递由 `uk_event_channel_recipient` + `INSERT IGNORE` 幂等吸收；消费失败返回 FAILURE，由 RocketMQ 重试，超过上限进入 `%DLQ%<group>`。
- **移除 HTTP 产出适配器**：删除 `HttpNotificationPort`、其 Sentinel 熔断资源与对应 Nacos 降级规则。`iboot-notify` 的 `/internal/notifications` 端点保留一个发布周期，作为旧 workflow 实例的滚动升级兼容层，标记弃用后在后续变更删除。
- **本地基础设施**：`docker/docker-compose.yml` 增加 RocketMQ NameServer / Broker / Proxy 与一次性 topic 初始化；Proxy 启用 `useEndpointPortFromRequest`，宿主机应用通过映射端口接入；新增 `ROCKETMQ_*` 环境变量与 `.env.example` 占位。
- **可观测性**：新增 outbox 积压、发送失败、DEAD、消费失败与死信计数；日志带 `eventId` 与 broker messageId，支持按 key 检索。
- **文档与路线图**：ROADMAP §3 移除 RocketMQ 未完成项，§4 补版本基线，R4 更新常驻进程数；docs/23 补接入、topic/group、死信查询与发布/回滚顺序。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `notification`: 通知触发从「事务提交后 HTTP 登记」改为「事务内 outbox + MQ 异步登记」；新增可靠投递、消费幂等、失败重试与死信可查的要求；调整「通知触发与业务主流程解耦」的执行时点与失败观测语义。
- `approval-workflow`: 新增审批待办事件的 outbox 持久化与中继要求；「事务一致性」扩展为流程状态、申请记录与 outbox 事件同事务提交。
- `container-deployment`: 核心中间件增加 RocketMQ 三组件与 topic 初始化；明确 RocketMQ 不可用不得阻止业务服务启动。

## Impact

- `iboot-workflow`：`NotificationPort` 契约与实现、`ApprovalApplicationService` 调用点、outbox 实体/仓储/Mapper/中继、RocketMQ 发布器、配置与指标、单测与集成测试；删除 `HttpNotificationPort` 及其测试。
- `iboot-notify`：新增 RocketMQ 消费者、消息 DTO、配置与指标；保留内部端点一个发布周期；补充重复消息与消费失败测试。
- `iboot-db-init`：新增 `V4__workflow_notification_outbox.sql`，只加表与索引，符合「先扩展后收缩」。
- `docker`：compose 增加 `rocketmq-namesrv` / `rocketmq-broker` / `rocketmq-proxy` / `rocketmq-init`，挂载 proxy 配置；`.env.example`、`scripts/dev-init.sh` 健康等待。
- 文档/路线图：ROADMAP、docs/23、README 启动说明。
- 无前端、API 路径与响应结构变化，`portal` 零改动。
- 回滚：先暂停中继并排空 outbox，再回滚应用；数据库表保留，不执行收缩迁移。
