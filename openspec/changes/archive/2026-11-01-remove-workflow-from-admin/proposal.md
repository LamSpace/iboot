# Proposal

## Why

`add-workflow-service` 已交付后，审批的全部外部流量经网关指向 `iboot-workflow`，但管理端仍保留整套旧实现（20 个 Java 文件含 6 个 DTO、8 个测试类、1 个 mapper XML、1 个 BPMN）并继续启动 Flowable 引擎。这是绞杀者过渡期的临时状态：

- 同一份业务存在两套代码与两套测试，相关改动要改两处；
- 33 张审批表同时存在于 `iboot_admin` 与 `iboot_workflow`，与「每服务独立 schema」的目标冲突；
- 管理端仍在启动流程引擎、加载 `ACT_*` 表与 BPMN，而审批流量已经全部由新服务承接；
- 审批相关的通知端口只剩旧实现使用，成为无消费者的跨服务契约件。

项目尚未对外发布，不存在需要保护的在线旧数据，收敛越早成本越低。删除模式与 `remove-notify-from-admin` / `remove-job-from-admin` 保持一致。

## What Changes

- **最终增量搬迁**：把 `iboot_admin` 中 33 张表在切流后新增的行复制到 `iboot_workflow`；目标库已有的同 id 行视为更新版本跳过。正常情况下切流后旧表不应再有新行，这一步是防御性校验。
- **删除管理端旧实现**：20 个 Java 文件（含 6 个 DTO）、8 个测试类、`LeaveApplicationMapper.xml`、`processes/leave-approval.bpmn20.xml`。
- **删除只服务于旧实现的契约件**：`NotificationPort`、`NotificationCommand`、`HttpNotificationPort`、`NotificationEventType`（已确认仅 `ApprovalApplicationService` 使用）。
- **管理端移除 Flowable**：`flowable-spring-boot-starter-process` 依赖、`flowable.*` 配置块，以及测试中的 Flowable 自动配置排除项。
- **保留** `iboot_admin` 中的 33 张表与审批菜单/权限种子一个发布周期，不删除；菜单属于系统域，随 `iboot-system` 迁移。
- **回归登记收敛**：`migrations.txt` 的 `/api/approval` 从 `transitional` 改为 `done`，刷新 `golden/admin.json`（预期消失 7 条路径）。
- **文档**：`ROADMAP` 状态表、`doc/23` §11.9、`README` 项目结构、`doc/07` schema 归属表。

**对外契约不变**：`/api/**` 路径、方法、响应结构、权限码均无变化，前端零改动。

**BREAKING（仅内部）**：回滚方式从「改回网关路由」变为 `git revert` + 数据源指回 `iboot_admin`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。本变更是部署归属与代码归属的收敛，不改变任何能力的外部行为；`approval-workflow` 的既有需求继续成立，由契约基线、冒烟与权限矩阵强制。

## Impact

**代码**

- 删除：`iboot-admin` 中审批的接口、应用、领域、基础设施、DTO、mapper XML 与 BPMN，以及 8 个测试类。
- 删除：仅被审批使用的 4 个通知契约件。
- 保留：`sys_menu` / `sys_role_menu` 中的审批菜单与权限种子；33 张表保留一个周期。

**数据**

- 最终增量复制脚本放 `scripts/migrate/`，执行后逐表校验行数与 `max(id)`。
- `QRTZ_*` 等与审批无关的数据不受影响。

**依赖与配置**

- `iboot-admin` 移除 Flowable 依赖与 `flowable.*` 配置；启动时间与堆占用应下降，记录前后数值。
- 不新增 JVM、不新增中间件。

**回归资产**

- `migrations.txt` 一条改 `done`；`golden/admin.json` 刷新；全量回归、路径守恒与路由覆盖通过。

**已知遗留**

- `iboot_admin` 中的 33 张表与审批菜单种子保留一个发布周期，清理另起条目。
- 跨库读 `sys_user` 与跨服务写审计表仍是临时约定，随 `iboot-system` / `iboot-monitor` 收敛。
