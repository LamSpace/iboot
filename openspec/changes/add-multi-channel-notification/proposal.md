# Proposal

## Why

0.6 要把通知从“仅站内 SSE”扩展为可插拔的多渠道，并让外部渠道的发送失败可见、可重试。当前审批流发起后，审批人只能靠轮询待办发现新任务；邮件、钉钉完全没有接入。短信受个人实名资质限制无法真实发送，因此需要一个默认关闭、但架构完整的短信适配器，避免以后有企业资质时重做渠道层。

## What Changes

- 新增通知渠道 SPI：`IN_APP`、`EMAIL`、`DINGTALK`、`SMS` 四个实现，按配置启用/禁用；发送逻辑与业务事件解耦。
- 新增投递记录表 `sys_notification_delivery`：同一业务事件按渠道、接收人拆行，记录状态、尝试次数、下次重试时间与最后错误。
- 复用 `sys_message_template` 渲染通知内容，新增 `APPROVAL_TODO` 模板；模板保持渠道无关，由各渠道适配器决定标题/正文形态。
- 审批流发起后触发通知：向审批人投递审批待办，接收人取自 `sys_user.email` / `sys_user.phone`。
- 站内渠道写入 `sys_message` + `sys_message_receiver`，复用现有 `MessageSentEvent` → `MessagePushListener` → SSE 链路，离线用户可在消息中心回看。
- 邮件渠道使用 Spring Mail，连接 QQ/163 SMTP；收件人缺失时标记 `SKIPPED`，不计为失败。
- 钉钉渠道使用群自定义机器人 Webhook + 加签，按 20 条/分钟限流；超限或网络失败进入重试。
- 短信渠道实现阿里云 Dysmsapi 适配器，但默认 `enabled=false`；本期不申请签名/模板，测试使用 Fake 实现。
- 新增 Quartz 重试任务，每分钟扫描到期投递记录；指数退避，超过最大次数进入 `DEAD`。
- 新增管理端“通知投递”页面与后端查询/手动重试接口，支持按状态、渠道筛选并查看最后错误。
- 新增权限 `notification:delivery:list`、`notification:delivery:retry`，默认授予管理员角色。
- 所有密钥与地址通过环境变量注入，不入仓、不写死。

## Capabilities

### New Capabilities

- `notification`: 定义通知事件的登记、渠道选择与启用规则、模板渲染、站内/邮件/钉钉/短信投递、投递记录、失败重试和管理端可见性。

### Modified Capabilities

无。审批流现有需求不变；审批发起后触发通知作为 `notification` 能力的新需求表达，不修改 `approval-workflow` 的既有行为。

## Impact

- **领域层**：新增 `com.iboot.admin.domain.notification`（投递模型、状态枚举、渠道枚举、仓储接口、渠道 SPI）。
- **应用层**：新增 `NotificationApplicationService`；`MessageApplicationService` 增加系统消息发送方法，供站内渠道复用。
- **基础设施层**：新增邮件/钉钉/短信渠道实现、DingTalk 加签客户端、投递记录持久化、通知配置属性、Quartz 重试任务。
- **接口层**：新增通知投递查询/重试 Controller 与 DTO。
- **数据库**：新增 Flyway `V18`，包含 `sys_notification_delivery` 表、`APPROVAL_TODO` 模板、重试任务 `sys_job`、投递页面菜单与权限。
- **前端**：新增通知投递页面、API 模块、路由与中英文文案。
- **依赖**：新增 `spring-boot-starter-mail`；新增阿里云 `dysmsapi20170525`（仅编译与默认关闭的适配器使用）；Guava 已在依赖中，用于钉钉限流。
- **配置**：新增 `iboot.notification.*` 与 `spring.mail.*`，取值来自 `.env` 环境变量。
- **非目标**：不实现短信真实发送、用户级渠道偏好/免打扰、钉钉消息批量聚合、RocketMQ 重投；这些留到后续阶段。
- **兼容性**：不修改既有 `sys_message`、`sys_message_template` 表结构；不改动现有消息中心与 SSE 契约；无破坏性 API 变更。
