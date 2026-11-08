# Spec Delta

## ADDED Requirements

### Requirement: 审批待办 outbox 与中继

审批提交 SHALL 在同一事务内写入一条通知 outbox 事件，事件 ID MUST 由流程实例 ID 稳定生成。后台中继 SHALL 按批抢占待发事件并投递到 RocketMQ；投递成功标记为已发送，失败按指数退避重排，超过上限标记为死信。中继 MUST 支持多实例并发而不重复发布同一事件，并 MUST NOT 因 broker 不可用阻塞审批提交。

#### Scenario: 提交成功即产生待发事件

- **WHEN** 申请人提交一条合法请假申请且事务提交成功
- **THEN** outbox 中存在一条待发事件，事件 ID 由流程实例 ID 稳定生成
- **AND** 中继投递成功后事件标记为已发送

#### Scenario: 审批事务回滚不留下事件

- **WHEN** 流程启动或申请落库失败导致事务回滚
- **THEN** outbox 中不存在该次提交的事件

#### Scenario: broker 停机后自动补投

- **WHEN** 提交审批时 broker 不可用
- **THEN** 审批提交成功，事件保持待发
- **AND** broker 恢复后中继自动投递，无需人工重放

#### Scenario: 多实例不重复发布

- **WHEN** 多个 workflow 实例同时轮询同一批待发事件
- **THEN** 每条事件只被一个实例抢占并发布
- **AND** 不会因并发轮询产生重复消息

#### Scenario: 超过重试上限标记死信

- **WHEN** 事件连续投递失败并超过最大尝试次数
- **THEN** 事件标记为死信并计入指标
- **AND** 可按事件 ID 查询到死信记录

## MODIFIED Requirements

### Requirement: 事务一致性

流程状态、业务记录与通知 outbox 事件 SHALL 在同一事务内更新，任一步骤失败时三者一起回滚。

#### Scenario: 审批过程中失败

- **WHEN** 审批操作在更新业务状态与流程任务之间发生异常
- **THEN** 申请状态保持不变，任务仍处于待处理，不产生部分写入

#### Scenario: outbox 写入失败

- **WHEN** 审批提交时通知 outbox 写入失败
- **THEN** 申请记录与流程状态一起回滚
- **AND** 不留下待发事件
