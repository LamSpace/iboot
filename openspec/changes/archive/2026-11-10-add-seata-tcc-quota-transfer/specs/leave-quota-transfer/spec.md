# Spec Delta

## Purpose

为管理员提供年度请假额度的跨用户划转能力：以 Seata TCC 的 Try/Confirm/Cancel 冻结、确认或取消一次划转，保证并发与故障下额度不丢、不重、不为负。

## ADDED Requirements

### Requirement: 划转接口与权限

系统 SHALL 提供额度划转接口，只允许持有 `approval:quota:transfer` 权限的用户调用。请求 MUST 校验划出方与划入方不同、次数为正整数、年度合法；不合法时 MUST 拒绝且不改变任何额度。

#### Scenario: 授权用户发起划转

- **WHEN** 持有 `approval:quota:transfer` 的用户提交合法的划转请求
- **THEN** 系统冻结划出方额度并创建一条划转记录
- **AND** 返回划转标识与初始状态

#### Scenario: 缺少权限

- **WHEN** 没有 `approval:quota:transfer` 权限的用户调用划转接口
- **THEN** 系统返回 403
- **AND** 不创建划转记录、不冻结额度

#### Scenario: 参数非法

- **WHEN** 划出方与划入方相同、次数不大于 0 或年度非法
- **THEN** 系统返回 400
- **AND** 不产生任何额度变化

### Requirement: 划转状态机

系统 SHALL 为每次划转维护 `TRYING`、`CONFIRMED`、`CANCELLED` 三种状态。Try 成功后状态为 `TRYING`；Confirm 成功后为 `CONFIRMED`；Cancel 成功后为 `CANCELLED`。状态 MUST 只允许从 `TRYING` 进入终态。

#### Scenario: 正常划转

- **WHEN** Try、Confirm 依次成功
- **THEN** 划转记录变为 `CONFIRMED`
- **AND** 划出方总额度减少、划入方总额度增加

#### Scenario: 取消划转

- **WHEN** Try 成功后收到 Cancel
- **THEN** 划转记录变为 `CANCELLED`
- **AND** 划出方冻结释放，双方总额度不变

#### Scenario: 终态不可回退

- **WHEN** 已 `CONFIRMED` 或 `CANCELLED` 的划转再次收到相反动作
- **THEN** 状态保持不变
- **AND** 额度不重复调整

### Requirement: 幂等与异常补偿

Confirm 与 Cancel MUST 幂等。Cancel 先于 Try 到达时 MUST 完成空回滚并留下 `CANCELLED` 墓碑；迟到的 Try 看到墓碑后 MUST NOT 冻结额度。同一划转标识或同一全局事务重复 Try MUST NOT 重复冻结。

#### Scenario: 重复确认

- **WHEN** 同一划转的 Confirm 被调用多次
- **THEN** 只有第一次调整额度
- **AND** 后续调用返回成功

#### Scenario: 空回滚

- **WHEN** Cancel 在没有 Try 记录时到达
- **THEN** 系统记录 `CANCELLED` 墓碑
- **AND** 不改变任何额度

#### Scenario: 悬挂

- **WHEN** Try 在 Cancel 墓碑之后到达
- **THEN** Try 不冻结额度
- **AND** 不覆盖墓碑状态

### Requirement: 划转失败关闭与并发

全局事务协调器或系统服务不可用时，划转 MUST 明确失败，MUST NOT 静默降级为本地提交。同一划出方的并发划转 MUST NOT 冻结超过其剩余额度。

#### Scenario: 协调器不可用

- **WHEN** 全局事务协调器不可用
- **THEN** 划转失败并返回明确错误
- **AND** 不留下部分状态

#### Scenario: 系统服务不可用

- **WHEN** 系统服务不可用
- **THEN** 划转失败并返回明确错误
- **AND** 额度与划转记录均不变化

#### Scenario: 并发划转

- **WHEN** 同一划出方剩余额度为 1 时两个划转并发执行
- **THEN** 只有一个冻结成功
- **AND** 冻结次数最终不超过剩余额度

### Requirement: 划转记录

系统 SHALL 为每次划转持久化划出方、划入方、年度、次数、状态与全局事务标识。记录 MUST 可用于幂等判断与事后审计；划转成功后 MUST NOT 被物理删除。

#### Scenario: 审计可查

- **WHEN** 划转完成或取消
- **THEN** 记录保留划出方、划入方、年度、次数、状态与 XID
- **AND** 状态与额度变化一致
