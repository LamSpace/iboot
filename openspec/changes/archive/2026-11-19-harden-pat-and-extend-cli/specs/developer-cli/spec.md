# Spec Delta

## MODIFIED Requirements

### Requirement: CLI 可管理个人访问令牌

CLI SHALL 提供 `iboot token create|list|revoke`，使用交互式登录会话管理当前用户自己的 PAT。签发 MUST 支持名称、有效期与可重复的 `--scope` 参数，明文 MUST 只在签发时输出一次；撤销 MUST 按令牌 ID 操作；使用 PAT 调用管理命令 MUST 被服务端拒绝。签发与撤销属于写操作，MUST 遵守 `--yes` 与 `--dry-run`。

#### Scenario: 签发令牌

- **WHEN** 用户执行 `iboot token create ci --expires-days 30 --yes`
- **THEN** CLI 输出令牌明文、ID 与过期时间
- **AND** 后续 `iboot token list` 不再显示明文

#### Scenario: 指定权限范围

- **WHEN** 用户执行 `iboot token create ci --scope job:run --scope backup:trigger --yes`
- **THEN** 创建响应包含这两个 scope
- **AND** 服务端拒绝用户不具备的权限码

#### Scenario: 列出令牌

- **WHEN** 用户执行 `iboot token list --json`
- **THEN** 输出当前用户的令牌元数据（ID、名称、状态、scope、过期时间、最近使用时间）
- **AND** 不包含任何令牌明文

#### Scenario: 撤销令牌

- **WHEN** 用户执行 `iboot token revoke <id> --yes`
- **THEN** 服务端撤销该令牌
- **AND** 使用该令牌的请求随后以认证失效退出码结束

#### Scenario: 演练模式

- **WHEN** 用户执行 `iboot token revoke <id> --dry-run`
- **THEN** CLI 打印方法与路径
- **AND** 不发送写请求

#### Scenario: 缺少确认

- **WHEN** 用户执行签发或撤销但未提供 `--yes`
- **THEN** CLI 拒绝发送请求
- **AND** 以缺少确认的退出码结束

#### Scenario: PAT 不能管理 PAT

- **WHEN** 使用 PAT 作为凭据执行 `iboot token create`
- **THEN** 服务端返回 403
- **AND** CLI 以越权退出码结束

## ADDED Requirements

### Requirement: CLI 列表可自动翻页

分页列表命令 SHALL 支持 `--all`：忽略 `--page`，按固定页大小拉取全部页并合并为一个结果；`--json` 输出 MUST 保持 `total` 与数据数组的结构。总记录数超过 10,000 时 MUST 拒绝并提示缩小筛选范围；未指定 `--all` 时 MUST 保持既有分页行为。

#### Scenario: 拉取全部页

- **WHEN** 用户执行 `iboot user list --all --json`
- **THEN** CLI 合并所有页的数据
- **AND** 输出的 `total` 与完整记录数一致

#### Scenario: 安全上限

- **WHEN** 符合筛选条件的记录数超过 10,000
- **THEN** CLI 拒绝执行并提示增加筛选条件
- **AND** 不输出不完整的结果冒充全部数据

#### Scenario: 保持 JSON 结构

- **WHEN** 用户使用 `--all --json`
- **THEN** 输出仍包含 `total` 与 `data` 数组
- **AND** 与单页输出结构一致

#### Scenario: 默认行为不变

- **WHEN** 用户未指定 `--all`
- **THEN** CLI 只请求 `--page` 指定的一页
- **AND** `--page` 与 `--size` 的语义保持不变

### Requirement: CLI 可等待任务执行结果

`iboot job run <id> --wait` SHALL 在触发前记录该任务最新执行日志，触发后轮询直到出现新的终态日志。成功 MUST 输出该日志并以成功退出码结束；失败 MUST 输出异常信息并以业务错误退出码结束；超时 MUST 停止轮询并提示当前状态。轮询 MUST 使用短请求，不得用单次长请求挂起；`--interval` 与 `--timeout` MUST 可配置。

#### Scenario: 等待执行成功

- **WHEN** 用户执行 `iboot job run 12 --wait --yes`
- **THEN** CLI 触发任务并等待本次执行日志
- **AND** 执行成功时输出日志并以 0 退出

#### Scenario: 等待执行失败

- **WHEN** 本次执行日志的状态为失败
- **THEN** CLI 输出异常信息
- **AND** 以业务错误退出码结束

#### Scenario: 等待超时

- **WHEN** 在配置的时限内没有出现新的终态日志
- **THEN** CLI 停止轮询并提示当前状态
- **AND** 不修改任务或日志

#### Scenario: 只匹配本次触发

- **WHEN** 该任务此前已有失败日志
- **THEN** `--wait` MUST NOT 把旧日志当作本次执行结果
- **AND** 只接受触发后新产生的日志

### Requirement: CLI 输出操作日志认证方式

`iboot log operate` SHALL 输出服务端返回的认证方式字段（`SESSION` / `PAT`），不得丢弃或改写该值。

#### Scenario: JSON 输出包含认证方式

- **WHEN** 用户执行 `iboot log operate --json`
- **THEN** 每条记录包含认证方式
- **AND** 取值与服务端一致

#### Scenario: 表格输出可见

- **WHEN** 用户执行 `iboot log operate`
- **THEN** 表格包含认证方式列
- **AND** 不因缺少该字段而失败
