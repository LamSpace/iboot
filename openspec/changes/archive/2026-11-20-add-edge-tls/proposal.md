# Proposal

## Why

B0 已经证明本地 L1 传输加密可行，但当前浏览器与 CLI 的公开入口仍是明文：门户 nginx 只监听 80，Vite 没有 HTTPS，OAuth2 issuer 与登记的回调地址固定在 `http://localhost:8088`，CLI 默认 context 也指向该明文地址。在进入 B2 的内部 TLS 之前，必须先把公开入口收敛为 TLS，并建立一套可复用的本地 CA、证书与信任链，否则后续每一层都要重复解决“证书从哪来、谁信任它”。

## What Changes

- **本地 CA 与证书脚本**：新增 `scripts/tls-init.sh`，用 mkcert 生成本地 CA、门户叶子证书与 Java PKCS12 信任库；幂等、可重签；证书目录纳入 `.gitignore`，私钥不入仓；脚本同时检查证书剩余有效期。
- **门户入口 TLS**：门户 nginx 增加 `listen 443 ssl`，80 只做 301 与 HSTS；在 B1 中仅门户入口加密，门户到网关仍为 HTTP，B2 再切换。
- **单一公开来源与 OAuth2 一致**：公开来源按运行模式固定——宿主机开发为 `https://localhost:5173`（Vite），Docker 全量部署为 `https://localhost`（门户 nginx）。认证服务的 issuer、门户回调、登出回调、门户 `VITE_OIDC_ISSUER`、CLI context 与回归脚本 MUST 使用同一个公开来源，不得混用 `http://localhost:8088`。
- **门户反代 OIDC 端点**：nginx 与 Vite 增加 `/oauth2/**`、`/.well-known/**`、`/login`、`/connect/**` 的反代，并保留原始 Host；新增 `/api` 反代供 CLI 使用。网关在 B1 仍为 HTTP，反代目标为现有网关地址。
- **BREAKING**：公开入口从 `http://localhost:8088` 改为 HTTPS 门户来源。已有的 CLI context、书签、回归环境变量与登记的回调地址必须同步更新；旧的明文入口不再作为客户端入口。
- **CLI HTTPS**：默认 context 改为 HTTPS 门户来源；`bin/iboot` 支持注入本地 CA 的 PKCS12 信任库；CLI MUST 拒绝非 loopback 的 `http://` base URL，并文档化 Docker 与宿主机开发两套 context。
- **回归脚本 HTTPS**：`REG_BASE` / `REG_AUTH_BASE` / `REG_GATEWAY` / `REG_OIDC_REDIRECT` 支持 HTTPS 与 `REG_CACERT`；登录、API 与冒烟全部经门户来源访问；明文请求 MUST 失败。
- **容器部署**：门户镜像暴露 443、挂载证书、构建期注入 `VITE_OIDC_ISSUER`；`docker-deploy.sh` 在启动前生成证书；门户健康检查使用本地 CA，不使用 `-k` 绕过。
- **文档**：新增 `docs/27-TLS 与零信任操作指南.md`；同步更新快速开始、部署、配置、CLI、CI、微服务底座与脚本索引。

**非目标**：网关与服务间 TLS、MySQL/Redis/Silo/Neo4j 等数据面 TLS（B2）；ES/Kafka（B3）；mTLS、工作负载身份与网络分段（B4/B5）；生产公网 CA 与 cert-manager；RocketMQ/Seata 的 TLS 例外处理；浏览器证书信任的自动化安装。

## Capabilities

### New Capabilities

- `transport-security`: 本地 CA 与证书生命周期、公开入口仅 TLS、客户端信任链统一分发、证书过期门禁与显式明文回退。

### Modified Capabilities

- `auth-service`: issuer、门户回调与登出回调 MUST 与 HTTPS 公开来源一致，不得在明文入口签发令牌。
- `developer-cli`: 默认 context 与登录 MUST 走 HTTPS，支持本地 CA 信任，拒绝非 loopback 的明文 base URL。
- `api-contract-regression`: 登录与契约回归 MUST 支持 HTTPS + 本地 CA，明文请求 MUST 失败。
- `container-deployment`: 门户对外只提供 443（80 仅重定向）、证书挂载与构建期 issuer 注入、健康检查使用 CA。

## Impact

- **脚本**：新增 `scripts/tls-init.sh`；修改 `scripts/dev-init.sh`、`scripts/docker-deploy.sh`、`scripts/regress/lib.sh`、`scripts/regress/smoke.sh`、`scripts/regress/cli.sh`、`scripts/README.md`。
- **前端**：`iboot-portal/nginx.conf`、`vite.config.ts`、`.env`、`Dockerfile`；`iboot-portal/.dockerignore` 保持 `.env` 可用。
- **认证**：`iboot-auth/src/main/resources/application.yml` 的 issuer 与回调默认值；环境变量由 `dev-init.sh` 与 compose 注入。
- **CLI**：`iboot-cli` 的 `Context.defaults()`、`Context` 说明与 `bin/iboot`；文档与回归 context。
- **部署与回归**：`docker/docker-compose.yml`、`docker/README.md`、`.env.example`、`.gitignore`、GitHub Actions 全栈任务。
- **文档**：`docs/05`、`docs/08`、`docs/10`、`docs/23`、`docs/25`、`docs/26`、`docs/README.md`；新增 `docs/27`；`ROADMAP.md` 增加“开发者安全基线”小节。
- **兼容性**：旧 `http://localhost:8088` 客户端入口失效；CLI context、书签与回归环境变量需更新。保留显式 `IBOOT_TLS_ENABLED=false` 回退，仅用于排障。
