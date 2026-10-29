# Tasks

## 1. 模板与领域模型

- [x] 1.1 新增只读领域记录 `GraphNodeMatch`（`type`、`id`、`name`、`label`、`parentName`、`permission`），按设计 D7 填充字段语义，补类级 Javadoc；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-ai -DskipTests compile` 通过
- [x] 1.2 `GraphQueryTemplate` 增加 `SEARCH_NODES("search_nodes")` 并确认 `fromWire` 可解析；验证：在 `GraphModelTest` 补充 `fromWire("search_nodes")` 断言并通过

## 2. 仓储层查询

- [x] 2.1 `Neo4jGraphRepository` 新增部门、用户、菜单三条只读 Cypher 与对应方法，全部复用 `$unrestricted` / `$visibleDeptIds` 范围参数；验证：编译通过且既有 5 个模板的 Cypher 未发生改动（`git diff` 只出现新增语句）
- [x] 2.2 在 `GraphQueryRepositoryTest` 补匹配用例：部门精确/包含、完全匹配优先排序、`parentName` 返回、用户按用户名与昵称命中、菜单名与权限码命中、无权限码菜单不返回；验证：`TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 mvn -s /home/lam/repo/settings.xml -pl iboot-ai -Dtest=GraphQueryRepositoryTest test` 通过
- [x] 2.3 在 `GraphQueryRepositoryTest` 补可见性与截断用例：范围外部门/用户返回空、可见范围内无用户持有菜单时菜单不可解析、超管返回全量、命中数超上限时标记截断；验证：同上命令通过

## 3. 应用服务

- [x] 3.1 `GraphQueryService` 新增 `search(keyword, nodeType, scope)`：关键词 trim 后为空抛 `IllegalArgumentException`；缺省类型时按 部门 → 用户 → 菜单 顺序各取 `limit + 1` 行合并后统一截断；沿用 `measured("search_nodes", ...)` 打点；补方法 Javadoc；验证：编译通过
- [x] 3.2 `GraphQueryServiceTest` 补用例：关键词缺失/空白被拒绝、缺省类型合并三类结果、超限截断标记、指标标签为 `search_nodes`；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-ai -Dtest=GraphQueryServiceTest test` 通过

## 4. 智能体工具

- [x] 4.1 `QueryOrgGraphTool` 的 `parameterSchema` 增加 `keyword`、`nodeType`（enum `dept|user|menu`），`execute` 分发到新模板；更新工具 `description`，写明"参数是主键/权限码，名称未知时先调用 `search_nodes`，返回空则向用户确认名称而不是换词重试"；验证：编译通过
- [x] 4.2 `QueryOrgGraphToolTest` 补用例：解析模板分发、缺关键词返回 FAILED、非法 `nodeType` 返回 FAILED、无 `ai:graph:use` 时 DENIED 且零查询、截断标记透传；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-ai -Dtest=QueryOrgGraphToolTest test` 通过
- [x] 4.3 确认工具注册与白名单行为不变（仍只有 `query_org_graph` / `query_dept_path` 两个工具名）；验证：`GraphToolRegistrationTest` 通过

## 5. 智能体评测接入

- [x] 5.1 `iboot-ai/src/test/resources/agent/eval-cases.json` 增加 g1–g4：名称→解析→查询、空结果→请用户确认、重名→列候选、越权→空结果；验证：JSON 可被现有读取逻辑解析（回放测试启动即验证）
- [x] 5.2 `AgentEvaluationReplayTest` 注册 `QueryOrgGraphTool` 并注入 mock 的 `GraphQueryService` / `GraphScopeResolver`，断言 g1–g4 的工具调用序列与最终状态；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-ai -Dtest=AgentEvaluationReplayTest test` 通过率 1.0
- [x] 5.3 `AgentEvaluationCloudTest` 同法接入图工具，跑真实模型；验证：`set -a; source .env; set +a && mvn -s /home/lam/repo/settings.xml -pl iboot-ai test -Dtest=AgentEvaluationCloudTest -Dsurefire.excludedGroups=disabled` 选择准确率不低于 `minSelectionAccuracy`，并按实测结果复核 `agent-eval-cloud-baseline.json`（阈值只依实测调整）

## 6. 文档

- [x] 6.1 `ROADMAP.md` 新增阶段 1.5 条目（能力、约束、可独立演示的终态、验收判据），并把第 256 行已知限制改写为"已由 1.5 解决"；验证：表格列数与既有行一致，1.4 段落不再声称该限制未处理
- [x] 6.2 `doc/22-AI 工作台操作指南.md` 更新 §13.2 模板表（新增 `search_nodes` 行）、§13.4 工具参数（`keyword`、`nodeType`）、§13.5 常见问题（名称无匹配、重名候选）；验证：文档中的参数名与 `parameterSchema` 完全一致
- [x] 6.3 `doc/spike/graph-1.4-report.md` §7 标注名称解析限制的后续处理与结论；验证：报告与新实现不冲突

## 7. 集成验收

- [x] 7.1 全量回归：`TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 mvn -s /home/lam/repo/settings.xml -pl iboot-ai test` 全绿，既有图模板用例无改动仍通过
- [x] 7.2 运行环境准备（用户执行）：`docker compose --profile graph up -d neo4j`，`.env` 已含 `IBOOT_AI_GRAPH_ENABLED` / `IBOOT_AI_GRAPH_PASSWORD`；启用后用超管登录触发 `POST /api/ai/graph/sync`，确认各类节点数量正常
- [x] 7.3 真实模型验收（用户执行）：准备临时重名部门（验收后清理），在门户提问"研发部有哪些人""某人的同事""谁能访问用户管理"；判据：无 `step_limit`，重名时列候选，回答可追溯到解析到的部门/权限
- [x] 7.4 红队验收：用可见范围只含单个部门的账号提问范围外部门，验证返回空且不泄漏；确认清理临时验收数据后重跑一次同步，图内不残留测试节点
