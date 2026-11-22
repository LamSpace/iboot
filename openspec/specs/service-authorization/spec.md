# service-authorization Specification

## Purpose

在 mTLS 证明调用方身份之后，按客户端证书 CN 对服务间请求做路径级授权，使“已认证”进一步收敛为“只能访问被允许的路径”。

## Requirements

### Requirement: 调用方身份由客户端证书 CN 确定

服务端 SHALL 从 TLS 客户端证书的 Subject CN 提取调用方工作负载名；无法提取时 MUST 视为匿名调用方。

- 服务证书 CN MUST 为服务名（如 `iboot-system`），门户证书 CN MUST 为 `iboot-portal`，探针证书 CN MUST 为 `prometheus` 或 `blackbox`。
- 身份提取 MUST 使用已由本地 CA 验证过的证书，MUST NOT 信任请求头、查询参数或代理注入的身份。
- 过滤器 MUST NOT 改写 JWT 认证结果；用户 API 仍由 Bearer 令牌与既有权限决定。

#### Scenario: 服务证书识别为服务名

- **WHEN** 网关以 `iboot-gateway` 证书访问系统服务
- **THEN** 过滤器识别调用方为 `iboot-gateway`

#### Scenario: 无法识别时按匿名处理

- **WHEN** 客户端证书缺少可解析的 CN
- **THEN** 调用方身份为空
- **AND** 命中授权规则的路径按未授权处理

### Requirement: 路径级调用方授权矩阵

服务端 SHALL 按路径模式匹配调用方白名单；规则未覆盖的路径保持既有 JWT/白名单语义。

- 专属规则 MUST 先于通配规则匹配，例如 `/api/auth/scope` 先于 `/api/**`。
- `/internal/**` MUST 只允许明确列出的内部调用方；`/actuator/**` MUST 只允许运维控制台、Prometheus、blackbox 与门户。
- 业务服务的 `/api/**` MUST 默认只允许网关；网关的 `/api/**` MUST 默认只允许门户。
- 服务间数据权限查询、AI 管理工具与通知内部接口 MUST 使用显式调用方列表。
- 规则 MUST 可配置，新增调用方 MUST 经过代码评审与回归。

#### Scenario: 内部额度接口只允许审批服务

- **WHEN** `iboot-workflow` 调用系统服务 `/internal/leave/quota/**`
- **THEN** 请求通过授权
- **AND** 其它服务证书访问同一路径时返回 403

#### Scenario: 业务 API 只允许网关

- **WHEN** 业务服务收到携带 `iboot-gateway` 证书的 `/api/**` 请求
- **THEN** 请求进入 JWT 认证
- **AND** 其它服务证书访问同一路径时返回 403

#### Scenario: 探针只能访问监控端点

- **WHEN** Prometheus 或 blackbox 访问 `/actuator/**`
- **THEN** 请求通过授权
- **AND** 普通业务服务证书访问同一路径时返回 403

### Requirement: 未授权请求失败关闭

授权失败 SHALL 在 JWT 认证之前返回 403，且 MUST NOT 调用下游业务逻辑。

- 响应 MUST 使用 JSON 错误体并包含稳定的“调用方无权访问”语义。
- 缺少客户端证书、CN 无法解析或调用方不在白名单时 MUST 拒绝。
- 授权过滤器 MUST NOT 因异常回退为放行；配置为启用时 MUST fail-closed。

#### Scenario: 越权调用被拒绝

- **WHEN** `iboot-file` 证书访问系统服务的 `/api/user/**`
- **THEN** 返回 403
- **AND** 不执行用户查询逻辑

#### Scenario: 授权过滤器异常时拒绝

- **WHEN** 策略配置无法解析或身份提取抛出异常
- **THEN** 命中规则的请求返回 403
- **AND** 不会默认放行

### Requirement: 授权覆盖入口与执行端

授权 SHALL 同时覆盖业务服务执行端、网关入口与运维控制台。

- 八个 Servlet 业务服务 MUST 通过共享内核注册授权过滤器。
- 网关 MUST 在路由转发前校验门户证书；运维控制台 MUST 在页面与实例接口前校验门户证书。
- 门户、Prometheus、blackbox、SBA 与内部服务调用 MUST 保持现有正向链路。
- 授权 MUST NOT 依赖网络位置或来源 IP。

#### Scenario: 网关只接受门户

- **WHEN** 门户以 `iboot-portal` 证书访问网关 `/api/**`
- **THEN** 请求进入 JWT 认证
- **AND** 业务服务证书直接访问网关时返回 403

#### Scenario: SBA 仍可巡检

- **WHEN** `iboot-ops` 以自身证书访问业务服务 `/actuator/**`
- **THEN** 请求通过授权
- **AND** 实例状态与指标保持可用

### Requirement: 授权开关与回归

服务授权 SHALL 由显式开关启用，并 MUST 有正反向回归覆盖。

- 默认值 MUST 为关闭，避免单元测试切片因缺少客户端证书失败。
- Docker 全栈与宿主机开发 MUST 显式启用。
- 回归 MUST 覆盖允许、拒绝、JWT 链路不被覆盖与探针链路。
- 任一越权用例未返回 403 MUST 阻止验收。

#### Scenario: 默认关闭

- **WHEN** 未设置授权开关启动测试上下文
- **THEN** 过滤器不拦截请求
- **AND** 既有单元测试保持通过

#### Scenario: 运行时启用

- **WHEN** 以 `IBOOT_SERVICE_AUTHORIZATION_ENABLED=true` 启动全栈
- **THEN** 越权服务证书得到 403
- **AND** 门户、网关、探针与 SBA 链路正常
