# Proposal

## Why

B3 完成后，业务与数据面已基本 TLS 化，但 Nacos 的 HTTP API、控制台与 gRPC 注册通道仍是明文。服务注册、配置拉取与 Sentinel 规则都经过 Nacos，这是当前内网最大的明文链路。B0 已验证 Nacos 3.3 可以启用 HTTPS 与 gRPC TLS，本变更把已验证的配置落到开发环境、容器编排与回归中。

## What Changes

- **Nacos 服务端**：8848 API 与控制台 8080 改为 HTTPS；9848 gRPC 启用 TLS 并要求客户端证书（mTLS）；加载本地 PKCS12 keystore 与 CA。
- **Nacos 客户端**：所有服务通过 JVM 系统属性启用 Nacos client gRPC TLS 与客户端证书，使用本地 CA 校验，禁止 `trustAll`。
- **控制台与脚本**：门户 Vite 的 Nacos 代理改为 HTTPS；`dev-init.sh` 健康检查、`sentinel-init.sh`、Docker 控制台 URL 全部改为 HTTPS + `--cacert`。
- **BREAKING**：Nacos 不再接受明文 HTTP；直接使用 `http://localhost:8848`、`http://localhost:8849` 的命令失效；客户端缺少 TLS 系统属性时无法注册或拉取配置。
- **回归与文档**：新增 Nacos TLS 正负向检查，更新 TLS 指南、服务发现/配置、部署、快速开始与容器说明。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`: 增加 Nacos HTTP/gRPC TLS、客户端信任链与明文拒绝要求。
- `service-discovery`: 注册、发现与配置拉取必须通过 TLS 完成。
- `container-deployment`: Nacos 证书挂载、HTTPS 健康检查、控制台 URL 与客户端 JVM 参数编排。

## Impact

- `docker/docker-compose.yml` 的 Nacos 服务、健康检查、端口与证书挂载；所有后端服务的 `JAVA_TOOL_OPTIONS`。
- `scripts/tls-init.sh` 生成 Nacos keystore；`scripts/dev-init.sh` 注入客户端 TLS 属性与 HTTPS 等待；`scripts/sentinel-init.sh`、`scripts/docker-deploy.sh`。
- `iboot-portal/vite.config.ts` 的 Nacos 代理；`.env.example` 的 Nacos 控制台地址。
- 文档：`docs/27`、`docs/23`、`docs/10`、`docs/05`、`docs/03`、`docker/README.md`、`ROADMAP.md`。
