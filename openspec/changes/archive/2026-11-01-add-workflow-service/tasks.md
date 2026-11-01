# Tasks

## 1. 模块与骨架

- [x] 1.1 根 `pom.xml` 增加 `iboot-workflow` 模块，模块 pom 依赖 `iboot-kernel`、Flowable、MyBatis-Plus、MySQL、Redis、Nacos 与 OTLP，并声明 `spring-boot-maven-plugin`；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-workflow -am package -DskipTests` 生成带主清单的 jar
- [x] 1.2 创建启动类与 `application.yml`（端口 8086、服务名 `iboot-workflow`、`iboot_workflow` 数据源、Redis、`jwt.*`、`iboot.security.whitelist`、`@MapperScan`、`spring.config.import`、OTLP、`iboot.logging.operate-log-table: iboot_admin.sys_operate_log`）；验证：服务启动后 `/actuator/health` 为 `UP`，Nacos 服务列表出现 `iboot-workflow`
- [x] 1.3 核对 `doc/23` §11.2 的四个隐式契约：CloudEvent `basePackages` 指向 `com.iboot.workflow.interfaces.controller`、`ApiVersionStrategy` 生效、内核 Redis 模板未被覆盖、`jwt.secret` 与其它服务逐字一致；验证：带令牌请求 `/api/approval/mine` 返回业务信封而不是 401/500
- [x] 1.4 `scripts/dev-init.sh` 的 `SERVICES` 增加 `iboot-workflow:8086`，并创建 `iboot_workflow` schema；验证：脚本输出的启动列表包含该服务，schema 存在

## 2. Schema 与数据搬迁

- [x] 2.1 新增 `V1__initial_schema.sql`：从 admin `V17` 复制 33 张表的 DDL 与 `ACT_GE_PROPERTY` 版本行，不复制 `sys_menu` / `sys_role_menu` 种子；验证：空库启动后 `flyway_schema_history` 成功，`iboot_workflow` 共 33 张业务表 + 1 张历史表
- [x] 2.2 新增 `scripts/migrate/workflow_to_iboot_workflow.sql`：按依赖顺序复制 33 张表，`ACT_GE_PROPERTY` 使用 `REPLACE INTO`，并包含逐表行数与 `max(id)` 校验；验证：在演练库执行后两侧逐表一致，`common.schema.version` 与 Flowable 版本一致
- [x] 2.3 在 `doc/23` §11.9 记录 schema 归属、搬迁顺序与回滚方式；验证：按文档能独立完成一次搬迁与校验

## 3. 代码搬迁与适配

- [x] 3.1 搬迁 `ApprovalController`、`ApprovalApplicationService`/`ApprovalDetail`/`ApprovalItem`、`domain/workflow`、`infrastructure/workflow`、`LeaveApplicationMapper` 与 XML、5 个 DTO、BPMN 与对应测试；验证：模块编译通过，迁移后的单测全部通过
- [x] 3.2 新增 `WorkflowUserReader` 跨库只读 `iboot_admin.sys_user` 的 `id/status/username/nickname`，替代 `UserRepository`，数据访问异常转换为明确的业务错误；验证：集成测试覆盖审批人有效、停用、不存在、用户目录不可用四种情况，行为与拆分前一致
- [x] 3.3 配置 Flowable（`database-schema-update=false`、`async-executor-activate=false`、`check-process-definitions=true`、IDM/Event Registry 关闭）并确认 BPMN 部署幂等；验证：连续重启两次，`ACT_RE_DEPLOYMENT` 与 `ACT_RE_PROCDEF` 行数不变
- [x] 3.4 通知端口改为按 Nacos 服务名寻址 notify，保留 `iboot.notification.base-url` 静态覆盖与 `X-Internal-Token`；验证：notify 在线时审批提交产生通知登记，notify 停止时审批仍成功且 `notification_trigger_failed_total` 增加
- [x] 3.5 迁移并补充 Testcontainers 集成测试（真实 MySQL + Flowable）：发起、待办隔离、通过、驳回、重复处理、自我审批、事务回滚；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-workflow test` 全部通过
- [x] 3.6 增加在途实例测试：迁移前创建一个待审批实例，搬迁后在测试库中完成审批；验证：该实例可查询、可通过，历史表出现对应记录

## 4. 网关与回归装置

- [x] 4.1 网关 `application-local.yml` / `application-nacos.yml` 增加 `/api/approval` → workflow，order 26；`routes.txt` 增加一行；验证：`bash scripts/regress/route-coverage.sh` 通过
- [x] 4.2 `regress.sh` 增加 `REG_WORKFLOW_BASE=http://localhost:8086`、快照与 golden 循环；`migrations.txt` 增加 `/api/approval admin workflow transitional`；验证：`bash scripts/regress/regress.sh` 第 1–3 段通过，`golden/workflow.json` 生成
- [x] 4.3 只读等价性覆盖审批的 `mine` / `todo` / `{id}`；验证：迁移前录制、经网关重放后响应归一化一致
- [x] 4.4 `smoke.sh` 审批块补充通过、驳回与详情断言；验证：经网关完成一次完整流转（发起 → 待办 → 通过 → 已办/详情）
- [x] 4.5 `permission-matrix.sh` 新增审批三组用例：仅 `approval:apply`、同时 `approval:approve`、无审批权限；验证：允许/拒绝结果与拆分前一致
- [x] 4.6 `trace-chain.sh` 增加 `gateway -> workflow` 断言；验证：一次审批查询的 TraceID 覆盖 `iboot-gateway` 与 `iboot-workflow`
- [x] 4.7 执行全量回归；验证：`bash scripts/regress/regress.sh` 全绿

## 5. 切换演练与文档

- [x] 5.1 完整停机演练：停 admin → 搬数据 → 启动 workflow → 切路由 → 完成在途审批 → 重启两次；验证：迁移前后逐表行数一致、在途实例可处理、部署行数不变
- [x] 5.2 双向故障隔离：停 workflow 时 `/api/approval/**` 返回明确 503 而 `/api/user/list` 正常；停 admin 时审批仍可完成；验证：两项实测通过
- [x] 5.3 记录 admin 与新服务的堆占用、启动时间；确认 `iboot-portal` 源码零改动；验证：`git diff --stat -- iboot-portal` 为空，数值记入 `doc/23` §11.9
- [x] 5.4 更新 `ROADMAP` §4.2 状态表与拆分顺序说明、`doc/23` 服务清单与 §11.9 验收记录、`README` 项目结构；验证：三处服务清单一致，顺序说明包含“workflow 先于 monitor”的理由
