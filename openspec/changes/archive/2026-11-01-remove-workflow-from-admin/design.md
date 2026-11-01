# Design

## Context

`add-workflow-service` 交付后，审批流量已经经网关指向 `iboot-workflow`，但 admin 仍保留完整旧实现并启动 Flowable。`iboot_admin` 中的 33 张审批表与 `iboot_workflow` 并存；审批菜单与权限种子仍由 admin 的 V17 提供，这是有意保留的系统域数据。

见 `proposal.md` 的 Why。

## Goals / Non-Goals

**Goals:**

- 删除重复实现与 Flowable 依赖，让 admin 不再加载流程引擎。
- 把 `migrations.txt` 收敛为 `done`，证明旧实现已无路径。
- 保留可回退的数据与菜单，不在一夜之间失去回滚能力。

**Non-Goals:**

- 不删除 `iboot_admin` 中的 33 张表与审批菜单种子（保留一个发布周期）。
- 不调整 `approval-workflow` 的任何行为要求。
- 不处理 monitor / system 的拆分。

## Decisions

### D1：直接删除，不用开关保留双实现

与 `remove-notify-from-admin` / `remove-job-from-admin` 一致，直接删除旧代码。

- 备选：用配置开关保留旧实现。会让两套代码与两套测试继续存在，正是本变更要消除的成本；项目尚未发布，不需要在线回退开关。否决。

### D2：保留 admin 的 V17 与 33 张表一个周期

V17 已执行过，Flyway 迁移不可修改；表保留可让回滚与对照继续成立。

- 备选：立即 `DROP TABLE`。会失去数据回退能力，且与 notify/job 的收敛节奏不一致。否决。
- 清理另起条目，在本变更的文档中登记退出条件。

### D3：最终增量复制是防御性校验，不覆盖新库

脚本先比较两侧逐表行数与 `max(id)`：

- 若 admin 侧没有切流后的新行，脚本只做校验并通过；
- 若存在新行（例如切流窗口内旧实例仍写入），按 id 复制缺失行，**不覆盖** `iboot_workflow` 已有行；
- 若同 id 内容冲突，脚本中止并输出差异，交由人工判断。

- 备选：无条件 `INSERT ... ON DUPLICATE KEY UPDATE`。会用旧库内容覆盖新库，风险高于收益。否决。

### D4：删除只服务于审批的通知契约件

`NotificationPort`、`NotificationCommand`、`HttpNotificationPort`、`NotificationEventType` 已确认只有 `ApprovalApplicationService` 使用；随旧实现一并删除。

- 备选：下沉到 `iboot-kernel` 作为通用通知客户端。当前没有第二个消费者，属于臆测性抽象。否决。

### D5：移除 Flowable 依赖与配置，保留迁移文件

删除 `flowable-spring-boot-starter-process`、`flowable.*` 配置块与测试中的 Flowable 自动配置排除项；`V17` 保留不动。

理由：迁移文件是历史，修改它会让已有库与新库的校验和一致性与历史不符；表保留一个周期，与数据回退策略配套。

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| 仍有代码或配置引用被删类，导致 admin 启动失败 | 删除后全量编译与启动；`rg` 检查 `Approval` / `LeaveApplication` / `NotificationPort` 残留 |
| 删除 Flowable 后测试切片仍排除 Flowable 自动配置 | 同步清理测试属性；全量单测通过 |
| 刷新 `golden/admin.json` 掩盖了非预期漂移 | 只接受 7 条 `/api/approval/**` 路径消失；逐条核对 diff，出现其它差异即失败 |
| 旧表在新库切换后仍有残留行 | D3 的增量校验；不一致时中止并人工处理 |
| 回滚成本上升 | 文档明确：`git revert` + 数据源指回 `iboot_admin`；表与数据保留一个周期 |

## Migration Plan

1. 停止 admin；执行 `scripts/migrate/` 的最终增量校验脚本，确认无冲突。
2. 删除旧实现、通知契约件、Flowable 依赖与配置。
3. 删除/迁移 8 个测试类；全量单测与集成测试通过。
4. `migrations.txt` 的 `/api/approval` 改为 `done`；刷新 `golden/admin.json`，核对只消失 7 条路径。
5. 启动 admin，确认无 Flowable 日志、启动时间与堆占用下降；执行全量回归。
6. 回滚：`git revert` 本变更；把 workflow 数据源指回 `iboot_admin`（表与数据仍在），再切回路由。

## Open Questions

无。
