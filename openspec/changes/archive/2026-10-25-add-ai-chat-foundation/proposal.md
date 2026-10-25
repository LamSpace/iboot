# Proposal

## Why

ROADMAP 1.1 要交付 AI 工作台的底座与智能对话，但当前工程里还没有任何 AI 服务：没有独立模块、没有鉴权、没有流式通道、没有成本控制。Spike 已经证明 Spring AI 2.0.1 + 百炼 `qwen3.8-flash` 在 Boot 4.0.3 / JDK 25 上可行（见 `doc/spike/ai-1.1-spike-report.md`），现在需要把验证结论落成一个可独立启动、可演示、可观测的服务，否则 1.2 的 RAG 与 1.3 的智能体都没有承载点。

## What Changes

- 新增 `iboot-ai` 独立 Maven 模块与独立进程（8081），包名 `com.iboot.ai`，内部沿用四层结构；**1.1 不做容器化**，Dockerfile 与 compose 留到阶段 3.1。
- 接入百炼 OpenAI 兼容端点：对话 `qwen3.8-flash`，默认 `enable_thinking=false`；Embedding `text-embedding-v4` 只落地抽象与配置，1.1 不消费。
- 鉴权复用现有登录态：校验 `jwt.secret` 签名并读取 Redis `login_tokens:<token>`，把 `userId / permissions / deptId` 放入安全上下文；不改造 `iboot-admin`。
- 智能对话：`ChatClient` + Redis 会话记忆；流式采用 **POST + `fetch`/`ReadableStream`**，新增 portal 侧 `aiStream.ts`，不改动现有 `utils/eventsource.ts` 的业务推送通道。
- 成本控制：调用前按全局日配额与单用户日配额做 Redis 原子计数，超限返回 429；默认全局 2000 万/天、单用户 20 万/天，均可配置。
- 可观测：Micrometer 记录请求量、token、首 token 延迟、失败率；新增 Prometheus job、Grafana 面板与 Alertmanager 规则。
- portal 新增 `/dashboard/ai` 对话视图与用量面板，通过 `iboot-admin` 的 Flyway 迁移插入菜单与权限。
- 测试：单元测试 + 假 OpenAI 兼容服务集成测试；真实百炼调用标记 `cloud` 分组，默认不跑。
- **非目标**：RAG、文件解析与入库、Agent 编排、知识图谱、SSO/认证中心、容器化。这些分别属于 1.2、1.3、1.4 与阶段 2/3。

## Capabilities

### New Capabilities

- `ai-chat`：AI 对话服务的行为契约，覆盖登录态校验、多轮会话记忆、流式输出、模型不可用时的降级、每日配额拦截、token/延迟/失败率观测与模型配置切换。

### Modified Capabilities

无。现有 6 个能力的需求均不发生变化；菜单与权限属于新增行为，由 `ai-chat` 承载。

## Impact

- **构建**：根 `pom.xml` 增加 `iboot-ai` 模块；新增 Spring AI 2.0.1 BOM、`spring-ai-starter-model-openai`、`spring-ai-starter-model-chat-memory-repository-redis`。
- **运行时**：新增一个独立 JVM（8081），依赖现有 Redis 与百炼公网端点；不新增 MySQL 表（会话只存 Redis）。
- **配置**：新增 `DASHSCOPE_CHAT_API_KEY`、`DASHSCOPE_EMBEDDING_API_KEY`、`DASHSCOPE_BASE_URL`，已存在于 `.env.example`；`jwt.secret` 与 Redis 连接信息通过环境变量共享。
- **前端**：`iboot-portal` 新增 `views/ai/`、`api/ai.ts`、`utils/aiStream.ts`，路由新增 `/dashboard/ai`，`vite.config.ts` 新增 `/iboot/ai` → `http://localhost:8081` 代理。
- **admin**：新增 Flyway `V24` 插入 AI 工作台菜单与权限，不改业务代码。
- **可观测**：`docker/scripts/prometheus/prometheus.yml` 新增 `iboot-ai` job；`docker/scripts/prometheus/alerts/` 新增 AI 告警；Grafana 新增 dashboard JSON。
- **兼容性**：无破坏性变更；`iboot-admin` 的认证、SSE 与现有接口行为不变。
