# Proposal

## Why

阶段 0.5 是阶段 0 剩余的两项之一。Flowable 8 Spike 已给出 Go 结论，但验证代码位于一次性分支并已丢弃：生产依赖、DDL、领域边界、API 与前端均不存在。现在需要把 Spike 结论转化为可运行、可测试、界面上真正可用的审批能力，同时把 Flowable 限制在 `infrastructure` 层，避免阶段 2 拆分 `iboot-workflow` 时返工。

## What Changes

- 在 `iboot-admin` 引入 Flowable **8.0.0**（`flowable-spring-boot-starter-process`），排除 HikariCP，保持 Druid 为唯一连接池。
- 按 Spike 要求关闭自动建表、IDM、异步执行器与事件注册表；`ACT_*` 表由 Flyway 接管。
- 将 `AsyncConfig.taskExecutor()` 适配为 `AsyncTaskExecutor`，避免与 Flowable 的同名 Bean 冲突。
- 新增 `domain/workflow`：`WorkflowService` 接口与纯业务类型，零 Flowable 依赖。
- 新增 `infrastructure/workflow`：`FlowableWorkflowAdapter` 实现领域接口。
- 新增请假审批业务：`biz_leave_application` 表、仓储、应用服务与 REST API（发起、待办、已办、我的申请、详情、通过、驳回）。
- 新增 `iboot-portal` 审批中心：待办 / 已办 / 我的申请三个页签，发起请假、查看详情与审批/驳回操作。
- 新增菜单与权限种子：`approval:list`、`approval:apply`、`approval:approve`、`approval:query`。
- 测试覆盖：领域/应用单测、Testcontainers MySQL 集成测试、应用上下文启动检查、权限与可见性用例。

## Capabilities

### New Capabilities

- `approval-workflow`: 请假审批的发起、待办与已办查询、通过/驳回、详情与历史，以及按申请人/审批人/管理员的可见性规则。

### Modified Capabilities

无。现有 `content-search`、`file-storage`、`portal-design-system` 的需求不发生变化；审批中心复用既有设计系统，不新增设计规范。

## Impact

- **构建**：新增约 46 个 Flowable 构件，应用 JAR 增加约 7 MB；`pom.xml` 与 `iboot-admin/pom.xml` 需要新增依赖与排除。
- **数据库**：新增 Flyway 迁移，包含 30 张 `ACT_*` 表（关闭事件注册表后为 29 张）、`biz_leave_application`、菜单与权限种子。
- **后端分层**：新增 `domain/workflow`、`infrastructure/workflow`、`application/service`、`interfaces/controller` 与对应 DTO、Mapper。
- **前端**：新增 `api/approval.ts`、`views/approval/*`、路由子项与中英文语言包；侧边栏由后端菜单树驱动。
- **运行时**：RSS 增加约 74–110 MB，热启动约 13.7 s；IDM 与异步执行器保持关闭。
- **安全**：审批数据按参与人隔离；管理员通过 `approval:list` 查看全量；审批人不得审批自己的申请。
