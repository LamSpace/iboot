# Spec Delta

## MODIFIED Requirements

### Requirement: PAT 权限是签发时快照

PAT MUST 以签发时用户的角色、权限与部门数据作为权限快照；指定 scope 时，有效权限 MUST 为该快照与 scope 的交集。功能权限与数据权限仍由服务端判定，PAT MUST NOT 绕过或扩大用户权限。角色或权限变更 MUST NOT 自动改写已签发的 PAT；操作指南 MUST 说明需要撤销并重建的时机。

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

#### Scenario: scope 进一步收窄权限

- **WHEN** 用户以用户管理权限签发一个只包含 `user:list` 的 PAT
- **THEN** 该 PAT 可以查询用户列表
- **AND** 调用用户新增或删除接口时返回 403

## ADDED Requirements

### Requirement: PAT 的权限范围可限制

签发 PAT 时 SHALL 可指定权限码集合（scope）；scope MUST 是用户当前权限的子集，未知或用户不具备的权限码 MUST 被拒绝。scope 为空表示继承用户全部权限。scope MUST 去重并限制数量；令牌声明与元数据 MUST 记录 scope，列表 MUST 可见。

#### Scenario: 指定 scope 签发

- **WHEN** 用户以 `job:run` 与 `backup:trigger` 两个 scope 签发 PAT
- **THEN** 创建响应与列表展示这两个 scope
- **AND** 令牌的有效权限只包含这两个权限

#### Scenario: 非子集 scope 被拒绝

- **WHEN** 请求的 scope 中包含用户当前不具备的权限码
- **THEN** 服务拒绝签发并返回明确错误
- **AND** 不写入令牌记录

#### Scenario: 空 scope 继承全部权限

- **WHEN** 签发请求未指定 scope
- **THEN** 令牌继承用户当前的全部权限
- **AND** 行为与本次变更前一致

#### Scenario: scope 去重与数量上限

- **WHEN** 请求包含重复权限码或超过允许数量的 scope
- **THEN** 重复项被去重
- **AND** 超过上限时拒绝签发

#### Scenario: 内核再次取交集

- **WHEN** 令牌声明中的权限超出其 scope（例如签发逻辑回归）
- **THEN** 资源服务只授予 scope 与声明的交集权限
- **AND** 超出 scope 的接口返回 403
