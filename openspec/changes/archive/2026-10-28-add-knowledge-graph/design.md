# Design

## Context

`iboot-ai` 是独立可部署模块（8081），内部按 `interfaces → application → domain → infrastructure` 分层；持久化用 `JdbcTemplate` 而非 ORM，检索用 Elasticsearch 客户端直连。1.3 已建立受控工具注册表 `AgentToolRegistry`、工具级权限继承与 `PermissionScopeClient`（带调用者 JWT 访问 admin 的 `/api/auth/scope`）。

与本变更相关的既有约束：

- 组织与 RBAC 数据只存在于 admin 的 `sys_*` 表。admin 已提供 `GET /api/dept/list`、`GET /api/user/list`（分页，含 `deptId` 与 `roleIds`）、`GET /api/role/{id}`（含 `menuIds` 与 `deptIds`）、`GET /api/menu/list`，均带 `@PreAuthorize`。
- `iboot-ai` 不持有 admin 数据源，且阶段 2 要求它保持零迁移成本（§3 唯一硬约束）。
- 智能体运行是响应式链路（`runService.stream(...).subscribe(...)`），工具执行可能不在请求线程上，`RequestContextHolder` 不可依赖。
- 本机内存是真实瓶颈（R4），已有 18 个中间件容器。
- Boot 4.0.3 托管 `neo4j-java-driver 6.0.2`；官方文档声明 6.x 驱动兼容 4.4 / 5.x / 2025.x 服务端。

## Goals / Non-Goals

**Goals:**

- 在不改 admin 业务代码、不引入服务账号的前提下，把四类组织实体的关系同步成图。
- 用固定模板覆盖 2 跳及以上问题，查询前完成数据权限过滤并失败关闭。
- 图能力默认关闭，对既有对话 / RAG / 智能体行为零影响。
- 集成测试可在本机 Docker 上确定性运行，不依赖云模型。

**Non-Goals:**

- 文档实体抽取、社区摘要等完整 GraphRAG 管线。
- 图可视化、前端任何改动。
- 定时同步与服务身份（留到阶段 2）。
- 自由 Cypher 生成、图算法（PageRank / 社区发现）。

## Decisions

### 1. Neo4j 5.26.29-community，compose 中用 `graph` profile 隔离

5.26 是 5.x 的 LTS 线，社区版满足单机单库需求；2025.x / 2026.x 日历版本线支持窗口更短且无本阶段需要的特性。镜像 `neo4j:5.26.29-community` 已从镜像站拉取到本地。

`docker/docker-compose.yml` 新增 `neo4j` 服务并设置 `profiles: ["graph"]`，因此 `docker compose up -d` 的行为与今天完全一致；需要图能力时用 `docker compose --profile graph up -d`。内存显式限制：`server.memory.heap.max_size=512m`、`server.memory.pagecache.size=256m`，认证走 `NEO4J_AUTH`。

替代方案：直接把 Neo4j 放进默认启动集——会无条件增加约 800MB–1GB 常驻内存，违反 R4，否决。

### 2. 使用官方驱动，不引入 Spring Data Neo4j

所有查询都是模板化的 Cypher，参数绑定即可；图模型只有 4 类节点，不需要 OGM。用 Boot 托管的 `org.neo4j.driver:neo4j-java-driver` 加一个薄仓储 `Neo4jGraphRepository`，与 `iboot-ai` 现有的 `JdbcTemplate` 风格一致，依赖面最小。

替代方案：`spring-boot-starter-data-neo4j`——带来 `@Node` 实体、Repository 代理与自动配置，收益是映射便利，代价是为一组固定查询引入一层与既有风格不一致的抽象。否决。

### 3. 图模型：四类节点、五类关系、唯一约束 + 同步水位

```
(:Dept {id, name, code, parentId, status, syncId})
(:User {id, username, nickname, deptId, status, syncId})
(:Role {id, code, name, dataScope, status, syncId})
(:Menu {id, name, type, permission, parentId, status, syncId})

(:Dept)-[:PARENT_OF]->(:Dept)
(:User)-[:BELONGS_TO]->(:Dept)
(:User)-[:HAS_ROLE]->(:Role)
(:Role)-[:GRANTS]->(:Menu)
(:Role)-[:SCOPE_DEPT]->(:Dept)
```

- 每类节点对 `id` 建唯一约束（`CREATE CONSTRAINT ... IF NOT EXISTS`），启动时幂等执行。
- 每次同步生成一个 `syncId` 并盖到本次写入的每个节点上；事务末尾删除 `syncId <> $syncId` 的同标签节点，从而一次性完成新增、更新与清理。
- 不用 APOC：最短路径用内置 `shortestPath()`。

替代方案：按时间戳逐节点比对删除——需要额外遍历与二次查询；`syncId` 方案在一次事务内完成，且天然原子。

### 4. 同步：调用者 JWT + 一次性快照 + 单事务写入

新增 `POST /api/ai/graph/sync`（要求 `ai:graph:use`，且调用者需具备 admin 的 `dept:list` / `user:list` / `role:list` / `menu:list`）。流程：

```
caller JWT
   |
   v
GraphSyncService
   |-- GET /api/dept/list          (flat, parentId)
   |-- GET /api/menu/list          (flat, parentId, permission)
   |-- GET /api/role/list          (paged) + GET /api/role/{id}  (menuIds, deptIds)
   |-- GET /api/user/list          (paged until total, deptId, roleIds)
   |
   v  内存中组装 GraphSnapshot（实体 + 关系 + syncId）
   |
   v  session.executeWrite(tx -> MERGE ... ; DELETE stale)
   |
   v  GraphSyncSummary {nodes by type, relations, removed, syncedAt}
```

要点：

- **原子性**：先取全量快照，再单事务写入；取数或写入失败时旧图不变。
- **单飞**：同步加互斥锁，并发请求直接返回“同步进行中”，避免重复写。
- **角色 N+1**：`/api/role/list` 不含 `menuIds`/`deptIds`，按角色逐个取详情。角色量级小（个位数），可接受；若将来角色超过约 50 个，再考虑在 admin 增加快照接口（已列为后续选项）。
- **分页**：用户按 `page-size` 翻页直至达到 total，设置最大页数保护。

替代方案：服务账号定时同步——自动刷新但引入固定凭据与账号初始化；admin 快照落 MinIO——边界最干净但链路更长；AI 直连 admin 库——破坏阶段 2 零迁移。均按提案结论否决，定时同步留到阶段 2。

### 5. 数据权限过滤：模板内参数化，失败关闭

`PermissionScopeClient` 新增按令牌取范围的重载（复用现有 60 秒 Caffeine 缓存），使不在请求线程上的工具也能取得范围；原方法委托给新重载，行为不变。

查询把范围作为参数传入，而不是在 Java 里事后过滤：

```cypher
... WHERE $unrestricted OR d.id IN $visibleDeptIds
```

- 超管传 `unrestricted=true`，其余传 `visibleDeptIds`。
- 范围获取失败 → 抛 `GraphScopeUnavailableException`，由 `AiExceptionHandler` 映射为可辨识错误；**不降级为全量查询**。
- 查询他人角色/菜单时额外要求 `user:list`；查询本人不要求。
- 被过滤实体不进入结果，也不通过计数或错误文案泄漏。

### 6. 查询模板与工具

| 工具 | 模板 | 必需权限 | 数据范围 |
|---|---|---|---|
| `query_org_graph` | `colleagues`、`dept_members`、`user_permissions`、`perm_holders` | `ai:graph:use`；查他人权限另需 `user:list` | `visibleDeptIds` |
| `query_dept_path` | `dept_path` | `ai:graph:use` | 路径上全部部门可见 |

- 模板名为枚举参数，非法值直接拒绝，不拼接 Cypher。
- 身份字段（`userId`）只作为**查询目标**；调用者身份始终取自 `AgentContext`，模型传入的身份字段被忽略。
- 结果统一序列化为 JSON 后交给既有 `ToolResultTrimmer`，并额外遵守 `max-rows`。
- 两个工具都 `readOnly=true`，不进入两段式确认流程。

`dept_path` 使用 `shortestPath`，并对路径上每个节点做可见性检查；不可见时返回空结果。

### 7. 开关、Bean 装配与端点行为

- `iboot.ai.graph.enabled` 默认 `false`。
- `Driver`、`Neo4jGraphRepository`、`GraphSyncService`、`GraphQueryService`、两个工具均加 `@ConditionalOnProperty`；关闭时不建立连接、工具不出现在注册表中。
- `GraphController` 始终注册，读取 `GraphProperties.enabled`：关闭时 `sync` / `status` 返回 503 + `graph_disabled`，而不是 404，便于运维区分“未启用”与“路径写错”。
- 配置项：`enabled`、`uri`、`username`、`password`、`database`、`sync.page-size`、`sync.max-pages`、`query.max-rows`、`query.timeout`。

### 8. 可观测与错误映射

- 复用 `AiMetrics.agentToolCall(tool, outcome, duration)` 记录工具调用。
- 新增 `graphSync(outcome, duration, nodeCount)` 与 `graphQuery(template, outcome, duration)`。
- 新增异常：`GraphDisabledException`(503)、`GraphUnavailableException`(503)、`GraphAccessDeniedException`(403)、`GraphSyncException`(502)，统一在 `AiExceptionHandler` 映射为 `{code, message, retryable}`。

### 9. 测试策略

- 单元：模板参数校验、scope→参数映射、失败关闭、同步快照组装（假 admin 客户端）、`syncId` 清理逻辑。
- 集成：Testcontainers `neo4j:5.26.29-community`（GenericContainer + 日志等待），覆盖 5 个模板、层级部门、重复同步幂等、删除节点清理、约束幂等。
- 上下文：关闭态启动测试（无连接、无工具）；开启态连接测试。
- 红队：可见范围外的部门查询返回空；scope 服务不可用 → 拒绝。
- Agent：用 `FakeOpenAiServer` 驱动模型选择图工具，验证只读、无确认、审计与指标。
- 容器测试沿用 `TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0`。

## Risks / Trade-offs

- **[内存增加约 800MB–1GB]** → 默认关闭 + `graph` profile + 显式堆与 pagecache 上限；不启用时零开销。
- **[手动同步导致图数据过期]** → `GET /api/ai/graph/status` 暴露上次同步时间；文档写明刷新方式；定时同步列为阶段 2 增量。
- **[同步耗时随组织规模增长，占用一个请求]** → 单飞锁 + 最大页数保护 + 驱动超时；当前组织规模小，单事务可接受。
- **[角色详情 N+1 调用]** → 角色数量小；超过约 50 个时改为 admin 快照接口。
- **[驱动 6.0.2 与 5.26 服务端组合]** → 官方兼容矩阵已确认；镜像版本固定，不做浮动 tag。
- **[`PermissionScopeClient` 缓存导致权限变更延迟 ≤60 秒]** → 与 1.2 RAG 现有行为一致，不额外扩大窗口。
- **[社区版单库、无集群]** → 本阶段单机练习足够；阶段 3 如需高可用改用托管图数据库。
- **[模型可能滥用模板参数探测数据]** → 参数只能是主键与枚举，结果始终经过范围过滤；被拒查询计入指标。

## Migration Plan

1. 上线 admin Flyway `V27`（新增 `ai:graph:use` 菜单并授权超级管理员）——纯增量，可先于 AI 服务发布。
2. 发布 `iboot-ai`，`iboot.ai.graph.enabled=false`，行为与现状一致。
3. 需要启用时：`docker compose --profile graph up -d neo4j` → 配置连接信息并置 `enabled=true` → 用超管账号调用 `POST /api/ai/graph/sync` → `GET /api/ai/graph/status` 确认水位 → 把两个工具加入目标智能体的工具白名单。
4. 回滚：置 `enabled=false` 即恢复原状态；Neo4j 数据卷独立，不影响业务库与知识库。

## Open Questions

（无。工具白名单的授予属于运维步骤，已写入迁移步骤与文档任务，不改变规格或任务拆分。）
