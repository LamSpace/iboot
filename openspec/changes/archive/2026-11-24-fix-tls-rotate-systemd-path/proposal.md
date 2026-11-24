# Proposal

## Why

安装轮换调度后手动触发 `iboot-tls-rotate.service` 失败：systemd user 环境默认 PATH 不含
SDKMAN/JDK 的 `keytool`，`tls-init.sh --check` 直接失败并进入重签，重签又因缺少 keytool 失败。

## What Changes

- `tls-rotate-schedule.sh` 生成的 systemd unit 增加 `Environment="PATH=$PATH"`，固化安装时的 PATH。
- `docs/27 §23.2` 说明 JDK 路径变化后需重新 `--install`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。

## Impact

- 脚本：`scripts/tls-rotate-schedule.sh`。
- 文档：`docs/27`。
