# Design

## Context

B5.1 之后，门户与十个业务服务已各自持有独立叶子证书，`iboot-tls.yml` 用同一个 `iboot` PEM bundle 同时提供 `server.ssl` 与出站 HTTP 客户端信任链。证书由 mkcert 签发，EKU 只有 `serverAuth`；服务端口目前只校验客户端提供的证书链是否存在，不要求客户端证书。详见 `proposal.md` 与 `specs/transport-security/spec.md`。

## Goals / Non-Goals

**Goals:**

- 服务端口只接受携带本地 CA 签发客户端证书的 TLS 连接。
- 每个调用方以自己或明确归属的证书证明身份：服务用自己的服务证书，门户用门户证书，探针用探针证书。
- 证书生成、回归、文档与现有 CA/信任库保持可重复、可回滚。

**Non-Goals:**

- 不按调用方做授权（例如只允许网关访问某路径）；mTLS 只证明“证书由本地 CA 签发”。
- 不引入 SPIFFE/SPIRE、服务网格、证书自动轮换或独立工作负载密钥托管。
- 不改 Nacos 注册客户端的共享证书、RocketMQ/Seata 的客户端身份与中间件客户端证书。
- 不为每个服务拆出独立的“客户端证书”文件；服务证书本身就是该服务的工作负载身份。

## Decisions

### 一张双用途证书代表一个工作负载

服务证书同时具备 `serverAuth` 与 `clientAuth`，既用于 `server.ssl`，也用于出站调用。这样每个服务只有一个身份和一份私钥，`iboot` bundle 不需要拆成服务端/客户端两套；与 SPIFFE SVID 的常见做法一致。

替代方案是“服务端证书 + 独立客户端证书”两套文件。它能在密钥泄露时缩小影响面，但会让十个服务各多一份私钥、两个 bundle 与两套路径，而本地基线的收益不足以抵消复杂度。

### 用本地 CA 的 openssl 签发叶子证书

mkcert 只签发 `serverAuth` EKU，JSSE 在客户端证书校验时要求 `clientAuth`，因此把 `tls-init.sh` 的叶子签发改为 `openssl req` + `openssl x509 -req`，仍使用 mkcert 生成的同一套 `rootCA.pem` / `rootCA-key.pem`。CA 与 `truststore.p12` 不变，浏览器信任不受影响；`--check` 增加 `openssl x509 -purpose` 的 `SSL client` 检查，旧证书会在下次执行时自动重签。

### 服务端只做链校验，不做调用方授权

`server.ssl.client-auth: need` 配合 bundle 的 truststore，只验证客户端证书链。任何由本地 CA 签发的服务证书都能通过，这是本批的明确边界；按调用方做授权需要额外的身份映射与策略，留待后续。

### 出站客户端复用现有 bundle

`spring.http.clients.ssl.bundle: iboot` 已覆盖自动配置的 `RestClient`、各服务的 `loadBalancedRestClientBuilder` 与 JWKS 拉取；这些客户端会自动把本服务证书作为客户端证书发出。网关是 WebFlux + Reactor Netty，额外设置 `spring.cloud.gateway.server.webflux.httpclient.ssl.ssl-bundle: iboot`，让 `lb://` 与静态 HTTPS 路由都使用同一身份。

门户 nginx 在 `http` 级设置 `proxy_ssl_certificate` / `proxy_ssl_certificate_key`；宿主机 Vite 的 `http-proxy` 只从 `target` 对象读取客户端证书，因此把网关与运维目标改为 `URL` 并在其上挂载 `ca` / `cert` / `key`。两者都使用门户证书，身份与 nginx 一致。

Prometheus 与 blackbox 各自使用 `clients/prometheus.pem` 与 `clients/blackbox.pem`，避免用门户身份冒充探针；两者都已在 `/certs` 挂载范围内，配置只需增加 `cert_file` / `key_file`。

### 回归客户端使用门户证书

回归脚本直连 8080–8089 时使用 `docker/certs/portal.pem` 作为客户端证书，而不是某个服务的证书。门户本来就是这些服务的合法调用方，脚本扮演门户比冒充 `iboot-system` 更符合真实链路，也避免回归本身掩盖“服务证书不可互换”的检查。

### 健康检查继续用 TCP

服务容器的健康检查只做 TCP 端口探活，不发起 TLS 握手，因此不会因为缺少客户端证书而误判。门户与数据面的健康检查已有 CA 配置，保持不变。

## Risks / Trade-offs

- **旧客户端全部失效** → 迁移步骤强制 `tls-init.sh --force` 并重建全部服务、门户、Prometheus 与 blackbox；文档给出无证书排障命令。
- **JSSE 拒绝 `serverAuth`-only 证书** → 叶子签发改为双 EKU，并在 `--check` 与回归中检查 `SSL client` 用途。
- **网关上游未使用 bundle** → 显式增加 gateway `ssl-bundle` 配置，并用 `/api/user/list` 的 401/403 与门户链路做端到端验证。
- **Nacos 注册仍用共享门户证书** → 在文档与 design 中登记为已知边界；它不经过业务服务端口，不影响本批 mTLS。
- **回归脚本漏带证书** → 在 `lib.sh`、`dev-init.sh`、`api-snapshot.sh`、APM/日志脚本中统一增加客户端证书参数，并在全量回归中验证。
- **证书探针只覆盖一个服务端点** → 所有叶子证书同批签发、到期时间一致，现有 `probe_ssl_earliest_cert_expiry` 仍能反映整体到期；拆分到期时间前必须扩展探针。

## Migration Plan

1. 执行 `scripts/tls-init.sh --force`，生成双 EKU 的服务/门户证书与探针客户端证书。
2. 更新 `iboot-tls.yml`、网关配置、门户 nginx/Vite、Prometheus/blackbox 与回归脚本。
3. 重建十个服务、门户、Prometheus 与 blackbox；确认健康检查、门户链路、指标抓取与证书探针。
4. 执行 `scripts/regress/regress.sh` 与内部 TLS 负向用例。
5. 回滚：移除 `server.ssl.client-auth` 并重建服务；证书仍可保留，旧的单向 TLS 客户端恢复可用。
