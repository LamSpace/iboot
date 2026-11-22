# Proposal

## Why

B5.2 让服务端口要求客户端证书，但服务端只校验证书链：任何由本地 CA 签发的服务证书都能调用任意服务、任意路径。B5.3 收掉了 RocketMQ/Seata 的缺口后，**“已认证但未授权”**成为零信任最大的剩余问题。

## What Changes

- 新增共享模块 `iboot-service-authz`：从客户端证书 CN 提取调用方身份，按路径规则匹配调用方白名单，Servlet 与 WebFlux 各提供一个过滤器。
- 八个 Servlet 服务通过 `iboot-kernel` 自动注册过滤器；网关注册 WebFlux 过滤器；运维控制台注册 Servlet 过滤器。
- 定义全局调用矩阵：`/internal/**` 只允许 workflow，`/api/auth/scope` 只允许相关服务与网关，AI 管理工具只允许访问 system 的指定路径，`/actuator/**` 只允许 ops/Prometheus/blackbox/portal，`/api/**` 在业务服务只允许网关、在网关只允许门户。
- JWT 认证保持原样：服务间授权只决定“这个工作负载能否到达该路径”，用户 API 仍需有效 Bearer 令牌与既有权限。
- 新增 `scripts/regress/service-authz.sh`：正反向验证允许/拒绝、JWT 链路不被覆盖，并接入全量回归。
- 文档增加服务间调用矩阵与排障说明；`IBOOT_SERVICE_AUTHORIZATION_ENABLED` 在 Docker/宿主机运行环境启用，默认关闭以保护单元测试切片。

## Capabilities

### New Capabilities

- `service-authorization`：按客户端证书身份对服务间请求做路径级授权，覆盖 Servlet 服务、网关与运维控制台。

### Modified Capabilities

- `transport-security`：明确 mTLS 只提供身份认证，调用方授权由 `service-authorization` 承担；移除“尚未按调用方授权”的旧边界。
- `container-deployment`：全栈编排与宿主机开发注入服务授权开关，并保证未启用时不影响本地测试。

## Impact

- 新增模块：`iboot-service-authz`（策略、身份提取、Servlet/WebFlux 过滤器、单元测试）。
- 依赖：`iboot-kernel`、`iboot-gateway`、`iboot-ops` 引用新模块；父 POM 与后端 builder Dockerfile 增加模块。
- 配置：三份 `iboot-tls.yml` 增加公共规则；各服务 `application.yml` 增加白名单规则；compose 与 `dev-init.sh` 注入开关。
- 回归：新增 `service-authz.sh`，并作为全量回归独立步骤。
- 文档：`docs/27`、`docs/23`、`docker/README.md`、`ROADMAP.md`。
- 兼容性：启用后，未经允许的服务证书访问对应路径返回 403；JWT 用户链路、门户、探针与 SBA 必须保持可用。
- 不包含：中间件每服务账号、SPIFFE/OPA、方法级业务权限重写。
