# Proposal

## Why

十个可部署服务目前共用同一张叶子证书。服务端 TLS 只能证明“持有本地 CA 签发的任意证书”，不能证明“我连到的确实是 `iboot-system`”。任何持有共享私钥的容器都能伪装成其它服务，证书过期探针也只能覆盖一个端点。

## What Changes

- `tls-init.sh` 为每个可部署服务生成独立叶子证书（SAN 含服务名、`localhost`、`127.0.0.1`、`::1`），并输出到 `docker/certs/services/`。
- 十个服务通过 `IBOOT_TLS_CERT_FILE` / `IBOOT_TLS_KEY_FILE` 加载自己的服务端证书；门户、Nacos、Tempo、可观测控制台继续使用共享门户证书。
- 回归新增服务证书 SAN 校验与唯一性校验；内部 TLS 检查继续覆盖全部服务端口。
- 文档登记“服务端身份已拆分、客户端证书仍未启用”的边界。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`: 新增“服务端证书按服务拆分”的要求。

## Impact

- 证书：`scripts/tls-init.sh`、`docker/certs/services/`（仍被 `.gitignore` 忽略）。
- 配置：`iboot-kernel/iboot-tls.yml`（与网关/运维控制台副本）、`docker/docker-compose.yml`、`scripts/dev-init.sh`。
- 回归：`scripts/regress/internal-tls.sh`。
- 文档：`docs/11`、`docs/23`、`docs/27`、`docker/README.md`。
- 运行时：证书重新签发后需重建十个服务；滚动期间新旧证书都受同一 CA 信任。
