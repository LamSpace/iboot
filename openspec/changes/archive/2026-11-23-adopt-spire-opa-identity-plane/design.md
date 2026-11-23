# SPIRE 工作负载身份与 OPA 策略平面 — 设计

## 1. OPA 决策路径

```text
Servlet Filter / WebFlux Filter
        │ caller (SPIFFE ID) + path + ordered rules
        ▼
ServiceAuthorizationPolicy
        │ opa-url 非空
        ▼
POST http://opa:8181/v1/data/iboot/authz/allow
        │ {"input":{"caller":"...","path":"...","rules":[...]}}
        ▼
Rego first-match → { "result": true|false }
```

- `ServiceAuthorizationProperties` 新增 `opa-url` 与 `opa-timeout`（默认 `500ms`）。
- `ServiceAuthorizationPolicy` 保留本地规则；`opa-url` 非空时构造 `OpaAuthorizationClient`。
- Servlet `isDenied` 使用同步封装；WebFlux `isDeniedAsync` 使用 `CompletableFuture`，过滤器用
  `Mono.fromFuture`，不阻塞事件循环。
- OPA 调用异常、超时、非 2xx、结果缺失时返回拒绝。
- 规则 input 保持 `extra-rules` 在前、`rules` 在后的顺序；Rego 用数组索引实现 first-match，
  未命中时 `default allow := true`。

## 2. OPA 策略

`docker/opa/policies/service_authz.rego`：

```rego
package iboot.authz

default allow := true

allow := false if {
    some i, rule in input.rules
    some pattern in rule.paths
    glob.match(pattern, ["/"], input.path)
    not caller_allowed(rule, input.caller)
    not earlier_match(i, input)
}

caller_allowed(rule, caller) if {
    some allowed in rule.callers
    allowed == caller
}

earlier_match(i, input) if {
    some j, other in input.rules
    j < i
    some pattern in other.paths
    glob.match(pattern, ["/"], input.path)
}
```

- `glob.match` 与 AntPathMatcher 在现有规则（`/api/**`、`/internal/**`、精确路径）上等价。
- 规则列表由 Java 端按现有顺序发送，避免在 Rego 中复制配置。
- 策略测试使用 `opa test`，覆盖精确路径、通配路径、首条命中、空调用方。

## 3. 启用与回退

- `x-app-env` 设置 `IBOOT_SERVICE_AUTHORIZATION_OPA_URL=http://opa:8181`；
  `dev-init.sh` 设置 `http://localhost:8181`。
- 三份 `iboot-tls.yml` 的 `service-authorization` 增加 `opa-url` 与 `opa-timeout` 绑定。
- `opa` 服务加入 `app` 网络并发布 `127.0.0.1:8181`；健康检查使用 OPA 自带 CLI 或 HTTP 探针。
- 回退：清空 `IBOOT_SERVICE_AUTHORIZATION_OPA_URL` 即恢复本地策略，无需重启证书或网络。

## 4. SPIRE

- `spire` profile 包含：
  - `spire-server`：SQLite 数据存储、disk key manager、join token 节点证明、disk upstream CA；
  - `spire-init`：生成/复用 CA、生成 join token、注册 demo 工作负载；
  - `spire-agent`：读取 join token，提供 `/run/spire/sockets/agent.sock`；
  - `spire-demo`：通过 Workload API 获取 SVID 并写出证书。
- 信任域 `iboot.local`，demo SPIFFE ID `spiffe://iboot.local/service/iboot-demo`。
- 注册 selector 使用 `docker:label:com.docker.compose.service:spire-demo`，不使用 `*`。
- 现有十个服务继续使用 mkcert 叶子证书；SVID 切换需要证书加载/轮换改造，列为下一阶段。
- 不部署 Kubernetes 网格；Compose 拓扑的 L3 平面是“SPIFFE 身份 + OPA 策略 + mTLS”。

## 5. 回归

- `opa-authz.sh`：`opa test`、HTTP 允许/拒绝、服务链路（`service-authz.sh` 在 OPA 启用下运行）、
  OPA 停止时 403。
- `spire-svid.sh`：启动 `spire` profile，等待健康，运行 demo，检查 SVID URI SAN；清理容器但保留卷。
- `regress.sh` 接入两个检查；SPIRE 检查在 profile 未启动时明确跳过，不阻塞默认回归。

## 6. 回滚

- OPA：清空 `opa-url` 并回退 compose 环境变量；删除 `opa` 服务。
- SPIRE：停止并删除 `spire` profile 容器；现有证书链不受影响。
