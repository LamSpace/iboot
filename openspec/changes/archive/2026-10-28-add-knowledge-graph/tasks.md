# Tasks

## 1. 环境、依赖与权限基线

- [x] 1.1 在 `docker/docker-compose.yml` 新增 `neo4j` 服务：`neo4j:5.26.29-community`、`profiles: ["graph"]`、7687/7474 端口、`NEO4J_AUTH`、堆 512m + pagecache 256m、数据卷、healthcheck；验证 `docker compose config` 通过，且 `docker compose --profile graph up -d neo4j` 后容器通过健康检查
- [x] 1.2 在 `.env.example` 与 `.env` 补充 Neo4j 连接变量；验证变量名与 compose 及 `application.yml` 占位符一一对应
- [x] 1.3 在 `iboot-ai/pom.xml` 增加 `org.neo4j.driver:neo4j-java-driver`（不写版本，由 Boot 托管）；验证 `mvn -s /home/lam/repo/settings.xml -pl iboot-ai dependency:tree` 显示 6.0.2
- [x] 1.4 新增 `GraphProperties` 与 `application.yml` 的 `iboot.ai.graph.*`（`enabled=false`、uri、账号、page-size、max-pages、max-rows、timeout）；验证属性绑定单元测试通过且 `AiContextTest` 不回归
- [x] 1.5 新增 admin Flyway `V27`：创建 `ai:graph:use` 菜单并授权超级管理员；验证 Testcontainers MySQL 迁移测试在空库与已迁移库上均可执行且重复执行幂等
- [x] 1.6 更新 `doc/08-部署指南.md`（Neo4j 服务、profile、端口、内存）、`doc/02-技术栈.md`（Neo4j 5.26.29 与驱动 6.0.2）、`README.md`（中间件清单 18 → 19）；验证文档中给出的启动命令按原样可执行

## 2. 图模型与仓储

- [x] 2.1 在 `domain/graph` 定义节点、关系、同步快照、查询模板、查询结果与同步摘要模型；验证编译通过且模型单元测试覆盖空集合规范化
- [x] 2.2 实现图约束初始化（四类节点 `id` 唯一，`IF NOT EXISTS`）；验证 Testcontainers 测试：同一库重复初始化不报错且约束数量不变
- [x] 2.3 实现 `Neo4jGraphRepository` 的写入路径：按 `syncId` 批量 `MERGE` 节点与关系，事务末尾清理旧 `syncId` 节点；验证 Testcontainers 测试：节点/关系数量正确、同快照重复写入幂等、快照中移除的实体被清理
- [x] 2.4 编写 `doc/spike/graph-1.4-report.md`：镜像 digest、驱动与服务端兼容性依据、容器内存占用实测、约束与索引说明；验证报告中的命令可复现

## 3. 同步链路

- [x] 3.1 实现 admin 快照客户端：`/api/dept/list`、`/api/menu/list`、`/api/role/list` + 逐角色 `/api/role/{id}`、`/api/user/list` 翻页；验证假 HTTP 服务器测试覆盖解析、翻页终止与部分权限缺失的失败分支
- [x] 3.2 实现 `GraphSyncService`：拉取完整快照后再单事务写入，失败时旧图不变，并发请求被单飞锁拒绝；验证单元测试（假仓储）覆盖成功、下游失败、并发三种情况
- [x] 3.3 实现 `GraphController` 的 `POST /api/ai/graph/sync` 与 `GET /api/ai/graph/status`；验证 MockMvc 测试：开启时返回同步摘要与水位，关闭时返回 503 + `graph_disabled`
- [x] 3.4 在 `doc/22-AI 工作台操作指南.md` 新增 §13.1 同步与环境变量说明；验证按文档步骤在本机完成一次同步并通过状态查询看到节点数量

## 4. 查询模板与数据权限

- [x] 4.1 实现 5 个查询模板及参数校验；验证 Testcontainers 测试：同部门同事（2 跳）、部门成员含子部门、用户角色菜单、权限持有人（3 跳）、跨部门最短路径结果正确，参数非法时返回可辨识错误
- [x] 4.2 为 `PermissionScopeClient` 增加按显式令牌取范围的重载并复用现有缓存；验证 `PermissionScopeClientTest` 原用例不回归，新用例覆盖非请求线程调用
- [x] 4.3 在查询中注入 `unrestricted` / `visibleDeptIds` 参数并实现失败关闭；验证红队测试：可见范围外部门返回空且不泄漏存在性、scope 服务不可用返回错误、查询他人权限缺少 `user:list` 被拒
- [x] 4.4 在 `doc/22` §13.2–13.3 补查询模板、权限模型与越权排除说明；验证文档示例查询与测试夹具数据一致

## 5. 智能体工具接入

- [x] 5.1 实现 `QueryOrgGraphTool` 与 `QueryDeptPathTool`：只读、模板枚举、忽略模型传入的身份字段、结果按 `max-rows` 截断；验证单元测试覆盖身份字段忽略、非法模板、截断标记
- [x] 5.2 注册工具并加启用条件；验证 `AgentToolRegistryTest`：关闭时注册表中不含图工具，开启时含两个工具
- [x] 5.3 编写 Agent 端到端测试：用 `FakeOpenAiServer` 驱动模型选择图工具；验证事件流不出现 `pending_approval`、结果返回且 `agentToolCall` 指标被记录
- [x] 5.4 在 `doc/22` §13.4 补工具清单与“把工具加入智能体白名单”的操作步骤；验证按文档修改白名单后调用成功

## 6. 可观测与错误映射

- [x] 6.1 在 `AiMetrics` 增加 `graphSync` 与 `graphQuery` 指标；验证 `AiMetricsTest` 断言指标名与标签（模板/结果/耗时）
- [x] 6.2 新增 `GraphDisabledException`、`GraphUnavailableException`、`GraphAccessDeniedException`、`GraphSyncException` 并在 `AiExceptionHandler` 映射为 503/403/502；验证 `AiExceptionHandlerTest` 覆盖状态码与 `code`
- [x] 6.3 在 `doc/22` §13.5 补可观测指标与排障错误码表；验证文档错误码与 `AiExceptionHandler` 实际输出一致

## 7. 集成与回归

- [x] 7.1 运行 `TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 mvn -s /home/lam/repo/settings.xml clean verify`；验证 admin 与 ai 两模块全部测试通过并记录通过数量
- [x] 7.2 关闭态回归：`iboot.ai.graph.enabled=false` 启动 `iboot-ai`，冒烟对话、知识库问答与智能体查询；验证行为与改动前一致且不建立 Neo4j 连接
- [x] 7.3 开启态冒烟：`docker compose --profile graph up -d` → 配置启用 → 超管触发同步 → 状态查询 → 智能体提问一个 2 跳问题；验证回答与图中数据一致
- [x] 7.4 收尾核对：更新 `ROADMAP.md`（1.4 状态与日期、§1.2 能力表、§1.3 容器数、§6.1 版本矩阵、§6.2 R4）并执行 `openspec validate add-knowledge-graph --strict`；验证校验通过且文档中的数字与实际一致
