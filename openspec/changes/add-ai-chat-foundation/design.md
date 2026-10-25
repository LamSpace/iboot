# Design

## Context

见 `proposal.md` 的 Why。影响本设计的既有事实与 Spike 结论：

- Spike 已验证：Spring AI 2.0.1 在 Boot 4.0.3 / JDK 25 上可装配；`qwen3.8-flash` 非流式、流式、Embedding 与配置切换可用；`enable_thinking=false` 时首 token p95 为 0.74–0.98 s，默认开启时为 3.0–4.4 s。
- 三个实现前提：`base-url` 必须含 `/v1`；必须提供全局 `spring.ai.openai.api-key`（否则 audio/image 自动配置启动失败）；流式 usage 位于 `choices: []` 的最后一个 chunk。
- 现有认证：`JwtAuthenticationFilter` 验签后读取 Redis `login_tokens:<token>`，会话含 `userId / permissions / deptId`，并刷新滑动过期；`online_token:<userId>` 保存用户到 token 的映射。
- 现有 SSE：`SseEmitterManager` 是业务推送通道，portal 通过 `EventSource` 消费；本变更不复用它，也不改动它。
- portal 菜单由后端 `sys_menu` 驱动，菜单与角色通过 `sys_role_menu` 关联；当前最高菜单 ID 为 9700。
- 仓库无 CI；本变更不新增 CI。

## Goals / Non-Goals

**Goals:**

- 交付可独立启动的 `iboot-ai`，覆盖对话、流式、记忆、鉴权、配额与观测。
- 把 Spike 的三个前提固化到配置与启动校验里，避免 1.2 重复踩坑。
- 让 portal 只通过 `/iboot/ai` 前缀访问新服务，后续接网关时只改代理/路由。

**Non-Goals:**

- 不做 RAG、文件解析、Agent、知识图谱。
- 不做容器化、Helm、Nacos 注册。
- 不引入数据库表；会话与配额全部落 Redis。
- 不做 markdown 富文本渲染、语音、多模态输入。
- 不做深度思考开关（1.1 固定关闭）。

## Decisions

### D1：模块与进程

新增 `iboot-ai` 模块，包名 `com.iboot.ai`，内部沿用 `interfaces / application / domain / infrastructure` 四层；端口 8081，启用虚拟线程；1.1 以独立 JVM 运行，不做 Dockerfile / compose。

- **理由**：ROADMAP §3 要求 AI 是独立可部署单元；容器化属于阶段 3.1，提前做会拖慢开发循环。
- **备选**：放进 `iboot-admin` 包内 —— 违反唯一硬约束，阶段 2 要挖出来重做，不采用。

### D2：模型配置与启动校验

- 依赖 `spring-ai-bom:2.0.1` + `spring-ai-starter-model-openai` + `spring-ai-starter-model-chat-memory-repository-redis`。
- 配置项：全局 `spring.ai.openai.api-key` 兜底，`chat.api-key` / `embedding.api-key` 分别覆盖；`base-url` 由 `${DASHSCOPE_BASE_URL}/v1` 提供；对话模型以字符串 `qwen3.8-flash` 透传。
- 默认 `extra-body.enable_thinking=false`，`max-tokens=4096`。
- 启动时校验 `base-url` 以 `/v1` 结尾、`api-key` 非空、`model` 非空；不满足直接失败并给出明确提示。
- **理由**：Spike 中 404 与 audio 模型启动失败都源于配置形态，启动校验把这两类错误前置。

### D3：鉴权

复制一个最小过滤器到 `com.iboot.ai.infrastructure.security`：

1. 白名单仅放行 `/actuator/health`、`/actuator/prometheus`；
2. 从 `Authorization` 或 `t` 参数取 token；
3. 验签 `jwt.secret`；
4. 读 Redis `login_tokens:<token>`，把 `userId`、`permissions`、`deptId` 写入 `SecurityContext`；
5. 与 admin 一致地刷新 `login_tokens:*` 与 `online_token:*` 的 TTL。

- **理由**：零改造 admin；会话语义与现有滑动过期一致。
- **代价**：安全逻辑暂时两份，阶段 2 认证中心上线后整段删除；Redis key 前缀作为配置项，避免硬编码分叉。
- **备选**：回调 admin 做 introspection —— 多一跳且 admin 无该端点，不采用。

### D4：会话记忆与会话索引

- 消息记忆使用 Spring AI 的 `ChatMemory` + `MessageWindowChatMemory`，窗口 20 条，底层使用**自定义** `ChatMemoryRepository`；key 前缀 `iboot:ai:chat:`，TTL 24 小时。
- **为什么不用官方 Redis 仓储 starter**：2.0.1 的实现依赖 RediSearch（`FT.CREATE`）与 RedisJSON，而项目的 `redis:7.0.14` 是标准镜像，没有这些模块。自定义仓储只用 Spring Data Redis 的 string/set/list 操作，约 100 行，接口不变。
- Spring AI 的仓储不提供“会话列表”，另建一个轻量索引：`iboot:ai:conversations:{userId}` 保存 `conversationId / title / updatedAt`，标题取首条用户消息前 30 字。
- 会话 ID 使用 UUID；读取历史时先校验索引归属，跨用户一律 404。

### D5：流式传输

- 接口：`POST /api/ai/chat/stream`，请求体 `{conversationId?, content}`，响应 `text/event-stream`。
- 服务端用 WebMVC `SseEmitter` 桥接 Spring AI 的 `Flux`；心跳 20 秒，超时 5 分钟；客户端断开时取消订阅。
- 事件契约：
  - `meta`：`{conversationId, messageId}`
  - `token`：`{delta}`
  - `done`：`{usage:{prompt,completion,total}, model}`
  - `error`：`{code, message, retryable}`
- 终止条件以显式 `done` / `error` 或流关闭为准，**不得**以 `choices: []` 判断结束。
- portal 新增 `utils/aiStream.ts`：`fetch` + `ReadableStream` 解析 SSE，支持 `AbortController`；现有 `utils/eventsource.ts` 保持不动。
- **理由**：`EventSource` 不支持 POST 与自定义头；两步式 GET 订阅需要服务端缓冲，易出竞态。

### D6：配额

- Redis 计数器：`iboot:ai:quota:global:{yyyy-MM-dd}` 与 `iboot:ai:quota:user:{userId}:{yyyy-MM-dd}`，首次写入时设置 48 小时过期。
- 调用前读取当前值并判断是否超限；超限直接 429，不调用模型。
- 调用完成后按 `usage` 原子累加；并发请求可能造成小幅超额，接受该误差并在设计上预留 5% 余量。
- 默认值：全局 2000 万/天、单用户 20 万/天，均在 `iboot.ai.quota.*` 配置。

### D7：可观测

| 指标 | 类型 | 标签 |
|---|---|---|
| `iboot.ai.chat.requests` | counter | `model`, `outcome` |
| `iboot.ai.tokens` | counter | `type=prompt/completion` |
| `iboot.ai.first_token` | timer | `model` |
| `iboot.ai.chat.duration` | timer | `model` |
| `iboot.ai.errors` | counter | `reason=timeout/unauthorized/rate_limited/upstream` |
| `iboot.ai.quota.rejected` | counter | `scope=global/user` |

- Prometheus 新增 job `iboot-ai`，目标 `host.docker.internal:8081`。
- Alertmanager：`AiServiceDown`、`AiDailyTokenBudgetHigh`（默认 500 万 token/天告警）、`AiChatErrorRateHigh`、`AiFirstTokenSlow`。
- Grafana 新增 dashboard，放入现有 `custom/` 目录。
- 不重复采集 Spring AI 自带的 observation 指标；只补业务语义标签。

### D8：portal

- 新增路由 `/dashboard/chat`、`views/ai/ChatView.vue`、`api/ai.ts`、`utils/aiStream.ts`。路由段用 `chat` 是为了与现有菜单取「路径最后一段」的约定对齐。
- 视图包含：会话列表、消息区、输入框、停止生成按钮、用量小面板。
- `vite.config.ts` 增加 `/iboot/ai` → `http://localhost:8081`，并 rewrite 去掉前缀。
- 中英文文案补齐；沿用 `portal-design-system` 既有的样式变量与组件。

### D9：菜单迁移

- `iboot-admin` 新增 Flyway `V24__add_ai_workbench_menu.sql`：
  - 目录 `9800`（`/ai`，`M`，icon `magic`）；
  - 菜单 `9801`（`chat`，`C`，component `ai/ChatView`，permission `ai:chat`）；
  - 仅授予超级管理员（role_id = 1），其他角色按需手工授权。
- 使用 `INSERT ... SELECT ... WHERE NOT EXISTS` 保证幂等。
- **理由**：菜单由后端驱动，硬编码前端路由不会出现在侧边栏；默认只给管理员，避免成本失控。

### D10：测试策略

- 单元：鉴权过滤器、配额、指标、会话服务、事件编码。
- 集成（Testcontainers Redis）：会话记忆、会话索引、配额并发。
- 集成（假 OpenAI 兼容服务）：流式事件顺序、首 token 计时、超时/401/429 降级；不消耗真实 token。
- 真实调用：`@Tag("cloud")`，默认被 `surefire.excludedGroups=cloud` 排除。
- 菜单迁移：Testcontainers MySQL + Flyway，断言菜单与角色关联存在且可重复执行。
- 回归：根 `mvn verify`、portal `build` + `check:ui`、admin 冒烟（登录、SSE、文件、i18n）。

## Risks / Trade-offs

- **鉴权逻辑重复** → 阶段 2 认证中心上线后删除；当前以配置化 key 前缀降低分叉风险。
- **单一供应商** → base-url / model 配置化，故障时可切 DeepSeek 兼容端点（Embedding 无等价兜底）。
- **配额并发超额** → 接受小幅超额，预留 5% 余量；必要时改用 Lua 原子校验。
- **SSE 被代理缓冲** → 服务端设置 `X-Accel-Buffering: no`，心跳 20 秒；vite 代理已支持流式。
- **Redis 会话无限增长** → 索引与消息均设 24 小时 TTL；会话列表只保留最近 50 条。
- **首 token 受网络影响** → 指标记录 p50/p95，告警阈值按 p95 设置。
- **429 未实测** → Spike 中因免费额度充足无法触发；列为验收前补测项。

## Migration Plan

- **部署**：先启动 Redis 与 admin，再以独立 JVM 启动 `iboot-ai`（8081），portal 通过 vite 代理访问；admin 无需停机。
- **数据库**：`V24` 在 admin 下次启动时自动执行；回滚时删除 `sys_role_menu` 与 `sys_menu` 中 9800/9801 两行即可。
- **回滚**：停止 `iboot-ai` 进程、回滚 `V24`、移除 portal 路由与代理；不涉及存量数据。

## Open Questions

- ~~Redis chat memory starter 是否支持按 key 设置 TTL~~ **已确认**：starter 支持 TTL，但依赖 RediSearch/RedisJSON，标准 Redis 7.0.14 不具备，改为自定义仓储。
- 会话标题是否需要在首轮后异步用模型生成 —— 1.1 先用首条消息截断，后续可选。
- 菜单是否需要同时开放给普通用户 —— 默认只给管理员，按成本策略再放开。
