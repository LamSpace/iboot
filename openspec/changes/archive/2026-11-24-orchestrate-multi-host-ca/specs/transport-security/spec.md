# Spec Delta

## ADDED Requirements

### Requirement: 跨主机 CA 阶段编排

跨主机 CA 轮换 SHALL 按 prepare → activate → finalize 分组执行，并允许逐台安装调度与巡检。

- 编排 MUST 支持 `--hosts` 指定主机列表，默认 dry-run，只有 `--execute` 才通过 SSH 执行。
- `--checklist` MUST 输出完整 runbook，顺序为：预检、调度安装、prepare、activate、finalize、收尾巡检。
- 同一阶段 MUST 在全部主机依次执行，任一主机失败 MUST 停止后续主机。
- `--install-schedules` MUST 同时安装证书轮换调度与 SVID 刷新调度。
- `--status` MUST 汇总每台主机的轮换调度、SVID 调度与 CA 阶段状态。
- 失败提示 MUST 指向 `tls-ca-rotate.sh --rollback`。
- 回归 MUST 用假 ssh 验证顺序、dry-run、失败停止与巡检命令。

#### Scenario: 两主机 dry-run

- **WHEN** 执行 `--hosts h1,h2 --stage prepare --dry-run`
- **THEN** 打印 h1、h2 的 prepare 命令
- **AND** 不调用 ssh

#### Scenario: 阶段顺序

- **WHEN** 执行 `--checklist --hosts h1,h2`
- **THEN** prepare 段先于 activate 段
- **AND** activate 段先于 finalize 段

#### Scenario: 主机失败

- **WHEN** h2 在 activate 阶段失败
- **THEN** 编排以非零退出码停止
- **AND** 不再执行后续主机
- **AND** 提示对已完成主机执行 rollback

#### Scenario: 调度巡检

- **WHEN** 执行 `--status --hosts h1,h2 --execute`
- **THEN** 每台主机输出轮换调度、SVID 调度与 CA 阶段状态
- **AND** 任一主机失败时编排返回非零
