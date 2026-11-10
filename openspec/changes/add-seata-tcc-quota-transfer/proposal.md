# Proposal

## Why

Change 1 用 Seata AT 解决了「审批通过 → 扣减额度」的短事务原子性。AT 的语义是「直接改、失败用 undo_log 撤销」，适合纯 DB、无需预占的写；它覆盖不了需要**先冻结资源、后确认或取消**的场景，也无法直观对比 TCC 的三段语义。

本次变更做一个独立的 TCC 实验：管理员把 A 的请假额度划转给 B。Try 冻结 A 的额度，Confirm 完成划转，Cancel 解冻。它不接入审批主路径，目的是在真实环境里练透 TCC 的幂等、空回滚、悬挂与补偿，同时验证一个关键约束：**TCC 动作不能与 AT 共享同一个被 Seata 代理的数据源**，否则 Try 里的 SQL 会额外产生 AT 分支，回滚时可能双倍补偿。

## What Changes

- 新增 `POST /api/approval/leave/quota/transfer`：管理员把指定年度、指定次数的额度从 A 划转给 B。
- 新增 `iboot-system` 的 TCC 动作 `leaveQuotaTransfer`（Try/Confirm/Cancel），使用**独立的非代理数据源**与专属事务管理器。
- 新增 `sys_leave_quota_transfer` 表，记录划转状态与 XID，支撑幂等、空回滚与悬挂处理。
- 新增 `approval:quota:transfer` 权限种子（V6 迁移，授予管理员角色）。
- 新增 `seata-tcc-demo` profile 的故障钩子，用于在 TCC Try 成功后触发全局回滚，验证 Cancel。
- 新增 `scripts/regress/seata-tcc.sh` 与 `docs/spike/seata-tcc-spike-report.md`。
- 明确不做：不改审批通过路径；不引入新中间件；不做门户 UI（先用 API 与脚本验收）；不与 AT 共享代理数据源。

## Capabilities

### New Capabilities

- `leave-quota-transfer`: 额度划转的接口、权限、TCC 状态机、幂等与失败语义。

### Modified Capabilities

- `leave-quota`: 补充 `frozen_count` 的冻结/释放语义，作为 TCC 预占的额度侧契约。
- `distributed-transaction`: 增加 TCC 分支要求，包括 Try/Confirm/Cancel、幂等、空回滚、悬挂，以及 TCC 与 AT 的数据源隔离。

## Impact

- 服务：`iboot-workflow`（TM，只编排、不写本地库）、`iboot-system`（RM，TCC 动作与状态表）。
- 数据库：`iboot` 新增 `sys_leave_quota_transfer`；新增 `approval:quota:transfer` 菜单/权限种子；`sys_user_leave_quota.frozen_count` 开始承载冻结语义。
- 数据源：`iboot-system` 增加第二个连接池 `tccDataSource`，排除 Seata AT 自动代理；新增 `tccTransactionManager`。
- 运维：不新增容器；system 多一个连接池与约 10 个连接；TCC 动作依赖 `seata-server` 的 TCC 分支处理。
- 前端：不新增页面；接口先由脚本与 API 验收。
- 文档：`docs/23 §17`、`ROADMAP`、`docs/02`、`docs/07`、`docs/10`、README 与 spike 报告。
