# Proposal

## Why

1.1 的对话与 1.2 的 RAG 解决了"AI 能说"，但管理端用户回答"最近谁登录失败""最近一次备份成功了吗"这类问题时，仍要跨多个页面手工翻找。管理端已有 25 个 Controller 的现成 API，缺的是一层受控的编排：让模型选择合适的工具，用**用户自己的身份**调用既有接口，并在写操作前停下来等人确认。

现在做的前提已经具备：`iboot-ai` 是独立可部署单元（1.1），登录态与五级数据权限可复用（1.1/1.2），AgentScope Java 2.0.1 已在 Maven Central 提供构建于 Spring Boot 4.0.3 的 starter，`qwen3.8-flash` 已验证支持 Function Calling。R8（JDK 25 未验证）仍需一次 Spike 收敛，但已从"可能不成立"降为"运行期验证"。

## What Changes

- **引入 AgentScope Java 2.0.1**（`io.agentscope:agentscope-bom` + `agentscope-openai-spring-boot-starter`），模型指向现有百炼 OpenAI 兼容端点；若 Spike 在 JDK 25 下失败，退回 Spring AI 原生 tool-calling 自管循环，能力契约不变。
- **新增智能体编排能力**：受控工具注册表，首批 4 个工具——查用户、查登录日志、查备份状态（只读）与触发备份（可写）。
- **工具以当前用户 JWT 代理调用 admin API**，不直连数据库、不复刻权限逻辑；admin 侧的 RBAC 与 `DataScopeInterceptor` 原样生效。
- **写操作两段式确认**：第一段停在 `pending_approval` 并返回一次性授权；确认后由服务端按原始参数确定性执行，再生成答复。模型无法在确认后篡改参数。
- **工具白名单与只读/可写分级**：模型不能调用未注册工具；调用前在本地校验用户权限，权限不足不发请求。
- **配额按每次模型调用计量**，并限制单次运行的步数与 token。
- **补齐统计接口权限**：`StatisticsController` 增加 `@PreAuthorize`，复用菜单中既有的 `report:view` / `usage:view`；否则"统计"工具会把无鉴权接口包装成 Agent 能力。
- **新增数据与配置**：`iboot-ai` Flyway `V2`（`ai_agent`、`ai_agent_run`）；admin Flyway `V26`（`ai:agent:use` / `ai:agent:manage` 菜单与权限）。
- **新增接口与事件**：`/api/ai/agent/chat/stream`、`/api/ai/agent/runs/{runId}/confirm`、Agent 配置 CRUD；SSE 增加 `step`、`tool_result`、`pending_approval`。
- **新增前端视图**：智能体对话、Agent 配置；扩展 `aiStream.ts` 事件类型。
- **新增回测**：固定评测集（工具选择、参数、HITL 合规、提示注入拒绝），产出 `target/agent-eval-report.json` 并保留基线。
- **文档同步**：`doc/22`、`doc/02`、`doc/03`、`doc/06`、`doc/07`、`doc/10`、`doc/11`、`README.md`、`ROADMAP.md`。

非目标（明确不做）：

- 不接 Nacos：Agent Card 的注册挪到阶段 2.1（1.3 只产出描述文件）。
- 不采用 AgentScope 的 memory / RAG / Nacos 扩展，避免与既有 Redis 记忆、知识库 RAG 重叠。
- 不做多智能体协作、A2A、MCP 工具接入；知识图谱属于 1.4。
- 不做写操作的批量编排与跨服务事务。

## Capabilities

### New Capabilities
- `ai-agent`: 智能体编排——受控工具注册与调用、工具级权限继承、写操作人机确认、运行轨迹与配额计量、行为回测。

### Modified Capabilities

（无。`ai-chat` 的登录态、会话隔离、流式与配额要求不变；智能体是并列的新能力，不改动既有对话行为。）

## Impact

**代码**

- `iboot-ai`：新增 `domain/agent`、`application/service` 的 Agent 运行与审批状态机、`infrastructure/tool` 工具注册与 admin 代理、`interfaces/controller` 的 Agent 端点、Flyway `V2`。
- `iboot-admin`：`StatisticsController` 增加权限注解，Flyway `V26` 新增菜单与权限。
- `iboot-portal`：新增智能体对话与 Agent 配置视图，扩展 `utils/aiStream.ts`。
- 根 `pom.xml`：新增 `agentscope-bom` 依赖管理。

**依赖与运行时**

- 新增 AgentScope 2.0.1；其 `agentscope-core` 依赖 Jackson 2.21，与 Boot 4 的 Jackson 3 并存，需在 Spike 中验证无冲突。
- 不新增中间件，不新增数据库实例；沿用 MySQL `iboot_ai`、Redis、既有 SSE 基建。

**兼容性**

- 无破坏性变更：既有对话与 RAG 链路不改动，新增端点与事件均为增量。
- 统计接口从"仅登录即可访问"收紧为需要 `report:view` / `usage:view`；未持有这些权限的角色将无法访问——这是有意的安全修复，需在变更说明中标注。
