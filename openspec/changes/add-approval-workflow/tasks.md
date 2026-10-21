# Tasks

## 1. 依赖与基础设施适配

- [x] 1.1 在父 POM 增加 `flowable.version=8.0.0` 与 `flowable-spring-boot-starter-process` 的依赖管理，在 `iboot-admin/pom.xml` 引入并排除 `HikariCP`；验证 `mvn -s /home/lam/repo/settings.xml -pl iboot-admin dependency:tree` 中无 HikariCP
- [x] 1.2 将 `AsyncConfig.taskExecutor()` 改为返回 `AsyncTaskExecutor`（`TaskExecutorAdapter` 包装虚拟线程执行器）；验证项目内没有按 `Executor` 类型强转该 Bean 的代码
- [x] 1.3 在 `application.yml` 增加 Flowable 配置（`database-schema-update=false`、`async-executor-activate=false`、`idm.enabled=false`、`eventregistry.enabled=false`）；验证配置项与 design D1 一致
- [x] 1.4 新增上下文守卫测试：`ApplicationContextRunner` 装配 `AsyncConfig` + Flowable 自动配置 + 测试数据源；验证上下文启动成功且 `taskExecutor` 不冲突

## 2. 数据库迁移

- [x] 2.1 从 Flowable 8.0.0 JAR 提取 common / engine / history 的 MySQL 建表脚本，生成 `V17__add_approval_workflow.sql`（去掉 `DROP`，保留 `ACT_GE_PROPERTY` 版本行）；验证空库执行迁移成功
- [x] 2.2 在 V17 中新增 `biz_leave_application` 表与 `applicant_id`、`approver_id`、`status`、唯一 `process_instance_id` 索引；验证迁移后表结构符合 design D5
- [x] 2.3 在 V17 中插入审批菜单 9100–9104 与管理员角色 1 的 `sys_role_menu` 授权；验证 SQL 查询返回 5 条菜单且角色 1 均有授权
- [x] 2.4 新增迁移集成测试：空库执行 Flyway V1–V17，断言 `ACT_*` 表数量与 `biz_leave_application` 存在，且第二次启动引擎不再执行 DDL；验证测试通过
- [x] 2.5 更新 `doc/07-数据库设计.md`，补充审批相关表与索引说明；验证文档中的表名与迁移一致

## 3. 领域模型与持久化

- [x] 3.1 新增 `domain/workflow/model`（`LeaveApplication`、`LeaveStatus`、`WorkflowTask`、`WorkflowHistoryItem`）与 `LeaveApplicationRepository` 接口；用单测验证状态流转只允许 `PENDING → APPROVED/REJECTED`
- [x] 3.2 新增 `WorkflowService` 领域接口（启动流程、查询当前任务、完成任务、查询历史）；验证 `grep -rn "com.flowable" domain application` 无结果
- [x] 3.3 新增 `LeaveApplicationPO`、MyBatis Mapper 与 XML、`LeaveApplicationRepositoryImpl`、MapStruct 转换器；用 Testcontainers MySQL 集成测试验证保存、按 ID 查询、分页与 `process_instance_id` 唯一约束

## 4. 流程定义与适配器

- [x] 4.1 新增 `resources/processes/leave-approval.bpmn20.xml`（start → approve → 通过归档 / 驳回结束）；验证集成测试中流程定义可部署且 key 为 `leaveApproval`
- [x] 4.2 实现 `FlowableWorkflowAdapter`（启动实例、查询活动任务、通过/驳回完成、历史查询）；集成测试验证通过路径与驳回路径的流程状态
- [x] 4.3 验证 `database-schema-update=false` 时引擎启动不建表；集成测试断言启动前后 `ACT_*` 表数量不变

## 5. 应用服务

- [x] 5.1 实现 `ApprovalApplicationService.submit`：校验审批人存在、启用且非本人，保存业务记录并启动流程；单测覆盖四类校验，集成测试验证成功与事务回滚
- [x] 5.2 实现待办、已办、我的申请查询与状态筛选；单测验证可见性与筛选条件，集成测试验证只返回当前用户数据
- [x] 5.3 实现 `approve` 与 `reject`：校验当前任务归属、禁止自审、重复处理返回冲突、驳回意见必填；集成测试覆盖全部失败分支
- [x] 5.4 实现详情与审批历史组装：参与人可见、管理员 `approval:list` 可见全部、无关用户返回 403/404；单测覆盖三类查看者

## 6. REST API 与权限

- [x] 6.1 新增请求/响应 DTO 与 `ApprovalController` 的 7 个端点（发起、待办、已办、我的、详情、通过、驳回）；MockMvc 验证成功路径与参数校验
- [x] 6.2 为端点添加 `@PreAuthorize` 并在服务层执行参与人校验；MockMvc 验证无权限 403、越权 404、重复审批冲突
- [x] 6.3 更新 `doc/04-功能模块.md` 与 `doc/06-API 集成.md`，补充审批模块与接口/权限说明；验证文档中的路径与控制器一致

## 7. 前端审批中心

- [x] 7.1 新增 `src/api/approval.ts` 与类型定义；验证 `cnpm run build` 的类型检查通过
- [x] 7.2 新增 `ApprovalCenterView.vue` 三页签（待办/已办/我的申请）与权限门控；验证无权限时页签与按钮隐藏
- [x] 7.3 新增 `LeaveApplyDialog.vue`、`ApprovalDetailDrawer.vue` 与通过/驳回交互；验证表单校验与驳回意见必填
- [x] 7.4 注册路由 `approval`、新增中英文 `approval.json` 并接入 locales；验证 `cnpm run build` 与 `cnpm run check:ui` 通过

## 8. 集成验证与交付

- [x] 8.1 运行 `TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 mvn -s /home/lam/repo/settings.xml clean verify`；验证全部测试通过
- [x] 8.2 启动后端与前端，用两个账号完成「发起 → 待办 → 通过/驳回 → 查看历史」手工冒烟；验证界面状态与后端一致
- [x] 8.3 在功能分支提交并用 `gh pr create --base master` 创建 PR，按 CLAUDE.md 流程合并；验证 PR 已合并且本地 `master` 已 `pull --ff-only`
