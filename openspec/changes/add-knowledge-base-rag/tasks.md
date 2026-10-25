# Tasks

## 1. 环境与前置

- [x] 1.1 确认 `DASHSCOPE_OCR_API_KEY` 已写入 `.env`，并确认 `qwen3.5-ocr` 可用；验证：一次真实 OCR 调用返回正确文本（已完成，单页 393 tokens）。
- [x] 1.2 创建 MySQL `iboot_ai` schema 与 MinIO `iboot-ai` bucket；验证：`SHOW DATABASES LIKE 'iboot_ai'` 有结果，`mc ls` 能看到 bucket，应用连接成功。
- [x] 1.3 启动 MySQL、Redis、MinIO、Elasticsearch 中间件；验证：四个容器健康，3306/6379/9000/9200 端口可达。
- [x] 1.4 准备 SELF / DEPT / CUSTOM 三个低权限测试用户，或授权插入测试数据；验证：登录后 `GET /api/auth/scope` 分别返回预期 `dataScope` 与 `visibleDeptIds`。
- [x] 1.5 补齐 `.env.example` 中 MySQL / ES / MinIO / RAG 配置说明（OCR 段已追加）；验证：按模板配置的新环境能启动 `iboot-ai` 并通过健康检查。

## 2. 权限底座

- [x] 2.1 从 `DataScopeInterceptor` 抽取 `UserDataScopeResolver`，拦截器改为委托；验证：五种 data_scope 的 SQL 片段特征测试与既有行为一致，`mvn -pl iboot-admin test -Dtest=DataScope*` 通过。
- [x] 2.2 新增 `GET /api/auth/scope`，只返回当前登录用户；验证：MockMvc 断言字段完整、未登录 401、传入 `userId` 参数不会查询他人。
- [x] 2.3 新增 admin Flyway `V25` 知识库菜单与 `ai:kb:query` / `ai:kb:manage` 权限；验证：Testcontainers MySQL 重复执行不报错，菜单与 role_id=1 关联存在。
- [x] 2.4 在 `iboot-ai` 启用方法级权限并加 `@PreAuthorize`；验证：缺少 `ai:kb:manage` 的写请求返回 403，缺少 `ai:kb:query` 的问答请求返回 403。
- [x] 2.5 实现 `PermissionScopeClient`：转发用户 JWT、Caffeine 缓存 60 秒、失败即 503；验证：stub admin 返回成功/500/超时三种情况下行为正确，失败时不发起检索。
- [x] 2.6 实现知识库可见范围解析与数据权限 ES 过滤条件构造；验证：五种 scope × 五种 KB 可见范围的单元真值表全部通过，空自定义部门返回 `match_none`。
- [x] 2.7 在 `doc/22-AI 工作台操作指南.md` 补充 scope 接口、V25 迁移与“迁移后需重新登录”说明；验证：按文档操作后新权限出现在会话中。

## 3. 数据模型、存储与配置

- [x] 3.1 为 `iboot-ai` 增加 JDBC/Flyway/ES/MinIO/PDFBox/POI 依赖；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-ai -am dependency:tree` 解析成功，`mvn -pl iboot-ai -am compile` 通过。
- [x] 3.2 增加 datasource、Flyway、ES、MinIO、RAG 与 OCR 配置，并只在 OCR 启用时校验 Key/模型；验证：配置绑定测试通过，OCR 关闭时缺少 Key 不影响启动。
- [x] 3.3 编写 `V1__create_knowledge_base.sql`（四张表、索引、唯一键）；验证：Testcontainers MySQL 8.0.45 从空 schema 执行成功，重复执行由 Flyway 管理。
- [x] 3.4 实现领域模型与 JDBC 仓储（知识库、文档、分块、部门授权）；验证：CRUD、内容哈希去重、删除后重传同内容、软删除语义的单测通过。
- [x] 3.5 实现 `KnowledgeStorage` 端口、MinIO 实现与内存假实现；验证：契约测试覆盖保存/读取/删除/不存在删除，MinIO 实现用 Testcontainers 或本地容器验证。
- [x] 3.6 实现 ES 索引初始化与 mapping（`iboot-kb-chunk-v1`）；验证：Testcontainers ES 9.2.3 断言索引存在、mapping 字段与维度正确、重复初始化幂等。

## 4. 解析、分块与 OCR

- [x] 4.1 实现 `DocumentParser` SPI 与 PDF/DOCX/TXT/MD 解析器；验证：测试 PDF 页码、DOCX 标题路径、空文档、损坏/加密 PDF，失败原因可辨识。
- [x] 4.2 实现分块器（600–800 字、100 字重叠、页/标题边界）；验证：中英文、超长段落、跨页内容、重叠去重的单测通过。
- [x] 4.3 实现 OCR SPI、`qwen3.5-ocr` 适配器与假适配器；验证：OCR 关闭不调用、启用后假适配器可入库、只对无文本页调用、配额耗尽不调用。
- [x] 4.4 为 OCR 增加隐私与指标（页数、token、耗时、失败原因），日志不输出图片或文本；验证：日志断言无正文，`SimpleMeterRegistry` 能读到 `iboot.ai.rag.ocr.pages`。

## 5. 入库管线

- [x] 5.1 实现文档状态机与内容哈希去重；验证：PENDING→PROCESSING→READY/FAILED、失败重试、重复内容不产生第二套分块的单测通过。
- [x] 5.2 实现定时 worker：`FOR UPDATE SKIP LOCKED` 认领、15 分钟 stale 恢复、1/5/15 分钟退避、最多 3 次；验证：MySQL Testcontainers 模拟重启与并发认领，任务不重复、不丢失。
- [x] 5.3 实现 Embedding 批量调用、维度校验与 token 指标；验证：假 Embedding 返回 1024 维时成功、返回 768 维时失败并记录维度错误，批次大小可配置。
- [x] 5.4 实现 ES 批量写入、按文档删除、重建幂等；验证：ES Testcontainers 中重复重建后同一分块只有一份，删除后检索不到。
- [x] 5.5 实现文档/知识库删除级联（DB、ES、MinIO）；验证：删除后三处均不可见，公告/消息检索不受影响。
- [x] 5.6 编写入库端到端集成测试（MySQL + ES + Redis + 假 MinIO + 假 Embedding）；验证：上传 PDF/DOCX 后状态 READY、分块写入、可检索。

## 6. 检索与 RAG 问答

- [x] 6.1 实现 BM25 + kNN + 原生 RRF 检索；验证：ES Testcontainers 中精确词、语义近似词、Top-K、每文档最多 2 块、无重复分块全部通过。
- [x] 6.2 把权限过滤同时挂到 BM25 与 kNN retriever；验证：构造越权文档在语义/关键字上都排第一，仍不出现在两路候选与 RRF 结果中。
- [x] 6.3 实现 RAG 查询服务：查询向量、BM25 降级、无命中不调用模型；验证：Embedding 失败时 `degraded=true` 且只走 BM25，无命中时模型调用次数为 0。
- [x] 6.4 实现提示词构造、引用列表与记忆隔离；验证：回答含 `[n]`、引用映射到实际分块、会话记忆不含检索上下文、编造编号不产生可点击来源。
- [x] 6.5 实现 `/api/ai/rag/chat` 与 `/stream`，事件顺序 `meta → citations → token* → done`；验证：集成测试断言事件顺序、引用内容、错误事件，且既有 `/api/ai/chat` 行为不变。
- [x] 6.6 实现知识库/文档 REST API（CRUD、上传、状态、重试、重建、下载、片段）；验证：MockMvc 覆盖权限、404、403、参数校验与跨用户隔离。
- [x] 6.7 增加 RAG 指标与 Embedding/OCR 日配额，更新 Prometheus/Grafana/Alertmanager 配置；验证：`promtool check config` / `promtool check rules` 通过，面板能查到指标。

## 7. 前端

- [x] 7.1 新增 `api/knowledge.ts` 与类型定义；验证：`cnpm run build` 通过，请求路径与后端一致。
- [x] 7.2 新增 `KnowledgeView.vue` 与 `/dashboard/knowledge` 路由；验证：列表、新建、上传、状态轮询、重试、删除在本地联调可用。
- [x] 7.3 在 `ChatView.vue` 增加知识库选择器与引用抽屉，`aiStream.ts` 增加 `citations` 事件；验证：选择知识库后走 RAG 端点，引用可点击，未选择时走 1.1 对话。
- [x] 7.4 补齐中英文文案；验证：切换语言无缺失 key，菜单与按钮文案正确。
- [x] 7.5 按权限控制菜单与写操作按钮；验证：无 `ai:kb:query` 不显示入口，无 `ai:kb:manage` 时新建/上传/删除不可用。

## 8. 安全、降级与评测

- [x] 8.1 编写红队矩阵：五种 data_scope × 五种 KB 可见范围 × 文档归属；验证：越权文档不出现在 BM25、kNN、RRF、引用、错误信息中，SELF/DEPT/CUSTOM 用例全部通过。
- [x] 8.2 编写降级用例：scope 服务 503、ES 503、Embedding 失败、对话模型失败；验证：分别返回 503/降级标记/既有错误，普通对话不受影响。
- [x] 8.3 生成 50 篇合成中文语料 + 50 问 + 标注分块；验证：假 Embedding 评测通过（只验证管线），`@Tag("cloud")` 真实 Embedding 评测 Recall@5 ≥ 0.80 并输出 `target/rag-recall-report.json`。
- [x] 8.4 验证索引隔离与既有检索回归；验证：删除/重建知识库后公告/消息检索结果不变，知识库检索不返回公告/消息。
- [x] 8.5 回归 1.1 对话与 admin 关键路径；验证：`ChatStreamingIntegrationTest`、配额、记忆、认证、文件、内容检索、数据权限测试全部通过。
- [x] 8.6 做一次 50 篇文档入库与检索延迟 smoke；验证：入库全部进入 READY/FAILED 可解释状态，检索 p95 记录在评测报告中。

## 9. 集成验收与交付

- [x] 9.1 运行根 `mvn -s /home/lam/repo/settings.xml clean verify`；验证：全部模块通过，`cloud` 分组默认排除。
- [x] 9.2 运行 portal `cnpm run build`、`cnpm run lint`、`cnpm run check:ui`；验证：三项均通过。
- [x] 9.3 手动端到端验收：上传文本 PDF/DOCX、扫描件 OCR、问答、点击引用、低权限用户越权尝试、重新登录；验证：验收清单逐项记录通过。
- [x] 9.4 更新 `doc/22-AI 工作台操作指南.md` 与 `ROADMAP.md` 1.2 状态；验证：文档中的命令、配置、验收步骤与实际一致。
- [ ] 9.5 从 `master` 切出 `feat/knowledge-base-rag`，提交并按 `CLAUDE.md` 流程创建 PR；验证：PR 通过 CI/本地检查，合并后 `master` 以 `pull --ff-only` 同步。
