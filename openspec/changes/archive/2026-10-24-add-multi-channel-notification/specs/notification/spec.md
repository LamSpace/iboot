# Spec Delta

## Purpose

为 iBoot 提供按业务事件向用户投递通知的能力：同一事件可经站内消息、邮件、钉钉和短信多渠道送达，每次投递可追踪、失败可重试，并受统一的渠道启停与密钥配置约束。

## ADDED Requirements

### Requirement: 审批待办事件触发通知

系统 SHALL 在请假申请提交成功后，为审批人生成一个审批待办通知事件，事件 ID 在该次申请提交内保持稳定。通知事件 SHALL 按启用的渠道拆分为独立投递记录；同一事件、同一渠道、同一接收人 MUST 只存在一条投递记录。

#### Scenario: 提交请假后按渠道拆分投递

- **WHEN** 申请人提交一条请假申请，审批人配置了邮箱，钉钉渠道已启用，短信渠道保持默认关闭
- **THEN** 系统为审批人创建站内、邮件、钉钉三条投递记录
- **AND** 不创建短信投递记录
- **AND** 三条记录共享同一个事件 ID 和模板编码 `APPROVAL_TODO`

#### Scenario: 接收人缺少某渠道地址

- **WHEN** 审批人没有配置邮箱
- **THEN** 邮件投递记录状态为 `SKIPPED`
- **AND** 站内和钉钉投递不受影响

#### Scenario: 没有启用任何渠道

- **WHEN** 所有外部渠道均被禁用且站内渠道也不可用
- **THEN** 系统不创建投递记录
- **AND** 请假申请仍然提交成功

### Requirement: 通知模板渲染

系统 SHALL 通过 `sys_message_template` 的模板编码读取通知模板，并用事件参数替换 `${变量名}` 占位符。模板不存在或已停用时，该事件的通知登记 SHALL 失败并返回明确错误，不得静默发送空内容。

#### Scenario: 渲染审批待办模板

- **WHEN** 系统处理 `APPROVAL_TODO` 事件，参数包含 `taskName`、`initiator`、`applyTime`
- **THEN** 投递记录中的内容为替换后的完整文本
- **AND** 未提供的占位符保持原样或以空值处理，不得产生 `null` 字样

#### Scenario: 模板被停用

- **WHEN** `APPROVAL_TODO` 模板的 `status` 为 0
- **THEN** 通知登记失败并记录明确错误
- **AND** 不产生任何渠道的投递记录

### Requirement: 站内通知落库并经 SSE 推送

站内渠道 SHALL 在 `sys_message` 与 `sys_message_receiver` 中创建一条面向接收人的系统消息，并复用现有 `MessageSentEvent` → SSE 链路推送。接收人在线时 SHALL 收到实时事件；离线时消息 SHALL 保留在消息中心并可回看。重试同一投递 MUST NOT 创建重复消息。

#### Scenario: 在线用户收到实时通知

- **WHEN** 审批人已建立 SSE 连接，站内投递执行成功
- **THEN** 审批人收到新消息事件
- **AND** 消息中心出现一条未读的审批待办消息

#### Scenario: 离线用户稍后回看

- **WHEN** 审批人当前不在线
- **THEN** 站内消息和接收记录仍然落库
- **AND** 审批人下次登录后可在消息中心看到未读消息

#### Scenario: 重试不产生重复消息

- **WHEN** 一条站内投递因后续步骤失败被重试，且首次已创建消息
- **THEN** 重试复用已有消息 ID
- **AND** 消息中心不出现第二条相同事件的消息

### Requirement: 邮件渠道投递

邮件渠道 SHALL 使用接收人 `sys_user.email` 作为收件地址，以模板名称作为主题、渲染后的内容作为正文，通过配置的 SMTP 服务发送。SMTP 配置缺失时渠道 MUST 被视为未启用；收件地址缺失时该投递标记为 `SKIPPED`。发送异常 SHALL 记录可读错误并进入重试。

#### Scenario: 邮件发送成功

- **WHEN** 审批人邮箱存在且 SMTP 配置有效
- **THEN** 邮件发送成功，投递状态更新为 `SUCCESS`
- **AND** 记录发送时间

#### Scenario: SMTP 发送失败

- **WHEN** SMTP 拒绝认证或连接超时
- **THEN** 投递状态为 `FAILED`
- **AND** 记录最后一次错误的脱敏摘要
- **AND** 设置下次重试时间

#### Scenario: 收件地址缺失

- **WHEN** 审批人 `email` 为空
- **THEN** 投递状态为 `SKIPPED`
- **AND** 不进行重试

### Requirement: 钉钉渠道投递

钉钉渠道 SHALL 向配置的群自定义机器人 Webhook 发送文本消息，并使用加签参数完成安全校验。渠道 MUST 遵守每机器人每分钟 20 条的上限；达到上限时 SHALL 将本次投递标记为可重试失败，不得丢弃。Webhook、Secret 和加签结果 MUST NOT 写入投递记录或日志。

#### Scenario: 钉钉发送成功

- **WHEN** Webhook 与加签 Secret 有效，且未触发频率限制
- **THEN** 钉钉群收到审批待办消息
- **AND** 投递状态更新为 `SUCCESS`

#### Scenario: 达到频率限制

- **WHEN** 当前分钟已发送 20 条消息
- **THEN** 本次投递状态为 `FAILED`，错误信息说明触发限流
- **AND** 设置下次重试时间，等待下一分钟继续

#### Scenario: 加签失败

- **WHEN** Secret 错误或时间戳超出允许窗口
- **THEN** 投递状态为 `FAILED`
- **AND** 错误摘要不包含 Secret 或完整 Webhook

### Requirement: 短信渠道默认关闭

系统 SHALL 提供阿里云 Dysmsapi 短信适配器，但默认 `enabled=false`。关闭时 MUST NOT 创建短信投递记录，也不得调用云服务。启用后 SHALL 使用配置的签名与模板 Code 和事件参数发送；接收人手机号缺失时标记 `SKIPPED`。本期不要求真实短信送达。

#### Scenario: 默认关闭

- **WHEN** `iboot.notification.sms.enabled=false`
- **THEN** 审批待办事件不创建短信投递记录
- **AND** 不发起任何阿里云请求

#### Scenario: 启用但手机号缺失

- **WHEN** 短信渠道启用，审批人 `phone` 为空
- **THEN** 短信投递状态为 `SKIPPED`
- **AND** 不进行重试

#### Scenario: 启用后云服务返回失败

- **WHEN** 短信渠道启用且阿里云返回限流、签名未报备或余额不足
- **THEN** 投递状态为 `FAILED`
- **AND** 记录脱敏后的错误码与错误信息

### Requirement: 投递生命周期与重试

每条投递 SHALL 处于 `PENDING`、`SENDING`、`SUCCESS`、`FAILED`、`DEAD` 或 `SKIPPED` 之一，并记录尝试次数、下次重试时间和最后错误。系统 SHALL 在业务事务提交后立即尝试一次投递；失败后按指数退避重试，达到最大次数后进入 `DEAD`。同一投递 MUST NOT 被并发重复发送；已成功的投递 MUST NOT 被再次发送。

#### Scenario: 首次失败后自动重试成功

- **WHEN** 首次邮件发送超时，投递进入 `FAILED` 并设置下次重试时间
- **THEN** 到期后重试任务再次发送
- **AND** 发送成功后状态变为 `SUCCESS`，尝试次数保留

#### Scenario: 超过最大尝试次数

- **WHEN** 投递连续失败达到配置的最大次数
- **THEN** 状态变为 `DEAD`
- **AND** 不再自动重试，等待人工处理

#### Scenario: 人工重试

- **WHEN** 管理员对一条 `FAILED` 或 `DEAD` 投递执行重试
- **THEN** 状态重置为 `PENDING`，尝试次数归零
- **AND** 立即触发一次投递

#### Scenario: 并发调度不重复发送

- **WHEN** 即时投递与 Quartz 重试任务同时选中同一条记录
- **THEN** 只有一个执行者将该记录置为发送中并实际调用渠道
- **AND** 另一个执行者跳过该记录

### Requirement: 投递失败可见且可管理

系统 SHALL 提供分页查询投递记录的接口，支持按状态、渠道和时间筛选，并返回接收人标识、模板编码、尝试次数、最后错误和重试时间。系统 SHALL 提供单条投递的人工重试接口。两个接口分别受 `notification:delivery:list` 与 `notification:delivery:retry` 权限保护。管理端 SHALL 提供“通知投递”页面展示记录并触发重试。

#### Scenario: 管理员查看失败投递

- **WHEN** 管理员拥有 `notification:delivery:list` 权限并按 `FAILED` 筛选
- **THEN** 返回失败投递的分页列表
- **AND** 每条记录包含脱敏后的最后错误和下次重试时间

#### Scenario: 无权限访问

- **WHEN** 用户没有对应权限访问投递接口
- **THEN** 返回 403，不泄露投递内容或接收人信息

#### Scenario: 管理员手动重试

- **WHEN** 管理员在页面上对一条 `DEAD` 记录点击重试
- **THEN** 调用重试接口，记录状态变为 `PENDING`
- **AND** 页面刷新后可看到后续发送结果

### Requirement: 密钥与敏感信息保护

所有 SMTP 授权码、钉钉 Webhook/Secret、阿里云 AccessKey MUST 通过环境变量或外部密钥注入，MUST NOT 提交到仓库、写入 `sys_config`、写入投递记录或输出到日志。投递记录中的邮箱和手机号 SHALL 脱敏展示。SMS 渠道默认关闭，缺少短信凭证时不得阻止应用启动。

#### Scenario: 仓库中无密钥

- **WHEN** 检查仓库与 Flyway 脚本
- **THEN** 不包含 SMTP 授权码、钉钉 Webhook/Secret 或阿里云 AccessKey
- **AND** `.env` 被 `.gitignore` 忽略

#### Scenario: 缺少外部渠道配置

- **WHEN** 未配置 SMTP 或钉钉 Webhook
- **THEN** 对应渠道被视为未启用
- **AND** 应用正常启动，其他渠道不受影响

#### Scenario: 错误信息脱敏

- **WHEN** 渠道返回包含凭据或完整地址的错误
- **THEN** 投递记录和接口响应只保留脱敏摘要
- **AND** 不包含授权码、Secret、AccessKey 或完整手机号
