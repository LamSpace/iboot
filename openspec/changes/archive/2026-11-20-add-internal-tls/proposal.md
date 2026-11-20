# Proposal

## Why

B1 已经把浏览器与 CLI 到门户的公开入口切到 HTTPS，但门户到网关、服务之间、JWKS 拉取，以及服务到 MySQL / Redis / Silo / Neo4j 的流量仍是明文。B0 已证明这些组件在本地可以启用 TLS；B2 的目标是在不改业务接口的前提下，把业务数据面的传输层补齐，使抓包看不到业务数据与凭据。

## What Changes

- **网关 HTTPS**：网关 `8088` 从 HTTP 改为 HTTPS；门户 nginx 与 Vite 的上游同步改为 `https://`。网关到各业务服务的路由也改为 HTTPS。
- **服务端 TLS**：九个业务服务与运维控制台通过共享的 Spring SSL bundle 启用 `server.ssl`；服务之间、服务到认证 JWKS 的调用使用同一份本地 CA 校验。
- **内部 HTTP 客户端**：替换 `JwkSetProvider`、`ServiceClientConfig`、`AgentToolConfig` 中不感知 SSL bundle 的 `SimpleClientHttpRequestFactory` / 裸 `RestClient.builder()`；负载均衡客户端也走 HTTPS。
- **核心数据面**：MySQL 开启 TLS 并设置 `require_secure_transport=ON`，Druid 使用 `VERIFY_IDENTITY`；Redis 只监听 TLS 端口，Lettuce 使用 `rediss` 语义；Silo 挂载证书、endpoint 改 HTTPS；Neo4j（`graph` profile）使用 `bolt+s` / `neo4j+s`。服务到数据面的密码与数据不再明文传输。
- **BREAKING**：内部地址从 `http://` 改为 `https://`；直接访问服务端口、健康检查、回归快照与内部脚本必须同步更新。网关与业务服务不再同时提供明文端口。
- **回归与容器**：`SVC_*` 直连地址改为 HTTPS 并共享 `SVC_CACERT`；新增内部 TLS 负向矩阵；`dev-init.sh` 的健康等待改用 HTTPS；compose 为数据面挂载证书并更新健康检查。
- **文档**：扩展 `docs/27-TLS 与零信任操作指南.md` 的 B2 章节，更新配置、部署、微服务底座、快速开始与脚本索引。

**非目标**：Nacos 注册/配置中心 TLS；ES / Kafka（B3）；RocketMQ / Seata（B3/B5）；服务间 mTLS 与工作负载身份（B4/B5）；端口收敛与网络分段（B4）；每服务独立数据库账号；生产公网 CA。Nacos 注册流量在 B2 结束时仍为明文，这一边界会在 spec 与 ROADMAP 中显式登记。

## Capabilities

### New Capabilities

无。`transport-security` 已在 B1 创建，本变更在其上增加内部与数据面要求。

### Modified Capabilities

- `transport-security`: 增加服务端口 TLS、内部客户端信任链、MySQL/Redis/Silo/Neo4j 传输加密与内部负向门禁。
- `api-gateway`: 网关自身以 HTTPS 提供服务，并以 HTTPS 访问业务服务。
- `database-lifecycle`: MySQL 连接必须使用 TLS，服务端拒绝明文连接。
- `file-storage`: Silo 内部/公开 endpoint 使用 HTTPS，预签名 URL 使用 HTTPS。
- `knowledge-graph`: Neo4j 连接使用 `bolt+s` / `neo4j+s`。
- `api-contract-regression`: 契约快照与内部回归使用 HTTPS 服务地址与本地 CA。
- `container-deployment`: 数据面证书挂载、服务 TLS 配置与健康检查使用 CA。

## Impact

- **共享内核**：`iboot-kernel` 的 SSL bundle 共享配置、`JwkSetProvider` 与安全自动配置。
- **网关与业务服务**：`iboot-gateway` 路由 scheme/信任库；九个业务服务与 `iboot-ops` 的 `server.ssl`、服务间客户端与健康检查。
- **数据面客户端**：`iboot-system` / `iboot-file` 的 MySQL、Redis、Silo；`iboot-ai` 的 Neo4j；`iboot-monitor` 的中间件资源检查。
- **脚本与编排**：`scripts/tls-init.sh`（如需服务密钥库）、`scripts/dev-init.sh`、`scripts/regress/*`、`docker/docker-compose.yml`、`.env.example`。
- **文档**：`docs/27`、`docs/10`、`docs/08`、`docs/23`、`docs/05`、`docker/README.md`、`scripts/README.md`、`ROADMAP.md`。
- **兼容性**：直接使用 `http://localhost:808x` 的客户端、脚本与健康检查失效；Nacos 注册地址仍为 `http://`，但业务调用已是 HTTPS。
