# Tasks

## 1. 本地 CA 与证书脚本

- [x] 1.1 新增 `scripts/tls-init.sh`：调用 mkcert 生成 CA、门户叶子证书（SAN 覆盖 `localhost`、`127.0.0.1`、`::1`、`host.docker.internal`、`*.iboot.local` 与全部服务名）和 PKCS12 信任库；支持幂等、`--force` 重签、剩余有效期检查与明确的 mkcert 缺失提示。验证：连续执行两次不改变已有证书；`openssl x509` 显示 SAN；`keytool -list` 能列出本地 CA；删除证书后重跑可重建。
- [x] 1.2 把 `docker/certs/` 加入 `.gitignore`，私钥权限设为 0600；验证 `git status` 不出现证书文件，`git check-ignore docker/certs/portal-key.pem` 命中，`stat -c %a` 为 600。
- [x] 1.3 更新 `.env.example` 与 `scripts/README.md`：登记 `IBOOT_TLS_ENABLED`、`IBOOT_PUBLIC_ORIGIN`、`IBOOT_TLS_CERT_DIR`、`IBOOT_CLI_TRUSTSTORE`、`REG_CACERT`；验证按文档中的命令可生成证书并找到信任库。

## 2. 认证服务与公开来源

- [x] 2.1 修改 `iboot-auth/src/main/resources/application.yml` 的 issuer、门户回调与登出回调默认值为宿主机开发来源（`https://localhost:5173`），保留环境变量覆盖；验证 `mvn -pl iboot-auth test` 通过，启动后 OIDC 发现文档的 issuer 为 HTTPS。
- [x] 2.2 开启认证服务的转发头处理与安全 Cookie（`server.forward-headers-strategy=framework`、`server.servlet.session.cookie.secure=true` 或等价环境变量）；验证经 HTTPS 门户代理登录后，浏览器收到的会话 Cookie 带 `Secure`，且登录页重定向地址为 `https://`。
- [x] 2.3 在 `scripts/dev-init.sh` 中调用 `tls-init.sh`，导出 `IBOOT_TLS_ENABLED`、`IBOOT_PUBLIC_ORIGIN`、`IBOOT_AUTH_ISSUER`、`IBOOT_AUTH_REDIRECT_URI`、`IBOOT_AUTH_LOGOUT_REDIRECT_URI` 和 `NODE_EXTRA_CA_CERTS`；`IBOOT_TLS_ENABLED=false` 时输出明文模式警告并跳过证书生成。验证：默认启动后 `curl --cacert` 读取发现文档得到 `https://localhost:5173`；关闭开关后日志出现明确警告。
- [x] 2.4 更新 `iboot-portal/.env` 的 `VITE_OIDC_ISSUER` 为 `https://localhost:5173`；验证 `cnpm run build` 产物的 `import.meta.env` 注入为 HTTPS 来源（或构建后搜索产物中的 issuer 字符串）。

## 3. 门户入口（Vite 与 nginx）

- [x] 3.1 修改 `iboot-portal/vite.config.ts`：启用 `server.https`（证书路径来自 `IBOOT_TLS_CERT_DIR` 或默认 `docker/certs/`），增加 `/oauth2`、`/.well-known`、`/login`、`/connect` 与 `/api` 代理；OIDC 代理使用 `changeOrigin: false` 并保留 Host。验证：`curl --cacert` 打开页面返回 200；经代理访问 HTTPS 服务返回业务响应；HMR WSS 可连接；不带 CA 失败。
- [x] 3.2 修改 `iboot-portal/nginx.conf`：新增 443 TLS server（TLS 1.2/1.3、HSTS 短时长），80 只返回 301；新增 OIDC 与 `/api` 反代并保留 Host、设置 `X-Forwarded-Proto`；既有 `/iboot/api`、`/iboot/ai` 保持。验证：容器内 `nginx -t` 通过；`curl --cacert` 打开 443 返回 200；`curl http://localhost` 返回 301；`openssl s_client` 不接受 TLS 1.0/1.1。
- [x] 3.3 修改门户 `Dockerfile` 接受 `ARG VITE_OIDC_ISSUER=https://localhost` 并注入构建阶段，`EXPOSE 80 443`；验证 `docker build` 成功，镜像内不存在证书私钥，构建产物中的 issuer 为 `https://localhost`。

## 4. CLI

- [x] 4.1 修改 `Context.defaults()` 为 `https://localhost:5173`，更新 `Context` 的 Javadoc；在构造器中拒绝 `http://` base URL 与非 loopback 的 `http://` issuer。验证：`mvn -pl iboot-cli test` 通过，新增单元测试覆盖默认值、拒绝明文、允许 loopback 回调。
- [x] 4.2 修改 `bin/iboot`：读取 `IBOOT_CLI_TRUSTSTORE` / `IBOOT_CLI_TRUSTSTORE_PASSWORD` 并注入 `-Djavax.net.ssl.trustStore*`；缺失信任库导致证书失败时给出指向 `docs/27` 的提示。验证：在宿主机开发与 Docker 两种来源下分别完成 `iboot login` 与 `iboot user list`；去掉信任库后以证书错误退出。
- [x] 4.3 更新 `docs/25-开发者 CLI 操作指南.md` 与 CLI 相关示例：默认 context、Docker context、信任库设置、明文 base URL 被拒绝。验证：文档中的命令在本地按原文执行成功。

## 5. 回归与持续集成

- [x] 5.1 修改 `scripts/regress/lib.sh`：默认来源改为宿主机 HTTPS 门户（`https://localhost:5173`），新增 `REG_CACERT`，所有 `curl` 添加 `--cacert`；`REG_OIDC_REDIRECT` 改为 HTTPS。同步更新 `smoke.sh`、`cli.sh`、`permission-matrix.sh` 中的 `REG_GATEWAY` 与直接 `curl`。验证：全栈环境下 `regress.sh` 与 `cli.sh` 通过。
- [x] 5.2 新增 `scripts/regress/tls.sh`：覆盖 HTTP→301、无 CA 失败、TLS 1.0/1.1 拒绝、证书剩余有效期 < 30 天失败、issuer 与公开来源一致、明文令牌端点不可用。验证：在启用 TLS 的环境全部通过；删除 CA 或把 issuer 改回 `http://localhost:8088` 时对应用例失败。
- [x] 5.3 更新 `.github/workflows/regress.yml` 的全栈任务：安装 mkcert、执行 `tls-init.sh`、以 `IBOOT_TLS_ENABLED=true` 启动 `dev-init.sh --frontend`、设置 `REG_CACERT` 与 HTTPS 来源，并在 `regress.sh` 后执行 `tls.sh`。验证：本地按工作流命令逐条执行成功；PR 静态任务不依赖证书。

## 6. 容器部署

- [x] 6.1 修改 `docker/docker-compose.yml` 的门户服务：发布 `80:80` 与 `443:443`，只读挂载 `./certs/portal.pem`、`portal-key.pem`、`rootCA.pem`，传入 `VITE_OIDC_ISSUER` 构建参数与 `SSL_CERT_FILE`；健康检查使用本地 CA 访问 HTTPS。验证：`docker compose build iboot-portal` 与 `docker compose up -d iboot-portal` 成功；`docker compose ps` 显示健康；`curl --cacert https://localhost` 返回 200。
- [x] 6.2 修改 `scripts/docker-deploy.sh`：启动前调用 `tls-init.sh`，证书缺失时在拉起容器前失败并输出生成命令；Docker 模式导出 `IBOOT_PUBLIC_ORIGIN=https://localhost`、issuer 与回调。验证：删除 `docker/certs/` 后执行部署脚本在启动前失败；重新生成后部署成功。
- [x] 6.3 更新 `docker/README.md`：门户 443/80、证书挂载、issuer 构建参数、明文回退限制与回滚方式。验证：文档命令与实际 compose 配置一致，链接可打开。

## 7. 文档与进度

- [x] 7.1 新增 `docs/27-TLS 与零信任操作指南.md`：证书生成与浏览器信任、两种公开来源、各客户端信任配置、登录/SSE/文件验收、排障、证书轮换、明文回退与回滚。验证：文档中的命令在本地按原文执行成功。
- [x] 7.2 更新 `docs/05-快速开始.md`（门户 HTTPS 与 5173 端口、登录地址）、`docs/08-部署指南.md`、`docs/10-后端配置详解.md`、`docs/23-微服务底座操作指南.md`、`docs/26-CI 与个人访问令牌.md`、`docs/README.md`、`scripts/README.md` 与根 `README.md` 的入口链接；不重复维护证书细节。验证：文档导航能到达 `docs/27`，所有 `http://localhost:8088` 的客户端入口引用已清理或标注为内部地址。
- [x] 7.3 在 `ROADMAP.md` 增加“开发者安全基线（本地 TLS/mTLS）”小节，登记 B0 已完成、B1 进行中、B2–B5 待启动及各自判据；验证条目与 `docs/27`、`openspec/changes/add-edge-tls/` 不矛盾。

## 8. 集成验收

- [x] 8.1 宿主机开发端到端：执行 `scripts/tls-init.sh`、`scripts/dev-init.sh --frontend`，用浏览器完成登录、SSE 推送、文件上传/下载；执行 `iboot login` 与 `iboot user list`；执行 `scripts/regress/regress.sh`、`cli.sh`、`tls.sh`。验证：全部通过，浏览器无证书告警（已安装本地 CA）。
- [x] 8.2 Docker 全量端到端：执行 `scripts/docker-deploy.sh up`，以 `https://localhost` 完成登录、SSE 与文件链路；以 Docker 来源执行回归与 `tls.sh`。验证：门户 443 可用、80 返回 301、明文入口不能完成登录。
- [x] 8.3 契约与构建门禁：执行 `openspec validate add-edge-tls --strict`、`mvn -pl iboot-cli test`、`mvn -pl iboot-auth test`、`cnpm run build`（门户）、`docker compose config`；验证全部通过，`git ls-files` 不含证书私钥。
- [x] 8.4 迁移与回滚演练：清理旧浏览器令牌与 CLI 凭据后重新登录；宿主机执行 `IBOOT_TLS_ENABLED=false` 启动，确认出现明文模式警告且回归不会把它当作 TLS 通过；记录 Docker 回退到上一版镜像/compose 的步骤。验证：演练结果写入 `docs/27` 的排障与回滚章节。

## Workflow follow-up

- 实现完成后按项目约定走功能分支、PR 与 `gh pr merge`，不要把变更直接提交到 `master`。
- 归档前确认 `docs/27`、`ROADMAP.md`、`openspec/specs/transport-security` 与各主 spec 的表述一致，并运行 `openspec validate add-edge-tls --strict`。
