# Proposal

## Why

GitHub 全栈回归（run `45336429392`）的 12g 失败：CI 设置 `IBOOT_OPS_ENABLED=false`，
`iboot-ops` 未启动，但 SVID 回归仍要求其进程加载 SVID。其余检查均通过。

## What Changes

- `scripts/regress/spire-svid-services.sh` 在 `IBOOT_OPS_ENABLED=false` 时跳过 `iboot-ops` 进程检查；
  SVID 文件与 mTLS 检查保持不变。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。

## Impact

- 回归脚本：`scripts/regress/spire-svid-services.sh`。
