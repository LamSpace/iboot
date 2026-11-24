# Design

## Context

宿主机开发的十个 Java 服务通过 `IBOOT_TLS_CERT_FILE`/`IBOOT_TLS_KEY_FILE` 加载 PEM；Nacos 发现地址
为回环。SPIRE SVID 支持 DNS/URI SAN，但不支持 IP SAN；SVID 的私钥必须可被宿主机进程读取。
Spring Boot 文件型 TLS bundle 没有热加载，SVID 刷新后需要重启。

## Decisions

- 每个服务对应一个 Docker label `com.iboot.svid.service=<service>` 的短生命周期工作负载容器，
  通过 Agent socket 取回 SVID；容器以宿主机 UID 运行，私钥归当前用户。
- 条目同时签发 `-dns <service>`、`-dns localhost`、`-dns host.docker.internal`：
  - 服务名用于 Docker 发现/容器内客户端；
  - localhost 用于宿主机发现与健康检查；
  - host.docker.internal 用于 Prometheus/blackbox 的宿主机抓取。
- 宿主机 Nacos 发现地址在 SVID 模式下使用 `localhost`，端口区分服务；避免 IP SAN 限制。
- SPIRE upstream CA 合并进 `rootCA.pem` 信任 bundle，`tls-init.sh --sync-runtime` 重建信任库；
  Prometheus/blackbox 重启后加载新 bundle。
- `ca_ttl=720h` 与 `x509SVIDTTL=2592000` 将 SVID 有效期放宽到 30 天；`--refresh --restart host`
  用于到期前滚动刷新。条目变更后重启 Agent，避免 SVID 缓存。
- Compose 全量模式不改：服务仍挂载 mkcert 证书；SVID 切换只作用于 `dev-init.sh` 宿主路径。

## Risks / Trade-offs

- [SVID 不支持 IP SAN] → 宿主机发现统一用 localhost；Docker 全量模式不在此变更。
- [无热加载] → 30 天 TTL + 手动/脚本 refresh；到期前执行 refresh 并滚动重启。
- [Agent 缓存] → setup 时重启 Agent。
- [Prometheus 信任 bundle] → 合并 CA 后重启 Prometheus/blackbox；回归覆盖。
- [回退] → `IBOOT_SVID_ENABLED=false` 回到 mkcert，无需改服务代码。

## Migration Plan

1. 合并后 `dev-init.sh` 默认执行 SVID setup；服务重启即切换。
2. 回退：`IBOOT_SVID_ENABLED=false dev-init.sh --stop && dev-init.sh`。
3. 到期前：`scripts/spire-svid-sync.sh --refresh --restart host`。

## Open Questions

无。
