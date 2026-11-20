# Spec Delta

## ADDED Requirements

### Requirement: CLI 默认通过 HTTPS 访问公开入口

CLI 的默认 context 与所有登录、发现、令牌与业务请求 SHALL 使用 HTTPS 公开入口。

- 默认 context 的 base URL 与 issuer MUST 为 HTTPS 地址。
- 新增 context 时，非 loopback 的 `http://` base URL MUST 被拒绝，并给出可操作的错误提示。
- CLI MUST 能通过显式配置的信任库信任本地 CA；未提供信任库时 MUST 以可读错误结束，不得静默降级为明文。
- 宿主机开发与 Docker 部署使用不同的公开来源时，MUST 通过不同 context 隔离，凭据不得跨 context 复用。

#### Scenario: 默认 context 使用 HTTPS

- **WHEN** 用户首次执行 `iboot login`
- **THEN** 登录与令牌交换都访问 HTTPS 公开入口

#### Scenario: 拒绝明文 base URL

- **WHEN** 用户新增 context 时把 base URL 设为非 loopback 的 `http://` 地址
- **THEN** CLI 拒绝该 context 并提示改用 HTTPS
- **AND** 不发起任何请求

#### Scenario: 本地 CA 信任

- **WHEN** 用户通过 CLI 配置或环境变量指定本地 PKCS12 信任库
- **THEN** CLI 能完成 HTTPS 登录与查询
- **AND** 未指定信任库时以证书校验失败结束，而不是改用明文

#### Scenario: 两套公开来源隔离

- **WHEN** 用户分别创建宿主机开发与 Docker 部署两个 context
- **THEN** 两个 context 的 base URL 与 issuer 分别指向各自的 HTTPS 来源
- **AND** 在一个 context 登录不会复用另一个 context 的凭据
