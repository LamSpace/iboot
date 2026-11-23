# service-authorization 增量规格

## ADDED Requirements

### Requirement: 服务间授权可由 OPA 策略决策

服务间授权 SHALL 支持把有序规则交给 OPA 做集中决策，并在 OPA 不可用时失败关闭。

- 配置了 `iboot.security.service-authorization.opa-url` 时，授权判定 MUST 调用 OPA，MUST NOT 继续只依赖本地规则。
- 请求 input MUST 包含调用方、请求路径与按“专属规则优先、公共规则其次”排序的规则列表。
- OPA 策略 MUST 保持 first-match 语义：首个命中规则决定允许或拒绝；未命中任何规则时放行，交给 JWT/白名单。
- OPA 超时、连接失败、非 200 响应或无法解析结果时 MUST 拒绝请求（fail-closed）。
- 网关 WebFlux MUST 使用非阻塞调用；Servlet 路径 MAY 使用同步封装，但 MUST 受同一超时约束。
- `opa-url` 为空时 MAY 回退到本地 `ServiceAuthorizationPolicy`，以支持单元测试与离线排障。
- 回归 MUST 覆盖 OPA 允许、拒绝、本地回退与 OPA 不可用时失败关闭。

#### Scenario: OPA 允许网关访问业务 API

- **WHEN** `iboot-gateway` 证书访问系统服务 `/api/user/**`，且 OPA 可用
- **THEN** OPA 返回允许
- **AND** 请求进入 JWT 认证

#### Scenario: OPA 拒绝越权服务

- **WHEN** `iboot-file` 证书访问 `/api/user/**`
- **THEN** OPA 返回拒绝
- **AND** 服务返回 403，不执行业务逻辑

#### Scenario: OPA 不可用时失败关闭

- **WHEN** `opa-url` 已配置但 OPA 停止或超时
- **THEN** 命中规则的请求被拒绝
- **AND** 不会回退到放行

#### Scenario: 未配置 OPA 时本地回退

- **WHEN** `opa-url` 为空
- **THEN** 使用本地规则做同样的 first-match 判定
- **AND** 既有单元测试保持通过
