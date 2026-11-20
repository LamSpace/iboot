# Spec Delta

## ADDED Requirements

### Requirement: 网关以 HTTPS 提供服务并转发到 HTTPS 服务

网关 SHALL 自身以 HTTPS 监听，并以 HTTPS 访问全部业务服务。

- 网关服务端口 MUST 启用 `server.ssl`，只接受 TLS 1.2 / 1.3。
- 门户 nginx 与 Vite 的上游地址 MUST 使用 `https://`，并信任本地 CA。
- 网关到业务服务的路由 MUST 使用 HTTPS scheme，并通过本地 CA 校验服务端证书。
- 上游 TLS 失败 MUST 返回可辨识的网关错误，MUST NOT 降级为明文重试。
- SSE 长连接 MUST 在 HTTPS 下保持既有超时与不缓冲语义。

#### Scenario: 网关以 HTTPS 响应

- **WHEN** 使用本地 CA 访问网关健康检查
- **THEN** 请求成功且协商 TLS 1.2 / 1.3

#### Scenario: 网关转发到 HTTPS 服务

- **WHEN** 经网关请求任一业务服务
- **THEN** 网关到上游的请求使用 HTTPS
- **AND** 上游证书通过本地 CA 校验

#### Scenario: 去掉上游 CA

- **WHEN** 网关未配置本地 CA 或 CA 不匹配
- **THEN** 上游调用失败并返回可辨识的网关错误
- **AND** 不退回明文

#### Scenario: SSE 在 HTTPS 下可用

- **WHEN** 客户端经 HTTPS 网关建立推送长连接
- **THEN** 事件在秒级内到达
- **AND** 连接不被网关提前关闭
