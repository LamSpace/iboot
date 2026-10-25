# Tasks

## 1. 临时工程与配置基线

- [x] 1.1 在 `/tmp/iboot-ai-spike` 建立独立 Maven 工程（`spring-boot-starter-parent:4.0.3` + `spring-ai-bom:2.0.1` + `spring-ai-starter-model-openai` + `spring-boot-starter-test`）；用 `mvn -s /home/lam/repo/settings.xml dependency:tree` 验证解析出 Spring AI 2.0.1，且不出现 deepseek starter
- [x] 1.2 编写 `application.yml`（base-url、`model: qwen3.8-flash`、`extra-body.enable_thinking: false`）；用一个不联网的上下文测试断言 `OpenAiChatOptions` 绑定后的 model 与 extraBody 值，验证配置确实进入 Bean
- [x] 1.3 确认运行方式：`set -a; source /home/lam/workspace/iboot/.env; set +a` 后 `DASHSCOPE_CHAT_API_KEY` / `DASHSCOPE_EMBEDDING_API_KEY` 可用；命令只打印前 7 位前缀，验证不泄露完整 Key

## 2. H1 / H2：非流式对话

- [x] 2.1 编写非流式调用测试，断言响应 `model` 回显 `qwen3.8-flash`、`content` 非空、`usage.prompt_tokens` 与 `completion_tokens` 均 > 0；`mvn test -Dtest=NonStreamingTest` 通过
- [x] 2.2 将完整原始响应（含 `usage`、`model`、`system_fingerprint` 若有）保存到 `evidence/h2-nonstream.json`，供报告引用；验证文件存在且 JSON 可解析
- [x] 2.3 记录实际使用的端点与 Key 前缀（脱敏），保存到 `evidence/h1-wiring.txt`；验证其中不含完整 Key

## 3. H3 / H4：流式与 thinking 对照

- [x] 3.1 实现流式调用：订阅 Spring AI 的 `Flux`，以单调时钟记录"请求发出 → 第一个非空 `delta.content`"的耗时，连续 10 次（丢弃首次），输出 p50 / p95、chunk 数与是否含 `usage`；结果写入 `evidence/h3-stream-thinking-off.json`
- [x] 3.2 同一提示词、不传 `enable_thinking` 再跑 10 次，记录相同指标，写入 `evidence/h3-stream-default.json`
- [x] 3.3 用裸 `curl -N` 对同一提示词跑 10 次作为基线，写入 `evidence/h3-curl-baseline.json`；验证三组的测量口径一致（均为首个非空 content 块）
- [x] 3.4 生成 thinking 关 / 默认 / 裸 curl 三组对照表（延迟、completion_tokens、是否出现 `reasoning_content`），写入 `evidence/h4-thinking-comparison.md`；验证若默认组出现思维链或显著更慢，报告必须据此给出"1.1 必须显式关闭 thinking"的结论
- [x] 3.5 确认流式响应最后一个 chunk 是否携带 `usage`，把结论写入 `evidence/h3-stream-usage.md`；验证结论明确到"有 / 无 / 需额外统计"

## 4. H5：Embedding

- [x] 4.1 调用 `text-embedding-v4` 单条输入，断言返回维度为 1024、`usage.prompt_tokens` > 0；结果写入 `evidence/h5-embedding.json`
- [x] 4.2 一次请求提交 10 条输入，断言行数与顺序正确；结果写入 `evidence/h5-embedding-batch10.json`
- [x] 4.3 一次请求提交 11 条输入，捕获并记录实际报错（状态码 + 错误体）；写入 `evidence/h5-embedding-batch11-error.json`，验证报告能据此确定 1.2 的分批大小

## 5. H6：错误形态

- [x] 5.1 用一把故意写错的 Key 发起对话请求，记录状态码与完整错误体，写入 `evidence/h6-401.json`；验证状态码为 401 或 403
- [x] 5.2 尝试复现 429：把百炼月度预算临时调到最低并触发一次调用，或捕获真实限流响应；写入 `evidence/h6-429.json`。若当天无法复现，在报告中明确标注"未实测，仅有文档依据"，不得编造
- [x] 5.3 把客户端读超时设到 1ms 触发超时，记录异常类型与堆栈摘要，写入 `evidence/h6-timeout.txt`；验证报告能区分"客户端超时"与"服务端 5xx"
- [x] 5.4 产出错误映射表（401 → 快速失败；429 → 退避重试；超时 → 降级提示），写入 `evidence/h6-error-mapping.md`；验证每条都能对应到 1.1 的降级设计

## 6. H7：配置切换

- [x] 6.1 仅修改配置文件，把对话模型从 `qwen3.8-flash` 切到 `qwen3.7-flash`，跑一次非流式调用，断言响应 `model` 随之变化；结果写入 `evidence/h7-model-switch.json`
- [x] 6.2 仅修改 `base-url`（改回默认值或等价端点），确认无需改代码即可启动并发起调用；用 `git diff --stat` 证明临时工程中没有代码变更（只有配置文件）

## 7. 报告与交付

- [x] 7.1 编写 `doc/spike/ai-1.1-spike-report.md`，结构对齐 `doc/spike/flowable-8-spike-report.md`：结论、版本事实、验证环境、硬门槛逐项（H1–H7）、证据索引、风险与 Go / No-Go 建议
- [x] 7.2 核对报告：H1–H7 每条都有明确结论与对应 `evidence/` 文件；用脚本检查无遗漏章节，且报告中不出现完整 API Key
- [x] 7.3 删除 `/tmp/iboot-ai-spike`；验证目录已不存在，仓库中只新增 `openspec/changes/ai-1-1-spike/` 与 `doc/spike/ai-1.1-spike-report.md`
- [x] 7.4 从 `master` 切出 `docs/ai-1.1-spike` 分支，提交变更文档与报告，`gh pr create --base master` 创建 PR 并按 CLAUDE.md 流程合并；验证 PR 已合并、本地 `master` 已 `pull --ff-only`
