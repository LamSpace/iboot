# Design

## Context

B5.2 已为服务端口启用 `client-auth: need`，B5.3 把 RocketMQ/Seata 收敛为 TLS-only 与专用网络。当前服务端只验证客户端证书链，任何 CA 签发的服务证书都能访问任意路径；JWT 过滤器只负责用户身份，不区分调用方工作负载。详见 `proposal.md` 与 `specs/service-authorization/spec.md`。

## Goals / Non-Goals

**Goals:**

- 从客户端证书 CN 得到调用方工作负载名，并在业务逻辑前执行路径级白名单。
- 在 8 个 Servlet 业务服务、网关与运维控制台保持一致语义。
- 不改变 JWT 认证与既有方法级权限；用户 API 仍由 Bearer 令牌决定。
- 用正反向回归证明越权被拒、允许链路不回归。

**Non-Goals:**

- 不实现方法级业务权限重写，不替换 JWT 权限模型。
- 不引入 OPA/SPIFFE/服务网格。
- 不拆分中间件账号，不改变 RocketMQ/Seata 的身份边界。
- 不基于来源 IP、请求头或网段做授权。

## Decisions

### 新增 `iboot-service-authz` 共享模块

策略、CN 提取与两个过滤器需要在 kernel、gateway、ops 之间复用。直接让 gateway/ops 依赖 `iboot-kernel` 会把 Servlet Web 栈与 JWT/Redis 依赖带进 WebFlux 网关，因此新增轻量模块 `iboot-service-authz`：

- 依赖 `spring-boot`、`spring-core`、`spring-web`、`jakarta.servlet-api`（provided）、`spring-webflux`（optional）。
- 提供 `ServiceAuthorizationProperties`、`ServiceAuthorizationPolicy`、`ClientCertificateIdentity`、`ServiceAuthorizationFilter`（Servlet）与 `ServiceAuthorizationWebFilter`（WebFlux）。
- 不包含自动配置；由各宿主显式注册。

### 身份取自证书 CN，不信任请求头

证书由 Tomcat/Reactor Netty 在 mTLS 握手后写入请求上下文：

- Servlet：`jakarta.servlet.request.X509Certificate` → `X509Certificate[]` → `SubjectX500Principal` → `LdapName` 取 CN。
- WebFlux：`ServerHttpRequest.getSslInfo().getPeerCertificates()` → 同一 CN 解析。
- 证书已由 truststore 校验；过滤器不再解析链，只看 CN。
- 不读取 `X-Forwarded-*`、`X-Client-Cert` 或请求头，避免伪造。

### 规则分两层：公共规则 + 服务专属规则

`iboot-tls.yml` 放公共规则（所有运行时复用），各服务 `application.yml` 放 `extra-rules`：

- 匹配顺序：`extra-rules` 先，`rules` 后；同一层按声明顺序，首个匹配生效。
- 这样业务服务的 `/api/auth/scope`、`/api/user/**` 等专属规则可以先于 `/api/**` 通配。
- 规则格式：

```yaml
iboot:
  security:
    service-authorization:
      enabled: ${IBOOT_SERVICE_AUTHORIZATION_ENABLED:false}
      extra-rules:
        - paths: ["/internal/leave/quota/**"]
          callers: ["iboot-workflow"]
```

### 公共规则

| 路径 | 允许调用方 |
|---|---|
| `/actuator/**` | `iboot-ops`、`prometheus`、`blackbox`、`iboot-portal` |
| `/oauth2/jwks` | 全部 `iboot-*` 服务、网关、门户 |
| `/oauth2/**`、`/.well-known/**`、`/login`、`/connect/**` | 网关、门户 |
| `/sba/**`、`/instances/**`、`/applications/**` | 门户 |
| `/druid/**`、Swagger/OpenAPI 路径 | 门户 |

### 服务专属规则

| 服务 | 规则 |
|---|---|
| system | `/internal/leave/quota/**` → workflow；`/api/auth/scope` → file/monitor/job/ai/gateway；`/api/user/**`、`/api/log/login/**` → ai/gateway；`/api/**` → gateway |
| notify | `/internal/notifications/**` → workflow；`/api/**` → gateway |
| job | `/api/monitor/backup/**` → ai/gateway；`/api/**` → gateway |
| file/monitor/workflow/auth/ai | `/api/**` → gateway |
| gateway | `/api/**` → portal |
| ops | 无额外规则，依赖公共 `/sba`、`/actuator` |

### 过滤器位置与失败语义

- Servlet 过滤器用 `FilterRegistrationBean` 注册 `/*`，顺序 `Ordered.HIGHEST_PRECEDENCE`，早于 Spring Security 链。
- WebFlux 过滤器实现 `Ordered`，在路由前执行。
- 命中规则且未授权：写 403 JSON，立即返回，不进入 JWT/业务逻辑。
- 未命中规则：放行，由既有 JWT/白名单处理。
- 启用时异常 fail-closed；未启用时完全跳过，保护单元测试。

### 开关

`IBOOT_SERVICE_AUTHORIZATION_ENABLED` 默认 `false`；compose 的公共环境与 `dev-init.sh` 显式设为 `true`。这样测试切片不需要客户端证书，运行时仍强制执行。

## Risks / Trade-offs

- **规则遗漏导致 403** → 用 `service-authz.sh` 覆盖门户、网关、探针、SBA、AI、scope、internal 与越权路径；全量回归兜底。
- **网关/ops 依赖新模块** → 新模块不引入 Servlet/WebFlux 强依赖，宿主按需注册过滤器。
- **CN 不是稳定身份** → 现有证书由 `tls-init.sh` 统一签发，CN 固定为服务名；未来引入 SPIFFE 时改为 SAN URI，规则模型不变。
- **JWT 与证书双重身份** → 授权只看“工作负载能否到达”，用户权限仍由 JWT 决定；不会用服务证书替代用户身份。
- **规则顺序错误** → 首个匹配生效；单元测试覆盖“专属规则先于通配规则”。

## Migration Plan

1. 新增模块与策略/过滤器，补单元测试。
2. kernel/gateway/ops 注册过滤器；三份 `iboot-tls.yml` 加公共规则；各服务 `application.yml` 加专属规则。
3. compose 与 `dev-init.sh` 启用开关；执行 `service-authz.sh` 与全量回归。
4. 回滚：将 `IBOOT_SERVICE_AUTHORIZATION_ENABLED` 设为 `false` 并重建服务；代码与规则保留。

## Open Questions

无。
