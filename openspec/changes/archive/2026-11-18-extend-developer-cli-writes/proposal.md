# Proposal

## Why

v0/v1 已经覆盖登录与只读查询，但运维人员在终端仍要退回 `iboot api --yes` 才能执行任务、备份与在线会话操作；这类高频动作缺少专用命令的参数校验、`--wait` 与可验证的审计语义。多环境用户还需要手改 `config.json` 才能新增 context。

## What Changes

- 新增 `iboot job run <id>`：立即执行一次定时任务；执行结果通过 `job logs --job-id` 查看。
- 新增 `iboot job logs --job-id <id>`：按任务过滤执行日志。
- 新增 `iboot backup trigger [--wait]`：手动触发备份；`--wait` 轮询备份记录直到 `SUCCESS`/`FAILED`，支持间隔与超时。
- 新增 `iboot online kick <tokenId>`：按 `online list` 返回的脱敏 tokenId 强制下线。
- 新增 `iboot config add/remove`：新增或删除命名 context，删除当前 context 时拒绝并提示先切换。
- 所有写命令复用现有写保护：默认拒绝，必须 `--yes`；`--dry-run` 只打印请求；`--json` 输出业务数据；继续由服务端 `@Log` 审计。
- 扩展单元测试、`scripts/regress/cli.sh` 与 CLI 操作指南；不新增后端接口。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `developer-cli`: 增加受控运维写命令、备份等待语义与 context 增删能力。

## Impact

- `iboot-cli`：新增 job/backup/online/config 子命令，复用 `BaseCommand` 写保护、`GatewayClient` 与 `Output`。
- `scripts/regress/cli.sh`：增加写命令、权限拒绝、`--dry-run`、审计与备份等待用例。
- 文档：`docs/25-开发者 CLI 操作指南.md`、`README.md`、`ROADMAP.md` 的开发者 CLI 条目。
- 无后端接口、数据库、认证、网关或会话模型改动；不启动 `add-multi-client-sessions`。

**非目标**：PAT/CI 自动化；用户删除、重置密码、角色授予；消息/审批命令；AI 对话/智能体命令；keyring、Windows 适配、自定义 CA 与 GraalVM native。
