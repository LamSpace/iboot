# Design

## Context

Tempo 2.10.9 单机运行在 `data` 网络，发布 4317（OTLP/gRPC）、4318（OTLP/HTTP）与 3200（HTTP 查询）到宿主机回环地址。应用使用 4318 上报，Grafana 使用 3200 查询；两条链路目前都是明文，已在 `plaintext-allowlist.txt` 登记。共享叶子证书已包含 `tempo` 与 `localhost` SAN，Java 进程启动时已加载本地 PKCS12 信任库。

## Goals / Non-Goals

**Goals:**

- 让 Tempo 的对外接收与查询端口只接受 TLS，并让应用与 Grafana 使用本地 CA 完成证书校验。
- 从明文允许清单移除 Tempo 相关条目，回归覆盖正向握手与负向明文检查。

**Non-Goals:**

- 不启用 Tempo 的客户端证书认证（mTLS），仍使用共享服务端证书；每服务身份属于 B5。
- 不将 Tempo 内部 gRPC 服务端口 9095 改为 TLS：它不发布到宿主机，且与 Tempo 单进程内 frontend↔querier 通信耦合；单独启用会打断内部调用。
- 不改变 Grafana、Prometheus、Thanos 的 UI 或其余数据源明文边界。
- 不调整证书生成脚本或证书有效期。

## Decisions

### 使用 `http_tls_config`，而不是 `server.tls`

Tempo 2.10 的 `server.Config` 没有 `tls` 字段，直接配置会在启动时解析失败。查询端口使用 `server.http_tls_config`；经验证，`/status`、`/api/traces` 在该配置下通过 HTTPS 提供。

### OTLP 接收器分别配置 TLS

`distributor.receivers.otlp.protocols.http.tls` 与 `...grpc.tls` 分别加载同一张叶子证书。应用当前使用 OTLP/HTTP，因此 4318 是主路径；4317 虽未被应用使用，也一并收紧，避免留下可匿名写入的接收口。

### gRPC 服务端口保持明文但不发布

`server.grpc_tls_config` 会让 Tempo 内部的 frontend processor 无法连接本机 9095（实测日志持续报 `error reading server preface: EOF`，`/ready` 无法就绪）。该端口不在 compose 中发布，调用方也只有 Tempo 自身，因此保留明文并在文档登记边界；对外查询统一走 3200 HTTPS。

### Grafana 数据源使用 CA 文件校验

沿用 Elasticsearch 数据源的做法：`jsonData.tlsAuthWithCACert: true`，`secureJsonData.tlsCACert: $__file{/certs/rootCA.pem}`，URL 改为 `https://tempo:3200`。Grafana 容器已挂载 `/certs`。

### 应用默认值与编排环境同时修改

九个服务的 `application.yml` 默认值改为 `https://localhost:4318/v1/traces`，编排 `TEMPO_OTLP` 改为 `https://tempo:4318/v1/traces`。宿主机进程的信任库已由 `dev-init.sh` 注入，容器进程由 `JAVA_TOOL_OPTIONS` 注入；无需新增证书文件。

## Risks / Trade-offs

- **Tempo 启动初期 `/ready` 返回 503** → 回归使用 `/status` 判断 HTTPS 可用，或用带重试的探针；不把启动窗口误判为失败。
- **宿主机旧进程仍指向 HTTP** → 修改默认值后必须重新打包并重启九个服务；验证时检查进程环境中 `TEMPO_OTLP` 与 `ss`/`lsof` 连接，避免旧进程静默上报失败。
- **Grafana 已有数据源缓存** → provisioning 在容器重建时覆盖；验证 `/api/datasources` 中 URL 为 https，并通过 Explore/Tempo 查询实际检索。
- **4317 客户端未知** → 当前仓库内无 OTLP/gRPC 调用方；若外部工具仍以明文连接，需要显式改为 TLS，不允许回退。

## Migration Plan

1. 修改 Tempo 配置、compose、应用默认值、Grafana 数据源与脚本。
2. 重建 Tempo 与 Grafana 容器；重新打包并重启宿主机服务进程。
3. 回归：Tempo 端口 TLS 正向/负向、明文审计、追踪链路查询、全栈冒烟。
4. 回滚：恢复旧配置与镜像，重建 Tempo/Grafana 并重启服务；删除允许清单中的临时条目不保留明文回退开关。

## Open Questions

无。
