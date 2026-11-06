# Proposal

## Why

`.github/workflows/regress.yml` 的 `full-regress` 任务仍按拆分前的拓扑启动：只起
`iboot-system`、`iboot-ai`、`iboot-gateway` 三个服务，也没有注入 AI 启动必需的
`DASHSCOPE_CHAT_API_KEY`。而 `scripts/regress/regress.sh` 要采集 8 个服务，登录还要走
`iboot-auth`（8083）。因此定时/手动回归会在第一个缺失服务处失败，实际上已长期失效。

`docs/23-微服务底座操作指南.md` 的前置条件也仍写“中间件与两个后端服务”，与现状不一致。

## What Changes

- 重写 `full-regress` 任务：用 `scripts/dev-init.sh` 一次拉起中间件（含 Nacos）、
  `iboot-db-init`、九个业务服务与网关，不再在 YAML 里重复服务启动顺序与健康等待；
  通过 `IBOOT_OPS_ENABLED=false` 跳过运维控制台。
- 注入 CI 专用环境变量：AI dummy key、关闭 mail health、关闭 tracing、开启 backup；
  用 `JAVA_TOOL_OPTIONS` 限制服务堆，避免 runner 内存不足；为 job 增加超时。
- 失败时输出 `logs/*.log`，便于判断是哪个服务未就绪。
- 更新 `docs/23` §1/§2：补充 `path-conservation.sh` 层、改为全栈前置条件、说明备份由
  `iboot-job` 承担。
- 更新 `scripts/README.md`：补充回归四层模型与 PR / 定时任务的分工。

## Capabilities

无。本变更只修 CI 与文档，不改变产品行为；通过 `.openspec.yaml` 的 `skip_specs: true` 跳过 specs。

## Impact

- `.github/workflows/regress.yml`
- `docs/23-微服务底座操作指南.md`
- `scripts/README.md`
- 不改变服务代码、API、数据库结构或运行时依赖。
