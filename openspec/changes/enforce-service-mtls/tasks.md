# Tasks

## 1. 证书生成与用途

- [x] 1.1 改造 `scripts/tls-init.sh`：用本地 mkcert CA 的 `rootCA.pem` / `rootCA-key.pem` 通过 `openssl` 签发门户与服务叶子证书，EKU 为 `serverAuth,clientAuth`；验证 `openssl x509 -purpose` 显示 `SSL server: Yes` 与 `SSL client: Yes`
- [x] 1.2 在 `tls-init.sh` 中新增 `prometheus`、`blackbox` 探针客户端证书（`clients/`），复制到 `runtime/clients/`；验证文件存在、宿主机私钥 0600、运行期副本 0644
- [x] 1.3 在 `cert_valid` 与 `check_expiry` 中校验所有叶子证书的 `SSL client` 用途、有效期与存在性；验证旧证书（仅 serverAuth）执行脚本时会触发重签
- [x] 1.4 执行 `scripts/tls-init.sh --force` 并验证 `scripts/tls-init.sh --check` 通过；确认 CA、`truststore.p12` 与浏览器信任链未变化

## 2. 服务端与出站身份

- [x] 2.1 在三份 `iboot-tls.yml` 中启用 `server.ssl.client-auth: need`；验证服务启动后无客户端证书的 TLS 握手失败
- [x] 2.2 在网关 `application.yml` 增加 `spring.cloud.gateway.server.webflux.httpclient.ssl.ssl-bundle: iboot`；验证网关经 `lb://` 与静态 HTTPS 路由转发成功
- [x] 2.3 确认各服务出站客户端继续使用 `iboot` bundle（`spring.http.clients.ssl.bundle`、`loadBalancedRestClientBuilder`、JWKS）；验证服务间调用与 JWKS 拉取成功

## 3. 门户与探针身份

- [x] 3.1 在 `iboot-portal/nginx.conf` 的 `http` 级增加 `proxy_ssl_certificate` / `proxy_ssl_certificate_key`，使用门户证书；验证门户到网关、系统与运维控制台的请求成功
- [x] 3.2 在 `iboot-portal/vite.config.ts` 的网关与运维代理上增加 `ca` / `cert` / `key`；验证宿主机 Vite 开发模式下 `/api`、`/sba` 链路成功
- [x] 3.3 在 `docker/scripts/prometheus/prometheus.docker.yml` 与 `prometheus.yml` 的服务抓取 `tls_config` 中增加 `cert_file` / `key_file`（`/certs/clients/prometheus.pem`）；验证所有服务 target 为 up
- [x] 3.4 在 `docker/scripts/blackbox/blackbox.yml` 的 `https_cert` 模块增加 `cert_file` / `key_file`（`/certs/clients/blackbox.pem`）；验证 `probe_success{job="tls-cert"}` 为 1

## 4. 脚本、回归与文档

- [x] 4.1 在 `scripts/regress/lib.sh` 增加 `REG_CLIENT_CERT` / `REG_CLIENT_KEY` 并注入 `reg_curl`；在 `regress.sh` 导出；验证 seata、smoke、权限矩阵等直连服务用例通过
- [x] 4.2 更新 `scripts/regress/api-snapshot.sh`、`scripts/regress/log-pipeline.sh`、`scripts/apm/measure.sh`、`scripts/regress/equivalence.sh` 的直连请求携带客户端证书；验证各自脚本通过
- [x] 4.3 更新 `scripts/dev-init.sh` 的服务等待逻辑携带客户端证书，并确认宿主机进程的 Nacos 客户端证书仍可用；验证 `dev-init.sh` 全栈启动就绪
- [x] 4.4 扩展 `scripts/regress/internal-tls.sh`：无客户端证书必须失败、每个服务证书 `SSL client` 可用、网关 8088 与 JWKS 带证书成功、服务证书指纹唯一；验证本地内部 TLS 全部通过
- [x] 4.5 更新 `docs/27`、`docs/23`、`docs/08`、`docs/10` 与 `docker/README.md`，登记 mTLS 边界、客户端证书用法与回滚方式；验证文档命令可执行且不再写“服务端不要求客户端证书”

## 5. 集成验证

- [x] 5.1 重建十个服务、门户、Prometheus 与 blackbox，执行全量 `scripts/regress/regress.sh`；验证公开入口、内部 TLS、可观测、日志与冒烟全部通过
- [ ] 5.2 在 CI 分支运行全栈回归，确认 host 模式与 Docker 模式均通过；记录已知备份冒烟例外

## Workflow follow-up

- 同步 delta specs 到主 specs 并归档变更。
- 新建功能分支、提交、创建 PR、合并到 master。
