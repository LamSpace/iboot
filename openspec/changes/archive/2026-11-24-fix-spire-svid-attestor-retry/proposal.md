# Proposal

## Why

GitHub 全栈回归（run `45330580803`）在 `dev-init.sh` 的 SVID setup 阶段失败：
`spire-agent api fetch` 返回 `no identity issued`。本地同样路径可通过，判断为 CI 上 Agent 重启后
Docker attestor 尚未稳定，3 次短重试不足。

## What Changes

- `spire-svid-sync.sh --setup` 先重启 Agent 清空 SVID 缓存，再注册条目，并留出稳定时间。
- SVID 取回重试从 3 次增加到 10 次；最后一次失败时输出工作负载容器标签、SPIRE 条目与 Agent 日志。
- 不改变证书来源、SAN 或服务启动路径。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。

## Impact

- 脚本：`scripts/spire-svid-sync.sh`。
