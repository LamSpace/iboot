# Spec Delta

## MODIFIED Requirements

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

#### Scenario: 重连期间保持在线

- **WHEN** 同一用户重新建立 SSE 连接，旧连接随后触发完成或错误回调
- **THEN** 新连接仍保留在在线连接池中
- **AND** 用户在该连接上仍能收到后续实时事件
