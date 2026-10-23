# Tasks

## 1. 依赖、配置与编码约定

- [x] 1.1 在 `iboot-admin/pom.xml` 增加 `spring-boot-starter-mail` 与 `com.aliyun:dysmsapi20170525`，版本由 Boot BOM / 显式属性管理；执行 `mvn -s /home/lam/repo/settings.xml -pl iboot-admin -am -DskipTests compile` 验证编译通过。
- [x] 1.2 新增 `NotificationProperties`（`iboot.notification.*`），覆盖站内、邮件、钉钉、短信、重试五组配置；新增配置绑定测试，验证环境变量占位符能绑定到属性对象。
- [x] 1.3 在 `application.yml` 增加 `spring.mail.*` 与 `iboot.notification.*` 映射，全部使用 `${ENV:default}` 占位；启动应用并检查日志中渠道启用状态（邮件/钉钉 enabled，短信 disabled）。
- [x] 1.4 新增/修改的 Java 类、方法、DTO 字段按 `CLAUDE.md` 第 5 节写中文 Javadoc；执行 `mvn -s /home/lam/repo/settings.xml -pl iboot-admin -am -DskipTests javadoc:javadoc` 验证无注释格式错误（或至少 `compile` 通过并人工核对类级 Javadoc）。

## 2. 数据模型与 Flyway 迁移

- [x] 2.1 新增 `V18__add_multi_channel_notification.sql`，创建 `sys_notification_delivery` 表、唯一键与查询索引；启动应用后执行 `SHOW CREATE TABLE sys_notification_delivery`，确认字段与索引存在。
- [x] 2.2 在同一迁移中插入 `APPROVAL_TODO` 模板（内容使用 `$${taskName}` 等 Flyway 转义写法）与 `notificationRetryTask.retry` 的 `sys_job` 记录，使用 `WHERE NOT EXISTS` 防重复；迁移后查询 `sys_message_template` 与 `sys_job` 确认各一条。
- [x] 2.3 新增领域枚举与模型：`NotificationChannelType`、`NotificationDeliveryStatus`、`NotificationEventType`、`NotificationDelivery`；新增单元测试覆盖状态判断与枚举转换。
- [x] 2.4 新增 `NotificationDeliveryPO`、Mapper XML、`NotificationDeliveryRepository` 与实现，覆盖插入、按 ID 查询、分页筛选、到期记录查询、条件领取、状态更新与重置；用 Mockito 单元测试验证条件参数传递，必要时用应用启动后的手工 SQL 冒烟验证。
- [x] 2.5 新增 `V18` 中的菜单 9200/9201 与权限 `notification:delivery:list`、`notification:delivery:retry`，授予管理员角色 1；登录管理端确认菜单可见，非管理员角色默认无权限。

## 3. 渠道 SPI 与通知编排

- [x] 3.1 新增 `NotificationChannel` 端口与 `NotificationDeliveryException`（含可重试标志、脱敏消息）；新增 Fake 渠道测试，验证编排服务只调用 `isEnabled()` 为 true 的实现。
- [x] 3.2 实现 `NotificationApplicationService.enqueue`：按启用的渠道和接收人拆分投递记录，使用 `(event_id, channel, recipient_key)` 幂等；单元测试覆盖四渠道 fan-out、短信关闭不登记、重复登记不新增记录。
- [x] 3.3 实现 `dispatch(deliveryId)`：先条件领取为 `SENDING`，再调用渠道，按结果更新 `SUCCESS`/`FAILED`/`SKIPPED`/`DEAD`；单元测试覆盖成功、失败退避、达到上限、领取冲突跳过。
- [x] 3.4 新增 `NotificationQueuedEvent` 与 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 监听器，在事务提交后触发即时投递；单元测试验证未提交事务不触发、提交后触发一次。
- [x] 3.5 实现人工重试方法：仅 `FAILED`/`DEAD` 可重试，重置 `attempts=0`、状态 `PENDING`、`next_retry_time=NOW()`；单元测试覆盖非法状态拒绝与重试后立即投递。

## 4. 渠道实现

- [x] 4.1 站内渠道：在 `MessageApplicationService` 增加 `sendSystemMessage(...)`，抽取并复用现有保存接收记录、标记发送、发布事件逻辑；修复 `MessagePushListener` 对 `senderId=null` 的处理；单元测试覆盖系统消息落库、SSE 事件发布、重试复用 `message_id` 不重复建消息。
- [x] 4.2 邮件渠道：实现 `MailNotificationChannel`，使用 `JavaMailSender` 发送 UTF-8 邮件，收件人取 `sys_user.email`，缺邮箱置 `SKIPPED`；Mock `JavaMailSender` 测试主题、正文、收件人和异常分类。
- [x] 4.3 钉钉渠道：实现 `DingTalkSigner`、`DingTalkClient`、`DingTalkNotificationChannel`，包含加签、关键词前缀和 Guava 20 条/分钟限流；用固定 timestamp/secret 的签名向量测试，用 Fake 客户端测试限流与错误脱敏。
- [x] 4.4 短信渠道：实现 `SmsClient` 接口与阿里云 `SendSms` 适配器、`SmsNotificationChannel`，默认 `enabled=false`，启用时校验 AccessKey、签名、模板 Code 和手机号；单元测试覆盖默认关闭、缺手机号 `SKIPPED`、Fake 客户端参数正确。
- [x] 4.5 渠道契约测试：为四个渠道定义统一行为断言（成功、可重试失败、跳过、脱敏错误），用 Fake 实现或 Mock 依赖执行；确认 `mvn -s /home/lam/repo/settings.xml -pl iboot-admin -am test` 中该测试通过。

## 5. 审批待办触发

- [x] 5.1 在 `ApprovalApplicationService.submit` 保存申请后调用 `NotificationApplicationService.enqueue`，生成 `APPROVAL_TODO` 事件，接收人为 `approverId`，参数包含 `taskName`、`initiator`、`applyTime`；单元测试验证事件 ID、模板编码、接收人和参数。
- [x] 5.2 验证登记失败时审批事务回滚：用 Mock 让 `enqueue` 抛异常，断言 `submit` 不返回成功且申请未保存；确认生产路径只做 INSERT，不引入远程调用。
- [x] 5.3 更新审批发起后的手工验收脚本/说明，确保测试用户有真实邮箱、钉钉群可用；执行一次真实提交，确认三条投递记录生成。

## 6. Quartz 重试任务

- [x] 6.1 实现 `NotificationRetryTask.retry()`：重置超过 5 分钟的 `SENDING` 记录，查询到期记录最多 100 条并逐条 `dispatch`；单元测试覆盖 stale 重置、批量上限、失败不影响后续记录。
- [x] 6.2 确认 `V18` 的 `sys_job` 记录 cron 为 `0 * * * * ?`、`concurrent=0`；启动应用后在任务管理页看到“通知投递重试”，手动执行一次并检查执行日志。
- [x] 6.3 配置 `iboot.notification.retry.max-attempts` 与 `backoff-seconds` 的边界校验（非正数回退默认值）；单元测试覆盖默认值与非法配置。

## 7. 管理端 API

- [x] 7.1 新增 `NotificationDeliveryController`：`GET /api/notification/delivery` 分页查询与 `POST /api/notification/delivery/{id}/retry`；使用 `@PreAuthorize` 保护两个权限，重试操作加 `@Log`。
- [x] 7.2 新增请求/响应 DTO，响应中邮箱与手机号脱敏、最后错误脱敏，不包含 Webhook/Secret/AccessKey；单元测试或 MVC 测试验证脱敏与字段裁剪。
- [x] 7.3 查询支持 `status`、`channel`、`eventType`、`recipient`、`startTime`、`endTime` 组合筛选与分页；控制器测试覆盖参数绑定、空结果和非法状态。
- [x] 7.4 补充 OpenAPI 注解与示例；启动后访问 `/v3/api-docs`，确认两个接口与权限说明可见。

## 8. 前端通知投递页面

- [x] 8.1 新增 `iboot-portal/src/api/notification.ts`，定义投递记录类型、分页查询与重试函数；执行 `cnpm run type-check` 或 `cnpm run build` 验证类型与导入。
- [x] 8.2 新增 `views/monitor/NotificationDeliveryView.vue`：状态/渠道筛选、分页表格、最后错误查看、`FAILED`/`DEAD` 行的重试按钮；页面复用现有 Element Plus 与设计系统组件。
- [x] 8.3 新增 `delivery` 子路由（访问路径 `/dashboard/delivery`）与中英文 i18n 文案；构建后登录管理端，确认菜单、页面、筛选和重试按钮可用。
- [x] 8.4 手工验收：把一条投递改为 `DEAD`（或使用错误配置触达上限），在页面点击重试，确认按钮 loading、成功提示和列表状态刷新。

## 9. 集成验收与文档

- [x] 9.1 使用 `.env` 启动后端，提交一条请假申请：审批人在线时收到 SSE，消息中心出现未读消息，QQ 邮箱收到邮件，钉钉群收到消息，短信无投递记录。
- [x] 9.2 故障演练：临时改错钉钉 Secret 提交一条，投递记录为 `FAILED` 且错误脱敏；恢复配置后在管理页手动重试，状态变为 `SUCCESS`。
- [x] 9.3 缺地址演练：清空测试用户邮箱后提交一条，邮件记录为 `SKIPPED`，站内与钉钉仍成功。
- [x] 9.4 更新 `ROADMAP.md` 0.6 的状态、约束与验收判据，明确“短信默认关闭、真实发送待企业资质”；同步整理 `doc/20-多渠道通知操作指南.md` 的实施与配置说明。
- [x] 9.5 执行 `mvn -s /home/lam/repo/settings.xml verify` 与 `cnpm run build`，确认后端测试、前端构建通过；执行 `openspec validate add-multi-channel-notification --strict`，确认规格校验通过。
