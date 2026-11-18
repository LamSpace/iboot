# Spec Delta

## Purpose

为开发者与本地自动化提供一个非浏览器的命令行调用面：复用现有 OAuth2 身份、RBAC 数据权限与审计，在终端完成登录、查询与排障，不引入新的权限模型或旁路接口。

## ADDED Requirements

### Requirement: CLI 使用标准授权码登录

CLI SHALL 通过系统浏览器完成 OAuth2 授权码 + PKCE 登录；CLI MUST NOT 接收或保存用户密码，令牌 MUST 以仅当前用户可读的权限保存。

#### Scenario: 浏览器登录

- **WHEN** 用户执行 `iboot login`
- **THEN** CLI 打开系统浏览器并完成授权码 + PKCE 流程
- **AND** 密码只在认证服务的登录页输入

#### Scenario: 凭据落盘权限

- **WHEN** 登录成功
- **THEN** 访问令牌与身份令牌写入凭据文件
- **AND** 服务端返回刷新令牌时一并写入，当前公共客户端不会返回刷新令牌
- **AND** 文件权限为 0600，日志与终端输出不含令牌原文

### Requirement: 令牌到期与撤销有确定行为

CLI SHALL 依赖服务端共享会话的滑动续期；会话有效期内 MUST NOT 因 JWT `exp` 接近到期而主动重新登录。仅当服务端确实返回刷新令牌时，CLI 才单飞续期一次；否则 401 表示会话已失效，MUST 以认证失效退出码结束并提示重新登录。

#### Scenario: 会话有效期内持续成功

- **WHEN** 用户持续操作超过 JWT 的 `exp`
- **THEN** 请求继续成功
- **AND** CLI 不提示重新登录

#### Scenario: 无刷新令牌时会话失效

- **WHEN** 请求返回 401 且凭据中没有刷新令牌
- **THEN** CLI 以认证失效退出码结束
- **AND** 提示用户重新登录

#### Scenario: 有刷新令牌时单飞续期

- **WHEN** 请求返回 401 且凭据中存在刷新令牌
- **THEN** CLI 只续期一次并重放请求
- **AND** 新的刷新令牌被持久化

#### Scenario: 并发刷新

- **WHEN** 多个 CLI 进程同时持有同一刷新令牌
- **THEN** 刷新在文件锁内完成
- **AND** 不会因 refresh token 轮换互相作废

### Requirement: CLI 提供只读查询命令

CLI SHALL 提供按资源分组的只读查询命令，至少覆盖用户、角色、部门、字典、参数、任务、备份、登录/操作日志与在线会话；默认输出人类可读表格。

#### Scenario: 查询用户

- **WHEN** 用户执行 `iboot user list`
- **THEN** 输出用户列表表格
- **AND** 数据与门户同页一致

#### Scenario: 受限数据范围

- **WHEN** 数据范围被限制为“本人”的用户执行同一查询
- **THEN** 输出只包含该用户可见的数据

### Requirement: JSON 输出是解包后的业务数据

CLI SHALL 在 `--json` 时输出解包后的业务 `data`；无论后端返回 CloudEvents 信封还是直接 DTO，输出结构 MUST 一致。

#### Scenario: CloudEvents 服务

- **WHEN** 查询返回 CloudEvents 信封
- **THEN** `--json` 输出内部的 `Result.data`
- **AND** 不输出信封字段

#### Scenario: 直接 DTO 服务

- **WHEN** 查询 `iboot-ai` 等直接返回 DTO 的服务
- **THEN** `--json` 原样输出 DTO

### Requirement: 逃生口不绕过写保护

CLI SHALL 提供 `iboot api` 调用任意已存在接口；非幂等方法 MUST 要求 `--yes`，`--dry-run` MUST 只打印请求而不发送。

#### Scenario: 只读逃生口

- **WHEN** 执行 `iboot api GET /api/user/list`
- **THEN** 请求被发送并按默认规则解包输出

#### Scenario: 写操作缺少确认

- **WHEN** 执行 `iboot api DELETE /api/user/1` 且未提供 `--yes`
- **THEN** CLI 拒绝发送请求
- **AND** 以缺少确认的退出码结束

#### Scenario: 演练模式

- **WHEN** 执行写操作并带上 `--dry-run`
- **THEN** CLI 打印方法、路径与请求体
- **AND** 不产生任何写请求，操作日志无新增

### Requirement: 权限判定只在服务端进行

CLI MUST NOT 在本地复刻功能权限或数据权限；越权请求 MUST 原样得到服务端的 403，并以越权退出码结束。

#### Scenario: 无功能权限

- **WHEN** 用户没有 `user:list` 权限却执行 `iboot user list`
- **THEN** 服务端返回 403
- **AND** CLI 以越权退出码结束，不重试

### Requirement: CLI 登录不干扰浏览器会话（过渡期）

在 `add-multi-client-sessions` 落地前，CLI 的授权码登录 MUST NOT 驱逐浏览器会话，也 MUST NOT 写入 `online_token:<userId>`；CLI 会话因此不出现在在线列表，管理员按用户强制下线不覆盖该会话。

#### Scenario: 门户会话保持

- **WHEN** 用户在门户已登录的情况下执行 `iboot login`
- **THEN** 门户的现有会话不被删除
- **AND** 门户下一次请求不因 CLI 登录而重新登录

#### Scenario: 在线列表不显示 CLI 会话

- **WHEN** 管理员打开在线用户页面
- **THEN** 列表不包含 CLI 会话
- **AND** 该限制记录在 CLI 操作指南中

### Requirement: 多上下文隔离

CLI SHALL 支持命名 context（至少包含 base URL 与 issuer），凭据 MUST 按 context 隔离；切换 context 不得复用其他环境的令牌。

#### Scenario: 切换环境

- **WHEN** 用户执行 `iboot config use dev`
- **THEN** 后续请求使用 dev 的 base URL 与凭据
- **AND** 其他 context 的凭据不受影响

### Requirement: 错误以可区分的退出码表达

CLI SHALL 用退出码区分成功、用法错误、认证失效、越权、网络/5xx、业务错误与缺少确认；错误信息 MUST 不包含令牌或凭据内容。

#### Scenario: 网络失败

- **WHEN** 网关不可达
- **THEN** CLI 以网络错误退出码结束
- **AND** 输出可读的连接失败原因

#### Scenario: 业务错误

- **WHEN** 服务返回业务码非 200
- **THEN** CLI 以业务错误退出码结束
- **AND** 输出业务消息
