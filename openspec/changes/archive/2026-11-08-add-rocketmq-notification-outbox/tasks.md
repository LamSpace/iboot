# Tasks

## 1. 依赖与客户端底座

- [x] 1.1 完成 Boot 4.0.3 + JDK 25 + `rocketmq-client-java:5.2.2` 一次性 spike：生产/消费往返成功、虚拟线程可用、无 BOM 冲突；结论已写入 `design.md` 的 Spike 结论。
- [x] 1.2 在 `iboot-workflow` 与 `iboot-notify` 的 `pom.xml` 增加 `rocketmq-client-java`（版本走父 POM 属性），验证 `mvn -s /home/lam/repo/settings.xml -pl iboot-workflow,iboot-notify -am -DskipTests package` 成功；`mvn dependency:tree` 确认 gRPC/Guava 已 shade，仅 `protobuf-java:4.32.0` 为客户端运行时依赖且与 Boot 4 BOM 无冲突。
- [x] 1.3 为两个服务增加 `RocketMqProperties`（endpoints、ssl-enabled、topic、consumer-group、relay 开关与批量参数），用 `ApplicationContextRunner` 或配置绑定测试断言默认值与环境变量覆盖。

## 2. 数据库与 outbox 持久化

- [x] 2.1 新增 `iboot-db-init/src/main/resources/db/migration/V4__workflow_notification_outbox.sql`，创建 `biz_notification_outbox` 与 `uk_event_id`、`idx_status_retry`；验证迁移在空库与已有 V1–V3 的库上都能执行，且 `SchemaMigratorTest` / 集成测试通过。
- [x] 2.2 在 `iboot-workflow` 增加 outbox 实体、仓储与 MyBatis Mapper：`insert`、`claimBatch`（`FOR UPDATE SKIP LOCKED`）、`markSent`、`markRetry`、`markDead`、`resetStaleSending`、`countByStatus`、`deleteSentBefore`；单测覆盖状态流转、并发 claim 与唯一键冲突。
- [x] 2.3 把 `NotificationPort` 契约改为「在调用方事务内持久化事件」，实现 `OutboxNotificationPort`；调整 `ApprovalApplicationService.submit` 的注释与兜底逻辑；单测验证提交成功写 outbox、事务回滚不写 outbox、事件 ID 稳定。

## 3. 中继与发布器

- [x] 3.1 实现 `RocketMqNotificationPublisher`：懒创建 `Producer`、`enableSsl` 与虚拟线程配置、失败关闭并按下次调用重建；用可注入的发送函数做单测，断言首次失败不会抛到审批线程。
- [x] 3.2 实现 `OutboxRelay`（`SmartLifecycle` + 虚拟线程 + 固定间隔）：claim 短事务、事务外发送、成功/退避/DEAD 更新、启动与每轮重置超租约 `SENDING`；单测覆盖正常、broker 不可用、达到上限、崩溃恢复与多实例 claim。
- [x] 3.3 增加中继指标（pending/sent/retry/dead）与 `SENT` 保留期清理任务；用 `SimpleMeterRegistry` 断言指标名称与标签，清理任务用固定时钟测试。
- [x] 3.4 删除 `HttpNotificationPort` 及其测试，移除 `iboot-workflow:notify-enqueue` 的 Nacos 降级规则与相关文档引用；验证 `grep -r "HttpNotificationPort"` 无命中，`mvn -s /home/lam/repo/settings.xml -pl iboot-workflow test` 通过。

## 4. 通知消费侧

- [x] 4.1 在 `iboot-notify` 增加消息 DTO 与 `RocketMqNotificationConsumerLifecycle`：后台重试创建 `PushConsumer`，broker 不可用或 topic 不存在时服务仍启动；用可 mock 的消费者工厂测试启动失败后的重试与关闭。
- [x] 4.2 实现监听器：消息体映射为 `NotificationCommand` 并调用 `NotificationApplicationService.enqueue`；单测覆盖正常消息、重复消息（`INSERT IGNORE` 幂等）、模板停用与接收人不存在（返回 FAILURE）。
- [x] 4.3 增加消费成功/失败指标与带 `eventId`、messageId 的日志；补充死信查询文档样例，验证按事件 ID 能查到 `%DLQ%iboot-notify-consumer` 中的消息。

## 5. 本地基础设施

- [x] 5.1 在 `docker/docker-compose.yml` 增加 `rocketmq-namesrv`、`rocketmq-broker`、`rocketmq-proxy`、`rocketmq-init`，限制堆内存并配置健康检查；挂载 `useEndpointPortFromRequest=true` 的 proxy 配置；`docker compose -f docker/docker-compose.yml config` 校验通过，`up -d` 后三个组件健康。
- [x] 5.2 `rocketmq-init` 创建 `iboot-notification` topic 与 `iboot-notify-consumer` 消费者组；验证 `mqadmin topicList` 与 `topicRoute` 能看到 topic，重复执行初始化幂等。
- [x] 5.3 在 `.env.example` 增加 `ROCKETMQ_ENDPOINTS` 等占位，在 `scripts/dev-init.sh` 增加 RocketMQ 健康等待与跳过开关；全新环境执行 `dev-init.sh` 后业务服务全部启动，broker 未就绪时服务仍能启动。

## 6. 端到端验证

- [x] 6.1 验证 broker 停机场景：停 broker 后提交请假，审批成功且 outbox 有 `PENDING`；恢复 broker 后事件变 `SENT`，审批人收到通知。
- [x] 6.2 验证重复投递场景：手工重发同一 `eventId` 消息，投递记录与站内消息均不重复。
- [x] 6.3 验证失败与死信场景：让消费者持续失败，确认重试计数增加、最终进入 `%DLQ%iboot-notify-consumer`，并可按事件 ID 检索。
- [x] 6.4 按 D5 顺序演练一次「先 notify 后 workflow」的滚动发布与回滚：暂停中继、排空 outbox、回滚 workflow 后旧 HTTP 路径仍可用（实际旧版本二进制回滚未执行，兼容层用内部端点验证）。

## 7. 文档与路线图

- [x] 7.1 在 `docs/23-微服务底座操作指南.md` 增加 RocketMQ 接入、topic/group、Proxy 端点、死信查询、发布/回滚顺序与 outbox 运维说明；按文档命令逐条执行成功。
- [x] 7.2 更新 `ROADMAP.md`：§3 移除 RocketMQ 未完成项，§4 增加服务端/客户端版本，R4 更新常驻进程数，§7 记录本次 outbox 决策。
- [x] 7.3 更新 `README.md` 与 `scripts/dev-init.sh` 的启动说明，验证文档中的命令与默认端口（`18081` / `rocketmq-proxy:8081`）一致。

## Workflow follow-up

- 观察一个发布周期后，另起变更删除 `iboot-notify` 的 `/internal/notifications` 端点、内部令牌配置与对应 Sentinel 入口流控规则。
- 归档本变更前确认 `openspec validate --change add-rocketmq-notification-outbox --strict` 通过。
