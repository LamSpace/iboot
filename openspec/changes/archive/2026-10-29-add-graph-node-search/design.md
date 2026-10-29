# Design

## Context

- 图查询链路：`GraphQueryTemplate`（5 个固定模板）→ `GraphQueryService`（参数校验、`limit + 1` 截断、按模板打点）→ `Neo4jGraphRepository`（固定 Cypher，所有语句都带 `$unrestricted` / `$visibleDeptIds` 两个范围参数）→ `QueryOrgGraphTool` / `QueryDeptPathTool`。
- 同步时已把 `Dept.name/code`、`User.username/nickname`、`Role.name/code`、`Menu.name/permission` 写入图，名称数据现成，只缺查询入口。
- 工具 Schema 目前只有整数主键与权限码；智能体默认 `max-steps=6`（`IBOOT_AI_AGENT_MAX_STEPS`），模型反复猜参就会撞上限。
- 图能力由 `iboot.ai.graph.enabled` 控制，默认关闭；关闭时工具不注册、不连库。
- 图工具当前未接入 `AgentEvaluationReplayTest` / `AgentEvaluationCloudTest`（两者只注册 4 个 admin 工具）。

## Goals / Non-Goals

**Goals:**

- 让模型能用名称完成现有 5 个模板所需的主键/权限码解析，端到端消除 `step_limit` 失败模式。
- 解析结果受与查询完全一致的数据权限过滤，菜单不因解析而扩大可见面。
- 不新增工具、不新增配置、不新增 REST 端点，对既有模板行为零影响。

**Non-Goals:**

- 不支持自由 Cypher、拼音/同义词/模糊纠错，也不做自然语言改写。
- 不解析角色（当前没有以角色为参数的模板，没有消费者）。
- 不做图节点的全文索引，不做定时同步（仍由调用者手动触发）。
- 不改门户视图（1.4 起本能力没有独立视图）。

## Decisions

### D1：做成 `query_org_graph` 的新模板，而不是新工具

- 备选 A：新增独立工具 `search_graph_nodes`。缺点是必须改每个智能体的 `toolAllowlist`，且多一个工具面要维护。
- 备选 B：给每个模板内联名称参数（如 `dept_members(deptName)`）。缺点是 5 个模板 × 两套参数，歧义协议要侵入每个模板，返回行类型也不再统一。
- 选择：模板方案。工具白名单零改动，`template` 枚举本来就是工具的对外契约，指标也会自动多出 `template="search_nodes"` 维度。

### D2：`nodeType` 可选、缺省三类都查

模型不一定能判断「用户管理」是菜单还是用户；要求先选类型会把一次解析变成多次试错。缺省时按 部门 → 用户 → 菜单 顺序各取 `limit + 1` 行，Java 侧合并后再统一截到 `max-rows`，因此截断语义与既有模板一致。

### D3：三类各一条 Cypher，不做 UNION

- 部门：`Dept` 按 `name` 包含匹配，`OPTIONAL MATCH` 取上级部门名，排序 `完全匹配优先, name`。
- 用户：`(User)-[:BELONGS_TO]->(Dept)`，部门过可见范围，匹配 `username` 或 `nickname`，返回 `deptId/deptName`。
- 菜单：从 `Menu` 出发，`permission IS NOT NULL`，条件是"超管 或 存在可见用户经角色持有该菜单"：

```cypher
MATCH (m:Menu)
WHERE m.permission IS NOT NULL
  AND (toLower(m.name) CONTAINS toLower($keyword)
       OR toLower(m.permission) CONTAINS toLower($keyword))
  AND ($unrestricted OR EXISTS {
        MATCH (u:User)-[:BELONGS_TO]->(dept:Dept)
        WHERE dept.id IN $visibleDeptIds
          AND (u)-[:HAS_ROLE]->(:Role)-[:GRANTS]->(m) })
RETURN DISTINCT m.name AS name, m.permission AS permission
LIMIT $limit
```

UNION 需要三支列对齐且会掩盖每类各自的排序/截断，分开写更好读也更好测。

### D4：菜单只经可见用户暴露

`knowledge-graph` 现有规格规定"角色与菜单信息 MUST 只作为可见用户的属性返回，不得单独返回可见范围之外的实体"。菜单本身没有部门归属，因此用"可见用户是否持有"作为可见性判据；这既满足规格，又正好回答了「谁能访问用户管理」的意图（解析出权限码后交给 `perm_holders`）。副作用：可见范围内没有任何用户的调用者解析不到任何菜单——这是失败关闭的正确表现，写入文档。

### D5：名称匹配用 `CONTAINS` 扫描，不建全文索引

当前组织规模在千级以内，label 扫描成本可忽略。Neo4j 默认 `standard` analyzer 对中文按单字切分，`db.index.fulltext` 不加 `cjk` analyzer 会带来大量误命中；加 `cjk` 又要额外的索引生命周期管理。留到节点数上万时再评估，届时只需替换仓储内的取数方式，模板契约不变。

### D6：空结果不报错，重名不替用户做决定

- 0 命中返回 `rows: []`。当前失败模式的本质是"模型不知道就继续猜"，所以必须在**工具描述**里写死顺序：名称未知 → 先 `search_nodes`；返回空 → 向用户确认名称，不要换词重试。
- 多命中返回全部候选（带 `parentName`），由模型列出让用户选。仓储不提供"取第一个"的隐式行为。

### D7：结果行结构

新增一个只读领域记录（`{type, id, name, label, parentName, permission}`），字段按类型填充：

| type | id | name | label | parentName | permission |
|---|---|---|---|---|---|
| dept | 部门主键 | 部门名称 | 部门编码 | 上级部门名称 | 空 |
| user | 用户主键 | 昵称（空则用户名） | 用户名 | 所属部门名称 | 空 |
| menu | 菜单主键 | 菜单名称 | 菜单类型 | 空 | 权限码 |

`label` 用来兜住"昵称重名但用户名唯一"和"菜单同名"这两种情况。

### D8：错误与权限行为复用现有通道

关键词缺失/空白 → `IllegalArgumentException` → 工具返回 FAILED（与既有模板参数校验一致）；无 `ai:graph:use` → DENIED 且零查询；范围服务不可用 → 仍抛 `GraphScopeUnavailableException`（失败关闭），不因解析是"只读小查询"而放宽。

### D9：测试接入评测基建

- 单测/集成：仓储用 Testcontainers 真 Neo4j 验证匹配、重名、可见性、菜单间接暴露、截断；服务与工具用 mock 验证参数校验与分发。
- 回放与真实模型评测：在两个评测测试里补注册 `QueryOrgGraphTool` + mock 的 `GraphQueryService` / `GraphScopeResolver`，`eval-cases.json` 增加 g1–g4（名称→解析→查询、空结果→请确认、重名→列候选、越权→空结果）。回放验证**运行时**行为，云端验证**模型是否真的先解析**。
- 云评测新增用例会改变准确率分母，首次运行后按实测结果复核 `agent-eval-cloud-baseline.json`，只在有实测数据支撑时调整阈值，不为了过测试而降门槛。

## Risks / Trade-offs

- [模型仍可能猜主键，不调用解析] → 工具描述写死"名称未知先解析"，并用 g1 用例在回放与云端双重覆盖；若云评测仍不达标，再考虑在工具返回中给空结果附加提示。
- [同名部门选错] → 返回 `parentName`，规格要求列候选请用户选择；验收时用临时重名部门实测。
- [菜单可见性判据被误解为"菜单全量可见"] → 规格明确"只能经可见用户路径暴露"，仓储测试覆盖"范围内无用户持有则该菜单不可解析"。
- [名称搜索成为扫描瓶颈] → 以 `iboot.ai.graph.query.duration{template="search_nodes"}` 观测；节点数上万时再引索引（D5）。
- [图内名称与 admin 源数据不同步] → 不新增风险，沿用全量替换同步；文档提示先同步后提问。
- [云评测基线漂移] → 见 D9，阈值调整必须有实测依据。

## Migration Plan

纯增量：无表结构变更、无数据迁移、无配置项变更。回滚只需回退分支；图库中已同步的数据无需处理。发布顺序无要求，但建议先同步一次图再验收（名称数据来自同步快照）。
