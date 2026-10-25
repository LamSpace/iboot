# Tasks

## 1. 模块与配置基线

- [x] 1.1 根 `pom.xml` 增加 `iboot-ai` 模块；新建模块 pom（web、security、data-redis、validation、actuator、springdoc、Spring AI BOM 2.0.1、`spring-ai-starter-model-openai`、`spring-ai-starter-model-chat-memory-repository-redis`）；用 `mvn -s /home/lam/repo/settings.xml -pl iboot-ai -am dependency:tree` 验证依赖解析，且 `mvn -pl iboot-ai -am package` 成功
- [x] 1.2 编写 `application.yml`：全局 `api-key` 兜底、`base-url: ${DASHSCOPE_BASE_URL}/v1`、`model: qwen3.8-flash`、`extra-body.enable_thinking=false`、`max-tokens=4096`、embedding 模型与维度；用 `AiPropertiesTest` 断言配置绑定，并验证 `base-url` 不以 `/v1` 结尾时启动失败且提示明确
- [x] 1.3 实现 `SpikeApplication` 等价的启动类、虚拟线程与 actuator 暴露；以独立进程启动 8081，验证 `curl localhost:8081/actuator/health` 返回 `UP`
- [x] 1.4 为 Embedding 落一个可注入的 `EmbeddingModel` 配置（1.1 不调用）；用上下文测试断言 Bean 存在且模型为 `text-embedding-v4`

## 2. 鉴权

- [x] 2.1 实现最小 JWT 过滤器与 `SecurityConfig`：白名单仅 `/actuator/health`、`/actuator/prometheus`；其余路径要求认证；用 MockMvc 验证无令牌返回 401、令牌签名无效返回 401
- [x] 2.2 读取 Redis `login_tokens:<token>` 建立安全上下文，并刷新 `login_tokens:*`、`online_token:*` 的 TTL；用 Redis Testcontainers 验证有效会话通过、登出后返回 401、TTL 被刷新
- [x] 2.3 联调验证：通过 admin 登录取得令牌，调用 `iboot-ai` 的受保护接口返回 200；同一请求去掉 Authorization 返回 401，记录到 `doc/22-AI 工作台操作指南.md`

## 3. 会话记忆与会话索引

- [x] 3.1 验证 Redis chat memory starter 的 TTL 能力：写一个双会话读写测试；若无法设置 TTL，改为实现自定义 `ChatMemoryRepository`（接口不变），并在 design 的 Open Question 下记录结论
- [x] 3.2 配置 `MessageWindowChatMemory` 窗口 20、key 前缀 `iboot:ai:chat:`、TTL 24 小时；用 stub 模型验证第二轮请求包含第一轮问答，超过窗口后旧消息被丢弃
- [x] 3.3 实现会话索引（`iboot:ai:conversations:{userId}`，保存 ID、标题、更新时间，最多 50 条，标题取首条消息前 30 字）；用测试验证列表、排序与跨用户读取返回 404

## 4. 对话与流式

- [x] 4.1 实现非流式对话服务：`ChatClient` + 默认关闭 thinking + `max-tokens` 上限；用假 OpenAI 兼容服务验证模型回显、内容非空、usage 被读取
- [x] 4.2 实现 `POST /api/ai/chat/stream`，按 `meta → token* → done` 发送 SSE；用集成测试断言事件顺序、`done` 携带 usage、空 `choices` 不触发结束
- [x] 4.3 实现客户端中断：`SseEmitter` 超时/完成/错误回调取消上游订阅；用测试模拟断开，验证不再写入且不发送 `done`
- [x] 4.4 记录首 token 延迟指标，并以真实模型测量 p50/p95；用 `@Tag("cloud")` 测试验证 p95 < 3 s（thinking 关闭），结果写入操作指南
- [x] 4.5 实现会话历史与消息查询接口；用测试验证按用户隔离、分页与消息顺序

## 5. 配额与降级

- [x] 5.1 实现 Redis 日配额（全局与单用户），调用前检查、调用后按 usage 累加，键按北京时间自然日与 48 小时过期；用单元测试与并发测试验证超限拒绝与跨日重置
- [x] 5.2 接入配额拦截：超限返回 429 且不调用模型；用集成测试断言模型客户端未被调用、响应含可辨识错误码
- [x] 5.3 实现错误映射：超时 → 降级提示；上游 401 → 配置错误；上游 429 → 可重试；其他 5xx → 通用失败；用假服务分别注入并断言 `error` 事件与指标标签
- [x] 5.4 把"免费额度导致预算熔断无法触发"的 429 补测条件写入操作指南，并标注为验收前补测项（不在本变更阻塞）

## 6. 可观测与用量

- [x] 6.1 实现 `iboot.ai.*` 指标（请求、token、首 token、耗时、错误、配额拒绝）；用 `SimpleMeterRegistry` 断言标签与计数
- [x] 6.2 在 `docker/scripts/prometheus/prometheus.yml` 增加 `iboot-ai` job；用 `promtool check config` 验证配置，并确认 `/actuator/prometheus` 能抓到 `iboot_ai_*` 指标
- [x] 6.3 新增 Grafana dashboard JSON（放入 `custom/`），验证 JSON 可被 provisioning 加载并在面板中看到请求、token、延迟、错误
- [x] 6.4 新增 Alertmanager 规则：服务不可用、日 token 超阈值、错误率、首 token 过慢；用 `promtool check rules` 验证，并手动触发一次告警确认可达
- [x] 6.5 实现 `GET /api/ai/usage`，返回当天已用 tokens、剩余配额与统计日期；用测试验证仅本人可查、未登录 401

## 7. portal

- [x] 7.1 新增 `src/utils/aiStream.ts`：`fetch` + `ReadableStream` 解析 `meta/token/done/error`，支持 `AbortController`；用本地假服务验证取消后停止读取
- [x] 7.2 新增 `src/api/ai.ts`、`views/ai/ChatView.vue` 与 `/dashboard/chat` 路由（菜单路径 `/ai/chat` 的最后一段为 `chat`），包含会话列表、消息区、输入框、停止生成按钮；`cnpm run build` 通过，并手动完成一次真实对话
- [x] 7.3 新增用量面板，调用 `/api/ai/usage` 展示当天用量与剩余配额；手动验证超限时的提示文案
- [x] 7.4 在 `vite.config.ts` 增加 `/iboot/ai` → `http://localhost:8081` 代理并 rewrite；手动验证流式响应经代理不被缓冲
- [x] 7.5 补齐中英文文案（`zh-CN` / `en-US`），验证切换语言后视图无缺失 key

## 8. 菜单迁移

- [x] 8.1 新增 `V24__add_ai_workbench_menu.sql`：目录 9800（`/ai`）、菜单 9801（`/ai/chat`）、permission `ai:chat`，仅授予 role_id=1，使用幂等 `INSERT ... WHERE NOT EXISTS`；用 MySQL Testcontainers + Flyway 验证重复执行不报错
- [x] 8.2 用迁移测试断言菜单与 `sys_role_menu` 关联存在；手动登录 admin 验证侧边栏出现"AI 工作台"且普通用户不可见

## 9. 集成验证与交付

- [x] 9.1 运行根 `mvn -s /home/lam/repo/settings.xml clean verify`，验证全部模块通过且 `cloud` 分组默认排除
- [x] 9.2 运行 `cnpm run build` 与 `cnpm run check:ui`，验证前端构建与设计令牌检查通过
- [x] 9.3 admin 冒烟：登录、用户 CRUD、上传文件、SSE 消息推送、中英切换，验证本变更未造成回归
- [x] 9.4 编写 `doc/22-AI 工作台操作指南.md`：启动方式、环境变量、配额配置、指标与告警、429 补测条件、故障排查（404/401/启动失败）
- [x] 9.5 更新 ROADMAP 1.1 行为已完成，并把模型名修正为 `qwen3.8-flash`（与 Spike 报告一致）
- [x] 9.6 从 `master` 切出功能分支（`feat/ai-chat-foundation`），提交并按 CLAUDE.md 流程创建 PR、合并，最后 `pull --ff-only` 同步本地 `master`
