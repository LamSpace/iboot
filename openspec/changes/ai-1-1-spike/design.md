# Design

## Context

见 `proposal.md` 的 Why。影响本设计的既有事实：

- 主干为 Spring Boot 4.0.3 / JDK 25.0.3 / Maven 3.9.16，本地仓库由 `/home/lam/repo/settings.xml` 指定；`spring-ai-bom:2.0.1` 已确认可解析。
- 已用 `curl` 直连验证：`qwen3.8-flash` 对话成功（`enable_thinking: false` 时 completion 仅 1 token）、`text-embedding-v4` 返回 1024 维。
- 两把 Key 均为 `sk-ws-` 前缀（新版按量付费、业务空间隔离），与 `https://dashscope.aliyuncs.com/compatible-mode` 配套可用。
- Spring AI 2.0.1 的 `DeepSeekApi.ChatModel` 枚举不含 `deepseek-flash`；`OpenAiChatOptions` 提供 `extraBody`，`spring.ai.openai.chat.options.extra-body` 可承载非标准参数。
- 仓库当前无 CI；本 Spike 不引入 CI。

## Goals / Non-Goals

**Goals:**

- 回答 H1–H7 七个硬门槛，每条都有可复现的命令与原始输出证据。
- 产出一份能让人不重跑也能判断 Go / No-Go 的报告。
- 让 TTFB 的测量口径可复现：同一机器、同一时段、10 次采样、记录 p50 / p95。

**Non-Goals:**

- 不建 `iboot-ai` 模块，不写生产代码，不改主干 `pom.xml`。
- 不验证 Redis 会话记忆、SSE 与 portal 的端到端链路（属于 1.1 实现）。
- 不验证 RAG、工具调用、AgentScope、OCR。
- 不修改 `ROADMAP.md`；若结论要求改文档，在后续独立变更处理。

## Decisions

### D1：Spike 工程放在仓库外

临时工程位于 `/tmp/iboot-ai-spike`，仓库只保留 `doc/spike/ai-1.1-spike-report.md`。

- **理由**：Spike 依赖与代码在结论落定后即丢弃；放进仓库会污染 `mvn verify` 和模块列表，还要额外清理。
- **备选**：在 `spike/ai-1.1` 分支建模块 —— 依赖仍会进分支历史，收益只是"方便回看"，不采用。

### D2：只引入 OpenAI starter，不引入 DeepSeek starter

依赖为 `spring-ai-bom:2.0.1` + `spring-ai-starter-model-openai`。

- **理由**：对话与 Embedding 同属百炼的 OpenAI 兼容端点，一个 starter 同时覆盖 chat 与 embedding；少一套自动装配，就少一个变量。
- **备选**：deepseek starter + openai starter 各一套 —— 与"全走百炼"的决策冲突，且会掩盖 OpenAI 兼容层的真实行为。

### D3：模型 ID 用字符串，不用枚举

配置 `spring.ai.openai.chat.options.model: qwen3.8-flash`。

- **理由**：Spring AI 的自带枚举不包含该模型；`OpenAiChatOptions.getModel()` 是 String，配置层直接透传最稳。
- **验证点**：H2 断言响应 `model` 字段回显 `qwen3.8-flash`，防止被服务端悄悄路由到别的模型。

### D4：thinking 三组对照

同一提示词分别测量：

1. 裸 `curl` + `"enable_thinking": false`（基线）；
2. Spring AI + `extra-body.enable_thinking=false`；
3. Spring AI 不传该参数（观察默认值）。

- **理由**：既验证框架有没有丢参数，也确认该模型的默认行为。对比项包括首 token 延迟、`completion_tokens`、是否出现 `reasoning_content`。
- **判据**：第 2 组应与第 1 组在同一量级；若显著更慢或返回思维链，说明 `extra-body` 未生效。

### D5：TTFB 的测量口径

**首 token 延迟**定义为：请求发出 → 收到第一个包含非空 `delta.content` 的 SSE 数据块。

- 在 Spike 的流式客户端里用单调时钟打点，不依赖日志时间戳；
- 每组 10 次，丢弃第 1 次（含连接建立），取剩余 9 次的 p50 / p95；
- 记录测量时刻，避免与供应商高峰时段混淆。
- **理由**：ROADMAP 的"首 token < 3s"只有在口径固定时才可比较。

### D6：不依赖 Docker / 数据库

Spike 只做模型层验证，不需要 MySQL、Redis、ES 或 Testcontainers。临时工程是一个普通 Maven 项目 + JUnit 5。

- **权衡**：无法验证"LLM 调用与虚拟线程"的整链路表现，那部分留给 1.1 的集成测试。

### D7：错误形态分两级采集

- **401**：用一把故意写错的 Key 复现，记录状态码与错误体；
- **429**：临时把百炼月度预算降到最低并触发一次调用（需要控制台操作），或在限流时被动捕获；若当天无法复现，报告标注"未实测，仅有文档依据"，不伪造结论。
- **超时**：用客户端超时（如 5s）触发，记录异常类型。

## Risks / Trade-offs

- **Spike 环境与生产不同**（无 Redis、无 SSE、无 portal）→ 报告明确区分"已验证的模型层"与"未验证的接入层"。
- **网络抖动污染 TTFB** → 多次采样取 p50 / p95，同时保留原始样本；结论中注明测量时段。
- **模型别名漂移** → 记录响应里的 `model` 与 `system_fingerprint`（若有），作为后续回归的基线。
- **`sk-ws-` Key 的额度行为与旧 Key 不同** → 报告记录实际扣费归属（免费额度 / 按量），避免上线后才发现计费口径不符。
- **临时工程丢失** → 报告附上关键 `pom.xml` 片段、配置片段与命令，保证结论可复核。

## Migration Plan

不适用。Spike 为一次性验证，结束后删除 `/tmp/iboot-ai-spike`；仓库侧只新增报告文件，无回滚动作。

## Open Questions

- `qwen3.8-flash` 的 thinking 默认值是否真的为开启 —— 由 H4 回答。
- 流式响应的最后一个 chunk 是否包含 `usage` —— 若不含，1.1 的 token 计量需要额外方案（如非流式统计或响应后查询）。
- 默认业务空间的 `sk-ws-` Key 是否需要带 WorkspaceId 的专属域名 —— 当前 `curl` 已验证不需要；若 Spike 中出现 401 再行排查。
