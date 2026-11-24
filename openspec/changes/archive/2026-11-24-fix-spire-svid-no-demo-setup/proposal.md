# Proposal

## Why

CI 全栈回归（run `45335019874`）在 `dev-init.sh` 的 SVID setup 失败：`spire-up.sh` 将 Server/Agent
启动与 demo 工作负载验证绑定，demo 的 Docker attestor 在 CI 上偶发失败，导致服务 SVID 初始化被阻断。
服务 SVID 并不需要 demo。

## What Changes

- `spire-up.sh` 新增 `--no-demo`：只启动 Server/Agent 并等待健康后退出。
- `spire-svid-sync.sh --setup` 使用 `--no-demo`，失败时输出启动日志的最后 10 行。
- demo 验证仍由 `spire-svid.sh` 回归单独执行。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。

## Impact

- 脚本：`scripts/spire-up.sh`、`scripts/spire-svid-sync.sh`。
