# observability-security Specification

## Purpose
定义 Prometheus 与 exporter 之间的采集链路安全：指标端点只接受 TLS，调用方必须出示受管客户端证书，
明文或匿名抓取必须失败。

## Requirements

### Requirement: 指标端点只接受 TLS 与受管客户端

MySQL、Redis、Node 与 Elasticsearch exporter 的指标端点 SHALL 只接受 TLS，并要求客户端证书。

- 每个 exporter MUST 使用独立的服务端证书，证书 SAN MUST 包含其在编排中的服务名。
- exporter MUST 校验证书链；未携带客户端证书或证书不受本地 CA 信任的请求 MUST 被拒绝。
- 明文 HTTP 请求 MUST NOT 返回指标。
- Prometheus MUST 使用自己的客户端证书与本地 CA 抓取全部 exporter。
- 证书与私钥 MUST 从只读挂载注入，MUST NOT 写入镜像。

#### Scenario: Prometheus 携带证书抓取成功

- **WHEN** Prometheus 使用 `prometheus` 客户端证书抓取任一 exporter
- **THEN** TLS 握手成功
- **AND** 返回指标文本

#### Scenario: 无客户端证书被拒绝

- **WHEN** 客户端只校验服务端证书但不提供客户端证书
- **THEN** 请求被拒绝
- **AND** 不返回指标

#### Scenario: 明文被拒绝

- **WHEN** 客户端对 exporter 端口发起明文 HTTP
- **THEN** 不返回指标

#### Scenario: 错误 CA 被拒绝

- **WHEN** 客户端使用不受信任的 CA 校验 exporter 证书
- **THEN** TLS 校验失败

### Requirement: 采集链路有负向回归

回归 SHALL 对每个 exporter 执行正向与负向检查，结果 MUST 接入全量回归与 CI。

- 正向 MUST 覆盖 Prometheus 抓取成功或等价客户端证书请求。
- 负向 MUST 覆盖明文、无客户端证书与错误 CA。
- 任一负向用例返回指标 MUST 阻止环境被判定为可验收。

#### Scenario: 每个 exporter 都有正反向用例

- **WHEN** 执行 exporter mTLS 回归
- **THEN** 四个 exporter 的携带证书请求成功
- **AND** 明文、无客户端证书与错误 CA 请求失败

#### Scenario: 负向结果阻止验收

- **WHEN** 任一 exporter 在无客户端证书时仍返回指标
- **THEN** 回归失败并指出该 exporter
- **AND** 环境不得判定为通过

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
