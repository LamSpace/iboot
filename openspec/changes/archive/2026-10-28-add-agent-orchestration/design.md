# Design

## Context

动机与范围见 `proposal.md`；行为契约见 `specs/ai-agent/spec.md`。本节只记录决定实现方式的现状约束。

**既有实现**

- `iboot-ai` 是独立 JVM（8081），DDD 四层：`interfaces / application / domain / infrastructure`。已有 `ChatClient`（Spring AI 2.0.1）、Redis 会话记忆、`QuotaService`、`AiMetrics`、SSE 控制器、`SubscriptionRegistry`。
- `PermissionScopeClient` 从 `RequestContextHolder` 读取 `Authorization`，调用 admin 的 `/api/auth/scope`，Caffeine 缓存 60 秒。它只在**请求线程**上工作——异步线程上取不到 token。
- admin 的权限模型是 `@PreAuthorize("hasAuthority('...')")` + `DataScopeInterceptor`，`Result<PageResult<T>>` 响应形态。
- 已有 `FakeOpenAiServer`（固定回答）、`@Tag("cloud")` 测试分组、`RagRecallEvaluationTest` 的"用例集 → 报告 → 阈值"回测范式。

**已核查的外部事实**

| 事实 | 结论 |
|---|---|
| `qwen3.8-flash` | 支持 Function Calling，OpenAI 兼容端点；参数可靠性需回测兜底 |
| AgentScope 2.0.1 | 提供 `agentscope-openai-spring-boot-starter`，构建于 Spring Boot 4.0.3 |
| `agentscope-core` 字节码 | Java 17（major 61），JDK 25 无类版本问题 |
| OpenAI starter 配置项 | `base-url` / `endpoint-path` / `model-name` / `api-key` / `stream` |
| 原语 | `PermissionEngine`（ALLOW/DENY/ASK/PASSTHROUGH）、`ToolBase.isReadOnly()`、`RuntimeContext`（`getUserId` / `put`） |
| 依赖 | `agentscope-core` 使用 Jackson 2.21，与 Boot 4 的 Jackson 3 并存 |

**两个必须处理的缺口**

1. 异步线程拿不到请求头：工具身份必须显式携带。
2. `StatisticsController` 与菜单 id 9 均无权限校验：统计工具必须以补权限为前提。

## Goals / Non-Goals

**Goals**

- 工具层与编排层解耦，编排运行时的更换不影响工具、审计、审批与回测。
- 身份与权限只有一条来源：当前登录态；没有任何代码路径可以提权。
- 写操作的可执行性只由服务端的授权记录决定，与模型输出无关。
- 回测可在无云凭据、无真实模型的情况下确定性运行。

**Non-Goals**

- 不做多智能体协作、A2A、MCP 工具接入。
- 不采用 AgentScope 的 memory / RAG / Nacos 扩展。
- 不改变 1.1 对话与 1.2 RAG 的既有链路与事件。
- 不做运行的中断恢复跨实例迁移（阶段 2 再考虑）。

## Decisions

### D1. 编排运行时：AgentScope 2.0.1，Spike 把关，Spring AI 自管循环为退路

采用 AgentScope 2.0.1 承载 ReAct 循环、工具选择与权限引擎；模型走 `agentscope-openai-spring-boot-starter` 指向现有百炼兼容端点。

Spike 以 S1/S2 为硬门槛：JDK 25 下上下文能启动、Jackson 2/3 共存不破坏现有序列化。**任一失败即切换**到 Spring AI 自管循环（`internalToolExecutionEnabled=false`，服务端显式驱动"模型 → 工具 → 模型"）。两条路径共用同一套 `AgentTool` 注册表、审批状态机、审计与回测，仅 `AgentRuntime` 实现不同，因此切换不影响 spec 与验收。

替代方案：直接选 Spring AI 自管循环（更少依赖，但放弃 AgentScope 的 PermissionEngine 与循环治理）；选 AgentScope 2.0.4（有 `AgentRun`/`RunControl`，但标记为 release draft 且有破坏性变更）。均暂不采用。

**Spike 实测结论（2026-10-28，详见 `doc/spike/ai-1.3-spike-report.md`）**

S1/S2/S3/S5/S6/S7/S8 通过；S4 不成立。据此追加三条硬约束：

- **只用 `AgentTool` + `registerAgentTool`，禁止 `@Tool` 注解注册**。Spring AI 2.0.1 带 victools 5.0.0（Jackson 3 返回类型），AgentScope 2.0.1 编译于 4.38.0（Jackson 2 返回类型），同一 JVM 只有一个能生效；反射注册路径在运行期抛 `NoSuchMethodError`。
- **禁用结构化输出重载**（`call(..., Class<?>)` / `stream(..., Class<?>, ...)`），`ReActAgent` 的该路径同样引用 victools。
- **写操作授权门必须自建**。`PermissionEngine` 的 `ASK` 只对 `ToolBase` 生效，`AgentTool` 会被直接执行；因此 D4 的"授权事实只来自服务端记录"是本设计的唯一保证。

### D2. 工具是实现于 `iboot-ai` 的 admin HTTP 代理

工具通过 `RestClient` 调用 admin 既有接口，携带调用者的 JWT。

- 权限、数据范围、审计全部由 admin 承担，不在 AI 侧复刻第二套。
- 阶段 2 网关化后只改 base-url，工具代码不动。
- 工具注册要求：仅接受具备显式 `@PreAuthorize` 的下游接口。统计接口原本无鉴权，已复用菜单中既有的 `report:view` / `usage:view` 补上校验，不新增权限编码。
- 替代方案：直连 admin 数据库（越权、阶段 2 必拆，否决）；MCP server（多一个进程，1.3 不需要）。

工具返回必须经过统一裁剪：限制条数、限制字符数（默认 2000）、剔除密码/令牌/密钥字段，再进入模型上下文。

### D3. 身份通过 `RuntimeContext` 显式传递

请求进入时捕获 `(userId, bearerToken, permissions)` 组成 `AgentContext`，放入 AgentScope 的 `RuntimeContext`；工具从 `ToolCallParam.getRuntimeContext()` 取出。**不使用 `ThreadLocal`/`RequestContextHolder`**，因为流式与工具执行会脱离请求线程。

模型提供的任何 `userId` / `deptId` / `token` 参数一律丢弃，身份只来自 `AgentContext`。

### D4. 写操作用两段式 + 一次性授权

- 第一段：可写工具不执行，生成 `PendingAction{tool, argsJson, argsDigest}` 持久化到运行记录，运行状态转 `PENDING_APPROVAL`，流以 `pending_approval` + `done{pending}` 收尾。
- 第二段：发起人提交确认；服务端校验运行归属、未过期、未使用、`argsDigest` 一致后，**按记录中的原始参数**执行工具（不重新解析模型输出），随后以"已执行 + 结果"为上下文再调用一次模型生成答复。

选择两段式而非长连接挂起：后者依赖单实例与 SSE 超时窗口，阶段 2 多实例即失效；且"执行发生在确认请求内"使授权路径可被单测穷举。

`PermissionEngine` 的 `ASK` 用于在模型侧表达"需要确认"，但**授权事实不来自它**，只来自服务端的授权记录——这样即使引擎行为随版本变化，也不影响"不可绕过"。

### D5. 运行状态机与数据模型

```
RUNNING --(写工具)--> PENDING_APPROVAL --(确认)--> EXECUTING --> COMPLETED
   |                        |                                  \-> FAILED
   |                        \--(拒绝/过期)--> REJECTED / EXPIRED
   \--(步数/配额/错误)--> FAILED
```

`iboot-ai` Flyway `V2`：

- `ai_agent`：名称、描述、系统提示词、模型、工具白名单 JSON、状态、审计列。
- `ai_agent_run`：会话、智能体、用户、状态、待确认动作（工具/参数/摘要/过期）、步骤 JSON、步数、用量、时间。

步骤只存摘要，不存原始敏感值。单表存轨迹而不拆 `ai_agent_step`，1.3 无按步查询需求。

### D6. 配额与指标挂在模型调用边界

AgentScope 不经过 Spring AI 的 `ChatClient`，现有 `QuotaService` / `AiMetrics` 不会自动生效。因此在每个模型调用前后由运行时适配层记账：调用前 `assertWithinQuota`，调用后按 usage 累加。若 AgentScope 拿不到 usage，则退化为按请求/响应字符估算并在指标中标记为估算值——这一取舍在 Spike 中确认。

新增指标：`iboot_ai_agent_runs_total`、`iboot_ai_agent_tool_calls_total`、`iboot_ai_agent_tool_duration_seconds`、`iboot_ai_agent_approvals_total`、`iboot_ai_agent_approval_bypass_blocked_total`、`iboot_ai_agent_steps_per_run`。

### D7. 接口与事件增量，不改既有链路

新增 `/api/ai/agent/chat/stream`、`/api/ai/agent/runs/{runId}/confirm`、`/api/ai/agent/agents`（CRUD）。事件：`meta → step → tool_result → (pending_approval) → token* → done | error`。`aiStream.ts` 增加事件成员，`ChatView` 不动；新增 `AgentChatView` 与 `AgentConfigView`。

### D8. 回测分两层

- 确定性层：脚本化假模型（按调用序列返回"tool_call → 最终答复"）+ 假 admin，断言事件序列、工具选择、参数、审批与注入拒绝。CI 默认运行。
- 云端层：`@Tag("cloud")`，真实模型跑固定评测集，产出 `target/agent-eval-report.json`，与仓库内基线对比。

两者共用 `test/resources/agent/eval-cases.json`。硬门禁（写操作必须停、注入不得调未登记工具）在两层都执行。

## Risks / Trade-offs

| 风险 | 处置 |
|---|---|
| JDK 25 下 AgentScope 运行期失败（反射/字节码库） | Spike S1/S2 硬门槛；失败即切 Spring AI 自管循环 |
| Jackson 2.21 与 Boot 4 Jackson 3 并存 | Spike 验证现有 `tools.jackson` 序列化与 ES 客户端不受影响；必要时隔离依赖 |
| 第二套模型抽象与现有 ChatClient 并存，配置与错误映射分裂 | 统一 base-url/model/key 来源；错误映射集中在一层适配器；Spike 确认 usage 可取 |
| 每次模型调用记账遗漏导致配额被绕过 | 配额检查下沉到唯一的模型调用入口，并加测试断言"一次 N 步运行计入 N 次" |
| 运行轨迹中泄露敏感字段 | 工具返回统一裁剪；审计只存摘要；测试断言密码/令牌不出现 |
| 步数爆炸或工具循环 | 步数上限、超时上限、重复调用同参检测 |
| 模型参数生成不稳定（第三方基准有扣分） | 参数解析失败不执行工具，返回可辨识错误；回测卡参数准确率 |
| 统计接口补权限后现有角色失效 | `V26` 同时给内置角色授权；文档标注需重新登录 |
| AgentScope 2.0.4 的破坏性变更 | 锁 2.0.1；升级单独立项并重跑回测 |
| Nacos Agent Card 在 1.3 不可交付 | 只产出描述文件，注册挪到阶段 2.1，ROADMAP 同步修正 |

## Migration Plan

1. 合并顺序：admin `V26`（菜单 + 权限 + 统计权限）→ `iboot-ai` `V2`（表）→ 服务发布 → 前端发布。
2. `iboot.ai.agent.enabled` 默认 `false`；开启后新端点才可用，未开启时既有对话与 RAG 完全不受影响。
3. 权限授予后用户需重新登录（会话中的 permissions 才更新）。
4. 回滚：关闭开关即可停用智能体；表结构与菜单保留，不影响既有功能。数据层无需回滚。

## Open Questions

- 是否在 2.0.4 转为正式版后升级并采用 `AgentRun`/`RunControl` 简化审批挂起——可延后，不影响当前设计。
- Agent Card 描述文件的字段与 2.1 Nacos 注册所需的元数据——留待阶段 2.1 决定。
