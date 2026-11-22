# Spec Delta

## MODIFIED Requirements

### Requirement: 门户对外只提供 TLS 入口

全量容器部署中，门户 SHALL 对外提供 HTTPS 入口，明文端口只用于重定向。

- 门户 nginx MUST 监听 443 并加载本地证书；80 MUST 只返回到 HTTPS 的 301。
- 门户到网关、系统服务与运维控制台的代理 MUST 保留原始 Host，使认证服务按公开来源生成页面与回调地址。
- 门户到上游的 HTTPS 代理 MUST 出示门户客户端证书，并通过本地 CA 校验上游服务端证书。
- 门户镜像 MUST 暴露 443；编排 MUST 同时发布 80 与 443。
- 门户健康检查 MUST 使用本地 CA 校验 HTTPS，MUST NOT 以跳过证书校验的方式通过。

#### Scenario: 容器部署门户可 HTTPS 访问

- **WHEN** 启动全量容器部署并访问门户
- **THEN** 页面通过 443 加载，证书由本地 CA 签发
- **AND** 80 端口返回 301

#### Scenario: 健康检查使用 CA

- **WHEN** 编排器执行门户健康检查
- **THEN** 检查请求通过本地 CA 校验 HTTPS
- **AND** 检查命令中没有关闭证书校验的参数

#### Scenario: 门户上游双向 TLS 成功

- **WHEN** 门户把 `/api` 请求代理到网关
- **THEN** 网关接受门户出示的客户端证书
- **AND** 请求返回业务响应或预期的鉴权状态

### Requirement: 数据面证书与服务 TLS 编排

容器编排 SHALL 为业务服务与数据面组件挂载各自需要的证书，并让服务端口在双向 TLS 下正常工作。

- 网关、业务服务与运维控制台 MUST 挂载本服务证书、私钥与信任库；证书 MUST 同时支持服务端与客户端用途。
- 门户 MUST 挂载门户证书并把它作为访问上游的客户端身份。
- Prometheus 与 blackbox MUST 挂载各自的探针客户端证书，用于抓取服务端口与执行证书探针。
- MySQL、Redis、Silo、Neo4j 的服务端 MUST 只提供 TLS 端口，容器内不得回退为明文。
- 服务健康检查 MUST 优先使用 TCP 探活，避免把“未带客户端证书”误判为服务不健康；门户与数据面健康检查 MUST 使用本地 CA。
- 证书缺失、信任库不匹配或客户端证书用途缺失时，容器 MUST 启动失败或健康检查失败，MUST NOT 以明文模式继续。

#### Scenario: 服务与数据面均加载证书

- **WHEN** 启动全量容器部署
- **THEN** 服务与数据面容器都加载各自的本地证书
- **AND** 服务端口只接受带客户端证书的 TLS

#### Scenario: 探针证书可用

- **WHEN** 编排启动 Prometheus 与 blackbox
- **THEN** 两者加载各自的客户端证书
- **AND** 服务指标抓取与证书探针成功

#### Scenario: 健康检查使用 CA

- **WHEN** 编排器执行门户或数据面健康检查
- **THEN** 检查请求使用本地 CA
- **AND** 检查命令中没有关闭证书校验的参数

#### Scenario: 证书缺失时失败

- **WHEN** 证书未生成、被移除或缺少 `clientAuth` 用途
- **THEN** 启动或健康检查失败
- **AND** 不会以明文或单向 TLS 模式继续
