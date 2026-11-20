# Spec Delta

## ADDED Requirements

### Requirement: 认证端点使用 HTTPS 公开来源

认证服务对外暴露的发现、授权、令牌、JWKS 与登出端点 SHALL 使用浏览器实际访问的 HTTPS 公开来源。

- OIDC 发现文档中的 issuer 与各端点地址 MUST 等于该公开来源，不得使用容器内部主机名或明文端口。
- 浏览器客户端的登记回调地址 MUST 为 HTTPS；原生客户端的 loopback 回调 MAY 保持 HTTP。
- 认证服务 MUST NOT 在明文公开入口上签发令牌。
- 宿主机开发与 Docker 部署的公开来源不同时，MUST 通过环境配置分别设置，不得在运行期混用。

#### Scenario: 发现文档使用公开来源

- **WHEN** 客户端读取 HTTPS 公开入口的 OIDC 发现文档
- **THEN** issuer 与授权、令牌、JWKS、登出地址都以该 HTTPS 来源开头

#### Scenario: 浏览器回调为 HTTPS

- **WHEN** 门户发起带 PKCE 的授权请求
- **THEN** 登记的回调地址是 HTTPS 公开来源下的 `/callback`
- **AND** 未登记的地址被拒绝

#### Scenario: 原生客户端 loopback 回调

- **WHEN** CLI 发起授权码 + PKCE 登录
- **THEN** 回调地址可以是 `http://127.0.0.1:8765/callback`
- **AND** 令牌交换仍通过 HTTPS 公开入口完成

#### Scenario: 明文令牌端点不可用

- **WHEN** 客户端通过明文入口请求令牌端点
- **THEN** 请求被重定向或拒绝
- **AND** 不返回令牌
