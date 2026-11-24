# Proposal

## Why

CI 全栈回归（run `45332078062`）的 SVID setup 已成功，但 `iboot-system` 启动失败：
Redis 客户端复用 `IBOOT_TLS_CERT_FILE`（已切到 SVID），而 Redis 服务端只在启动时加载
`/certs/rootCA.pem`，此时尚未合并 SPIRE CA，返回 `unknown_ca`。

## What Changes

- `spire-svid-sync.sh` 在合并 SPIRE CA 后重启 Redis 并等待 healthy，使 Redis 的
  `tls-ca-cert-file` 包含 SPIRE CA；保留 Prometheus/blackbox 重启。
- 不改变 SVID 来源、SAN 或服务业务配置。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。

## Impact

- 脚本：`scripts/spire-svid-sync.sh`。
