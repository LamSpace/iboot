# Spike 报告索引

本目录保存已归档变更的 Spike 报告，是选型与门禁结论的历史证据，不是当前操作指南。
当前版本与配置以 `docs/02-技术栈.md`、`docs/23-微服务底座操作指南.md` 为准。

2026-11-06 清理时删除了可执行 Spike 工程（`spike/`、临时测试类与验证分支），
只保留报告。需要复现时先用 `git log --all -- spike/` 找到历史提交，再取回对应目录；
每份报告的“复现步骤”仍保留原始命令。

## 报告清单

| 报告 | 对应归档变更 | 结论 | 代码去向 |
|---|---|---|---|
| [AI 1.1 Spike 报告](./ai-1.1-spike-report.md) | `2026-10-25-ai-1-1-spike` | Go（有条件）：Spring AI 2.0.1 + 百炼 qwen3.8-flash 在 Boot 4.0.3 / JDK 25 可用 | 仓库外临时工程，已删除 |
| [AI 1.3 Spike 报告](./ai-1.3-spike-report.md) | `2026-10-28-add-agent-orchestration` | Go（有条件）：AgentScope 2.0.1 可用；禁止 `@Tool`，写操作确认需自建 | 临时验证类，已删除 |
| [Flowable 8 Spike 报告](./flowable-8-spike-report.md) | `2026-10-21-flowable-workflow-spike` | Go（有条件）：Flowable 8.0.0 在现有技术栈运行，四项硬门槛通过 | 一次性验证分支，已删除 |
| [知识图谱 1.4 验证报告](./graph-1.4-report.md) | `2026-10-28-add-knowledge-graph` | Neo4j 5.26.29-community + driver 6.0.2 可用；按需启用 | 无独立代码 |
| [阶段 2.0 门禁 Spike 报告](./microservice-2-0-spike-report.md) | `2026-10-31-add-microservice-foundation` | Go：Spring Cloud Alibaba 2025.1.0.0 / Nacos 3.1.1 / Sentinel 与跨服务 TraceID 均通过 | `spike/` 下的 gate-a / gate-b，已删除 |

## 使用约定

- 报告中的版本号是执行当时的事实，不要据此升级或降级依赖。
- 报告中的“复现步骤”是历史记录，路径与端口可能已随服务拆分变化。
- 新 Spike 的报告继续放在本目录，并在上表登记对应变更与代码去向。
