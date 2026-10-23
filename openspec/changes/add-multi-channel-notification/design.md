# Design

## Context

- 现有站内消息链路：`MessageApplicationService` 保存 `sys_message` / `sys_message_receiver` 后发布 `MessageSentEvent`，`MessagePushListener` 在事务提交后通过 `PushEventService` 推送 SSE。`MessagePushListener` 当前用 `Map.of` 构造推送数据，不接受 `null` 的 `senderId`。
- 现有模板能力：`sys_message_template` + `MessageTemplateApplicationService.fillTemplate`，占位符为 `${key}`；Flyway 迁移中需写成 `$${key}` 以转义。
- 现有审批链路：`ApprovalApplicationService.submit` 启动流程并保存 `LeaveApplication`；审批人、申请人、请假类型、时间都在同一事务内可获得。
- 现有调度：Quartz + `sys_job`，`concurrent=0` 表示禁止并发；内容索引对账任务已提供可复用的注册方式。
- 现有 SPI 先例：`FileStorageStrategy` 定义在 domain，`FileStorageConfig` 在 infrastructure 选择实现。
- 约束：Boot 4.0.3、Java 25、DDD 四层、阶段 0 单体；不引入 MQ；短信因个人资质默认关闭；密钥只走环境变量。

## Goals / Non-Goals

**Goals:**

- 用统一 SPI 承载站内、邮件、钉钉、短信四个渠道，渠道启停只改配置。
- 用投递记录表实现可追踪、可重试、可人工干预的发送生命周期。
- 审批待办事件端到端跑通：站内消息中心、QQ/163 邮件、钉钉群。
- 失败在管理端可见，支持手动重试；自动重试由 Quartz 承担。
- 保持现有消息中心、SSE、模板表的 API 契约不破坏。

**Non-Goals:**

- 不实现短信真实送达，不申请阿里云签名/模板；只保留默认关闭的适配器。
- 不做用户级渠道偏好、免打扰、按用户选择渠道。
- 不做钉钉消息批量聚合；本期只做限流 + 重试。
- 不引入 RocketMQ；阶段 2 再把重投职责迁走。
- 不做审批通过/驳回结果通知；本期只做审批待办。
- 不改造现有消息中心 UI，只新增通知投递管理页。

## Decisions

### D1. 渠道 SPI 放在 domain，实现分散在 application / infrastructure

定义 `NotificationChannel` 端口：

- `NotificationChannelType channelType()`
- `boolean isEnabled()`
- `void send(NotificationDelivery delivery)`

实现：

- `InAppNotificationChannel` 放在 application 层，依赖 `MessageApplicationService`，因为站内渠道需要复用系统消息落库。
- `MailNotificationChannel`、`DingTalkNotificationChannel`、`SmsNotificationChannel` 放在 infrastructure，分别依赖 Spring Mail、JDK HttpClient、阿里云 SDK。
- 编排服务 `NotificationApplicationService` 只依赖端口列表，按 `channelType()` 选择实现，不写 `if (channel == ...)`。

备选：直接在审批服务里调用四个发送器。拒绝原因：渠道增加会持续修改业务服务，且无法统一记录与重试。

### D2. 投递记录表作为 outbox，登记与业务同事务

`ApprovalApplicationService.submit` 在保存 `LeaveApplication` 后调用 `NotificationApplicationService.enqueue(command)`。`enqueue` 在同一个数据库事务内为每个启用渠道插入 `sys_notification_delivery` 记录。

- 业务事务提交后发布 `NotificationQueuedEvent`，由 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 触发一次即时投递。
- 即使即时投递失败或进程崩溃，记录仍在表中，Quartz 会重试。
- 唯一键 `(event_id, channel, recipient_key)` 保证重复登记不会产生重复记录。

备选：提交后再用监听器登记。拒绝原因：提交与登记之间崩溃会永久丢通知。

权衡：通知登记失败会让审批一起回滚。登记只是一条 INSERT，故障面很小；相比丢通知，这个取舍可接受。

### D3. 投递状态与并发领取

状态：`PENDING`、`SENDING`、`SUCCESS`、`FAILED`、`DEAD`、`SKIPPED`。

- `PENDING`：已登记，等待首次或下一次尝试。
- `SENDING`：已被某个执行者领取，防止即时投递与 Quartz 重复发送。
- `SUCCESS`：渠道返回成功。
- `FAILED`：本次失败，仍有重试机会。
- `DEAD`：达到最大尝试次数，等待人工处理。
- `SKIPPED`：接收人缺少地址或渠道在发送时已禁用，不重试。

领取使用条件更新：

```sql
UPDATE sys_notification_delivery
SET status = 'SENDING', update_time = NOW()
WHERE id = ?
  AND status IN ('PENDING', 'FAILED')
  AND (next_retry_time IS NULL OR next_retry_time <= NOW())
```

受影响行数为 1 才继续发送。Quartz 启动时把 `SENDING` 且 `update_time` 超过 5 分钟的记录重置为 `FAILED`，错误写“发送超时，等待重试”，避免进程崩溃后永久卡住。

### D4. 站内渠道复用系统消息与现有 SSE

在 `MessageApplicationService` 增加 `sendSystemMessage(title, content, messageType, priority, userIds)`：

- 不读取 `SecurityUtils`，可在无登录上下文的后台线程调用。
- `senderType="0"`、`senderId=null`、`createBy="system"`。
- 保存消息与接收记录、标记已发送，发布现有 `MessageSentEvent` 与 `ContentIndexEvent`。
- 返回消息 ID，写回投递记录的 `message_id`；重试时若已有 `message_id`，直接复用，不重复创建。

把 `sendMessage` 中“保存接收记录 → 标记发送 → 发布事件”的逻辑抽成私有方法，两个入口共用；不改变现有 `sendMessage` 的外部行为。

必须修复 `MessagePushListener` 对 `senderId=null` 的处理：把 `Map.of` 改为可空 `LinkedHashMap`，或在 `senderId` 为 `null` 时不放该键。

备选：站内渠道直接调用 `PushEventService`。拒绝原因：刷新后通知消失，消息中心查不到，离线用户丢失通知。

### D5. 模板保持渠道无关，不新增 channel 列

复用 `sys_message_template`：

- 标题取 `template_name`，正文取 `fillContent(params)`。
- 新增 `APPROVAL_TODO` 模板，`message_type='2'`（待办提醒）。
- 邮件主题、钉钉文本、站内消息标题都使用模板名称；正文使用同一份渲染结果。
- 短信未来启用时使用阿里云模板 Code 与参数 JSON，不发送本地渲染的长文本；本期不涉及。

备选：给模板表加 `channel` 列并为每个渠道建一条模板。拒绝原因：当前四个渠道内容一致，加列会产生重复数据；等短信真正启用再按需扩展。

### D6. 邮件渠道

- 新增 `spring-boot-starter-mail`，由 Boot 4.0.3 管理版本。
- 配置映射：`spring.mail.host/port/username/password` 从 `MAIL_*` 环境变量注入。
- 渠道启用条件：`iboot.notification.email.enabled=true` 且 `spring.mail.host` 非空。
- 使用 `MimeMessageHelper`，UTF-8；主题为模板名称，正文为渲染内容；发件人取 `spring.mail.username`，可用 `iboot.notification.email.from` 覆盖。
- 收件人为空 → `SKIPPED`；认证失败、超时、连接失败 → `FAILED` 并重试。
- 错误摘要只保留异常类型和脱敏后的服务端响应。

### D7. 钉钉渠道

- 不引入钉钉 SDK；用 JDK `HttpClient` + `HmacSHA256` 实现加签。
- 配置：`iboot.notification.dingtalk.enabled`、`webhook`、`secret`、`keyword-prefix`（默认 `【iBoot】`）。
- 请求体使用 `msgtype=text`；正文前缀 `keyword-prefix`，兼容同时启用“自定义关键词”的机器人。
- 限流：Guava `RateLimiter.create(20.0 / 60.0)`，每次发送前 `tryAcquire(0, TimeUnit.SECONDS)`；失败抛出可重试异常，交给投递重试。
- 时间戳使用毫秒，签名为 `Base64(HMAC-SHA256(secret, timestamp + "\n" + secret))` 后再 URL 编码。
- Webhook、Secret、签名结果不写日志、不写投递记录；错误摘要只保留钉钉 `errcode` / `errmsg`。
- 集群下每个实例各自限流；阶段 2 有多个实例时再改为 Redis 令牌桶。

### D8. 短信渠道

- 依赖 `com.aliyun:dysmsapi20170525`（实现时锁定 Maven Central 上的稳定版本，当前为 `4.1.1`）。
- 配置：`iboot.notification.sms.enabled=false`、`access-key-id`、`access-key-secret`、`sign-name`、`template-code`、`region`。
- `isEnabled()` 同时要求开关为 true 且四个必填项非空；任一缺失只记录启动日志，不阻止应用启动。
- 关闭时不登记短信投递；启用后手机号为空 → `SKIPPED`。
- 通过一个 `SmsClient` 小接口封装阿里云 `SendSms`，便于用 Fake 做契约测试；真实实现只发阿里云模板，不发送本地长文本。
- 错误按阿里云 `Code` 分类：限流、余额不足、签名未报备等统一记为 `FAILED`，保留脱敏后的错误码；达到最大次数进入 `DEAD`。

### D9. 重试策略

- 新增 `NotificationRetryTask.retry()`，通过 Flyway 写入 `sys_job`，cron `0 * * * * ?`，`concurrent=0`。
- 每轮最多处理 100 条：`status IN ('PENDING','FAILED') AND next_retry_time <= NOW()`，按 `next_retry_time` 升序。
- 失败后 `attempts = attempts + 1`；未达上限时 `next_retry_time = NOW() + base * 2^(attempts-1)`，默认 base 60 秒、上限 5 次。
- 达到上限置 `DEAD`，`next_retry_time=null`。
- 人工重试把 `attempts` 归零、`status='PENDING'`、`next_retry_time=NOW()`，并立即触发一次投递。
- 即时投递与定时任务共用同一个 `dispatch(deliveryId)`，通过 D3 的条件更新避免并发重复。

### D10. 失败可见与人工重试

后端：

- `GET /api/notification/delivery`：分页，支持 `status`、`channel`、`eventType`、`recipient`、`startTime`、`endTime`。
- `POST /api/notification/delivery/{id}/retry`：仅 `FAILED` / `DEAD` 可重试。
- 权限：`notification:delivery:list`、`notification:delivery:retry`。
- 返回字段：事件 ID、事件类型、渠道、接收人（脱敏）、模板编码、尝试次数、状态、下次重试时间、最后错误、创建/发送时间。
- 不返回 Webhook、Secret、AccessKey；内容字段按权限返回，前端默认只展示摘要。

前端：

- 新增 `views/message/NotificationDeliveryView.vue`，前端子路由为 `delivery`（访问路径 `/dashboard/delivery`），组件路径 `message/NotificationDeliveryView`。菜单路径 `/notification/delivery` 由侧边栏取最后一段 `delivery` 后映射到该路由，符合现有菜单约定。
- 站内审批待办消息增加 `business_type=APPROVAL_TODO`；点击铃铛消息或“我的消息”里的“查看审批”按钮跳转 `/dashboard/approval`。普通消息仍打开个人收件箱弹窗，不跳管理员的消息管理页。
- 新增 `api/notification.ts`，复用现有 axios 封装。
- 页面提供状态/渠道筛选、分页、最后错误查看和“重试”按钮。
- Flyway 新增菜单 9200（页面）与 9201（重试按钮），挂在消息管理（parent_id=4）下，授予管理员角色 1；V20 把早期放在监控管理下的 9200 移回消息管理。通知投递属于消息/通知运维，不是监控指标。
- 新增中英文 i18n 文案。

### D11. 配置与密钥

新增 `iboot.notification.*` 配置属性：

```yaml
iboot:
  notification:
    in-app:
      enabled: true
    email:
      enabled: true
      from: ${MAIL_FROM:}
    dingtalk:
      enabled: ${IBOOT_DINGTALK_ENABLED:false}
      webhook: ${IBOOT_DINGTALK_WEBHOOK:}
      secret: ${IBOOT_DINGTALK_SECRET:}
      keyword-prefix: "【iBoot】"
    sms:
      enabled: ${IBOOT_SMS_ENABLED:false}
      access-key-id: ${IBOOT_SMS_ACCESS_KEY_ID:}
      access-key-secret: ${IBOOT_SMS_ACCESS_KEY_SECRET:}
      sign-name: ${IBOOT_SMS_SIGN_NAME:}
      template-code: ${IBOOT_SMS_TEMPLATE_CODE:}
      region: ${IBOOT_SMS_REGION:cn-hangzhou}
    retry:
      max-attempts: ${IBOOT_NOTIFICATION_RETRY_MAX:5}
      backoff-seconds: ${IBOOT_NOTIFICATION_RETRY_BACKOFF_SECONDS:60}
      batch-size: 100
```

邮件 SMTP 继续使用 Spring Boot 标准 `spring.mail.*`，从 `MAIL_*` 注入。所有密钥只出现在环境变量与 `NotificationProperties` 中，不写 `application.yml` 字面量、不写数据库。

### D12. 数据模型

新增 `sys_notification_delivery`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT PK | 自增 |
| `event_id` | VARCHAR(64) | 业务事件 ID，同一事件所有渠道共享 |
| `event_type` | VARCHAR(64) | 如 `APPROVAL_TODO` |
| `channel` | VARCHAR(20) | `IN_APP` / `EMAIL` / `DINGTALK` / `SMS` |
| `recipient_key` | VARCHAR(128) | `USER:123` 或 `GROUP:default`，用于唯一约束 |
| `recipient_id` | BIGINT NULL | 用户 ID；群渠道为空 |
| `recipient_address` | VARCHAR(255) NULL | 邮箱/手机号；群渠道为空 |
| `template_code` | VARCHAR(100) | 模板编码 |
| `title` | VARCHAR(200) | 模板名称/消息标题 |
| `content` | TEXT | 渲染后的内容，冻结重试时的文本 |
| `params_json` | TEXT | 模板参数 JSON，短信启用后使用 |
| `status` | VARCHAR(20) | 见 D3 |
| `attempts` | INT | 已尝试次数 |
| `max_attempts` | INT | 最大尝试次数 |
| `next_retry_time` | DATETIME NULL | 下次自动重试时间 |
| `last_error` | VARCHAR(1000) NULL | 脱敏后的最后错误 |
| `message_id` | BIGINT NULL | 站内消息 ID，用于重试复用 |
| `sent_time` | DATETIME NULL | 成功时间 |
| `create_time` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 更新时间 |

索引与约束：

- `UNIQUE KEY uk_event_channel_recipient (event_id, channel, recipient_key)`
- `KEY idx_status_retry (status, next_retry_time)`
- `KEY idx_channel_create (channel, create_time)`

记录只追加，不做逻辑删除；管理页按时间倒序查询。

### D13. 错误分类与脱敏

- 渠道实现抛出 `NotificationDeliveryException`，带 `retryable` 标志与已脱敏的消息。
- 收件地址缺失、渠道关闭 → `SKIPPED`，不抛异常。
- 模板不存在/停用 → 在 `enqueue` 阶段抛 `BusinessException`，审批事务回滚，不创建半套投递。
- 认证失败、限流、超时、网络错误 → `FAILED`，进入重试。
- 参数错误、接收人格式非法 → `DEAD` 或 `SKIPPED`，避免无意义重试。
- 脱敏规则：邮箱保留首字符与域名，手机号保留前 3 后 4；Webhook/Secret/AccessKey 一律替换为 `***`。

### D14. 测试策略

单元测试（不依赖 Docker）：

- `NotificationApplicationServiceTest`：渠道 fan-out、禁用渠道不登记、缺地址 `SKIPPED`、重复登记幂等。
- `NotificationRetryTest`：失败退避、达到上限 `DEAD`、人工重试重置、`SENDING` 领取互斥。
- `InAppNotificationChannelTest`：复用 `message_id`，重试不创建第二条消息。
- `MailNotificationChannelTest`：Mock `JavaMailSender`，验证主题/正文/收件人，缺邮箱跳过。
- `DingTalkSignerTest`：固定 timestamp/secret 的签名向量；`DingTalkNotificationChannelTest` 用 Fake 客户端验证限流与失败分类。
- `SmsNotificationChannelTest`：默认关闭不发送；启用且缺手机号跳过；Fake `SmsClient` 验证参数。
- `NotificationDeliveryControllerTest`：权限、分页参数、重试状态校验。

端到端手工验收（使用 `.env` 中的真实 QQ SMTP 与钉钉机器人）：

1. 启动应用，确认三个渠道日志为 enabled，短信为 disabled。
2. 提交一条请假申请，审批人收到站内铃铛/消息中心、QQ 邮件、钉钉群消息。
3. 查询投递记录，三条均为 `SUCCESS`；短信无记录。
4. 临时把钉钉 Secret 改错，再提交一条，记录变为 `FAILED`；改回后手动重试，变为 `SUCCESS`。
5. 把某测试用户邮箱清空，再提交一条，邮件记录为 `SKIPPED`。

不新增 Testcontainers 镜像；数据库迁移由现有 Flyway 启动流程验证，避免触发本机 Docker Hub 拉取限制。

## Risks / Trade-offs

- **通知登记失败回滚审批** → 登记只做一次批量 INSERT，故障面小；如未来要求审批不被通知拖累，改为独立 outbox 事务并补对账。
- **重试可能重复发送** → 投递记录先领取再发送，成功即置 `SUCCESS`；极端情况下仍可能重复，邮件/钉钉按“至少一次”语义接受，站内渠道用 `message_id` 去重。
- **钉钉 20 条/分钟限流** → 超出即进入重试，可能积压；本期接受，后续做聚合或换企业机器人。
- **个人 QQ/163 SMTP 限额与垃圾邮件策略** → 只发测试用户，失败可见；生产应换企业邮箱或 DirectMail。
- **短信未真实验收** → 只用 Fake 与契约测试覆盖；ROADMAP 与验收结论必须明确标注“默认关闭、待企业资质”。
- **集群下限流器各自为政** → 阶段 0 单实例可接受；阶段 2 多实例时改 Redis 令牌桶。
- **菜单 ID / 权限冲突** → 使用未占用的 9200/9201，迁移中用 `WHERE NOT EXISTS` 防重复。
- **Actuator `env` 端点暴露** → 环境变量名包含 `SECRET`/`PASSWORD`，不开启 `show-values=ALWAYS`；本地可移除 `env` 暴露。
- **Flyway 占位符转义** → 模板 SQL 写成 `$${taskName}`，落库后为 `${taskName}`；迁移后手工核对一次。

## Migration Plan

1. 增加依赖、配置属性与环境变量映射；默认 `email/dingtalk` 按现有 `.env` 启用，`sms=false`。
2. 新增 Flyway `V18`：投递表、`APPROVAL_TODO` 模板、重试任务、菜单与权限；全部使用 `WHERE NOT EXISTS` 保证幂等。
3. 启动应用，Flyway 自动迁移；确认 `sys_job` 中出现 `notificationRetryTask.retry`，管理员菜单出现“通知投递”。
4. 用真实 QQ 邮箱与钉钉群完成端到端验收；短信只验证“无投递记录”。
5. 回滚策略：先把 `iboot.notification.*.enabled` 全部置 false，停止新投递；表与菜单可保留，不影响现有功能。需要彻底回退时，手工删除 `sys_job`、菜单、模板并 drop 投递表。
6. 阶段 2 迁移：把 Quartz 轮询替换为 RocketMQ 消费者，投递表继续作为 outbox，渠道 SPI 不变。

## Open Questions

无会改变规格或任务拆分的未决问题。以下留到后续增量：审批结果通知、短信真实启用、钉钉消息聚合、用户级渠道偏好。
