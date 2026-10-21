# Design

## Context

本设计的约束来自当前工程与已验证的外部事实，动机见 `proposal.md`。

**工程现状**

- Spring Boot 4.0.3、JDK 25.0.3、Maven 3.9.16；Jackson 3（`tools.jackson.*`）已在使用。
- 持久层为 Druid + 自定义 `SqlSessionFactory`（`infrastructure/config/MyBatisConfig.java`），并挂载 `DataScopeInterceptor`；`@MapperScan` 仅覆盖 `com.iboot.admin.infrastructure.persistence.mapper`。
- Quartz 已在用（`ScheduleJobInit`），Tomcat 与异步执行器启用虚拟线程。
- 测试风格为纯 JUnit 5 + Testcontainers，当前没有任何 Spring 上下文测试；容器测试需 `TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0`。
- 本机已有 `mysql:8.0.45` 镜像；Maven 走阿里云镜像，已确认提供 `org.flowable:*:8.0.0`。

**外部事实**

- Flowable 7 不支持 Boot 4；Flowable 8.0.0（2026-02-27）基于 Spring Framework 7 / Boot 4，默认 Jackson 3，并在自身 POM 中显式规避了 Boot 4 自带 Artemis 与 Java 25 的不兼容。
- Spring Statemachine 已移入 `spring-attic` 归档，无 Boot 4 官方基线。

## Goals / Non-Goals

**Goals:**

- 在 2 天时间盒内给出 Flowable 8.0.0 的 **Go / No-Go** 结论，并附可复现的证据与实测数字。
- 验证四条硬判据：应用上下文内引擎启动、BPMN 部署、请假流程双路径单测、CI 可自动运行。
- 观测与现有栈的真实共存成本：依赖冲突、连接池、数据权限拦截器、事务、启动耗时、内存、表数量。
- 修正 ROADMAP 中已失效的版本前提。

**Non-Goals:**

- 不实现 0.5 的审批流业务（待办页面、表单、审批人规则、前端）。
- 不把工作流拆成独立服务，也不产出 `iboot-workflow` 的代码。
- 不决定生产环境的 DDL 归属、异步执行器与 Quartz 的最终分工——只记录观测量。
- 不实现 fallback（自研状态机或暂缓）。

## Decisions

### D1：引擎与依赖范围

采用 `org.flowable:flowable-spring-boot-starter-process:8.0.0`，不使用 umbrella `flowable-spring-boot-starter`。

- **理由**：只需要 BPMN 流程引擎；umbrella starter 会连带引入 App / CMMN / DMN 三个引擎，放大依赖面与共存风险。
- **备选**：Flowable 7（Boot 4 不可用）；Camunda 8（许可与架构模型均超出路线图）；Spring Statemachine（归档）；纯自研（失去引擎能力与学习目标）。

### D2：Spike 的物理隔离

在 `spike/flowable-workflow` 分支上改动，产物一次性、不合并。

- **理由**：真正的风险是"与现有栈共存"，仓库外的独立脚手架证明不了这一点；但验证完成后代码应整体丢弃，避免半成品进入主干。
- **备选**：仓库外临时项目（快，但只验证裸引擎，无法回答共存问题）。

### D3：测试策略

分两级，各自回答不同问题：

```
Level A  最小 Spring 上下文 + Testcontainers MySQL
         ApplicationContextRunner 或精简 @SpringBootTest
         回答：H1 引擎启动 / H2 BPMN 部署 / H3 双路径流程 / H4 CI 可跑

Level B  真实 iboot-admin 应用 + compose 中间件
         回答：与 Druid、自定义 SqlSessionFactory、Security、Quartz 的真实共存
```

- **理由**：Level A 可进 CI 且启动快；Level B 才能暴露自动配置冲突，但它依赖完整中间件，不适合每次构建都跑。
- **备选**：只做 Level B（慢且脆弱）；只做 Level A（漏掉真实共存风险）。

### D4：决策规则

**硬门槛（一票否决）**：H1–H4 全部通过；Apache 2.0；不把 Flowable 类型泄漏到 `domain` 以上。

**加权评分（仅用于通过硬门槛后的方案比较）**：

| 维度 | 权重 |
|---|---|
| 集成成本（需排除/覆盖的依赖与自动配置数） | 25% |
| 生产就绪度（历史、并行网关、会签、委托、补偿） | 25% |
| 交付速度（到跑通请假流程的工时） | 20% |
| 运维足迹（内存、启动时间、ACT_* 表数、常驻线程） | 15% |
| 演进路径（阶段 2 抽成 `iboot-workflow` 的成本） | 10% |
| 学习价值 | 5% |

**止损规则**：

- 总时间盒 2 天，硬上限 3 天；过不了 Level A 的 H3 即停止投入。
- 若为跑通需要降级 Boot 或 JDK → 直接判不通过。
- 若需要修改现有 `MyBatisConfig` / `DataScopeInterceptor` 才能共存 → 记为高风险，倾向不通过。

### D5：目标边界形态（Spike 只验证，不实现）

```
interfaces/controller      ApprovalController
application/service        ApprovalApplicationService
domain/workflow            WorkflowService <接口> + 纯业务类型（零 Flowable 依赖）
infrastructure/workflow    FlowableWorkflowAdapter implements WorkflowService
```

判别标准：删除 `infrastructure/workflow` 包后，`domain` 与 `application` 仍能编译。Spike 不写这套代码，只把该形态作为采纳建议写入报告。

### D6：Spike 内的引擎配置

- `flowable.async-executor-activate=false`：避免与现有 Quartz / 虚拟线程模型混淆，把这个决策留给 0.5 实现。
- 关闭 Flowable IDM（若配置项可用）：不引入第二套用户/组模型，assignee 直接使用 `sys_user` 的 id 字符串。
- `database-schema-update=true`：Spike 允许引擎自建 `ACT_*` 表；生产 DDL 归属留作报告中的结论项。

### D7：与现有基础设施的冲突处理

- 从 Flowable starter 中排除 `HikariCP`，保持 Druid 为唯一连接池。
- 用 `mvn dependency:tree` 检查 Jackson 2 / 3 混用；Flowable 8 默认 Jackson 3，与项目一致，但仍需确认没有 Jackson 2 被重新拉入。
- 实测 `DataScopeInterceptor` 不会作用到 Flowable 自建的 `SqlSessionFactory` 上。

### D8：文档修正的范围

只改 `ROADMAP.md` 四处：0.5 行（技术栈与约束）、§6.1 版本矩阵、R7 风险。fallback 的表述先写成"由 Spike 结果决定，已排除 Spring Statemachine（归档）"，具体取舍见 Open Questions。

### D9：报告落点

`doc/spike/flowable-8-spike-report.md`。不占用现有编号文档序列，便于随变更归档后仍可追溯。

## Risks / Trade-offs

- **[Flowable 8 发布仅约 8 个月，生产成熟度未知]** → 报告记录版本节奏与已知 issue；决策时可附加"等待 8.0.x 补丁"的选项。
- **[`spring-boot-starter-jdbc` 把 HikariCP 带回 classpath]** → 显式 exclusion，并在 T0 门禁中用依赖树验证。
- **[Jackson 2 / 3 混用导致变量序列化异常]** → T0 依赖树检查 + 流程变量读写测试。
- **[数据权限拦截器误伤 Flowable 的 SQL]** → 用带 Mapper 注解的查询与流程推进混合场景验证；一旦误伤，按止损规则判高风险。
- **[Flowable 后台线程与 Quartz/虚拟线程争用]** → Spike 关闭 async executor；冲突结论留给报告。
- **[Flyway 与引擎自建表的所有权冲突]** → Spike 使用 auto-DDL，只记录表数量与脚本位置，不在本变更内解决。
- **[JDK 25 非官方基线]** → H1 作为硬门槛；失败即记录完整异常栈后停止。
- **[Spike 代码残留]** → 报告完成后删除分支；文档修正单独走 `docs/` 分支与 PR。

## Migration Plan

本变更不涉及生产部署。

1. 从 `master` 切出 `spike/flowable-workflow`，只加依赖、BPMN 与一次性测试。
2. 按 Level A → Level B 推进，产出 `doc/spike/flowable-8-spike-report.md`。
3. 依据报告做出 Go / No-Go 决策；无论结果如何，删除 Spike 分支。
4. ROADMAP 修正走独立 `docs/` 分支，经 PR 合并（遵循 CLAUDE.md 的分支流程）。
5. 回滚策略：无生产影响；丢弃分支即可。

## Open Questions

- **Flowable 8 不通过时的 fallback**：自研最小状态机，还是暂缓 0.5 先做 0.6？这只在 No-Go 时才有意义，不改变本变更的任务拆解，待报告出炉后决策。
- **Spike 流程样本是否需要业务化**：默认使用通用请假流程；是否改成真实场景（如用户开通申请）可在 Go 之后、进入 0.5 实现时再定。
- **是否跟随 8.0.x 补丁**：Spike 钉 8.0.0；补丁跟随策略在采纳时决定。
