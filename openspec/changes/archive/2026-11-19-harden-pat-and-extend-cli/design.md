# Design

## Context

- PAT 目前由 `PersonalAccessTokenService` 在签发时读取用户、角色与权限，全部写入 `permissions` 声明；内核 `JwtAuthenticationFilter` 直接据此建立 authorities。
- 操作日志由内核 `LogAspect` 记录，`OperateLogRecorder` 异步写入 `sys_operate_log`；监控服务提供查询、导出与清理，系统域只读消费用于仪表盘。
- CLI 的分页命令共用 `PageOptions`（`--page`/`--size`），`BaseCommand.get` 只发单页请求；`JobCommand.RunCommand` 触发后立即返回。
- `JobLog` 的 `status` 为 0（失败）/1（成功），执行完成后由 Quartz 任务写入 `sys_job_log`，可按 `jobId` 分页查询。

## Goals / Non-Goals

**Goals:**

- 用现有权限码表达 PAT 的最小权限，不引入第二套权限模型。
- 审计能区分 PAT 与交互式会话，且不改变既有查询/分页/数据范围语义。
- CLI 能一次取全量列表并等待任务执行结果，不新增后端接口。

**Non-Goals:**

- PAT 权限实时同步、scope 预设分组或按路径授权。
- 管理员查看/撤销他人 PAT。
- 管理类 CRUD 命令、MCP、`add-multi-client-sessions`。

## Decisions

### D1：scope 取值为权限码，签发时校验子集

PAT 创建请求新增 `scopes`（权限码列表）：

- 空列表 = 继承用户全部权限，保持兼容；
- 非空时逐项校验必须存在于用户当前权限集合，未知或未持有的权限码直接拒绝签发；
- 去重、trim，单次最多 50 个；
- 有效权限 = 用户权限快照 ∩ scopes；由于已校验子集，实际等于 scopes；
- `scopes` 写入 JWT 声明与 `sys_user_pat.scopes`，列表返回。

内核 PAT 分支再做一次交集：若令牌带非空 `scopes` 声明，则 `permissions` 只保留与 `scopes` 的交集。这样即使签发侧出现回归，资源服务也不会授予超出 scope 的权限。不带 `scopes` 的旧 PAT 继续按 `permissions` 执行。

备选方案：按 API 路径前缀授权。实现更简单，但与 RBAC 权限码脱节，且同一路径下可能混有读写操作，语义不够精确，不采用。

### D2：审计新增 `auth_type` 字段

`sys_operate_log` 增加 `auth_type varchar(16) NOT NULL DEFAULT 'SESSION'`：

- `LogAspect` 通过 `SecurityUtils.isPersonalAccessToken()` 判断当前主体，写入 `PAT` 或 `SESSION`；
- `OperateLog` 与 `OperateLogRecorder` 增加字段与插入列；
- 监控服务的 PO、ResultMap、响应 DTO、导出 VO 与控制器透传该字段；
- 系统域仪表盘的最近操作日志 PO 与响应同步增加，避免两套读取模型分叉；
- 门户操作日志列表增加认证方式列，中英文案同步；
- 旧记录由数据库默认值解释为 `SESSION`。

滚动发布期间，未升级实例写入的记录会使用数据库默认值 `SESSION`，可能把少量 PAT 操作标错；升级完成后恢复准确。该窗口在迁移计划中说明。

备选方案：复用 `operator_type` 或拼接 `operator_name`。前者语义不符，后者破坏查询与展示，不采用。

### D3：`--all` 在 CLI 侧合并分页

- `PageOptions` 增加 `--all`；`BaseCommand` 增加分页拉取方法。
- `--all` 忽略 `--page`，固定每页 100 条，读取第一页的 `total` 后继续请求直到取完。
- 合并后的结果保持 `PageResult` 形状（`total`、`data`、`pageNum=1`、`pageSize=100`），`--json` 与单页输出结构一致。
- 总数超过 10,000 时直接拒绝并提示增加筛选条件，避免无界请求与内存膨胀。
- 只对混入 `PageOptions` 的命令生效；不新增后端参数或接口。

备选方案：让后端支持 `pageSize=-1` 或导出接口。需要改多个服务且扩大单次响应体，CLI 侧合并对现有接口无侵入，故不采用。

### D4：`job run --wait` 以日志 ID 为基线轮询

- 触发前先查询该任务最新一条执行日志，记录最大 `id`（无日志时为 0）。
- 触发成功后，按固定间隔查询 `GET /api/job/log/list?jobId=<id>&pageNum=1&pageSize=1`；
- 出现 `id` 大于基线的记录即视为本次执行结果，按 `status`（1 成功、0 失败）结束；
- 失败时输出 `exceptionInfo` 或 `jobMessage`；超时输出当前状态；
- 默认间隔 2 秒、超时 300 秒，支持 `--interval`/`--timeout`；
- 全部使用短请求，不引入服务端长连接或新接口。

备选方案：修改 `POST /api/job/{id}/run` 返回日志 ID。触发接口当前返回 `Result<Void>`，改返回值会影响接口契约与前端；基线轮询保持后端不变，故不采用。

## Risks / Trade-offs

- **scope 为静态快照** → 角色或权限调整后需撤销重建；文档与错误提示明确说明。
- **旧 PAT 无 scope** → 继续保持全部权限；升级后建议为 CI 重新签发受限令牌。
- **滚动发布期间审计标记可能短暂偏差** → 数据库默认 `SESSION`，升级完成后恢复；迁移计划说明。
- **`--all` 期间数据可能变化** → 只保证尽力而为的一致性，不承诺跨页快照；超过上限直接拒绝，不返回不完整结果。
- **同名任务并发执行** → 基线取最大日志 ID，可能把并发产生的另一条日志当作本次结果；任务执行通常串行，风险可接受，文档说明。
- **`auth_type` 增加写入列** → 所有使用内核记录器的服务必须同批升级；旧版本插入依赖数据库默认值，不会失败。

## Migration Plan

1. `V14__personal_access_token_scopes.sql` 为 `sys_user_pat` 增加 `scopes`（可空）。
2. `V15__operate_log_auth_type.sql` 为 `sys_operate_log` 增加 `auth_type`，默认 `SESSION`。
3. 先发布内核、认证、监控与系统服务，再发布门户与 CLI；滚动窗口内旧实例的审计记录按 `SESSION` 解释。
4. 升级完成后，为 CI 重新签发带 scope 的 PAT；旧 PAT 在到期或主动撤销前仍按全部权限执行。
5. 回滚：新增列可保留，旧版本忽略；CLI 的 `--all`/`--wait` 为客户端功能，回滚 CLI 即可。

## Open Questions

无。
