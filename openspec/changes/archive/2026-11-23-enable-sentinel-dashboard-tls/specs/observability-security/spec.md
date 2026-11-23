# observability-security 增量规格

## ADDED Requirements

### Requirement: Sentinel Dashboard 只提供 HTTPS 并要求登录

Sentinel Dashboard 的宿主机入口 SHALL 只提供 HTTPS，并且 MUST 要求登录后才能访问管理接口。

- Dashboard MUST 使用本地 CA 签发的服务端证书；证书 SAN MUST 覆盖 `localhost`、`127.0.0.1` 与 `sentinel-dashboard`。
- 明文 HTTP 请求 MUST NOT 获得 Dashboard 页面或 API 响应。
- 未登录访问管理接口 MUST 被拒绝或重定向到登录页，MUST NOT 返回机器、规则或簇点数据。
- 登录凭据 MUST 从环境变量注入；生产覆盖文件 MUST 拒绝空值，MUST NOT 回退到镜像默认口令。
- Docker 内客户端 MUST 使用 `https://sentinel-dashboard:8858`，宿主机客户端 MUST 使用 `https://localhost:8858`。
- 门户 MUST 使用 HTTPS 地址；启用登录时 MUST NOT 依赖跨源 iframe 自动共享会话。

#### Scenario: HTTPS 可访问且带登录

- **WHEN** 使用本地 CA 访问 `https://localhost:8858/`
- **THEN** TLS 握手与证书校验成功
- **AND** 页面返回登录页或登录后的控制台

#### Scenario: 明文被拒绝

- **WHEN** 访问 `http://localhost:8858/`
- **THEN** 不返回 Dashboard 页面或 API 数据

#### Scenario: 匿名管理接口被拒绝

- **WHEN** 不携带登录会话访问机器或规则管理接口
- **THEN** 请求被重定向到登录页或返回未授权
- **AND** 不返回机器列表、规则或簇点数据

#### Scenario: 生产口令缺失时启动失败

- **WHEN** 叠加生产覆盖文件且未提供 `SENTINEL_DASHBOARD_USER` 或 `SENTINEL_DASHBOARD_PASSWORD`
- **THEN** compose 配置解析失败
- **AND** 不使用默认口令启动
