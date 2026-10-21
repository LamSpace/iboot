# Flowable 8 Spike 报告

> 对应变更：`openspec/changes/flowable-workflow-spike`
> 执行日期：2026-10-21
> 执行分支：`spike/flowable-workflow`（一次性验证分支，结论落定后删除）

## 1. 结论

**Go（有条件）。**

Flowable 8.0.0 可以在 Spring Boot 4.0.3 + JDK 25 + 现有技术栈上运行：应用上下文内引擎启动、BPMN 自动部署、请假流程双路径、事务同步回滚、全量回归（83 个测试）全部通过。四项硬门槛均满足。

采纳前需落实以下条件：

1. `AsyncConfig.taskExecutor()` 保持 `AsyncTaskExecutor` 返回类型（本 Spike 已适配，1 行改动 + 包装）。
2. 继续排除 Flowable 传递引入的 `HikariCP`，保持 Druid 为唯一连接池。
3. 生产环境由 Flyway 接管 `ACT_*` 表，设置 `flowable.database-schema-update=false`。
4. 初期保持 `flowable.async-executor-activate=false`、`flowable.idm.enabled=false`，工作流只通过 `domain` 层接口暴露。

## 2. 版本事实

| 事实 | 结论 |
|---|---|
| Flowable 7 × Boot 4 | **不可用**：Flowable 7 不支持 Spring Boot 4 |
| Flowable 8.0.0 | 2026-02-27 发布，基于 Spring Framework 7 / Boot 4，默认 Jackson 3，不再支持 Boot 3 |
| Spring Statemachine | 已移入 `spring-attic` 归档，无 Boot 4 官方基线，不再作为 fallback 首选 |

## 3. 验证环境

- Spring Boot 4.0.3、JDK 25.0.3、Maven 3.9.16
- MySQL 8.0.45（Testcontainers 与 compose 各验证一次）
- Druid 连接池、自定义 `SqlSessionFactory` + `DataScopeInterceptor`、Quartz、虚拟线程
- Jackson 3（`tools.jackson.*`）
- Testcontainers 2.0.5，Ryuk 固定为本地可用的 `testcontainers/ryuk:0.12.0`

## 4. 硬门槛结论

| # | 门槛 | 结果 | 证据 |
|---|---|---|---|
| H1 | `ProcessEngine` 在 Boot 应用上下文中启动 | **通过** | `engineStartsAndCreatesSchema`；完整应用启动成功，`/actuator/health` 返回 200 |
| H2 | BPMN 从 classpath 部署 | **通过** | `leaveProcessDefinitionIsDeployed`；启动日志出现 `SpringBootAutoDeployment` 部署 `processes/leave.bpmn20.xml` |
| H3 | 请假流程双路径单测通过 | **通过** | `approvePathArchivesProcess`（3 个历史任务）、`rejectPathEndsProcess`（2 个历史任务，无归档任务） |
| H4 | 可在 CI 中自动运行 | **通过** | `mvn clean verify`：83 个测试，0 失败，0 跳过 |
| — | Apache 2.0 许可 | **通过** | Flowable OSS 采用 Apache 2.0 |
| — | 引擎类型不泄漏到 `domain` 以上 | **设计已定，待实现** | 目标形态：`domain/workflow/WorkflowService` + `infrastructure/workflow/FlowableWorkflowAdapter` |

附加验证：业务写与流程推进处于同一事务，强制异常后业务表与 `ACT_RU_*` 同步回滚（`businessWriteAndWorkflowAdvanceRollBackTogether`）。

## 5. 依赖与冲突清单

| 项 | 发现 | 处理 |
|---|---|---|
| 依赖数量 | 引入 46 个 `org.flowable` 构件 | 可接受；只使用 `starter-process`，未引入 App/CMMN/DMN |
| 连接池 | `spring-boot-starter-jdbc` 会带回 HikariCP | 已在 `iboot-admin/pom.xml` 排除，Druid 保持唯一 |
| Jackson 2/3 | Jackson 2 仅来自既有 `io.minio:minio:8.5.7` 与 springdoc；Flowable 未引入 Jackson 2 | 无需处理；变量映射实测为 `Jackson3VariableJsonMapper` |
| `taskExecutor` 重名 | 项目 `AsyncConfig` 返回 `Executor`，Flowable 的 `@ConditionalOnMissingBean` 按 `AsyncTaskExecutor` 判断，导致同名注册冲突，应用启动失败 | 已将返回类型改为 `AsyncTaskExecutor`（`TaskExecutorAdapter` 包装虚拟线程执行器），行为不变 |
| MyBatis 拦截器 | Flowable 自建 `SqlSessionFactory`，未挂载 `DataScopeInterceptor`；且该拦截器只改写带 `@DataScope` 的查询 | 无需处理；已在测试中断言 Flowable 工厂不含该拦截器 |
| Quartz | 完整应用中与 Flowable 共存正常；最小测试上下文需排除 `QuartzAutoConfiguration` | 仅测试配置处理，生产无影响 |
| IDM | `flowable.idm.enabled=false` 后未创建任何 `ACT_ID_*` 表 | 保持关闭，审批人使用既有 `sys_user` id |
| 异步执行器 | `flowable.async-executor-activate=false` 后无 `flowable-async` 线程 | 继续保持关闭，异步/定时职责暂由 Quartz 承担 |

## 6. 实测指标

| 指标 | 基线（无 Flowable） | Flowable 8 冷启动 | Flowable 8 热启动 |
|---|---|---|---|
| 启动耗时 | 17.3 s（含 Flyway 首次建表） | 22.4 s（含 Flowable 建 30 张表） | **13.7 s** |
| RSS | 586 MB | 696 MB | 660 MB |
| 线程数 | 86 | 85 | 80 |
| 应用 JAR | 128.8 MB | — | 136.0 MB |
| `ACT_*` 表 | 0 | 30 | 30 |
| Flyway 迁移 | 16 | 16（未变） | 16（未变） |

`ACT_*` 表分布：`ACT_HI_*` 10、`ACT_RU_*` 13、`ACT_RE_*` 3、`ACT_GE_*` 2、`ACT_PROCDEF_INFO` 1、`ACT_EVT_LOG` 1。

说明：冷启动的 +5.1 s 主要来自一次性建表；热启动 13.7 s。RSS 增量为 +74～110 MB（受 GC 时序影响）。基线热启动未单独测量，这是本报告的一个数据缺口。

## 7. 生产化待决项

1. **DDL 归属**：Flowable 的建表脚本位于 JAR 内（`org/flowable/common/db/create/flowable.mysql.create.common.sql`、`org/flowable/db/create/flowable.mysql.create.engine.sql` 等）。建议转成 Flyway 迁移，生产关闭自动建表。
2. **异步执行器**：当前关闭。0.5 实现时需决定 Flowable 异步作业与 Quartz 的职责边界。
3. **历史级别**：当前默认 `audit`。若需要完整变量审计，评估是否升到 `full`。
4. **事件注册表**：即使只用流程引擎，仍创建了 `ACT_EVT_LOG`。确认是否可以关闭 event registry 以进一步缩小表数量。
5. **Schema 隔离**：当前与业务表同库同 schema；阶段 2 拆分时应独立 schema。
6. **集群**：本 Spike 未验证多实例部署下的作业抢占与锁行为，留待阶段 2。

## 8. 加权评分

| 维度 | 权重 | 得分（1–5） | 说明 |
|---|---|---|---|
| 集成成本 | 25% | 3 | 需要 1 处 Bean 适配 + Hikari 排除；无 Jackson/MyBatis 冲突 |
| 生产就绪度 | 25% | 5 | BPMN、历史、网关、会签、补偿等开箱可用 |
| 交付速度 | 20% | 4 | 半天内完成引擎启动与双路径流程 |
| 运维足迹 | 15% | 3 | +74～110 MB RSS、+7 MB JAR、30 张表、46 个构件 |
| 演进路径 | 10% | 4 | 通过 domain 接口可在阶段 2 平移为 `iboot-workflow` |
| 学习价值 | 5% | 5 | 与路线图的 BPMN/工作流学习目标直接对应 |
| **加权总分** | | **3.9 / 5** | |

由于 Spring Statemachine 已归档、自研引擎缺乏生产级能力，Flowable 8 实际上是当前唯一可满足 0.5 目标的候选；上述评分用于校准信心，而非在多方案之间做横向比较。

## 9. 风险与缓解

| 风险 | 缓解 |
|---|---|
| Flowable 8 发布仅约 8 个月，生产成熟度待观察 | 钉 8.0.0；关注 8.0.x 补丁；业务侧只依赖 `WorkflowService` 接口，保留替换空间 |
| `ACT_*` 表与业务表同 schema、且不由 Flyway 管理 | 0.5 落地时转为 Flyway 迁移；阶段 2 拆独立 schema |
| `taskExecutor` 适配若被回退，应用将无法启动 | 在 0.5 的验收测试中加入应用上下文启动检查 |
| 异步执行器与 Quartz 职责重叠 | 保持关闭，0.5 设计阶段单独决策 |
| 单实例验证，未覆盖集群 | 阶段 2 拆分时补充多实例验证 |

## 10. 复现步骤

```bash
# 依赖解析与冲突检查
mvn -s /home/lam/repo/settings.xml -pl iboot-admin dependency:tree

# Level A：最小上下文 + 流程双路径 + 事务回滚
TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 \
  mvn -s /home/lam/repo/settings.xml -pl iboot-admin test -Dtest='Flowable*Test'

# Level B：完整应用启动
docker compose -f docker/docker-compose.yml up -d mysql redis minio
java -jar iboot-admin/target/iboot-admin-1.0.0.jar \
  --flowable.async-executor-activate=false --flowable.idm.enabled=false

# 全量回归
TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 \
  mvn -s /home/lam/repo/settings.xml clean verify
```

## 11. 证据位置

- 测试：`iboot-admin/src/test/java/com/iboot/admin/infrastructure/workflow/FlowableWorkflowSpikeTest.java`
- 流程定义：`iboot-admin/src/main/resources/processes/leave.bpmn20.xml`
- 依赖声明：`pom.xml`、`iboot-admin/pom.xml`
- 启动日志：`/tmp/iboot-flowable.log`（冷启动）、`/tmp/iboot-flowable2.log`（热启动）
