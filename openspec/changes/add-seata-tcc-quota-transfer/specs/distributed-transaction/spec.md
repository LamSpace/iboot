# Spec Delta

## ADDED Requirements

### Requirement: TCC 分支隔离

TCC 动作的本地写入 MUST NOT 产生 AT 分支；同一全局事务中 TCC 分支的回滚 MUST 只执行 Cancel，MUST NOT 同时执行 AT undo。TCC 动作 MUST 使用未被 Seata AT 自动代理的数据源，或采用等效的隔离手段。

#### Scenario: 只有 TCC 分支

- **WHEN** 一个只包含 TCC 动作的全局事务提交或回滚
- **THEN** 事务日志中只有 TCC 分支
- **AND** 回滚只调用 Cancel，不产生 AT 分支回滚

#### Scenario: 隔离失效

- **WHEN** TCC Try 的写入经过 AT 代理数据源
- **THEN** 该实现视为不满足本要求
- **AND** 必须在进入主线前修正

### Requirement: TCC 幂等与悬挂

Try、Confirm、Cancel MUST 幂等。Cancel 先于 Try 到达时 MUST 空回滚成功；Try 在 Cancel 之后到达时 MUST 被悬挂保护拦截。重复的 Confirm 或 Cancel MUST NOT 重复改变业务状态。

#### Scenario: 重复 Confirm

- **WHEN** 同一分支的 Confirm 被调用多次
- **THEN** 业务状态只改变一次
- **AND** 后续调用返回成功

#### Scenario: 空回滚

- **WHEN** Cancel 在没有 Try 的分支上到达
- **THEN** 记录取消墓碑
- **AND** 不产生业务副作用

#### Scenario: 悬挂

- **WHEN** Try 在取消墓碑之后到达
- **THEN** Try 不执行业务预留
- **AND** 终止该分支

### Requirement: TCC 可观测性

系统 MUST 在日志中记录 TCC 分支的全局事务标识、动作名与分支状态。运维 MUST 能判断一次 TCC 事务是确认、取消还是失败，并能在协调器不可用时收到告警。

#### Scenario: 分支日志关联

- **WHEN** 一次 TCC 事务完成
- **THEN** Try、Confirm 或 Cancel 的日志包含同一 XID
- **AND** 可看到动作名与分支状态

#### Scenario: 失败可定位

- **WHEN** TCC 事务回滚或协调器不可用
- **THEN** 日志与告警能定位失败动作
- **AND** 不依赖业务表猜测结果
