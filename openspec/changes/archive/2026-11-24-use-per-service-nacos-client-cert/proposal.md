# Proposal

## Why

Nacos gRPC 已开启 `mutualAuth`，但十个服务都以共享 `portal.pem` 作为客户端证书注册；Nacos 侧无法从证书区分
是哪个工作负载，单个服务证书泄露或轮换也会影响全部服务。该边界已登记在 `docs/27` §18.5。

## What Changes

- 每个服务改用自己已有的 `docker/certs/services/iboot-<service>.pem` 与私钥连接 Nacos。
- Docker 部署把每服务证书挂载到稳定路径 `/certs/nacos-client.pem` 与 `/certs/nacos-client-key.pem`，
  `x-app-env` 的 Nacos 客户端属性指向该路径，避免为十个服务复制整段 `JAVA_TOOL_OPTIONS`。
- 宿主机开发由 `dev-init.sh` 直接指向 `services/iboot-<service>.pem`。
- 回归断言每个服务实际使用的证书指纹等于自己的服务证书，且 Nacos 客户端属性不再引用 `portal.pem`。
- 同步更新 `docs/27` §18.5、`docs/23`、`docs/08` 与 `docker/README.md`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`：新增“Nacos 客户端注册使用每服务工作负载证书”要求，收窄共享 `portal.pem` 的边界。

## Impact

- 配置：`docker/docker-compose.yml`、`scripts/dev-init.sh`。
- 回归：新增 `scripts/regress/nacos-client-cert.sh`，并入 `scripts/regress/regress.sh`。
- 运行时：十个服务连接 Nacos 9848 时提交的客户端证书。
- 文档：`docs/27`、`docs/23`、`docs/08`、`docker/README.md`。
