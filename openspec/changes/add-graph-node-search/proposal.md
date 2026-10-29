# Proposal

## Why

知识图谱（1.4）的查询模板只接受主键与权限码（如 `deptId=2`、`permission=user:list`），模型无法把「研发部」「用户管理」这类名称解析成主键。用户用自然语言提问时，模型只能反复猜测参数，最终耗尽步数预算以 `step_limit` 结束——图里有名称数据，却没有任何入口能查它。这是 ROADMAP.md 第 256 行记录的已知限制，也是"智能体能用多跳关系回答问题"这一验收目标在真实问法下的最后一道坎。

## What Changes

- `query_org_graph` 工具新增 `search_nodes` 模板：按名称把部门、用户、菜单解析为查询所需的标识，**不新增智能体工具**，因此已配置的工具白名单无需改动。
- 搜索参数为 `keyword`（必填）与 `nodeType`（`dept` / `user` / `menu`，可选，缺省三类都查）；返回 `{type, id, name, label, parentName, permission}`，沿用现有 `limit + 1` 的截断约定。
- 菜单只能经「可见用户 → 角色 → 菜单」路径暴露，返回其权限码；这与现有规格"角色与菜单信息只作为可见用户属性返回"保持一致，不扩大可见面。
- 空结果返回空行集而非错误，重名返回候选——两者都要求工具描述与规格引导模型**向用户确认名称**，而不是继续换参重试。
- 查询仍走固定 Cypher 模板与查询前数据权限过滤；不加 Neo4j full-text 索引（中文 `standard` analyzer 按单字切分会产生误命中），当前规模用 label 扫描 + `CONTAINS` 足够。
- 同步逻辑、`GraphController`、权限模型、失败关闭行为均不变。
- ROADMAP 新增阶段 1.5 条目（带验收判据），并把 1.4 的已知限制改写为"已由 1.5 解决"。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `knowledge-graph`: 新增"按名称解析图节点"的能力与约束——解析只能通过固定模板、结果受同一数据权限过滤、菜单只经可见用户暴露、空结果不得报错、重名返回候选、结果有规模上限。

## Impact

- 代码：`GraphQueryTemplate`（新增枚举项）、`Neo4jGraphRepository`（三条只读 Cypher）、`GraphQueryService`（参数校验与度量）、`QueryOrgGraphTool`（参数 Schema、模板分发、工具描述）。
- 测试：`GraphQueryRepositoryTest` / `Neo4jGraphRepositoryTest`（Testcontainers）、`GraphQueryServiceTest`、`QueryOrgGraphToolTest`；`AgentEvaluationReplayTest` 与 `AgentEvaluationCloudTest` 注册图工具，`eval-cases.json` 增加图能力用例。
- 文档：`ROADMAP.md`、`doc/22-AI 工作台操作指南.md` §13.2 / §13.4 / §13.5、`doc/spike/graph-1.4-report.md` §7。
- 契约：`query_org_graph` 的 JSON Schema 新增两个可选参数，属**向前兼容**的增量；新增模板值 `search_nodes` 不改变既有模板语义。不新增 REST 端点，前端无影响。
- 依赖与配置：无新增中间件、无新增配置项；复用 `iboot.ai.graph.query.max-rows`。
