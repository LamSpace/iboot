# AI 1.3 Spike 报告（AgentScope Java 2.0.1）

> 对应变更：`openspec/changes/add-agent-orchestration`
> 执行日期：2026-10-28
> 执行方式：`iboot-ai` 测试目录下的临时验证类，结论落定后删除；仓库只保留本报告。

## 1. 结论

**Go（有条件）。**

AgentScope Java 2.0.1 可以在 Spring Boot 4.0.3 + JDK 25 上完成一次真实的"模型 → 工具 → 模型"往返，流式事件、运行上下文与 token 用量均可用。采纳前必须落实三个条件：

1. **禁止使用 `@Tool` 注解注册工具**，必须实现 `AgentTool` 并用 `registerAgentTool` 注册。原因是 victools 与 Spring AI 的版本冲突（见 §5）。
2. **不能依赖 `PermissionEngine` 的 `ASK` 行为做写操作确认**。`ASK` 只对 `ToolBase` 生效，`AgentTool` 会被直接执行（见 §6）。授权门必须由我们自己的工具实现 + 运行状态机承担。
3. **工具 schema 必须手写 JSON Schema**，不依赖反射生成。

第 1、2 条不改变 `design.md` 的总体设计（D4 本来就规定"授权事实不来自引擎"），但需要把"只用 `AgentTool`"写成硬约束。

## 2. 版本事实（Maven Central / settings.xml 实测）

| 事实 | 结论 |
|---|---|
| 坐标 | `io.agentscope:agentscope-bom:2.0.1` + `agentscope-openai-spring-boot-starter` |
| 镜像 | 阿里云 `maven.aliyun.com/repository/public` 可解析，POM 返回 200 |
| Starter 构建基线 | Spring Boot **4.0.3**（openai / dashscope starter 均如此） |
| `agentscope-core` 字节码 | Java 17（major 61），JDK 25 无类版本问题 |
| 模型配置项 | `agentscope.openai.api-key` / `base-url` / `endpoint-path` / `model-name` / `stream` |
| 自动装配条件 | `agentscope.model.provider=openai` 且 `agentscope.openai.enabled` 才创建模型 Bean；`agentscope.agent.enabled=true` 才创建 Agent 相关 Bean |
| 可用版本线 | 2.0.1 / 2.0.2 / 2.0.3 / 2.0.4 / 2.1.0-BETA1；2.0.x 全部使用 victools 4.38.0 |
| 依赖仲裁结果 | Jackson 2 从 2.21.1 降到 **2.20.2**；OkHttp 从 5.3.2 降到 **4.12.0**（均为 Boot 托管） |

## 3. 验证环境

- JDK 25、Maven 3.9、`iboot-ai` 模块；真实百炼 `qwen3.8-flash`。
- `enable_thinking=false` 通过 `GenerateOptions.additionalBodyParam` 传入。
- 端点：`https://dashscope.aliyuncs.com/compatible-mode/v1`。
- 测试分组 `cloud`，以 `-Dsurefire.excludedGroups=disabled` 运行。

## 4. 硬门槛结论

| # | 门槛 | 结果 | 证据 |
|---|---|---|---|
| **S1** | JDK 25 + Boot 4.0.3 启动并装配模型 Bean | **通过** | `ApplicationContextRunner` 装配 `OpenAIAutoConfiguration`，`Model` Bean 存在；完整 `AiContextTest` 启动 4.6s |
| **S2** | Jackson 2/3 共存且既有测试全绿 | **通过（有保留）** | Jackson 2 与 3 双向序列化正常；`mvn -pl iboot-ai test` 共 **109** 个测试全部通过；但 victools 冲突见 §5 |
| **S3** | 一次真实工具往返 | **通过** | 工具被调用，回答"杭州当前天气：晴，气温 25℃" |
| **S4** | `ASK` 能中止而非挂起 | **不成立（改设计）** | 配置 ASK 规则后工具仍被执行（`toolCalled=true`），未出现 `REQUIRE_USER_CONFIRM`；见 §6 |
| **S5** | `RuntimeContext` 把身份传给工具 | **通过** | 工具读到 `probe=MARKER-9527`、`userId=u-42` |
| **S6** | 模型 usage 可取 | **通过** | `ChatUsage{input=351, output=12, total=363}` |
| **S7** | 流式事件可映射到 SSE | **通过** | 事件序列含 `AGENT_START / MODEL_CALL_START / TOOL_CALL_START/DELTA/END / TOOL_RESULT_* / TEXT_BLOCK_DELTA / AGENT_RESULT / AGENT_END` |
| **S8** | 关闭 thinking 后工具循环正常 | **通过** | 全程无 `THINKING_BLOCK_*` 事件，工具往返与最终回答正常 |

## 5. 关键发现：victools 双版本冲突

这是本次 Spike 最重要的发现，且**不是 Jackson 问题**。

| 组件 | victools 版本 | `SchemaGenerator.generateSchema` 返回类型 |
|---|---|---|
| Spring AI 2.0.1（`spring-ai-model`） | **5.0.0** | `tools.jackson.databind.node.ObjectNode`（Jackson 3） |
| AgentScope 2.0.1（`agentscope-core`） | **4.38.0** | `com.fasterxml.jackson.databind.node.ObjectNode`（Jackson 2） |

Maven 就近仲裁让 5.0.0 胜出，于是 AgentScope 的反射式工具注册在运行期抛：

```
java.lang.NoSuchMethodError: 'com.fasterxml.jackson.databind.node.ObjectNode
com.github.victools.jsonschema.generator.SchemaGenerator.generateSchema(java.lang.reflect.Type, java.lang.reflect.Type[])'
  at io.agentscope.core.util.JsonSchemaUtils.generateSchemaFromType(JsonSchemaUtils.java:133)
  at io.agentscope.core.tool.ToolSchemaGenerator.extractParameterInfo(ToolSchemaGenerator.java:129)
  at io.agentscope.core.tool.ReflectiveFunctionTool.create(ReflectiveFunctionTool.java:129)
```

**影响范围**：仅 `Toolkit.registerTool(对象)` 的反射路径（`@Tool` 注解）。改用 `registerAgentTool(AgentTool)` 后，同一场景通过。

**处置**：不改依赖仲裁。AgentScope 2.0.1 的 `ReActAgent` 也引用了 `JsonSchemaUtils`（结构化输出路径），因此**禁止使用结构化输出重载**（`call(..., Class<?>)`），只走纯文本 `call/stream`。这两条写入 `design.md` 的决策约束。

**为什么不强行统一 victools 版本**：降到 4.38.0 会让 Spring AI 的结构化输出/工具 schema 路径反向断裂；升到 5.0.0 则 AgentScope 反射路径断裂。两边都在用，且无法在同一 JVM 同时满足。避免使用冲突路径成本最低。

## 6. 确认机制：内置 ASK 不适用于 AgentTool

`PermissionEngine.checkPermission(ToolBase, Map)` 的签名决定了规则匹配只覆盖 `ToolBase`。`AgentTool` 绕过该检查被直接执行：

- 配置 `addAskRule("get_weather", PermissionRule(..., ASK, ...))` 后，真实调用中 `toolCalled=true`；
- 事件序列里没有 `REQUIRE_USER_CONFIRM`；
- 事件中出现了完整的 `TOOL_RESULT_START/TEXT_DELTA/END`。

结论：**写操作确认必须由我们自己实现**：

1. 写工具的 `callAsync` 先查 `RuntimeContext` 中的一次性授权记录；无授权时**不发起下游请求**，返回 `ToolResultBlock.text("awaiting_confirmation")` 或 `ToolResultBlock.suspended(...)`；
2. 运行服务在观察到待确认结果后结束当前流，转 `PENDING_APPROVAL`；
3. 第二段由服务端按记录的原始参数执行，不经过模型。

这与 `design.md` D4 一致，本节只是把"为什么不用引擎"钉死。

## 7. 事件与用量

`ReActAgent.streamEvents(List<Msg>, RuntimeContext)` 返回 `Flux<AgentEvent>`，事件名与 1.3 的 SSE 设计一一对应：

| AgentScope 事件 | 对应 1.3 SSE |
|---|---|
| `AGENT_START` | `meta` |
| `TOOL_CALL_END` | `step` |
| `TOOL_RESULT_END` | `tool_result` |
| `REQUIRE_USER_CONFIRM` | `pending_approval`（本次未触发，由我们自己的门产生） |
| `TEXT_BLOCK_DELTA` | `token` |
| `AGENT_RESULT` / `AGENT_END` | `done` |
| `EXCEED_MAX_ITERS` | `error{code=step_limit}` |

用量：`Msg.getUsage()` 返回 `ChatUsage{inputTokens, outputTokens, cachedTokens, totalTokens}`。本次为最后一次模型调用的聚合值；**按调用计量需从 `MODEL_CALL_END` 事件累加**，这一点在实现配额时验证。

## 8. 风险与后续

| 风险 | 处置 |
|---|---|
| 团队成员误用 `@Tool` / 结构化输出，触发 `NoSuchMethodError` | 在 `AgentTool` 注册表处加注释与启动自检；代码评审卡住；本报告作为依据 |
| Spring AI 未来升级换用 victools 5.x 之外版本 | 每次升级 Spring AI 后重跑 §5 的最小复现用例 |
| OkHttp 被降到 4.12.0 | 本次工具往返未走 OkHttp 5 专有 API；若出现 `NoSuchMethodError`，优先显式提升 okhttp 到 5.3.2 并回归 MinIO |
| 每调用用量需累加 | 在 `AgentRuntime` 适配层订阅 `MODEL_CALL_END` 累加，并以单测锁定 |
| `@Tool` 反射路径不可用 | 工具 schema 手写；与 §5 同一处置 |

## 9. 复现要点

```xml
<dependency>
  <groupId>io.agentscope</groupId>
  <artifactId>agentscope-openai-spring-boot-starter</artifactId>
</dependency>
```

```yaml
agentscope:
  model:
    provider: openai
  openai:
    api-key: ${DASHSCOPE_CHAT_API_KEY}
    base-url: ${DASHSCOPE_BASE_URL}/v1
    model-name: qwen3.8-flash
    stream: true
```

```java
GenerateOptions options = GenerateOptions.builder()
        .additionalBodyParam("enable_thinking", false)
        .build();
OpenAIChatModel model = OpenAIChatModel.builder()
        .apiKey(key).baseUrl(base + "/v1").modelName("qwen3.8-flash")
        .stream(true).generateOptions(options).build();

Toolkit toolkit = new Toolkit();
toolkit.registerAgentTool(new WeatherTool());   // 不要用 registerTool(Object)

ReActAgent agent = ReActAgent.builder()
        .name("spike-agent")
        .sysPrompt("需要数据时必须调用工具，不得臆造。")
        .model(model).toolkit(toolkit).maxIters(3).build();

agent.streamEvents(List.of(userMsg), runtimeContext)
     .subscribe(event -> { /* 映射 SSE */ });
```

运行命令（真实调用）：

```bash
set -a; source .env; set +a
mvn -s /home/lam/repo/settings.xml -pl iboot-ai test \
  -Dtest='AgentScopeRealCallSpikeTest' -Dsurefire.excludedGroups=disabled
```

## 10. 对变更的影响

1. `design.md` D1 增加两条硬约束：只用 `AgentTool`、禁用结构化输出重载。
2. `tasks.md` 的 4.1 需按 `AgentTool` 实现适配层；4.3 需从 `MODEL_CALL_END` 累加用量。
3. 回测的假模型需要模拟 AgentScope 的事件流（或直接测 `AgentTool` 与运行服务），不能依赖 Spring AI 的 `ChatClient` 夹具。
