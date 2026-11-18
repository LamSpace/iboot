# Spec Delta

## ADDED Requirements

### Requirement: CLI 可管理个人访问令牌

CLI SHALL 提供 `iboot token create|list|revoke`，使用交互式登录会话管理当前用户自己的 PAT。签发 MUST 支持名称与有效期参数，明文 MUST 只在签发时输出一次；撤销 MUST 按令牌 ID 操作；使用 PAT 调用管理命令 MUST 被服务端拒绝。签发与撤销属于写操作，MUST 遵守 `--yes` 与 `--dry-run`。

#### Scenario: 签发令牌

- **WHEN** 用户执行 `iboot token create ci --expires-days 30 --yes`
- **THEN** CLI 输出令牌明文、ID 与过期时间
- **AND** 后续 `iboot token list` 不再显示明文

#### Scenario: 列出令牌

- **WHEN** 用户执行 `iboot token list --json`
- **THEN** 输出当前用户的令牌元数据（ID、名称、状态、过期时间、最近使用时间）
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

### Requirement: CI 可从标准输入注入令牌

CLI SHALL 支持从标准输入读取访问令牌（`--token-stdin`），其优先级高于 `IBOOT_TOKEN` 与本地凭据。读取后 MUST NOT 回显令牌、写入日志或持久化到凭据文件。

#### Scenario: 标准输入注入

- **WHEN** 用户执行 `iboot user list --token-stdin < token.txt`
- **THEN** CLI 使用标准输入中的令牌发起请求
- **AND** 不读取本地凭据

#### Scenario: 不回显令牌

- **WHEN** 通过标准输入使用令牌执行命令
- **THEN** 终端输出不包含令牌原文
- **AND** 凭据文件保持不变

#### Scenario: 优先级

- **WHEN** 同时设置 `IBOOT_TOKEN` 与 `--token-stdin`
- **THEN** CLI 使用标准输入中的令牌
