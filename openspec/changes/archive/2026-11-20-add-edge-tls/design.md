# Design

## Context

See `proposal.md` for motivation and the B0 report at `docs/spike/tls-zero-trust-spike-report.md` for the measured constraints. Current state that shapes this change:

- 门户 nginx 只监听 80；Vite 开发服务器在 5173（`docs/05` 仍写 3000，属于本次要修的陈旧值）。
- 认证 issuer 默认 `http://localhost:8088`；门户回调默认 `http://localhost:5173/callback`；CLI 默认 context 指向 `http://localhost:8088`；回归默认直连 `http://localhost:8080/8083/8088`。
- 门户 Dockerfile 在构建阶段复制 `.env`，因此 `VITE_OIDC_ISSUER` 必须在构建期确定；compose 目前只发布 80。
- B0 已证明 mkcert 信任链、Vite HTTPS/HMR、容器内 CA 校验、MySQL/Redis/Silo 的 TLS 均可落地；Nacos、RocketMQ、Seata 的问题属于 B2/B3，不在本次范围。

## Goals / Non-Goals

**Goals:**

- 浏览器与 CLI 的公开入口只通过 HTTPS 提供页面、OIDC 与 API。
- 一套可重复生成的本地 CA/证书/信任库，供门户、CLI、回归与后续 B2 复用。
- 认证 issuer、门户回调、CLI、回归脚本在一个运行模式内使用同一个公开来源。
- 保留显式、仅在排障时使用的明文回退。

**Non-Goals:**

- 网关、服务间与数据面 TLS（B2/B3）。
- mTLS、工作负载身份、端口收敛与网络分段（B4/B5）。
- 生产公网 CA、cert-manager、浏览器自动信任安装。
- 修改令牌模型、权限模型或业务接口。
- RocketMQ / Seata 的 TLS 例外处理（B5 单独决定）。

## Decisions

### D1. 公开来源按运行模式固定，而不是固定网关端口

- 宿主机开发：`https://localhost:5173`（Vite 开发服务器）。
- Docker 全量部署：`https://localhost`（门户 nginx 443）。

OAuth2 issuer 必须等于浏览器实际访问的授权服务器来源。宿主机开发若把 issuer 固定到 `https://localhost:8443` 的网关，会额外暴露一个公开入口；若让 Vite 监听 443，则需要 root 权限；若在宿主机再加一个 443 反代，则引入一个常驻特权组件。按运行模式设置 issuer，每个环境仍然只有一个客户端入口，改动面最小。

**Alternatives considered:**

- 网关 `https://localhost:8443` 作为稳定 issuer：拒绝。多一个公开入口，且 B4 收敛端口时仍要改 issuer。
- 宿主机 nginx/443：拒绝。新增守护进程与端口占用，开发体验变差。
- Vite 监听 443：拒绝。需要 root，跨平台不一致。

**Trade-off**: 存在两个 issuer 值，但它们分属两套独立启动方式，由 `dev-init.sh` 与 compose 各自注入，不共享运行期状态。

### D2. 门户是唯一公开入口，OIDC 与 `/api` 由门户反代到 HTTP 网关

B1 只加密“浏览器/CLI → 门户”这一段，门户到网关仍为 HTTP，符合“边缘优先、内部随后”的批次边界。门户 nginx 与 Vite 增加以下反代，全部指向现有网关：

- `/oauth2/**`、`/.well-known/**`、`/login`、`/connect/**`：OIDC 与登录页；必须保留原始 Host，并把 `X-Forwarded-Proto: https` 传给上游。
- `/api/**`：供 CLI 使用，直接转发到网关，不做重写。
- 既有 `/iboot/api`、`/iboot/ai` 保持不变。

Vite 的 OIDC 代理必须 `changeOrigin: false`，否则认证服务看到的 Host 变成 `localhost:8088`，生成的回调/页面地址会与公开来源不一致；`/api` 与 `/iboot/api` 可以使用 `changeOrigin: true`。nginx 的 OIDC location 显式设置 `proxy_set_header Host $host` 与 `X-Forwarded-Proto $scheme`。

**Alternatives considered:**

- 浏览器直连网关的 HTTPS 端口：拒绝。形成第二个公开入口，违背“门户是唯一客户端入口”。
- 把 OIDC 路径改写后转发：拒绝。认证服务依赖原始路径与 Host 生成发现文档和登录页。

### D3. 证书由 `scripts/tls-init.sh` 生成，mkcert 是唯一生成器

证书目录固定为 `docker/certs/`（已被 `.gitignore` 覆盖），结构：

```text
docker/certs/
├── ca/                    # mkcert CAROOT；rootCA-key.pem 仅本机可读
├── portal.pem             # 叶子证书，SAN 覆盖 localhost / 127.0.0.1 / ::1 / *.iboot.local / 服务名
├── portal-key.pem
├── rootCA.pem             # PEM CA，供 nginx / Node / curl
└── truststore.p12         # Java 信任库
```

脚本行为：

- 默认在证书不存在或剩余有效期不足 30 天时生成/重签；`--force` 强制重签。
- 调用 `mkcert` 生成 CA 与叶子证书；mkcert 缺失时明确失败并给出安装命令，不退回手写 openssl。
- 用 `keytool` 生成 PKCS12 信任库；私钥权限 0600。
- `mkcert -install` 是独立的、需要 sudo 的浏览器信任步骤；脚本只检测并提示，不自行 sudo。
- 输出证书路径，供 `dev-init.sh` 与 `docker-deploy.sh` 复用。

**Alternatives considered:**

- step-ca：能力更接近生产，但多一个常驻服务，B0 已明确不引入。
- 手写 openssl 脚本：可控但易错、SAN/轮换维护成本高；B0 报告建议避免。
- 每个组件各自自签：无法形成统一信任链，回归无法证明“去掉 CA 必须失败”。

### D4. 认证服务按模式注入 issuer、回调与 Cookie 安全属性

- 宿主机开发：`IBOOT_AUTH_ISSUER=https://localhost:5173`，回调 `https://localhost:5173/callback`，登出回调 `https://localhost:5173/login`。
- Docker：`IBOOT_AUTH_ISSUER=https://localhost`，回调 `https://localhost/callback`，登出回调 `https://localhost/login`。
- CLI 的 loopback 回调保持 `http://127.0.0.1:8765/callback`，属于原生客户端例外。
- 认证服务启用转发头处理（`server.forward-headers-strategy=framework`），使 `X-Forwarded-Proto` 生效；浏览器会话 Cookie 设置 `Secure`。直接访问认证服务的 HTTP 端口不再作为浏览器登录入口。

issuer 变化后，旧令牌的 `iss` 与新 issuer 不一致。资源服务当前只验签不校验 issuer，但门户与 CLI 的本地凭据仍可能让用户误以为“已登录”。迁移步骤要求清理旧浏览器令牌与 CLI 凭据并重新登录，避免新旧 issuer 混用。

### D5. CLI 默认 context 指向宿主机公开来源，明文 base URL 被拒绝

- `Context.defaults()` 改为 `baseUrl=https://localhost:5173`、`issuer=https://localhost:5173`。
- Docker 用户通过 `iboot config add docker --base-url https://localhost --issuer https://localhost` 建立独立 context；凭据按 context 隔离。
- `Context` 校验：base URL 以 `http://` 开头时拒绝；issuer 允许 loopback HTTP（回调/本地例外），其余 `http://` 拒绝。
- `bin/iboot` 读取 `IBOOT_CLI_TRUSTSTORE` 与 `IBOOT_CLI_TRUSTSTORE_PASSWORD`，注入 `-Djavax.net.ssl.trustStore*`；未设置时保持 JDK 默认，证书失败时给出指向 `docs/27` 的错误提示。

**Alternatives considered:**

- 默认指向 Docker 的 `https://localhost`：拒绝。宿主机开发是主要开发路径，默认应开箱即用；Docker 用户显式新增 context。
- CLI 内置 CA 文件：拒绝。把证书生命周期耦合进 CLI 二进制，轮换需要重新发版。

### D6. 回归统一经公开来源执行，CA 显式传入

- `scripts/regress/lib.sh` 默认：`REG_BASE=https://localhost:5173`、`REG_AUTH_BASE=https://localhost:5173`、`REG_GATEWAY=https://localhost:5173`、`REG_OIDC_REDIRECT=https://localhost:5173/callback`；`REG_CACERT` 默认 `docker/certs/rootCA.pem`。
- Docker 全量回归通过环境变量覆盖为 `https://localhost` 与 `https://localhost/callback`。
- 所有 `curl` 增加 `--cacert "$REG_CACERT"`；登录流程仍用 cookie jar、PKCE 与表单提交，只是来源改为 HTTPS 门户。
- 新增 `scripts/regress/tls.sh`，覆盖：HTTP→301、无 CA 失败、TLS 1.0/1.1 拒绝、证书剩余有效期、issuer 一致、明文令牌端点不可用。
- CI 全栈任务先安装 mkcert、执行 `tls-init.sh`，再以 `REG_CACERT` 与 HTTPS 来源运行；PR 静态任务不依赖证书。
- CI 的公开来源是 `https://localhost:5173`，因此全栈任务必须启动门户开发服务器（`dev-init.sh --frontend`），让回归走与本地开发相同的 OIDC/`/api` 反代路径。若 CI 不启动前端，回归会失去“公开来源一致”的证明；不接受用直连网关的路径替代。

**Alternatives considered:**

- 回归继续直连各服务 HTTP 端口：拒绝。这样只能证明服务可用，不能证明公开入口与 OIDC 一致。
- 用 `-k` 简化：拒绝。会让“明文/错误证书必须失败”的负向判据失效。

### D7. 容器部署的证书与 issuer 注入

- 门户 Dockerfile 构建阶段接受 `ARG VITE_OIDC_ISSUER=https://localhost` 并以环境变量形式提供给 Vite；进程环境变量优先于 `.env`，因此不会改变宿主机开发使用的 `.env`。
- compose 门户服务：发布 `80:80` 与 `443:443`；只读挂载 `./certs/portal.pem`、`./certs/portal-key.pem` 与 `./certs/rootCA.pem`；`EXPOSE 80 443`。
- 门户健康检查使用本地 CA（`SSL_CERT_FILE` 或显式 `--cacert` 等价物）。若 BusyBox `wget` 不支持 CA 参数，则在镜像中加入一个最小校验方式（例如 `openssl s_client -CAfile`），**不允许**以 `--no-check-certificate` 通过。
- `docker-deploy.sh` 在构建/启动前调用 `tls-init.sh`；证书缺失时明确失败。宿主机开发由 `dev-init.sh` 调用同一脚本并导出 `NODE_EXTRA_CA_CERTS`、issuer 与回调变量。

### D8. 明文回退的范围

- 宿主机开发：`IBOOT_TLS_ENABLED=false` 时 Vite 以 HTTP 启动，脚本输出明文模式警告；回归与验收不得在该模式下判定通过。
- Docker：B1 不提供明文回退；回滚通过上一版镜像/上一份 compose 或 `git revert` 完成。理由是 nginx 配置的静态证书路径无法在运行期安全地降级；为一个排障开关维护两套 nginx 配置不值得。

### D9. 文档与进度

- 新增 `docs/27-TLS 与零信任操作指南.md`，作为证书生成、信任分发、模式来源、排障与回退的唯一操作权威。
- `ROADMAP.md` 增加“开发者安全基线（本地 TLS/mTLS）”小节，登记 B0–B5 状态；B1 完成后把 B2 标为下一步。
- 文档只保留入口与差异，不复制同一份证书步骤到多处；端口/拓扑事实仍以 `docs/23` 为准。

## Risks / Trade-offs

- [宿主机开发必须同时运行 Vite 才能使用 CLI 登录] → 文档明确 `dev-init.sh --frontend` 是 CLI 的前置；Docker 用户使用 `https://localhost` context。
- [issuer 变化导致旧令牌/凭据混用] → 迁移步骤清理浏览器 localStorage 与 CLI 凭据；验收用全新登录。
- [Vite/nginx 代理 OIDC 时 Host/Proto 处理错误] → OIDC 代理 `changeOrigin: false`、显式 `Host` 与 `X-Forwarded-Proto`；回归校验发现文档 issuer 与登录回调。
- [BusyBox wget 不支持 CA 参数] → 实现时先验证；不支持则用镜像内可用的 `openssl s_client -CAfile` 或加入最小校验工具，禁止 `-k`。
- [mkcert 浏览器信任需要 sudo，无法自动化] → `tls-init.sh` 只提示，不 sudo；B1 验收前由用户执行一次 `mkcert -install`。
- [Docker 无明文回退] → 明确列为 B1 限制；回滚用上一版镜像/compose，不以放宽 TLS 作为回滚手段。
- [证书目录被误提交] → `.gitignore` 覆盖 `docker/certs/`；回归检查 `git ls-files` 不包含私钥。

## Migration Plan

1. 在旧环境生成证书并安装本地 CA；旧入口保持可用。
2. 更新认证 issuer、门户回调、门户 `VITE_OIDC_ISSUER`、CLI context 与回归地址；清理旧浏览器令牌与 CLI 凭据。
3. 启动新门户入口（宿主机 Vite HTTPS 或 Docker 443），执行登录、SSE、文件上传/下载与全量回归。
4. 回滚：宿主机设置 `IBOOT_TLS_ENABLED=false` 并恢复旧 issuer/回调；Docker 回退到上一版镜像与 compose。两种回滚都必须在文档中明确为“排障/回滚”，不得作为验收状态。

## Open Questions

无。B0 已经回答了证书、信任链与 Vite 代理的关键未知项；剩余问题（BusyBox 健康检查的具体命令、认证服务转发头配置）都能在实现阶段用现有环境验证，不改变方案或任务拆分。
