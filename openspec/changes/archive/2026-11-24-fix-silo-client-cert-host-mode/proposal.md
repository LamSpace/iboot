# Proposal

## Why

Silo 客户端证书在 compose 容器模式已启用，但宿主机开发模式由 `dev-init.sh` 启动进程，未注入
`IBOOT_*_SILO_CLIENT_CERT_ENABLED` 与 keystore 路径，导致 CI 全栈回归的 file/AI 服务仍走 access key，
12e 检查失败。

## What Changes

- `dev-init.sh` 为 `iboot-file`/`iboot-ai` 注入 `IBOOT_<SVC>_SILO_CLIENT_CERT_ENABLED=true` 与
  `docker/certs/services/<service>.p12`。
- 更新 `docs/27`，说明容器与宿主机两种注入方式。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。仅补齐宿主机模式的配置注入。

## Impact

- 脚本：`scripts/dev-init.sh`。
- 文档：`docs/27`。
