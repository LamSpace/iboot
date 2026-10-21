# Proposal

## Why

阶段 0.5 审批流的技术前提已经失效：ROADMAP 假设的 Flowable 7 不支持 Spring Boot 4，而项目锁定在 Boot 4.0.3 / JDK 25；原定 fallback Spring Statemachine 也已移入 `spring-attic` 归档。Flowable 8.0.0 是唯一基于 Spring Framework 7 / Boot 4 的版本线（2026-02-27 发布），但它与本项目现有栈（Druid、自定义 `SqlSessionFactory` 与数据权限拦截器、Quartz、Jackson 3、虚拟线程）的共存性尚未经过任何验证。

在投入 0.5 的实际实现之前，需要一次判据明确、时间盒受限、带止损规则的 Spike 来回答"Flowable 8 能否安全落在本工程里"，同时修正 ROADMAP 中已经过时的版本表述，避免按旧前提继续规划。

## What Changes

- 将 0.5 的技术前提由 Flowable 7 修正为 **Flowable 8.0.0**，并同步更新 ROADMAP 的 0.5 行、§6.1 版本矩阵与 R7 风险登记。
- 新增一次有边界的 Flowable 8 Spike：
  - 在隔离子分支验证依赖解析与冲突（Jackson 2/3、连接池、MyBatis）。
  - 验证 `ProcessEngine` 在 Boot 应用上下文中启动、BPMN 从 classpath 部署成功。
  - 验证请假流程两条路径：发起 → 审批 → 归档、发起 → 驳回。
  - 验证测试可在 `mvn verify` 中自动运行（Testcontainers MySQL）。
  - 观测与现有栈的真实共存（整应用启动、事务回滚、现有测试回归、启动耗时/内存/表数量）。
- 产出 Spike 报告：兼容性结论、实测指标、风险清单与 Go / No-Go 建议。
- **不引入生产代码**：Spike 依赖、BPMN 与测试只存在于临时分支，结论落定后即丢弃，不合并进 `master`。

## Capabilities

### New Capabilities

无。本变更不改变任何可观测的系统行为：它是一次技术可行性验证加文档修正，因此 `.openspec.yaml` 标记 `skip_specs: true`。审批流本身的业务需求将由 0.5 实现阶段的独立变更引入。

### Modified Capabilities

无。现有 `content-search`、`file-storage`、`portal-design-system` 三个能力的需求均不受影响。

## Impact

- **文档**：`ROADMAP.md` 四处（0.5 技术栈与约束、§6.1 Flowable 版本、R7 风险处置）。
- **构建**：Spike 分支临时增加 `flowable-spring-boot-starter-process:8.0.0` 及其传递依赖，并需要评估/排除 HikariCP 与重复连接池；不进入主干。
- **测试**：新增一次性的 Spike 测试类与 BPMN 资源；现有 17 个测试类不受影响，回归结果计入报告。
- **运行时**：仅影响 Spike 分支的本地启动；生产部署无变化。
- **外部依赖**：依赖从现有阿里云 Maven 镜像获取；镜像已确认提供 8.0.0，无需新增仓库。
