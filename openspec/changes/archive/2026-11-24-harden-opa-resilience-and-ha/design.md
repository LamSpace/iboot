# Design

## Context

`OpaAuthorizationClient` 只有 500ms 超时与失败关闭；`ServiceAuthorizationPolicy` 每次请求都调用 OPA。
Compose 只有单个 `opa` 容器，`opa-authz.sh` 通过停止 `iboot-opa` 验证失败关闭。

## Goals / Non-Goals

**Goals:**

- 连接失败/超时有界重试，仍保持失败关闭。
- 明确决策短 TTL 缓存，降低 P99 与 OPA 压力。
- 两个 OPA 副本 + 健康检查 LB；单副本故障不整体拒绝。
- 回归覆盖重试、缓存与两种故障场景。

**Non-Goals:**

- 不改变 OPA 策略语义或规则内容。
- 不引入外部缓存（Redis）或策略分发服务。
- 不实现跨主机/跨集群的 OPA 编排；本地 Compose 只提供双副本与 LB。
- 不允许缓存失败结果或无限期缓存。

## Decisions

### 1. 重试只针对瞬时传输错误

`OpaAuthorizationClient` 增加 `opaRetryAttempts`（默认 1，表示首次失败后最多再试 1 次）。仅在
`HttpTimeoutException`、`IOException` 等传输错误时重试；HTTP 非 200、JSON 解析失败与策略拒绝不重试。
最终失败仍返回 `false`，由调用方返回 403。

### 2. 短 TTL、容量受限的决策缓存

- 缓存键：`path + caller + rules fingerprint`；规则在服务启动时固定，fingerprint 防止配置热更新后误命中。
- 只缓存 HTTP 200 且 `result` 可解析的布尔结果；失败不缓存。
- 缓存默认禁用（TTL 0），避免全部 OPA 副本故障时仍从缓存放行；显式开启后 TTL 默认 5s，
  容量上限 4096，过期条目惰性清理，超限时整体清空。
- 开启缓存时必须在文档中写明最长陈旧窗口；回归的“全副本故障失败关闭”在默认缓存关闭时验证。

### 3. 双副本 + nginx

- `opa` 服务改为 2 个副本（`deploy.replicas: 2`，不再使用固定 `container_name`），共享策略目录与健康检查。
- 新增 `opa-lb`（本地已有 `nginx:1.30-alpine`）监听 8181，使用 Docker DNS 与 `upstream ... resolve`
  发现两个副本，并通过 `proxy_next_upstream` 做被动故障切换。
- `x-app-env` 的 `IBOOT_SERVICE_AUTHORIZATION_OPA_URL` 改为 `http://opa-lb:8181`；宿主机仍为
  `http://localhost:8181`（LB 发布回环端口）。
- 回归通过 `docker compose stop/start` 或按容器名停止单个副本验证故障切换。

### 4. 回归改造

- `opa-authz.sh`：
  - 策略测试与 Java 测试保持不变；
  - 通过 LB 验证允许/拒绝；
  - 停止一个副本后再次允许/拒绝，断言仍成功；
  - 停止全部副本后断言 403；恢复全部副本并等待健康。
- `ServiceAuthorizationOpaTest` 增加重试与缓存单元测试（可用本地 HTTP server 或现有测试桩）。

## Risks / Trade-offs

- [缓存导致策略变更延迟生效] → TTL 5s；规则 fingerprint 变更即失效；文档写明最大延迟。
- [重试增加尾延迟] → 仅传输错误重试一次；总时间受超时预算约束。
- [双副本增加内存] → OPA 进程轻量；nginx 容器开销很小。
- [nginx DNS 发现依赖 Docker DNS] → 使用 `resolver 127.0.0.11` 与 upstream `resolve`；回归覆盖单副本故障。

## Migration Plan

1. 更新 Java 客户端与配置项，补充单元测试。
2. 增加 nginx 配置与双副本 compose，服务指向 `opa-lb`。
3. 重建 `iboot-service-authz` 依赖的服务与 OPA 栈。
4. 运行 `opa-authz.sh` 与全量回归。
5. 回滚：把 `opa-url` 指回单副本 `opa:8181`，移除 LB；缓存/重试配置可保留。

## Open Questions

无。
