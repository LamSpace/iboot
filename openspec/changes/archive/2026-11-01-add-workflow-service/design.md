# Design

## Context

审批能力当前完全在 `iboot-admin`：

- `ApprovalController`（7 个接口）、`ApprovalApplicationService`、`domain/workflow`、`infrastructure/workflow/FlowableWorkflowAdapter`、`LeaveApplicationMapper.xml`、`processes/leave-approval.bpmn20.xml`，共 18 个 Java 文件（含 5 个 DTO）与 8 个测试类。
- `V17__add_approval_workflow.sql` 创建 33 张表（30 张 `ACT_*`、2 张 `FLW_*`、`biz_leave_application`），并在末尾写入审批菜单与角色种子。
- Flowable 8.0.x，`flowable.database-schema-update=false`、`async-executor-activate=false`、`check-process-definitions=true`、IDM 与 Event Registry 关闭。
- 审批发起时读取 `sys_user` 校验审批人；提交后通过 `HttpNotificationPort` 调用 notify 的 `/internal/notifications`，失败不阻断审批。

新服务的接入约定来自 `doc/23` §11.1（十个步骤）与 §11.2（四个隐式契约）；测试沿用 `ApprovalSchemaMigrationTest` 的 Testcontainers MySQL 8.0.45 方式。回归装置已有 admin/ai/file/auth/notify/job 六个服务。

见 `proposal.md` 的 Why；行为契约见 `specs/approval-workflow/spec.md`。

## Goals / Non-Goals

**Goals:**

- 审批能力独立部署，API、权限码与前端零变化。
- 审批的业务表与引擎表归新服务独占，事务边界留在服务内部。
- 切流后，迁移前已存在的在途流程实例仍可继续审批。
- 管理端旧实现保留到 `remove-workflow-from-admin`，本变更不删文件。

**Non-Goals:**

- 不引入 Seata：审批的一致性边界不跨服务。
- 不改造审批业务模型，不新增超时升级、催办、会签。
- 不把菜单/权限种子迁到工作流服务。

## Decisions

### D1：业务表与引擎表放在同一个 schema

`biz_leave_application` 与 `ACT_*` / `FLW_*` 都放 `iboot_workflow`，共用同一数据源。

- 理由：审批规格要求「流程状态与业务记录在同一事务内更新」。同库同数据源时这就是本地事务；拆成两个数据源会引入分布式事务。
- 备选：引擎表单独 schema + Seata。为一个可本地一致的问题引入分布式事务，成本与故障面都更高。否决。

### D2：新服务 Flyway 管理 DDL，菜单种子不随迁

V1 从 V17 复制 33 张表的 DDL 与 `ACT_GE_PROPERTY` 的版本行，**不复制 `sys_menu` / `sys_role_menu` 的插入**。

- 理由：菜单与权限属于系统域，仍由 admin 的 V17 提供；新服务不持有系统数据。
- 备选：把菜单种子也复制过去。会在两个 schema 产生同名菜单与权限，后续 `iboot-system` 拆分时冲突。否决。
- 备选：让 Flowable 自动建表。与当前「Flyway 管理、引擎禁止 DDL」的约束冲突，升级时不可控。否决。

### D3：停机窗口内「先建表、再搬数据、最后启动」

顺序固定为：

1. 停 admin，确认没有审批写入；
2. 创建 `iboot_workflow` 并执行 V1（只建表与版本行）；
3. 复制 33 张表的全部行（含 `ACT_GE_PROPERTY`、`ACT_RE_*`、`ACT_RU_*`、`ACT_HI_*`、`ACT_GE_BYTEARRAY`）；
4. 校验逐表行数与 `max(id)`；
5. 启动 workflow，切网关路由。

- **为什么不先启动**：`check-process-definitions=true` 会在首次启动时部署 BPMN；如果先启动再搬 `ACT_RE_*`，会得到重复的部署与流程定义版本。先搬后启，引擎看到内容哈希一致，不再部署。
- **为什么不只搬定义表**：会丢掉在途实例，违反验收判据。
- `ACT_GE_PROPERTY` 与 V1 的种子行冲突：复制脚本对该表使用 `REPLACE INTO`，并在搬迁前后断言 `common.schema.version` 一致；不一致说明两侧 Flowable 版本不同，必须停下。

### D4：用户目录跨库只读，失败关闭

新增 `WorkflowUserReader`，以 `iboot_admin.sys_user` 只读 `id/status/username/nickname`；数据访问异常转换为明确的业务错误，拒绝发起。

- 备选：在 `biz_leave_application` 增加申请人/审批人姓名快照。会改变业务语义（改名后历史展示变化），不应混进拆分。否决。
- 备选：等 `iboot-system` 提供用户 API。会把本变更阻塞在最后一个拆分之后。否决。
- 退出条件：`iboot-system` 拆分后改调系统服务，与 file/notify/job 的跨库读一起收敛。

### D5：通知按 Nacos 服务名寻址，失败不阻断

沿用 `NotificationPort` 抽象与 `/internal/notifications` 契约；新服务使用 `@LoadBalanced` 的 `RestClient.Builder` 指向 `http://iboot-notify`，保留 `iboot.notification.base-url` 作为本地静态覆盖。

- 备选：照抄 admin 的静态 `http://localhost:8084`。新服务没有历史包袱，不应继续增加静态地址债务。否决。
- 通知调用在事务提交后异步执行，失败只计数与告警；这一点与现状一致。

### D6：安全与响应信封完全复用内核

与 file/notify/job 一致：使用内核的 `JwtAuthenticationFilter`、统一异常处理、`CloudEventResponseBodyAdvice`（`basePackages` 指向本服务 controller 包）、`ApiVersionStrategy`、内核 Redis 模板；`jwt.secret` 与其它服务逐字一致。不自建 `SecurityFilterChain`。

### D7：网关按前缀路由，order 26

`/api/approval` → `lb://iboot-workflow`，order 26，位于 admin 兜底（100）之前。与 job 的 `/api/monitor/backup` 无前缀重叠，不需要路径级特例。

### D8：审计日志按 `fix-operate-log-write-path` 的约定接入

workflow 自身 schema 没有 `sys_operate_log`，因此新服务同样配置 `iboot.logging.operate-log-table: iboot_admin.sys_operate_log`。审批的 3 个 `@Log` 端点不因拆分而丢审计。

### D9：回滚只在切流窗口内是「改回路由」

切流后新产生的审批只写入 `iboot_workflow`。因此：

- **窗口内回滚**：admin 旧实现仍在，路由改回即可，数据未分叉。
- **窗口后回滚**：需要先做一次反向增量复制（workflow → admin），再改路由；脚本随本变更交付。
- `remove-workflow-from-admin` 会把回滚方式改为 `git revert` + 数据源指回 `iboot_admin`。

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| 首次启动重复部署 BPMN，产生新流程定义版本 | 先搬 `ACT_RE_*` 再启动；验收断言两次重启后 `ACT_RE_DEPLOYMENT` 行数不变 |
| `ACT_GE_PROPERTY` 版本不一致导致引擎 schema 校验失败 | 搬迁脚本 `REPLACE` 后断言 `common.schema.version`；不一致立即中止 |
| 在途实例在新服务无法继续 | 迁移前记录一个在途实例 ID；切流后在新服务完成一次审批 |
| 过渡期 admin 与新服务各有一个 Flowable 引擎 | 两侧都保持 `async-executor-activate=false`；路由已切走，旧引擎不会被调用；不开启异步执行器即不会重复执行后台作业 |
| 跨库读 `sys_user` 的授权或可用性问题 | 沿用 file/notify/job 的跨库约定；启动自检与失败关闭；`doc/23` 登记退出条件 |
| 新增第 8 个 JVM 带来内存压力 | 本地按需启动；`dev-init.sh` 增加服务但不默认全量拉起；记录 admin 与新服务的堆占用 |
| 通知地址配置错误导致通知静默失败 | 保留失败计数 `notification_trigger_failed_total`；冒烟检查审批成功与通知失败解耦 |
| Testcontainers 拉取 Ryuk 失败（本机已知） | 沿用现有测试配置中的本地 `ryuk:0.12.0` 方案，不修改 |

## Migration Plan

1. **准备**：备份 `iboot_admin` 的 33 张表；记录在途实例与各表行数。
2. **停机**：停 admin；确认 33 张表在停写后行数稳定。
3. **建库**：创建 `iboot_workflow`；执行 V1。
4. **搬迁**：执行 `scripts/migrate/workflow_to_iboot_workflow.sql`；逐表校验行数与 `max(id)`；断言 `common.schema.version`。
5. **启动与验收**：启动 workflow；`/actuator/health`、Nacos 注册、引擎启动、在途实例可审批、重启不重复部署。
6. **切流**：网关路由指向 workflow；经网关跑审批冒烟、权限矩阵、链路追踪与故障隔离。
7. **回滚**：窗口内改回路由；窗口后先执行反向增量复制再改路由。

## Open Questions

无。dashboard/statistics 的归属、monitor 的日志写入长期方案均不影响本变更的规格、实现与任务拆分，留到各自变更。
