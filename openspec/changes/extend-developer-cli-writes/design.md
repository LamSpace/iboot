# Design

## Context

See `proposal.md` for motivation and `specs/developer-cli/spec.md` for the behavior contract.

当前 CLI 已有可直接复用的基础设施：

- `BaseCommand.write(...)` 已实现 `--yes` 与 `--dry-run` 写保护；
- `GatewayClient` 统一携带 Bearer、`Accept-Version: 1` 与 `X-Client: iboot-cli`；
- `ConfigStore.put(...)` 已能新增/替换 context，`CredentialStore.delete(...)` 可清理凭据；
- 后端接口已存在：`POST /api/job/{id}/run`、`GET /api/job/log/list?jobId=`、`POST /api/monitor/backup/records`、`GET /api/monitor/backup/records/{id}`、`DELETE /api/online/{tokenId}`。

因此本变更只改 `iboot-cli`，不改后端。

## Goals / Non-Goals

**Goals:**

- 为高频运维动作提供带参数校验、`--yes`、`--dry-run` 与 `--json` 的专用命令；
- 让备份触发可以等待到确定终态，同时不占用单个长 HTTP 请求；
- 补齐 context 的新增与删除，使多环境使用不必手改 `config.json`。

**Non-Goals:**

- PAT/CI 自动化；用户删除、重置密码、角色授予；消息/审批命令；AI 命令；
- 修改后端接口、数据库、认证或会话模型；
- 为 `job run` 做运行级等待或结果关联（日志过滤已足够）。

## Decisions

### D1. 命令形态与后端接口一一对应

| 命令 | 请求 | 说明 |
|---|---|---|
| `job run <id>` | `POST /api/job/{id}/run` | 返回 `Result<Void>`，命令只报告已触发 |
| `job logs --job-id <id>` | `GET /api/job/log/list?jobId=<id>` | 复用只读日志接口，仅增加过滤参数 |
| `backup trigger [--wait]` | `POST /api/monitor/backup/records` | 返回 `BackupRecordResponse`，含 `id` 与 `status` |
| `online kick <tokenId>` | `DELETE /api/online/{tokenId}` | `tokenId` 来自 `online list` 的脱敏值 |
| `config add/remove` | 本地 | 不经过网关 |

**Alternatives considered:** 继续只用 `iboot api`。实现最快，但缺少参数校验、等待语义与可读输出，且难以在回归中稳定断言。

### D2. 备份等待采用轮询，不挂起单请求

`backup trigger --wait` 触发后读取返回记录 ID，按固定间隔调用
`GET /api/monitor/backup/records/{id}`，直到 `status` 为 `SUCCESS` 或 `FAILED`。

- 默认间隔 2 秒，默认超时 300 秒，可通过 `--interval` / `--timeout` 覆盖；
- 超时以业务错误退出码结束，并提示最后一次状态；
- `--wait` 失败（`FAILED`）以业务错误退出码结束，输出脱敏后的错误信息；
- 不使用单个长请求，避免网关 10 秒响应超时。

**Alternatives considered:** 用 `JobApiClient` 式的长轮询端点。当前备份接口没有该端点，新增后端接口超出本变更范围。

### D3. `job run` 不等待，结果通过日志过滤查看

任务立即执行接口返回 `Result<Void>`，没有运行 ID；本变更不尝试从日志中猜测某一次运行。

- `job run` 只报告“已触发”；
- `job logs --job-id` 让用户查看该任务最近的执行记录；
- 文档说明日志可能有秒级延迟。

**Alternatives considered:** `job run --wait` 轮询日志。需要时间窗口与运行关联，容易误判；本变更不做。

### D4. `online kick` 直接使用 `online list` 的脱敏 tokenId

在线列表返回脱敏 `tokenId`，强制下线接口按同一脱敏值查找完整 Redis 键，因此 CLI 原样传递即可。

**Alternatives considered:** `online kick --username`。需要先查询在线列表并按用户名消歧；留到后续，不在 v1 写命令中引入隐式查询。

### D5. context 增删复用本地存储，删除当前 context 时拒绝

- `config add <name> --base-url <url> [--issuer <url>] [--client-id <id>] [--redirect-uri <uri>] [--use]`：
  - `issuer` 省略时回落到 `base-url`；
  - `client-id` 省略时使用 `iboot-cli`；
  - `--use` 时同时切换当前 context。
- `config remove <name>`：
  - 当前 context 拒绝删除，提示先切换；
  - 删除配置后同时删除该 context 的本地凭据；
  - 不要求 `--yes`：操作本地、可逆，且不会触达服务端。

**Alternatives considered:** 只提供 `config add`。删除仍需手改文件；补齐删除更完整。

### D6. 写命令复用现有写保护与输出

- 所有运维写命令调用 `BaseCommand.write(...)`，默认拒绝，必须 `--yes`；
- `--dry-run` 只打印方法、路径、请求体，不发送；
- `--json` 输出解包后的业务数据；`backup trigger` 的 JSON 至少包含 `id` 与 `status`；
- 服务端 `@Log` 继续记录操作人、方法与参数；CLI 已发送 `X-Client: iboot-cli`。

### D7. 测试分层

- 单元测试：命令参数、写保护、`--dry-run`、备份等待状态机（用 JDK `HttpServer` 模拟 RUNNING → SUCCESS/FAILED/超时）；
- 全栈回归：管理员执行 `job run`、`online kick`；受限用户得到 403；审计日志可检索；
- 备份用例在宿主机/CI（`IBOOT_BACKUP_ENABLED=true`）验证成功/失败；Docker 环境验证“备份未启用”被映射为业务错误；
- 扩展 `scripts/regress/cli.sh` 并接入现有 CI。

## Risks / Trade-offs

- **备份功能在 Docker 默认关闭** → 回归区分环境：Docker 只断言错误映射，完整等待链路在宿主机/CI 验证。
- **轮询超时或间隔设置不当** → 提供默认值与显式参数；超时后不修改备份记录。
- **任务日志延迟导致刚执行的任务暂时查不到** → 文档说明；`job logs --job-id` 可重复执行。
- **脱敏 tokenId 复制错误** → 错误由服务端返回“会话不存在或已过期”，CLI 以业务错误退出。
- **删除 context 时凭据残留** → 先保存配置，再删除该 context 的本地凭据；失败时提示手动清理路径。
- **范围膨胀** → 不新增后端接口、不引入 PAT、不做用户/角色破坏性写命令。

## Migration Plan

1. 新增命令与测试，不改变现有命令行为；
2. 发布新的 CLI jar 与 `bin/iboot` 启动脚本；
3. 回滚只需回退 CLI 模块；本地配置与凭据格式不变，已有 context 不受影响。

## Open Questions

无。`job run --wait`、`online kick --username`、`config remove --yes` 均明确留到后续，不影响本变更的规格与任务拆分。
