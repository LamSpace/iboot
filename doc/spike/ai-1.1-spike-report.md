# AI 1.1 Spike 报告

> 对应变更：`openspec/changes/ai-1-1-spike`
> 执行日期：2026-10-25
> 执行方式：仓库外临时工程 `/tmp/iboot-ai-spike`，结论落定后删除；仓库只保留本报告。

## 1. 结论

**Go（有条件）。**

Spring AI 2.0.1 可以在 Spring Boot 4.0.3 + JDK 25 上接入百炼的 OpenAI 兼容端点：非流式、流式、Embedding、配置切换与主要错误形态均已实测通过。

采纳前必须落实三个条件：

1. **`base-url` 必须包含 `/v1`**。OpenAI starter 会在 base-url 后拼接 `/chat/completions`；填 `.../compatible-mode` 会得到 404，填 `.../compatible-mode/v1` 才正确。
2. **必须提供全局 `spring.ai.openai.api-key`**，或显式关闭未使用的 audio / image 自动配置。否则 `OpenAiAudioSpeechModel` 因缺少凭据导致整个上下文启动失败。
3. **默认对话必须显式设置 `enable_thinking=false`**。该模型 thinking 默认开启；不关闭时首 token p95 达到 3.0–4.4 s，不满足 ROADMAP 的"< 3 s"，且 completion token 约为关闭时的 5 倍。

## 2. 版本事实

| 事实 | 结论 |
|---|---|
| Spring Boot | 4.0.3 |
| Spring AI | 2.0.1（BOM 可解析，starter 可装配） |
| JDK / Maven | 25.0.3 / 3.9.16 |
| 对话模型 | `qwen3.8-flash`，1M 上下文，兼容 OpenAI 协议 |
| thinking 默认值 | **开启**；需通过 `enable_thinking=false` 显式关闭 |
| Embedding | `text-embedding-v4`，默认 1024 维，单请求最多 10 条输入 |
| 端点 | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| Key 形态 | `sk-ws-` 前缀（新版按量付费、业务空间隔离） |

## 3. 验证环境

- Linux，8 核 / 30 GB 内存；未使用 Docker、MySQL、Redis、ES 或 Testcontainers。
- 两把 `sk-ws-` Key 分别用于对话与 Embedding，经环境变量注入，未写入任何被提交的文件。
- 测量时段：2026-10-25 13:52–14:20（北京时间）。
- 首 token 口径：请求发出 → 第一个非空 `delta.content`；每组 10 次，丢弃首次后取 p50 / p95。

## 4. 硬门槛结论

| # | 门槛 | 结果 | 关键证据 |
|---|---|---|---|
| **H1** | Spring AI 2.0.1 在 Boot 4.0.3 装配 | **通过（有条件）** | 依赖树含 `spring-ai-starter-model-openai:2.0.1`，无 deepseek starter；上下文启动成功；初次失败原因是 audio 模型缺凭据 |
| **H2** | 非流式对话 | **通过** | `model=qwen3.8-flash`，内容"收到"，`usage = 30 prompt / 1 completion` |
| **H3** | 流式与首 token | **通过** | Spring AI（thinking off）p50 **650 ms** / p95 **741 ms**；裸 curl p50 559 / p95 982 ms |
| **H4** | thinking 开关有效 | **通过** | 默认组出现 `reasoning_content`；completion 从 16 增至 75（其中 50 reasoning） |
| **H5** | Embedding | **通过** | 1024 维；10 条批量成功；11 条返回 400 |
| **H6** | 错误可辨识 | **部分通过** | 401 与客户端超时已实测；429 未实测 |
| **H7** | 配置切换 | **通过** | 模型切换只改 1 行配置；base-url 指向本地 mock 时请求路径为 `/v1/chat/completions` |

## 5. thinking 对照（核心数据）

提示词：`请用一句话介绍杭州。`

| 组 | p50 | p95 | reasoning | completion tokens |
|---|---|---|---|---|
| 裸 curl + `enable_thinking=false` | 559 ms | 982 ms | 否 | 16 |
| Spring AI + `extra-body.enable_thinking=false` | **650 ms** | **741 ms** | 框架未暴露 | 16 |
| 裸 curl + 不传 | 1313 ms | 4403 ms | **是** | 75（50 reasoning） |
| Spring AI + 不传（配置移除 extra-body） | 1487 ms | 3017 ms | 同源 | 75 |

结论：关闭 thinking 后，p95 从 3.0–4.4 s 降到 0.74–0.98 s，同时输出 token 降到约 1/5。**1.1 的默认路径必须关闭**，深度思考作为按会话开启的选项。

## 6. 其他实测细节

### 6.1 流式 usage

流式响应**携带 usage**，且出现在最后一个 `choices` 为空的 chunk 中：

```json
{"choices":[],"model":"qwen3.8-flash","usage":{"completion_tokens":43,
"completion_tokens_details":{"reasoning_tokens":25,"text_tokens":43},
"prompt_tokens":66,"total_tokens":109}}
```

1.1 的 SSE 解析器必须：以 `[DONE]` 或流结束作为终止条件，**不能把 `choices: []` 当成结束**；token 计量从该 chunk 取。

### 6.2 401

```json
{"error":{"message":"Incorrect API key provided.","type":"invalid_request_error",
"code":"invalid_api_key"}}
```

### 6.3 Embedding 超批量

```json
{"error":{"message":"<400> InternalError.Algo.InvalidParameter: Value error,
batch size is invalid, it should not be larger than 10.: input.contents",
"type":"InvalidParameter","code":"InvalidParameter"}}
```

1.2 的入库管线必须按 **≤10 条/批**切分。

### 6.4 客户端读超时

本地 mock 服务接受连接但不响应，`curl --max-time 1`：

```
curl: (28) Operation timed out after 1001 milliseconds with 0 bytes received
```

### 6.5 配置切换

- 模型：`qwen3.8-flash` → `qwen3.7-flash`，`git diff --stat` 仅 1 行配置变更，调用成功且响应 `model` 随之变化。
- base-url：改为本地 mock 后启动成功，mock 收到的路径为 `/v1/chat/completions`，无任何代码改动。

## 7. 证据索引

以下文件在 Spike 临时工程中采集，工程已按计划删除；关键原文已内联在本文档。

| 文件 | 内容 |
|---|---|
| `h1-dependency-tree.txt` | 依赖树，Spring AI 2.0.1，无 deepseek |
| `h1-config-binding.json` | 模型与 extra-body 绑定结果 |
| `h2-nonstream.json` / `h2-nonstream-framework.json` | 裸 HTTP 与框架非流式响应 |
| `h3-stream-thinking-off.json` | Spring AI 流式，thinking off |
| `h3-stream-default.json` | Spring AI 流式，默认 thinking |
| `h3-curl-thinking-off.json` / `h3-curl-default.json` | 裸 curl 基线 |
| `h3-stream-usage.md` | 流式 usage 结论 |
| `h4-thinking-comparison.md` / `h4-token-comparison.jsonl` | thinking 对照 |
| `h5-embedding.json` / `h5-embedding-batch10.json` | Embedding 单条与 10 条 |
| `h5-embedding-batch11-error.json` | 超批量报错 |
| `h6-401.json` / `h6-timeout.txt` / `h6-429.json` | 错误形态 |
| `h6-error-mapping.md` | 错误到降级设计的映射 |
| `h7-model-switch.json` / `h7-config-diff.txt` / `h7-baseurl-mock.log` | 配置切换 |

## 8. 风险与后续条件

| 风险 | 处置 |
|---|---|
| **429 未实测** | 1.1 上线前用 ¥0.01 月度预算做一次熔断演练，把真实错误体补进实现与测试 |
| **模型别名漂移** | `qwen3.8-flash` 是别名，供应商可静默升级；1.1 需保留 prompt 回归集，模型变更后重跑 |
| **starter 过度装配** | OpenAI starter 默认装配 audio / image 等模型；1.1 应显式关闭未使用项，避免无关凭据要求 |
| **base-url 路径差异** | 配置值必须含 `/v1`；在 `iboot-ai` 的配置类中加注释与启动校验，避免再次踩 404 |
| **流式结束语义** | usage 位于空 `choices` chunk；解析器需以 `[DONE]` 为终止条件 |
| **单一供应商** | 对话与 Embedding 同属百炼，故障半径重叠；保留 base-url / model 配置化作为切换前提 |

## 9. 对 ROADMAP 的后续修正（不在本变更内）

1. §2.1、§4.1、§6.1、§7.2 中的 DeepSeek 表述改为百炼 `qwen3.8-flash`（备用 `qwen3.7-flash`），Embedding 保持 `text-embedding-v4`。
2. §4.1 "多供应商抽象是架构前提" 的理由由"DeepSeek 无 embedding"改为"单一供应商的故障切换"。
3. R1 更新为"百炼 OpenAI 兼容层"已通过 Spike 验证；新增"模型别名漂移"风险。

## 10. 复现要点

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.ai</groupId>
      <artifactId>spring-ai-bom</artifactId>
      <version>2.0.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

```yaml
spring:
  ai:
    openai:
      api-key: ${DASHSCOPE_CHAT_API_KEY}          # 全局兜底，避免 audio/image 装配失败
      base-url: ${DASHSCOPE_BASE_URL}/v1          # 必须带 /v1
      chat:
        api-key: ${DASHSCOPE_CHAT_API_KEY}
        options:
          model: qwen3.8-flash
          max-tokens: 4096
          extra-body:
            enable_thinking: false
      embedding:
        api-key: ${DASHSCOPE_EMBEDDING_API_KEY}
        options:
          model: text-embedding-v4
          dimensions: 1024
```

运行命令（Key 从环境变量注入，不落盘）：

```bash
set -a; source .env; set +a
mvn -s /home/lam/repo/settings.xml test -Dtest='NonStreamingTest,EmbeddingTest,StreamingTest'
```
