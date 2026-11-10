# Design

## Context

- Change 1（`add-seata-at-leave-quota`）已合并：审批通过使用 Seata AT，把 `biz_leave_application` / Flowable 表与 `sys_user_leave_quota` 放进同一个全局事务。AT 的数据源代理会为全局事务中的 SQL 自动登记分支。
- `sys_user_leave_quota` 已有 `frozen_count` 列，Change 1 中恒为 0；本次开始承载 TCC 冻结语义。
- Seata 2.5.0 的字节码确认：`ConnectionProxy.register()` 在全局事务中始终以 `BranchType.AT` 登记分支，`RootContext.inGlobalTransaction()` 只看 XID。TCC 的 Try 如果走同一个被代理数据源，会额外产生 AT 分支，回滚时 TCC Cancel 与 AT undo 可能双倍补偿。
- 审批主路径不参与本次变更。TCC 需要的是一个**独立、短事务、资源可预占**的场景，额度划转正好符合：Try 冻结划出方，Confirm 完成划转，Cancel 解冻。
- 当前 `iboot-system` 只有一个 Druid 数据源，被 Seata 自动代理；`iboot-workflow` 已具备 `RestClient` + `SeataXidClientInterceptor` 与 `@GlobalTransactional`。
- 需求见 `specs/`；动机见 `proposal.md`。

## Goals / Non-Goals

**Goals:**

- 用一次真实的额度划转跑通 Seata TCC：Try/Confirm/Cancel、幂等、空回滚、悬挂。
- 证明 TCC 分支与 AT 分支隔离：TCC 全局事务中只有 TCC 分支，回滚只执行 Cancel。
- 建立可重复的验收脚本与文档，作为 AT 的对照实验。

**Non-Goals:**

- 不改审批通过与拒绝路径，不与 AT 共享代理数据源。
- 不做门户 UI，不做额度划转审批流。
- 不做多租户、拆库或阶段 3 的 K8s 部署。
- 不引入 Seata TCC Fence（本次手工实现状态机，便于观察三段语义）。

## Decisions

### D1. 独立额度划转，不接入审批路径

划转是一个管理员操作，不改变请假申请的状态。把它独立出来，避免把 TCC 叠到 AT 主路径上造成混合分支，也让失败只影响一次划转，不影响审批可用性。

**备选**：把审批通过改成 TCC。否决原因：审批的主路径已经由 AT 覆盖，TCC 化会重写已验证的路径，且没有额外的业务收益。

### D2. TM 在 workflow，RM 在 system，TM 不写本地库

- TM：`iboot-workflow` 新增 `POST /api/approval/leave/quota/transfer`，在 `@GlobalTransactional` 内只调用 system 的 TCC Try，不写本地表。
- RM：`iboot-system` 执行 TCC 动作并写 `sys_user_leave_quota` / `sys_leave_quota_transfer`。

TM 不写本地库是刻意的：如果 workflow 在同一个全局事务里写 Flowable 或业务表，就会产生 AT 分支，重新引入混合分支问题。全局事务里只有 TCC 分支。

### D3. 非代理数据源 + 专属事务管理器

system 增加第二个小连接池 `tccDataSource`（同一 `iboot` 库），使用独立子类
`TccDruidDataSource`，并在配置中：

```yaml
seata:
  excludes-for-auto-proxying:
    - com.iboot.system.infrastructure.config.TccDruidDataSource
```

Spike 实测：Seata 2.5.0 的 `SeataAutoDataSourceProxyCreator.shouldSkip` 用
`beanClass.getName()` 与排除集合比较，**不是 Bean 名**；主数据源与 TCC 数据源同为
Druid 连接池，必须用子类区分。另外，Druid starter 的 `@ConditionalOnMissingBean`
会因为出现第二个 `DataSource` 而退让，因此主数据源改为显式声明并保持 `@Primary`；
同理，`tccJdbcTemplate` 会让 `JdbcTemplateAutoConfiguration` 整体退让，必须同时显式声明
`@Primary` 的主 `jdbcTemplate`，否则旧仓储会静默注入 TCC 数据源。

TCC 动作类标注 `@LocalTCC` 与 `@TwoPhaseBusinessAction`：前者是 Seata 识别 TCC 资源、
登记分支的前提，缺失时全局事务会以零分支提交。TCC 动作的仓储使用
`@Qualifier("tccDataSource")` 的 `JdbcTemplate`，并通过 `tccTransactionManager`
（`DataSourceTransactionManager`）的 `TransactionTemplate` 包住 Try、Confirm、Cancel。
主 `dataSource` 仍是 `@Primary` 且继续被 AT 代理，互不影响。

**备选**：关闭自动代理并为两个数据源手工建 `DataSourceProxy`。否决原因：主数据源是 Druid starter 创建的，手工接管会与 `@ConditionalOnMissingBean` 的启动次序耦合；类名排除是更小的改动。

### D4. 手工 TCC 状态机，而不是 TCC Fence

`sys_leave_quota_transfer` 记录 `transfer_id`、`xid`、`from_user_id`、`to_user_id`、`year`、`count`、`status`：

- Try：插入 `TRYING`；冻结划出方；若已存在同 `transfer_id` 或同 `xid` 的记录，按状态幂等返回；若存在 `CANCELLED` 墓碑，直接终止（悬挂保护）。
- Confirm：仅 `TRYING` → `CONFIRMED`；释放冻结、划出方总额度减少、划入方总额度增加；重复 Confirm 返回成功。
- Cancel：仅 `TRYING` → `CANCELLED` 并释放冻结；无记录时写入 `CANCELLED` 墓碑（空回滚）；重复 Cancel 返回成功。

**备选**：`@TwoPhaseBusinessAction(useTCCFence = true)` + `tcc_fence_log`。否决原因：本次目标是看清 Try/Confirm/Cancel 的状态与幂等边界，Fence 会把关键逻辑藏进框架；Fence 留作后续生产化选项。

### D5. 幂等键与 XID

- `transfer_id` 由 TM 生成，作为业务幂等键。
- `xid` 唯一，Try、Confirm、Cancel 都通过 `BusinessActionContext.getXid()` 定位记录。一个划转对应一个全局事务，不再额外依赖参数传递。
- Try 要求 `RootContext.getXID()` 非空；为空时失败关闭，避免在普通本地事务里冻结额度。

### D6. 额度调整语义

- 冻结：`frozen_count += count`，条件 `total_count - used_count - frozen_count >= count`。
- 取消：`frozen_count -= count`，双方 `total_count` 不变。
- 确认：划出方 `frozen_count -= count`、`total_count -= count`；划入方 `total_count += count`。
- 所有更新用条件 SQL 并检查影响行数；`frozen_count` 不得为负。

### D7. 权限与接口

新增权限 `approval:quota:transfer`，由 V6 迁移插入菜单（挂在审批中心 9100 下）并授予管理员角色（`ROLE_ADMIN`）。接口：

```
POST /api/approval/leave/quota/transfer
{ "fromUserId": 1, "toUserId": 2, "year": 2026, "count": 1 }
```

参数校验：划出方 ≠ 划入方、`count > 0`、年度合法、划入方存在且启用。不做门户 UI；先用 API 与脚本验收。

### D8. 故障钩子与验证

- 新增 `seata-tcc-demo` profile 的故障切面：识别 `X-Seata-Demo-Fail: after-tcc-try`，在 TCC Try 返回后抛出异常，使全局事务回滚，驱动 TC 调用 Cancel。
- 复用现有 `SeataDemoFaultContext` / `SeataDemoFaultFilter`，把过滤器扩大到 `seata-demo` 与 `seata-tcc-demo` 两个 profile；AT 与 TCC 的切面分别只在自己的 profile 注册。
- 端到端脚本 `scripts/regress/seata-tcc.sh` 覆盖：正常划转、Try 后失败取消、TCC 协调器不可用、system 不可用、并发、权限、重复调用。
- 空回滚与悬挂在动作层单测覆盖（直接调用 Cancel/Try，不需要 TC）。

### D9. 可观测性

- TCC Try/Confirm/Cancel 日志带 XID、动作名、transfer_id 与状态。
- TC 日志应能看到 `branchType=TCC` 的分支；端到端脚本断言成功/取消后的额度与状态。
- 不新增指标；沿用审批 5xx 与 Seata 日志告警。

## Risks / Trade-offs

- **`excludes-for-auto-proxying` 行为未经实测** → spike 门禁先验证 TCC Try 只产生 TCC 分支；不通过则改用非代理数据源的其他隔离方式。
- **TCC over HTTP 的 Confirm/Cancel 路由** → spike 验证 TC 能回调 system 的 TCC 方法；不通过则退回手工补偿（Saga 风格）并记录结论。
- **第二个连接池增加内存与连接数** → 小池（initial 1 / max 5），只给 TCC 动作使用；计入 R4。
- **TCC 重试与悬挂** → 状态机 + 唯一键 + 墓碑；单元测试覆盖重复 Confirm/Cancel、空回滚、悬挂。
- **全局事务只有 TCC 分支** → spike 验证 TC 能正常提交/回滚单 TCC 分支。
- **无 UI** → 本变更只做 API 与脚本；管理页面另起变更，避免把实验做成半成品界面。
- **权限种子新增一条菜单** → V6 只新增按钮权限与管理员授权，不改现有菜单结构。

## Migration Plan

1. **Spike 门禁**：第二个数据源 + `excludes-for-auto-proxying`；TCC Try over HTTP；TC 驱动 Confirm/Cancel；验证只有 TCC 分支。产出 `docs/spike/seata-tcc-spike-report.md`。不通过即停止。
2. **数据库**：V6 新增 `sys_leave_quota_transfer` 与 `approval:quota:transfer` 种子；更新 `SchemaMigratorTest` 的表数与索引断言。
3. **system**：TCC 数据源/事务管理器、状态机仓储、`@TwoPhaseBusinessAction` 动作、内部 Try 端点与单测。
4. **workflow**：TCC 客户端、`@GlobalTransactional` 划转接口、`seata-tcc-demo` 故障切面与单测。
5. **验证与文档**：`scripts/regress/seata-tcc.sh`、docs/23 §17、ROADMAP、docs/02/07/10、README、spike 报告；跑全量回归与 AT 的 `seata.sh` 防止回归。

回滚：V6 只新增表与权限，向前兼容；回退发布即可。TCC 失败不影响审批主路径。

## Spike 结论

以下问题已由 `docs/spike/seata-tcc-spike-report.md` 回答：

- `seata.excludes-for-auto-proxying` 在 2.5.0 里按**数据源类名**匹配，不是 Bean 名；
  主数据源与 TCC 数据源同为 Druid，因此用 `TccDruidDataSource` 子类区分。
- TCC Try 通过 HTTP 进入 system 时可以被 TCC 拦截器织入并登记分支，但动作类必须标注
  `@LocalTCC`；否则全局事务以零分支提交，Confirm 不会执行。
- TC 能把 Confirm/Cancel 回调到 system 的 TCC Bean，回调在 RM 的 RPC 线程执行。
- 只含 TCC 分支的全局事务提交/回滚与 AT 一致：`GlobalCommit` / `GlobalRollback` +
  `PhaseTwo_Rollbacked`，`undo_log` 始终为空。
