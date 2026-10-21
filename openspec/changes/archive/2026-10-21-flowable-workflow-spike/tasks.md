# Tasks

## 1. 隔离分支与依赖基线

- [x] 1.1 从 `master` 切出 `spike/flowable-workflow` 分支，并确认工作区干净、分支名正确（`git status` + `git branch --show-current`）
- [x] 1.2 在改动依赖前采集基线：用 `docker compose up -d mysql redis minio` 启动所需中间件，启动现有应用并记录启动耗时、RSS 与线程数（记录到报告草稿的基线表）
- [x] 1.3 在 `iboot-admin/pom.xml` 增加 `org.flowable:flowable-spring-boot-starter-process:8.0.0`，排除其传递引入的 `HikariCP`；用 `mvn -s /home/lam/repo/settings.xml dependency:tree` 验证解析成功且 HikariCP 不出现
- [x] 1.4 用依赖树确认没有重新引入 Jackson 2（`com.fasterxml.jackson.core:jackson-databind`）；记录任何残留项并确认流程变量映射使用 Jackson 3

## 2. 最小上下文引擎启动

- [x] 2.1 新增最小 Spring 上下文测试（`ApplicationContextRunner` 或精简 `@SpringBootTest`）+ Testcontainers `mysql:8.0.45`；验证 `ProcessEngine` Bean 存在且 `ACT_*` 表被创建
- [x] 2.2 新增 `src/main/resources/processes/leave.bpmn20.xml`（发起 → 审批 → 归档 / 驳回）；验证流程定义可从 classpath 部署，部署后 `RepositoryService` 能查到定义
- [x] 2.3 在测试配置中关闭异步执行器与 Flowable IDM；验证引擎启动后没有额外的作业执行线程，且未创建 Flowable 自己的用户/组表

## 3. 请假流程双路径验证

- [x] 3.1 编写通过路径测试：发起 → 审批通过 → 归档；验证流程实例结束状态与历史任务数量符合预期
- [x] 3.2 编写驳回路径测试：发起 → 驳回；验证流程实例结束状态与驳回意见可查询
- [x] 3.3 用 `TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 mvn -s /home/lam/repo/settings.xml test -Dtest='Flowable*Test'` 运行，验证两条路径在本地自动通过（无需人工步骤）

## 4. 真实应用共存与回归

- [x] 4.1 带 Flowable 依赖启动完整 `iboot-admin`（compose 中间件已启动），验证应用启动成功且 `/actuator/health` 返回正常
- [x] 4.2 验证 `DataScopeInterceptor` 不影响 Flowable 自建 `SqlSessionFactory`：在同一上下文中执行一次带数据权限的 Mapper 查询与一次流程推进，验证前者仍被重写、后者 SQL 未被改写（日志或单测断言）
- [x] 4.3 验证事务一致性：在同一事务内写业务表 + 推进流程并强制抛出异常，确认业务表与 `ACT_RU_*` 同步回滚
- [x] 4.4 运行全量 `TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 mvn -s /home/lam/repo/settings.xml clean verify`，验证现有 17 个测试类全部通过、无回归
- [x] 4.5 采集增量指标：启动耗时、RSS、线程数、`ACT_*` 表数量、Flyway 与引擎自建表的共存情况；记录到报告草稿的对比表

## 5. Spike 报告与决策

- [x] 5.1 编写 `doc/spike/flowable-8-spike-report.md`：包含版本事实、硬门槛逐项结论、依赖冲突清单、实测指标、风险与备选方案
- [x] 5.2 按 design.md 的硬门槛 + 加权评分给出明确的 Go / No-Go 建议，并写明若 No-Go 时的 fallback 候选；验证报告对每条硬门槛都有结论
- [x] 5.3 依据报告与用户确认最终决策，随后删除 `spike/flowable-workflow` 分支；验证分支已删除且 `master` 无 Spike 代码

## 6. ROADMAP 修正

- [x] 6.1 更新 `ROADMAP.md` 0.5 行的技术栈与约束（Flowable 7 → Flowable 8.0.0）；验证 `grep -n "Flowable 7" ROADMAP.md` 无残留
- [x] 6.2 更新 §6.1 版本矩阵中的 Flowable 行（`7.x（未验证）` → `8.0.0`）；验证表格与 design.md 的事实一致
- [x] 6.3 更新 R7 风险登记为"Flowable 8 与现有技术栈共存"，处置改为 Spike 门禁 + fallback 待定；验证不再引用 Spring Statemachine 作为既定降级方案
- [x] 6.4 在 `docs/flowable-spike-roadmap` 分支提交文档修正，用 `gh pr create --base master` 创建 PR 并按 CLAUDE.md 流程合并；验证 PR 已合并且本地 `master` 已 `pull --ff-only`
