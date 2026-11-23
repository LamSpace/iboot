# SPIRE 工作负载身份与 OPA 策略平面

## Why

B5.8 已经把工作负载身份写入证书 URI SAN，但策略仍由每个服务本地的
`ServiceAuthorizationPolicy` 执行，SPIRE Workload API 与 OPA 都没有部署。L3 边界因此仍是
“SPIFFE 格式 + 本地策略”，不是可运维的身份与策略控制面。

## What Changes

- **OPA**：
  - 新增 `opa` 服务与 `docker/opa/policies/service_authz.rego`，按调用方、路径和有序规则做
    first-match 决策。
  - `ServiceAuthorizationPolicy` 在配置 `opa-url` 时把有序规则作为 input 发给 OPA；OPA 不可用、
    超时或返回异常时失败关闭。
  - 网关 WebFlux 使用异步 OPA 调用，避免阻塞事件循环；Servlet 路径使用同步封装。
  - compose 与 `dev-init.sh` 显式启用 OPA；保留 `opa-url` 为空时的本地策略回退。
- **SPIRE**：
  - 新增 `spire` profile：SPIRE Server、Agent、一次性 join token 初始化与一个 demo workload。
  - Workload API 签发 X.509-SVID，URI SAN 为 `spiffe://iboot.local/...`；回归验证 demo SVID。
  - 现有十个服务本轮仍使用 mkcert 叶子证书；SVID 切换作为下一阶段，保留回滚路径。
- **服务网格边界**：本项目部署目标是 Docker Compose；不引入 Kubernetes 控制面或 sidecar 网格，
  以“SPIFFE 身份 + OPA 策略 + 现有 mTLS”作为 Compose 拓扑的 L3 安全平面，并在文档中明确。
- 新增 `scripts/regress/opa-authz.sh`、`scripts/regress/spire-svid.sh`，接入 `regress.sh`。
- 更新 `docs/27`、`docs/23`、`docker/README.md`、`ROADMAP.md`。

## Capabilities

### New Capabilities

- `workload-identity`：SPIRE Workload API 与 X.509-SVID 的签发、验证和边界。

### Modified Capabilities

- `service-authorization`：新增“策略由 OPA 决策”的要求，保留本地策略作为未配置时的回退。

## Impact

- 修改：`iboot-service-authz`（OPA 客户端/异步策略）、三份 `iboot-tls.yml`、compose、`dev-init.sh`、
  `scripts/regress/regress.sh`、文档与路线图。
- 新增：`docker/opa/**`、`docker/spire/**`、`scripts/regress/opa-authz.sh`、`scripts/regress/spire-svid.sh`。
- 不改变现有客户端证书与 mTLS 信任根；OPA 失败关闭是唯一新增运行时依赖。
- 风险：OPA 策略语义必须与本地 first-match 一致；SPIRE 为 opt-in profile，不进入默认启动路径。
