# 27-TLS 与零信任操作指南

> 适用版本：iBoot 1.0.0。本文是本地 TLS 的唯一操作权威：证书生成、公开来源、
> 客户端信任、验收、排障与回滚。B0 的实测结论见
> [TLS 与零信任 Spike 报告](./spike/tls-zero-trust-spike-report.md)。

## 1. 范围与边界

当前阶段只加密**浏览器/CLI → 门户**这一段（L1 边缘 TLS）：

- 门户是唯一公开入口；宿主机开发用 Vite，Docker 全量部署用门户 nginx 443。
- 门户到网关、服务间、MySQL/Redis/Silo 等数据面仍是明文，属于 B2/B3。
- 网关 `localhost:8088` 保留为内部/调试地址，**不再是浏览器或 CLI 的客户端入口**。
- 不引入本机服务网格、SPIFFE/OPA；mTLS 与工作负载身份留给 B4/B5。

## 2. 证书

### 2.1 生成

```bash
scripts/tls-init.sh          # 不存在或剩余有效期 < 30 天时生成/重签
scripts/tls-init.sh --force  # 强制重签
scripts/tls-init.sh --check  # 只检查有效期
```

证书写入 `docker/certs/`（已被 `.gitignore` 忽略）：

| 文件 | 用途 |
|---|---|
| `ca/rootCA.pem` | 本地 CA，供 nginx / Node / curl |
| `ca/rootCA-key.pem` | CA 私钥，权限 0600，不入仓 |
| `portal.pem` / `portal-key.pem` | 门户叶子证书与私钥 |
| `truststore.p12` | Java 信任库，口令 `changeit` |

叶子证书的 SAN 覆盖 `localhost`、`127.0.0.1`、`::1`、`host.docker.internal`、
`*.iboot.local` 与全部服务名；B2 可直接复用同一份信任链。

脚本依赖 `mkcert` 与 `keytool`。mkcert 缺失时：

```bash
# Ubuntu
sudo apt-get install -y mkcert libnss3-tools
# 或从源码构建
go install filippo.io/mkcert@v1.4.4
export PATH="$(go env GOPATH)/bin:$PATH"
```

### 2.2 浏览器信任

`mkcert -install` 需要写系统/浏览器信任库，可能需要管理员权限；脚本不会代为执行：

```bash
CAROOT=docker/certs/ca mkcert -install
```

完成后重启浏览器。未执行该步骤时，浏览器会告警；CLI 与 curl 不受影响，它们使用
`docker/certs/` 下的 CA。

### 2.3 轮换与过期

- 叶子证书剩余有效期小于 30 天时，`tls-init.sh` 会重签，`scripts/regress/tls.sh`
  会失败并提示。
- 重签后需要重启门户（Vite/nginx）与使用证书的容器；浏览器无需重新安装 CA。
- CA 私钥只在本机；删除 `docker/certs/ca/` 会使全部叶子证书失效，需要重新生成并重新信任。

## 3. 公开来源

OAuth2 issuer 必须等于浏览器实际访问的授权服务器来源。两种运行模式各有一个公开来源：

| 模式 | 公开来源 | 门户 | 认证 issuer | CLI context |
|---|---|---|---|---|
| 宿主机开发 | `https://localhost:5173` | Vite | `https://localhost:5173` | `dev`（默认） |
| Docker 全量 | `https://localhost` | 门户 nginx 443 | `https://localhost` | `docker` |

门户反代以下路径到网关：`/oauth2/**`、`/.well-known/**`、`/login`、`/connect/**`、
`/api/**`（CLI）以及既有的 `/iboot/api`、`/iboot/ai`。OIDC 路径必须保留原始 Host；
Vite 的 OIDC 代理使用 `changeOrigin: false`，并转发 `X-Forwarded-Proto: https`。

## 4. 配置

### 4.1 宿主机开发

`scripts/dev-init.sh` 自动完成：

- 调用 `scripts/tls-init.sh`；
- 导出 `IBOOT_PUBLIC_ORIGIN=https://localhost:5173`；
- 推导 `IBOOT_AUTH_ISSUER`、`IBOOT_AUTH_REDIRECT_URI`、`IBOOT_AUTH_LOGOUT_REDIRECT_URI`；
- 导出 `NODE_EXTRA_CA_CERTS=docker/certs/ca/rootCA.pem`；
- 启动门户开发服务器并等待 `https://localhost:5173` 就绪。

```bash
scripts/dev-init.sh --frontend
# 门户：https://localhost:5173
```

### 4.2 Docker 全量部署

```bash
scripts/docker-deploy.sh up
# 门户：https://localhost
```

`docker-deploy.sh` 在构建/启动前调用 `tls-init.sh`；证书缺失会在拉起容器前失败。
compose 只读挂载 `docker/certs/` 到门户容器，门户健康检查用本地 CA 校验 443。
门户构建参数 `IBOOT_PORTAL_OIDC_ISSUER` 默认 `https://localhost`，不会复用宿主机开发的
`VITE_OIDC_ISSUER`。

### 4.3 CLI

```bash
export IBOOT_CLI_TRUSTSTORE="$PWD/docker/certs/truststore.p12"
./bin/iboot login
```

宿主机开发默认 context `dev` 指向 `https://localhost:5173`，需要 Vite 正在运行。
Docker 部署新增独立 context：

```bash
./bin/iboot config add docker \
  --base-url https://localhost --issuer https://localhost --use
```

`bin/iboot` 读取 `IBOOT_CLI_TRUSTSTORE` / `IBOOT_CLI_TRUSTSTORE_PASSWORD` 并注入
`javax.net.ssl.trustStore`。非 loopback 的 `http://` base URL 会被拒绝。

### 4.4 回归

宿主机回归需要门户开发服务器正在运行（`scripts/dev-init.sh --frontend`）；Docker 全量部署
需要门户容器健康。

```bash
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/regress.sh
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/tls.sh
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" \
IBOOT_CLI_TRUSTSTORE="$PWD/docker/certs/truststore.p12" scripts/regress/cli.sh
```

Docker 全量部署覆盖为 `https://localhost` 与 `https://localhost/callback`。回归脚本
禁止用 `-k` 绕过证书校验。

## 5. 验收

1. `curl --cacert docker/certs/ca/rootCA.pem https://localhost:5173/`（或 Docker 的
   `https://localhost/`）返回 200；不带 CA 必须失败。
2. 浏览器完成登录、登出、SSE 推送、文件上传/下载；无证书告警。
3. OIDC 发现文档的 issuer 与公开来源一致：
   `curl --cacert ... https://localhost:5173/.well-known/openid-configuration`。
4. `scripts/regress/tls.sh` 全部通过；`regress.sh`、`cli.sh` 通过。
5. 证书剩余有效期不少于 30 天。

## 6. 排障

| 现象 | 处理 |
|---|---|
| 浏览器证书告警 | 执行 `CAROOT=docker/certs/ca mkcert -install`，重启浏览器 |
| `curl` 证书校验失败 | 加 `--cacert docker/certs/ca/rootCA.pem`；不要用 `-k` |
| CLI 证书错误 | 设置 `IBOOT_CLI_TRUSTSTORE="$PWD/docker/certs/truststore.p12"` |
| Vite 启动失败 `ENOENT portal.pem` | 执行 `scripts/tls-init.sh`；确认 `IBOOT_TLS_CERT_DIR` |
| 登录后回调地址错误 | 确认 issuer、`VITE_OIDC_ISSUER`、`IBOOT_AUTH_REDIRECT_URI` 指向同一公开来源 |
| 登录页 404 或跳回门户首页 | 确认门户/Vite 代理了 `/login`、`/oauth2/**`、`/.well-known/**` |
| Cookie 未带 `Secure` | 确认认证服务 `IBOOT_AUTH_COOKIE_SECURE=true`，且代理转发 `X-Forwarded-Proto: https` |
| Docker 门户不健康 | 确认 `docker/certs/` 三个文件已挂载；查看 `docker compose logs iboot-portal` |
| `https://localhost` 打不开 | 确认 compose 发布了 `443:443`；`docker compose ps` 检查门户健康 |
| 301 循环 | 确认 80 只做 301，443 server 不再重定向 |

## 7. 明文回退与回滚

- **宿主机开发**：`IBOOT_TLS_ENABLED=false scripts/dev-init.sh --frontend` 会以 HTTP 启动
  Vite，并在日志中输出明文模式警告。该模式仅用于排障，回归与验收不得判定通过。
- **Docker**：B1 不提供明文回退；回滚使用上一版镜像与 compose，或 `git revert` 本次变更。
  不要通过关闭 nginx 证书校验或把 443 改回 80 来“绕过”问题。
- 回滚后旧的 `http://localhost:8088` 客户端入口会重新可用，但需要同步恢复认证 issuer、
  门户回调、CLI context 与回归地址；不要在新旧来源之间混用令牌。

## 8. 与后续批次的关系

- B2：门户到网关、服务间、JWKS、MySQL/Redis/Silo/Neo4j 的 TLS；Nacos 的 `file:`
  路径与 `JAVA_TOOL_OPTIONS` 约束见 B0 报告。
- B3：ES/Kafka 日志与检索面；RocketMQ 只能作为加密通道，不能宣称服务端认证。
- B4/B5：mTLS、工作负载身份、端口收敛与网络分段；Seata 无原生 TLS，需要单独决策。
