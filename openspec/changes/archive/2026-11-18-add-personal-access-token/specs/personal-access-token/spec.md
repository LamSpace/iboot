# Spec Delta

## Purpose

为无浏览器环境提供长期有效、可撤销、可审计的个人访问令牌，复用既有用户身份、RBAC 与数据权限，不引入新的权限模型。

## ADDED Requirements

### Requirement: PAT 由用户自助签发与撤销

用户 SHALL 能在已登录状态下签发、查看与撤销自己的个人访问令牌（PAT）。创建 MUST 指定名称与有效期；默认 30 天、最长 90 天，越界 MUST 被拒绝；每用户活动令牌数量 MUST 有上限（默认 10）。令牌明文 MUST 只在创建响应出现一次，服务端 MUST NOT 保存令牌原文或可重建令牌的敏感材料。

#### Scenario: 签发成功

- **WHEN** 已登录用户创建一个未超过上限的 PAT
- **THEN** 响应返回令牌明文、令牌 ID、名称与过期时间
- **AND** 后续查询只返回元数据，不再返回明文

#### Scenario: 有效期越界

- **WHEN** 请求的有效期超过 90 天或小于 1 天
- **THEN** 服务拒绝创建并返回明确错误
- **AND** 不写入任何令牌记录

#### Scenario: 超过活动令牌上限

- **WHEN** 用户的活动 PAT 数量已达到上限
- **THEN** 服务拒绝创建并提示先撤销不再使用的令牌

#### Scenario: 撤销自己的令牌

- **WHEN** 用户撤销自己的 PAT
- **THEN** 该令牌立即失效
- **AND** 列表中的状态变为已撤销

#### Scenario: 拒绝撤销他人令牌

- **WHEN** 用户尝试撤销不属于自己的 PAT
- **THEN** 服务拒绝操作
- **AND** 目标令牌保持不变

#### Scenario: 名称缺失

- **WHEN** 创建请求没有名称或名称为空白
- **THEN** 服务拒绝创建并返回参数错误

### Requirement: PAT 验证独立于交互式会话

PAT SHALL 通过签名、专用声明与共享存储中的撤销记录验证；有效期内 MUST NOT 因闲置失效，MUST NOT 参与交互式会话的 30 分钟滑动续期。PAT 请求 MUST NOT 写入或延长 `online_token:<userId>`，MUST NOT 驱逐或刷新门户与 CLI 会话，也 MUST NOT 出现在在线列表。过期或已撤销的 PAT MUST 返回认证失败。

#### Scenario: 长时间闲置后仍可用

- **WHEN** PAT 在有效期内连续数天未被使用
- **THEN** 下一次请求仍然成功
- **AND** 不要求用户重新登录

#### Scenario: 不进入在线名单

- **WHEN** 管理员打开在线用户页面
- **THEN** 列表不包含由 PAT 发起的会话

#### Scenario: 不驱逐门户会话

- **WHEN** 用户在门户已登录的情况下使用 PAT 发起请求
- **THEN** 门户会话保持有效
- **AND** 门户的下一次请求不因 PAT 而重新登录

#### Scenario: 撤销立即生效

- **WHEN** PAT 被撤销后立即发起请求
- **THEN** 请求被拒绝
- **AND** 返回认证失败而不是业务错误

#### Scenario: 过期拒绝

- **WHEN** 使用超过有效期的 PAT 发起请求
- **THEN** 请求被拒绝
- **AND** 提示重新签发令牌

### Requirement: PAT 权限是签发时快照

PAT MUST 以签发时用户的角色、权限与部门数据作为权限快照；功能权限与数据权限仍由服务端判定，PAT MUST NOT 绕过或扩大用户权限。角色或权限变更 MUST NOT 自动改写已签发的 PAT；操作指南 MUST 说明需要撤销并重建的时机。

#### Scenario: 数据范围生效

- **WHEN** 数据范围被限制为“本人”的用户使用 PAT 查询用户列表
- **THEN** 只返回该用户可见的数据

#### Scenario: 越权返回 403

- **WHEN** PAT 对应的用户缺少接口所需的功能权限
- **THEN** 服务端返回 403
- **AND** CLI 以越权退出码结束

#### Scenario: 权限变更不自动同步

- **WHEN** 用户的角色或权限在 PAT 签发后发生变更
- **THEN** 已签发的 PAT 继续按签发时的权限快照执行
- **AND** 操作指南要求撤销并重建 PAT 后新权限才生效

### Requirement: PAT 随用户生命周期失效

用户停用或删除时，该用户全部 PAT MUST 立即失效，MUST NOT 等到令牌过期才收敛。会话级强制下线 MUST NOT 被视为 PAT 的撤销入口；PAT 的撤销 MUST 通过令牌管理接口或用户停用、删除完成。

#### Scenario: 停用用户

- **WHEN** 管理员停用某用户
- **THEN** 该用户全部 PAT 立即被拒绝
- **AND** 不需要逐个撤销令牌

#### Scenario: 删除用户

- **WHEN** 管理员删除某用户
- **THEN** 该用户全部 PAT 立即被拒绝

#### Scenario: 会话级强退不撤销 PAT

- **WHEN** 管理员在在线列表中强退某用户的一个会话
- **THEN** 该会话被删除
- **AND** 该用户的 PAT 保持有效，直至被显式撤销或用户被停用、删除

### Requirement: PAT 不能管理 PAT

签发、查看与撤销 PAT 的接口 MUST 只接受交互式登录会话；使用 PAT 调用这些接口 MUST 被拒绝，避免已泄漏的 PAT 自行续期或扩散。

#### Scenario: PAT 调用创建接口

- **WHEN** 使用 PAT 作为凭据调用 PAT 创建接口
- **THEN** 服务返回 403
- **AND** 不创建新令牌

#### Scenario: 交互式会话可管理

- **WHEN** 用户使用浏览器授权码登录得到的会话调用 PAT 管理接口
- **THEN** 可以查看、创建与撤销自己的令牌

### Requirement: PAT 操作可审计且不泄露明文

PAT 的签发与撤销 MUST 进入操作日志；日志与错误响应 MUST NOT 包含令牌明文。

#### Scenario: 签发进入操作日志

- **WHEN** 用户签发或撤销 PAT
- **THEN** 操作日志出现对应记录
- **AND** 记录包含操作人、接口与结果

#### Scenario: 日志不含明文

- **WHEN** 查询操作日志
- **THEN** 日志内容不包含 PAT 明文
- **AND** 错误响应也不回显明文
