# Proposal

## Why

1.1–1.3 交付了对话、RAG 与智能体编排，但「谁属于哪个部门、谁通过哪个角色拿到哪项权限、两个部门之间隔着几层」这类**多跳关系问题**仍只能靠人工翻页面拼答案。管理端已有 `sys_dept` / `sys_user` / `sys_role` / `sys_menu` 四张现成表，缺的是一层能把它们的关系显式表达出来的检索能力。

现在做的前提已经具备：Neo4j 5.26 是 5.x 的 LTS 线，Boot 4.0.3 托管的官方驱动 `neo4j-java-driver 6.0.2` 官方支持 4.4 / 5.x / 2025.x 服务端；镜像 `neo4j:5.26.29-community` 已通过镜像站拉取到本地并写入 `scripts/pull-docker-images.sh`；1.3 的 `AgentTool` 注册表、工具级权限继承与数据权限查询链路可直接复用。

## What Changes

- **引入 Neo4j 5.26.29-community**：`docker/docker-compose.yml` 新增 `neo4j` 服务并置于 `graph` profile，默认 `docker compose up -d` 不启动；堆 512MB + pagecache 256MB，受本机内存约束（R4）控制。
- **`iboot-ai` 使用官方驱动而非 Spring Data Neo4j**：新增 `neo4j-java-driver`（版本由 Boot 4.0.3 托管），查询全部走固定 Cypher 模板 + 参数绑定。
- **新增 `knowledge-graph` 能力**：`Dept` / `User` / `Role` / `Menu` 四类节点与 `PARENT_OF` / `BELONGS_TO` / `HAS_ROLE` / `GRANTS` / `SCOPE_DEPT` 五类关系入图；同步全部使用 `MERGE`，重复执行幂等，并清理已删除节点；唯一约束在启动时幂等创建。
- **同步由调用者触发**：新增 `POST /api/ai/graph/sync`，使用**发起请求者自己的 JWT** 调用 admin 现有的 `/api/dept/list`、`/api/user/list`、`/api/role/{id}`、`/api/menu/list`。不引入服务账号、不直连 admin 数据库、不改 admin 的同步逻辑；定时同步留到阶段 2 有了服务身份后再评估。
- **只读智能体工具**：新增 `query_org_graph`（同事、部门成员、用户权限、权限持有人）与 `query_dept_path`（跨部门最短路径）。模型只能选择模板与填写参数，**不能生成自由 Cypher**。
- **权限与失败关闭**：新增权限 `ai:graph:use`（admin Flyway `V27` 菜单与授权），工具执行前本地校验；返回结果按 `PermissionScope.visibleDeptIds` 过滤，非超管不得看到可见部门之外的用户与部门；权限范围服务不可用时按失败关闭处理，不返回未过滤结果。
- **默认关闭**：`iboot.ai.graph.enabled=false`。关闭时不建立 Neo4j 连接、不注册图工具，1.1–1.3 行为完全不变。新增 `GET /api/ai/graph/status` 供运维确认同步水位与节点数量。

明确不做（非目标）：

- **不做文档实体抽取入图**：从知识库语料抽实体、建社区摘要属于完整 GraphRAG，需要真实语料与额外 LLM 成本，且与 1.2 的向量检索职责重叠。1.4 只交付「图增强问答」。
- **不做图可视化视图**：ROADMAP §4.1 已明确 1.4 不新增独立视图。
- **不做定时同步**：等待阶段 2 的服务身份（Nacos / mTLS / OAuth client credentials）。
- **不允许模型生成 Cypher**：注入面过大，固定模板已覆盖验收场景。
- **不做多租户、属性图分析算法**（PageRank / 社区发现）等本阶段无消费者的能力。

## Capabilities

### New Capabilities
- `knowledge-graph`: 组织架构与 RBAC 关系入图、幂等同步、固定模板的多跳查询、查询前数据权限过滤与失败关闭、只读智能体工具接入。

### Modified Capabilities

（无。`ai-agent` 的「只调用已注册工具」「工具级权限继承」「写操作确认」等要求保持原样；新增图工具属于增量注册，不改变既有要求。）

## Impact

**代码**

- `iboot-ai`：新增 `domain/graph`、`application/service/graph`、`infrastructure/graph`（驱动封装、约束初始化、同步客户端）、`infrastructure/tool/graph`（2 个只读工具）、`interfaces/controller/GraphController`；`pom.xml` 新增 `neo4j-java-driver`；`application.yml` 新增 `iboot.ai.graph.*`。
- `iboot-admin`：Flyway `V27` 新增 `ai:graph:use` 菜单并授权超级管理员；不新增同步接口、不改业务代码。
- `iboot-portal`：无改动。

**依赖与运行时**

- 新增中间件 Neo4j 5.26.29（仅 `graph` profile 时启动），默认堆 512MB + pagecache 256MB。
- 新增依赖 `org.neo4j.driver:neo4j-java-driver`（Boot 4.0.3 托管 6.0.2）。
- 不新增数据库实例：图数据库与 `iboot_ai` schema 相互独立，不回写业务库。

**兼容性**

- 无破坏性变更：图能力默认关闭；关闭时应用启动、测试与既有接口行为不变。
- `iboot-ai` 仍保持独立可部署，阶段 2 只需为同步链路补网关路由与服务身份。

**测试与文档**

- 新增 Testcontainers Neo4j 集成测试与红队用例；沿用 `TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0`。
- 同步更新 `ROADMAP.md`、`doc/02`、`doc/08`、`doc/22`、`README.md` 与新增的 `doc/spike/graph-1.4-report.md`。
