# Proposal

## Why

PAT 已经是 CI 的长期凭据，但目前继承用户全部权限：一个只需触发备份的流水线令牌，同时拥有用户、角色与文件管理能力，泄漏后的影响面过大。

同时，操作日志只记录操作人，无法区分一次备份是人工在门户操作还是 CI 用 PAT 触发；审计人员需要额外比对时间与来源。

CLI 还有两个日常摩擦：列表命令只能逐页取数，脚本需要自己处理分页；`job run` 只返回“已触发”，无法等待执行结果，CI 只能轮询日志。

## What Changes

- **PAT 最小权限**：签发时可为 PAT 指定权限码集合（scope），必须是用户当前权限的子集；PAT 的有效权限为 `用户权限快照 ∩ scope`。未指定 scope 时保持现状（继承全部权限）。scope 写入令牌声明与元数据，`token list` 可见；内核验证时再次取交集，避免签发侧回归。
- **审计区分认证方式**：`sys_operate_log` 新增 `auth_type`（`SESSION` / `PAT`），内核日志切面按当前主体写入；监控查询、导出、门户口径与 CLI `log operate` 均返回该字段。
- **CLI 自动翻页**：分页命令增加 `--all`，按固定页大小拉取全部页并合并，设置 10,000 条安全上限；不改变后端接口。
- **任务执行等待**：`job run <id> --wait` 在触发前记录该任务最新日志 ID，触发后轮询直到出现新的终态日志，成功输出日志、失败以业务错误退出；支持 `--interval` 与 `--timeout`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `personal-access-token`: 增加 scope 的签发、子集校验、有效权限取交集与展示契约。
- `operation-logging`: 审计记录新增认证方式字段，写入方必须在记录中区分 PAT 与交互式会话。
- `ops-monitoring`: 操作日志查询与导出返回认证方式，口径与写入方一致。
- `developer-cli`: 增加 `--all` 翻页、`job run --wait`、token scope 参数与 `log operate` 认证方式展示。

## Impact

- `iboot-db-init`：新增迁移，为 `sys_user_pat` 增加 `scopes`，为 `sys_operate_log` 增加 `auth_type`。
- `iboot-kernel`：`OperateLog` 与 `OperateLogRecorder` 增加 `authType`，`LogAspect` 按主体写入；PAT 验证分支按 scope 与权限取交集。
- `iboot-auth`：PAT 请求 DTO、服务、元数据仓储与响应增加 scopes；签发时校验子集。
- `iboot-monitor`：操作日志 PO、Mapper、Repository、响应 DTO、导出 VO 与控制器增加 `authType`。
- `iboot-system`：仪表盘最近操作日志的 PO 与响应增加 `authType`（只读消费）。
- `iboot-portal`：操作日志列表增加认证方式列与文案（中/英）。
- `iboot-cli`：`token create --scope`、`token list` 展示；`PageOptions`/`BaseCommand` 增加 `--all`；`JobCommand.RunCommand` 增加 `--wait`；`log operate` 输出认证方式。
- 文档与回归：`docs/25`、`docs/26`、CLI SKILL.md、`scripts/regress/cli.sh`。

**非目标**：PAT 权限实时同步；scope 预设分组或按路径授权；管理员查看/撤销他人 PAT 的页面；管理类 CRUD 命令；MCP 与 Web 智能体通道；`add-multi-client-sessions`。
