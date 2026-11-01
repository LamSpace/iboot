# Spec Delta

## MODIFIED Requirements

### Requirement: 恢复演练限定在独立演练库

恢复演练 MUST 只允许以状态为成功的备份为输入，并 MUST 还原到独立的演练库。目标库名 SHALL 固定为 `iboot_restore_drill`，且 MUST 匹配 `iboot_restore_` 前缀；任何指向业务库（`iboot`）或其他库名的目标 MUST 被拒绝。每次演练 SHALL 先删除再重建演练库，业务库 MUST NOT 被写入或删除。

#### Scenario: 演练还原到独立库

- **WHEN** 管理员对一条成功的备份发起恢复演练
- **THEN** 系统重建 `iboot_restore_drill` 并将备份内容还原到该库
- **AND** `iboot` 的数据和结构保持不变

#### Scenario: 目标库被篡改时拒绝

- **WHEN** 请求试图将恢复目标指定为 `iboot` 或不以 `iboot_restore_` 开头的库名
- **THEN** 系统拒绝执行并返回明确错误
- **AND** 不执行任何 DDL 或数据写入

#### Scenario: 非成功备份不可演练

- **WHEN** 对状态为运行中或失败的备份发起演练
- **THEN** 系统拒绝执行
- **AND** 不创建演练记录

#### Scenario: 密文损坏时拒绝还原

- **WHEN** 备份对象无法通过 GCM 认证或解密后校验和与记录不一致
- **THEN** 演练失败并记录明确原因
- **AND** 系统不向演练库写入任何数据
