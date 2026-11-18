# Spec Delta

## ADDED Requirements

### Requirement: CLI 提供受控运维写命令

CLI SHALL 提供任务立即执行、备份触发与在线会话强制下线命令；这些命令默认拒绝执行，必须带 `--yes`，`--dry-run` MUST 只打印请求而不发送。

#### Scenario: 立即执行任务

- **WHEN** 用户执行 `iboot job run <id> --yes`
- **THEN** CLI 调用任务立即执行接口并输出成功结果
- **AND** 执行记录可通过 `iboot job logs --job-id <id>` 查看

#### Scenario: 触发备份

- **WHEN** 用户执行 `iboot backup trigger --yes`
- **THEN** CLI 调用手动备份接口并输出备份记录 ID 与状态

#### Scenario: 强制下线在线会话

- **WHEN** 用户执行 `iboot online kick <tokenId> --yes`
- **THEN** CLI 调用在线会话强制下线接口
- **AND** 该 tokenId 来自 `iboot online list` 的输出

#### Scenario: 写操作缺少确认

- **WHEN** 用户执行任一运维写命令但未提供 `--yes`
- **THEN** CLI 拒绝发送请求
- **AND** 以缺少确认的退出码结束

#### Scenario: 演练模式

- **WHEN** 用户执行运维写命令并带上 `--dry-run`
- **THEN** CLI 打印方法、路径与请求体
- **AND** 不产生写请求，操作日志无新增

#### Scenario: 无写权限

- **WHEN** 用户缺少对应权限却执行运维写命令
- **THEN** 服务端返回 403
- **AND** CLI 以越权退出码结束，不重试

### Requirement: 备份触发可等待终态

CLI SHALL 支持 `backup trigger --wait`：触发后轮询备份记录，直到状态为 `SUCCESS` 或 `FAILED`；等待 MUST 通过轮询实现，不得用单个长请求挂起。

#### Scenario: 备份成功

- **WHEN** 用户执行 `iboot backup trigger --wait --yes` 且备份完成
- **THEN** CLI 输出最终状态 `SUCCESS` 与备份记录 ID
- **AND** 退出码为成功

#### Scenario: 备份失败

- **WHEN** 等待的备份记录进入 `FAILED`
- **THEN** CLI 输出最终状态 `FAILED` 与可读错误信息
- **AND** 使用可区分的业务错误退出码

#### Scenario: 等待超时

- **WHEN** 备份在配置的等待时限内未进入终态
- **THEN** CLI 停止轮询并提示当前状态
- **AND** 不删除或修改该备份记录

### Requirement: 任务执行日志可按任务过滤

CLI SHALL 支持 `job logs --job-id <id>`，把任务 ID 作为查询条件传给日志接口，只返回该任务的执行记录。

#### Scenario: 按任务过滤

- **WHEN** 用户执行 `iboot job logs --job-id <id>`
- **THEN** 输出只包含该任务的执行日志
- **AND** 结果字段与门户任务日志页一致

### Requirement: CLI 可管理 context

CLI SHALL 支持新增与删除命名 context；新增时至少保存 base URL 与 issuer，删除当前 context 时 MUST 拒绝并提示先切换。

#### Scenario: 新增 context

- **WHEN** 用户执行 `iboot config add prod --base-url https://iboot.example.com`
- **THEN** 新 context 写入配置文件
- **AND** 可通过 `iboot config use prod` 切换

#### Scenario: 删除非当前 context

- **WHEN** 用户执行 `iboot config remove <name>` 且该 context 不是当前 context
- **THEN** 该 context 被移除
- **AND** 其他 context 与凭据不受影响

#### Scenario: 拒绝删除当前 context

- **WHEN** 用户尝试删除当前 context
- **THEN** CLI 拒绝操作
- **AND** 提示先切换到其他 context
