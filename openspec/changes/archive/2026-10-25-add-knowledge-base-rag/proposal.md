# Proposal

## Why

ROADMAP 1.2 要求把 AI 工作台从"能对话"推进到"能基于自有知识回答"，并把权限一致性作为同批交付的安全底线。当前 `iboot-ai` 只有对话与 Redis 记忆，没有任何知识库、文档解析、向量索引或检索能力；直接接入 RAG 而不做检索前置权限过滤，等于把全量文档泄漏给最低权限用户，这是安全缺陷而不是功能缺口。

现在具备落地条件：1.1 的对话、流式、配额与观测已完成；0.3 的 ES 全文检索与可见性契约已完成；宿主机已有 MySQL 8.0.45、Redis 7.0.14、MinIO、ES 9.2.3；Spring AI 2.0.1 已配置 `text-embedding-v4`；`qwen3.5-ocr` 已用真实调用验证可用。本变更把这些存量能力组织成一条可演示、可验收、可回归的知识库 RAG 链路。

## What Changes

- **知识库与文档管理**：`iboot-ai` 新增知识库、文档、分块三张业务表（独立 schema `iboot_ai`），支持知识库 CRUD、PDF/DOCX/TXT/MD 上传、入库状态、重试、重建索引与删除。
- **对象存储**：新增 `iboot-ai` 专用 MinIO bucket 与最小存储适配器；原文件不进入 `sys_file`，入库链路不依赖 `iboot-admin` 可用性。
- **解析与分块**：PDF 用 PDFBox、DOCX 用 POI，按页/段落切分并保留页码与标题路径；扫描页走可插拔 OCR 解析器，默认关闭，启用时使用百炼 `qwen3.5-ocr`。
- **向量化与索引**：使用 Spring AI `EmbeddingModel`（`text-embedding-v4`，1024 维），写入独立 ES 索引 `iboot-kb-chunk-v1`（`dense_vector` + `cjk` 文本字段），与既有 `iboot-content-v1` 完全隔离。
- **混合检索**：BM25 与 kNN 两路检索都带同一套权限过滤，使用 ES 9 原生 RRF 融合，返回 Top-K 分块与页码/标题溯源信息。
- **RAG 问答**：新增独立的 `/api/ai/rag/**` 端点与流式事件（`meta → citations → token* → done`），回答只依据检索上下文；无命中时不调用模型，明确提示"未在知识库中找到相关内容"。
- **权限一致性**：`iboot-admin` 新增只读 `GET /api/auth/scope`，返回当前用户的五级 `data_scope`、部门与自定义部门集合；`iboot-ai` 在检索前同时施加 RBAC（`ai:kb:query` / `ai:kb:manage`）与数据权限过滤，权限服务不可用时拒绝检索而不是放开过滤。
- **菜单与前端**：admin 新增 `V25` 知识库菜单与权限；portal 新增知识库管理视图、KB 选择器、引用抽屉与中英文文案。
- **测试与评测**：单元、Testcontainers（MySQL/ES/Redis）、假模型端到端、红队越权用例、降级用例、50 篇合成语料 Recall@5 评测（真实 Embedding，`cloud` 分组）。
- **非目标**：智能体编排（1.3）、知识图谱（1.4）、扫描件 OCR 默认启用、查询改写/重排序、文档级独立 ACL、多租户、容器化与 Helm（阶段 3）。

## Capabilities

### New Capabilities

- `knowledge-base-rag`：知识库管理、文档上传与解析、分块与向量化、BM25+kNN 混合检索、引用溯源、RBAC 与五级数据权限一致性、OCR 可插拔解析、降级与成本观测、知识库前端视图。

### Modified Capabilities

无。1.1 的 `ai-chat` 契约保持不变：RAG 使用独立端点，现有 `/api/ai/chat` 与 `/api/ai/chat/stream` 的行为、事件序列与权限不变。`content-search` 的公告/消息检索契约与 `file-storage` 的存储提供方契约也不变；RAG 只复用其可见性与存储原则，不改变既有要求。

## Impact

- **构建**：`iboot-ai` 新增 JDBC/MyBatis/Flyway、Elasticsearch 客户端、MinIO、PDFBox、POI 依赖；根 `pom.xml` 的依赖管理保持集中。
- **运行时**：`iboot-ai` 新增 MySQL `iboot_ai` schema、ES 索引 `iboot-kb-chunk-v1`、MinIO `iboot-ai` bucket；启动前置从 Redis 扩展为 Redis + MySQL + ES。纯对话路径不查询新依赖，但 Flyway 与索引初始化会在启动时执行。
- **配置**：新增 `IBOOT_AI_OCR_*`、`DASHSCOPE_OCR_API_KEY`、MySQL/ES/MinIO 连接与知识库参数；OCR 默认关闭，密钥只经环境变量注入。
- **admin**：新增只读 scope 接口与 `V25` 菜单迁移；不改变现有数据权限 SQL 语义，抽取解析器时以特征测试保护。
- **前端**：`iboot-portal` 新增 `views/ai/KnowledgeView.vue`、`api/knowledge.ts`，扩展 `aiStream.ts` 的 `citations` 事件；现有对话视图在未选择知识库时行为不变。
- **可观测**：新增 RAG 请求量、检索耗时、Embedding token、OCR 页数、入库失败率指标与 Grafana/Alertmanager 规则。
- **兼容性**：无破坏性变更；未传知识库的对话、内容检索、文件管理行为均保持不变。
