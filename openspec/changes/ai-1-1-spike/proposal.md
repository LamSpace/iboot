# Proposal

## Why

阶段 1.1 的底座前提是 **Spring AI 2.0.1 + Spring Boot 4.0.3 + JDK 25**，对话与向量化统一走百炼（`qwen3.8-flash` + `text-embedding-v4`）。目前只验证了 BOM 可以解析、`curl` 直连可以调通；尚未验证：

- Spring AI 的 OpenAI 自动装配能否在 Boot 4.0.3 上正常启动；
- `enable_thinking` 这类非标准参数能否通过 `extra-body` 真正进入请求体；
- 关闭思考模式后，流式首 token 是否满足 ROADMAP 的 **< 3s** 判据；
- embedding 的 10 条/请求批量上限与 1024 维是否按预期工作；
- 401 / 429 / 超时分别返回什么形态，能否支撑 1.1 的降级设计。

这些事实直接决定 `iboot-ai` 的写法。先写代码再发现框架层不兼容，返工成本远高于一次时间盒 Spike。

## What Changes

- 新增一次有边界的技术 Spike，**不引入任何生产代码、不创建 Maven 模块**：
  - 在仓库外的临时工程中引入 `spring-ai-bom:2.0.1` 与 `spring-ai-starter-model-openai`，`base-url` 指向百炼 OpenAI 兼容端点；
  - 验证非流式对话：模型回显 `qwen3.8-flash`，`usage` 含 prompt / completion tokens；
  - 验证流式对话：增量到达，记录首 token 延迟（thinking 开 / 关各 10 次，取 p50 / p95）；
  - 验证 `enable_thinking: false` 通过 `extra-body` 生效：对比有无思维链、输出 token 与延迟差异；
  - 验证 `text-embedding-v4`：1024 维、10 条批量成功、第 11 条的报错形态；
  - 验证错误可辨识：无效 Key → 401、预算熔断或限流 → 429、超时可捕获；
  - 验证只改配置即可从 `qwen3.8-flash` 切到备用模型。
- 产出 `doc/spike/ai-1.1-spike-report.md`：版本事实、硬门槛逐项结论、实测指标、风险与 Go / No-Go 建议。
- **不改动** `pom.xml`、`iboot-admin`、`iboot-portal` 或 `ROADMAP.md`；`iboot-ai` 骨架与文档修正留到 Spike 通过后的独立变更。

## Capabilities

### New Capabilities

无。本变更不改变任何可观测的系统行为：它是一次技术可行性验证，因此 `.openspec.yaml` 标记 `skip_specs: true`。AI 工作台的能力需求由 1.1 实现阶段的独立变更引入。

### Modified Capabilities

无。现有 6 个能力（`approval-workflow`、`content-search`、`data-backup`、`file-storage`、`notification`、`portal-design-system`）的需求均不受影响。

## Impact

- **文档**：新增 `doc/spike/ai-1.1-spike-report.md`；`ROADMAP.md` 不在本变更内修改。
- **构建**：仓库外临时工程；主干 `pom.xml` 与模块列表零变化，`mvn verify` 行为不变。
- **运行时**：不新增进程、不新增中间件；不需要 MySQL / Redis / ES / Docker。
- **外部依赖**：百炼 OpenAI 兼容端点与两把已配置的 API Key（经 `.env` 注入，不入仓）；Spike 总消耗预计 < ¥0.1。
- **风险**：若 H1（框架装配）不通过，1.1 需改用原生 HTTP 客户端承载模型调用，方案与工期都要重估。
