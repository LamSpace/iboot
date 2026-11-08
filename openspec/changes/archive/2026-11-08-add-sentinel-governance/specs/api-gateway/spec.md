# Spec Delta

## MODIFIED Requirements

### Requirement: 入口限流

网关 MUST 能在入口按路由限制请求速率，保护后端不被突发流量压垮。

- 限流 MUST 支持按路由（目标服务或路由 ID）独立配置阈值；单个路由被限流 MUST NOT 影响其它路由。
- 被限流的请求 MUST 返回 `429`，响应体 MUST 为 JSON 并能表明是限流而非业务错误，结构与既有网关错误响应一致。
- 限流规则 MUST 可在运行期调整并生效，无需重启网关。
- 限流阈值与规则来源 MUST 遵守 `service-resilience` 对「配置中心为权威来源」的约束。

#### Scenario: 超过阈值被限流
- **WHEN** 某路由在窗口期内的请求数超过配置阈值
- **THEN** 超出部分返回 `429`
- **AND** 响应体表明是限流

#### Scenario: 规则调整后即时生效
- **WHEN** 限流阈值被下调
- **THEN** 后续请求按新阈值被限流，且网关未重启

#### Scenario: 单路由限流不影响其它路由
- **WHEN** 路由 A 因超限被限流
- **THEN** 指向路由 B 的请求仍正常返回

#### Scenario: 限流响应与网关错误契约一致
- **WHEN** 请求被限流
- **THEN** 响应体包含网关统一的 `code`、`message` 与 `path` 字段
- **AND** 客户端可据此区分限流与上游不可用
