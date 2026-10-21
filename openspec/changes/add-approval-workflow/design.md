# Design

## Context

动机见 `proposal.md`，选型依据见 `doc/spike/flowable-8-spike-report.md`。本设计只补充落地所需的工程约束。

**现有约定**

- 后端严格四层：`interfaces` → `application` → `domain` → `infrastructure`；`domain` 只放实体、值对象与仓储接口。
- 持久层为 Druid + 自定义 `SqlSessionFactory`，Mapper XML 位于 `resources/mapper`，PO 位于 `infrastructure/persistence/po`。
- 权限使用 `@PreAuthorize("hasAuthority('xxx:yyy')")`；当前用户通过 `SecurityUtils` 获取。
- 前端侧边栏由后端菜单树驱动，路由为 `router/index.ts` 中的静态子路由；菜单 `path` 的最后一段必须与路由子路径一致。
- 前端命令使用 `cnpm`；构建校验为 `vue-tsc && vite build`。

**Spike 已确认的事实**

- Flowable 8.0.0 可在 Boot 4.0.3 / JDK 25 上启动并完成流程。
- `AsyncConfig.taskExecutor` 必须返回 `AsyncTaskExecutor`，否则同名 Bean 冲突。
- 必须排除 HikariCP；Jackson 3 与 MyBatis 拦截器无需额外处理。
- 30 张 `ACT_*` 表中包含事件注册表；关闭事件注册表可减少表数量。

## Goals / Non-Goals

**Goals:**

- 把 Flowable 限制在 `infrastructure/workflow`，`domain` 与 `application` 不出现任何 Flowable 类型。
- 审批数据与流程状态在同一事务内更新。
- 请假审批在管理端界面上端到端可用：发起、待办、已办、我的申请、通过、驳回、详情历史。
- 权限、可见性与菜单种子可验证。

**Non-Goals:**

- 不做多级审批、会签、加签、转办、撤回、附件与抄送。
- 不做多渠道通知（属于 0.6）。
- 不做管理员「全部申请」独立列表页；`approval:list` 仅用于详情可见性与后续扩展。
- 不做多实例集群下的作业抢占验证（属于阶段 2）。
- 不引入 Flowable 异步执行器、IDM 或事件注册表。

## Decisions

### D1：引擎依赖与配置

使用 `flowable-spring-boot-starter-process:8.0.0`，并设置：

```yaml
flowable:
  database-schema-update: false
  async-executor-activate: false
  check-process-definitions: true
  idm:
    enabled: false
  eventregistry:
    enabled: false
```

- **理由**：Schema 由 Flyway 拥有；只需要流程引擎；IDM 会引入第二套用户模型；事件注册表对本变更无用。
- **备选**：保留自动建表（生产不可控）；保留 IDM（与 `sys_user` 冲突）。

### D2：AsyncConfig 适配

`taskExecutor()` 返回 `AsyncTaskExecutor`，内部用 `TaskExecutorAdapter` 包装 `Executors.newVirtualThreadPerTaskExecutor()`。

- **理由**：Flowable 的 `@ConditionalOnMissingBean` 按 `AsyncTaskExecutor` 判断；适配后 Flowable 退让，虚拟线程语义不变。
- **备选**：开启 `allow-bean-definition-overriding`（会静默替换项目的执行器）；重命名 Bean（影响 `@Async` 默认解析）。

### D3：DDL 与迁移

新增 `V17__add_approval_workflow.sql`，包含：

1. Flowable 的 common / engine / history 建表脚本（从 JAR 的 `org/flowable/**/db/create/*.mysql.create.*.sql` 提取，去掉 `DROP`）。
2. 业务表 `biz_leave_application`。
3. 审批菜单与按钮权限、管理员角色授权。

- **理由**：Flyway 是唯一 Schema 变更入口。实测确认 `ACT_EVT_LOG` 由 engine DDL 创建，关闭事件注册表不会移除它；迁移后共 30 张 `ACT_*` 表、2 张 `FLW_RU_BATCH*` 表与 1 张业务表。
- **备选**：生产保留 `database-schema-update=true`（与 Flyway 版本管理冲突）。
- **验证**：空库执行 Flyway 后，Flowable 引擎第二次启动不得再执行任何 DDL。

### D4：领域边界

```
domain/workflow
├── model/       LeaveApplication, WorkflowTask, WorkflowHistoryItem, LeaveStatus
├── repository/  LeaveApplicationRepository
└── service/     WorkflowService   <- 纯接口，零 Flowable 依赖

infrastructure/workflow
└── FlowableWorkflowAdapter implements WorkflowService
```

- **理由**：阶段 2 把 `infrastructure/workflow` 整体搬进 `iboot-workflow` 即可，`domain`/`application` 不改。
- **判别**：删除 `infrastructure/workflow` 后，`domain` 与 `application` 仍可编译。

### D5：业务表模型

`biz_leave_application` 字段：

| 字段 | 说明 |
|---|---|
| `id` | 主键 |
| `applicant_id` | 申请人用户 ID |
| `approver_id` | 审批人用户 ID |
| `leave_type` | 请假类型（字典驱动） |
| `start_time` / `end_time` | 起止时间 |
| `reason` | 请假事由 |
| `status` | `PENDING` / `APPROVED` / `REJECTED` |
| `process_instance_id` | Flowable 流程实例 ID，唯一索引 |
| `approve_comment` | 审批意见或驳回原因 |
| `create_time` / `update_time` | 时间戳 |

索引：`applicant_id`、`approver_id`、`status`、`process_instance_id` 唯一。

### D6：流程定义

`processes/leave-approval.bpmn20.xml`，流程 key `leaveApproval`：

```
start --> approve(assignee=${approverId}) --> gateway(${approved})
                                                |-- true  --> end(archived)
                                                +-- false --> end(rejected)
```

- **理由**：发起动作由 API 完成，不需要额外的用户任务；「归档」通过通过的结束事件表达，避免多一次人工点击。
- **变量**：`applicantId`、`approverId`、`applicationId`、`approved`。

### D7：API 设计

统一前缀 `/api/approval`，全部带 `version = "1"`：

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/leave` | `approval:apply` | 发起请假 |
| GET | `/todo` | `approval:approve` | 我的待办 |
| GET | `/done` | `approval:approve` | 我的已办 |
| GET | `/mine` | `approval:apply` | 我的申请，支持状态筛选 |
| GET | `/{id}` | 登录 + 参与人校验 | 详情与历史 |
| POST | `/{id}/approve` | `approval:approve` | 通过 |
| POST | `/{id}/reject` | `approval:approve` | 驳回，意见必填 |

- **理由**：以业务申请 ID 作为操作入口，由服务端解析当前任务，避免向前端暴露 Flowable taskId。
- **备选**：`/tasks/{taskId}/complete`（泄漏引擎模型，且前端难以拿到 taskId）。

### D8：状态与可见性

- 状态机：`PENDING` → `APPROVED` / `REJECTED`，无回退。
- 可见性：申请人可见自己的；审批人可见指派给自己的；拥有 `approval:list` 或 `approval:query` 的用户可见全部。
- 禁止自审：审批人必须不等于申请人，提交时与审批时各校验一次。
- 越权访问统一返回 403 或 404，不区分「不存在」与「无权」，避免探测。

### D9：事务边界

`submit`、`approve`、`reject` 在 `@Transactional` 中完成「业务表更新 + 流程推进」。Flowable 使用同一个 `PlatformTransactionManager`，任一步骤异常时二者一起回滚。

- **验证**：集成测试强制在两者之间抛异常，断言业务状态与任务状态都未改变。

### D10：应用服务编排

`ApprovalApplicationService` 负责：

1. 校验申请人、审批人与可见性。
2. 保存业务记录。
3. 调用 `WorkflowService` 启动或推进流程。
4. 回写业务状态与审批意见。
5. 组装详情与历史响应。

仓储与流程服务通过构造器注入，便于单测替换。

### D11：前端结构

```
src/api/approval.ts                         类型化 API
src/views/approval/ApprovalCenterView.vue   三页签：待办 / 已办 / 我的申请
src/views/approval/LeaveApplyDialog.vue     发起请假
src/views/approval/ApprovalDetailDrawer.vue 详情 + 审批历史 + 通过/驳回
src/router/index.ts                         + { path: "approval", name: "ApprovalCenter" }
src/locales/{zh-CN,en-US}/approval.json     + 注册到 locales/index.ts
```

- **权限控制**：页签与按钮使用 `permissionStore.hasPermission`；无 `approval:apply` 隐藏「发起」与「我的申请」，无 `approval:approve` 隐藏「待办」「已办」与操作按钮。
- **理由**：复用现有页面模式与设计系统，不新增组件库。

### D12：菜单与权限种子

| ID | 类型 | 名称 | 路径 | 权限 |
|---|---|---|---|---|
| 9100 | C | 审批中心 | `/approval` | `approval:apply` |
| 9101 | F | 发起请假 | — | `approval:apply` |
| 9102 | F | 审批操作 | — | `approval:approve` |
| 9103 | F | 全部申请 | — | `approval:list` |
| 9104 | F | 申请详情 | — | `approval:query` |

- 图标使用 `finished`；`sys_role_menu` 授予角色 1（超级管理员）。
- **理由**：菜单 ID 避开现有 9011 以下的范围；侧边栏按最后一段 `approval` 跳转 `/dashboard/approval`。

### D13：测试策略

| 层级 | 内容 | 工具 |
|---|---|---|
| 领域/应用单测 | 状态流转、可见性、自审拒绝、校验 | JUnit 5 + Mockito |
| 集成测试 | Flyway 全量迁移、Flowable 启动、发起/通过/驳回、事务回滚 | Testcontainers MySQL 8.0.45 |
| 上下文守卫 | 导入 `AsyncConfig` + Flowable 自动配置的上下文可启动 | `ApplicationContextRunner` |
| 接口测试 | 权限、参数校验、越权 | MockMvc + `spring-security-test` |
| 前端 | 类型检查与构建 | `cnpm run build`、`cnpm run check:ui` |

- **理由**：Spike 已证明运行时可行；本次测试重点是迁移、事务、权限与 Bean 冲突回归。

## Risks / Trade-offs

- **[Flyway 迁移脚本与 Flowable 8.0.0 版本强绑定]** → 在迁移文件头注明来源版本；升级 Flowable 时必须对比 DDL 差异。
- **[关闭自动建表后，手工迁移遗漏表]** → 以空库全量迁移 + 引擎启动成功作为门禁；集成测试覆盖。
- **[审批人离职/停用导致待办无人处理]** → 本变更不做自动转办；在详情中展示审批人状态，后续变更补转办。
- **[并发重复审批]** → 以任务是否存在 + 业务状态双重校验；重复提交返回冲突。
- **[前端无自动化测试]** → 以 `vue-tsc` + 构建 + 手工冒烟为验收，后续再补组件测试。
- **[`approval:list` 暂无独立列表页]** → 记录为非目标，后续需要时再加页签。

## Migration Plan

本变更包含生产代码与数据库变更，但仅影响本地/开发环境，未部署。

1. 新增 Flyway `V17`；在空库上执行迁移并启动应用。
2. 后端按层落地，先领域接口与适配器，再应用服务与 API。
3. 前端增加审批中心与路由、语言包。
4. 全量 `mvn clean verify` 与 `cnpm run build` 通过后提交 PR。
5. 回滚：删除 `V17` 迁移与相关代码即可；数据库可通过重建开发库恢复。生产场景下回滚需单独的 down 脚本（本次不提供）。

## Open Questions

- 审批人停用后的自动转办策略，留待后续变更。
- 是否需要在阶段 2 之前把 `ACT_*` 表迁到独立 schema，留待拆分时决定。
