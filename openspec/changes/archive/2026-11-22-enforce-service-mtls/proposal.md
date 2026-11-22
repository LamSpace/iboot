# Proposal

## Why

B5.1 已为十个业务服务拆出独立服务端证书，但服务端仍只校验证书链，不要求客户端出示身份。任何能连上内部端口、且持有本地 CA 签发的证书的进程都能调用业务服务，服务间流量无法区分“谁在调用”。在每服务证书已经就绪的前提下，本批启用服务端客户端证书校验，让网关、服务间调用、门户与探针都以各自证书证明工作负载身份。

## What Changes

- **BREAKING**：十个业务服务的 HTTPS 端口要求客户端证书；不带证书的直接调用（含现有回归脚本、手工 `curl`、未升级的探针）将握手失败。
- `tls-init.sh` 改为用本地 CA 签发同时具备 `serverAuth` 与 `clientAuth` 用途的叶子证书；新增 Prometheus 与 blackbox 专用客户端证书；证书有效性与用途纳入 `--check`。
- 服务出站 HTTP 客户端复用本服务证书（网关补上上游 SSL bundle），服务间调用以调用方自己的证书完成双向认证。
- 门户 nginx 与宿主机 Vite 代理以门户证书作为客户端身份；Prometheus 与 blackbox 以各自探针证书抓取服务端口。
- 内部 TLS 回归新增无客户端证书失败、每服务证书可用、网关/门户链路与探针成功等用例；回归脚本与开发等待逻辑统一携带客户端证书。
- 文档登记本批边界：服务端只验证“证书由本地 CA 签发”，不按调用方做授权；Nacos 注册客户端仍使用共享门户证书；SPIFFE/服务网格不在本批。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`：内部服务端口从“只接受 TLS”升级为“要求客户端证书的双向 TLS”；新增每服务工作负载身份、调用方证书覆盖与 mTLS 负向门禁；移除“客户端证书不得强制启用”的旧边界。
- `container-deployment`：编排为业务服务、门户、Prometheus 与 blackbox 注入各自的客户端证书路径；服务健康检查仍以 TCP 为准，不因 mTLS 误判。

## Impact

- 证书与脚本：`scripts/tls-init.sh`、`scripts/dev-init.sh`、`scripts/regress/*`、`scripts/apm/*`。
- 服务配置：三份 `iboot-tls.yml`、网关 `application.yml`、门户 `nginx.conf` 与 `vite.config.ts`。
- 编排与可观测：`docker/docker-compose.yml`、`docker/scripts/prometheus/*.yml`、`docker/scripts/blackbox/blackbox.yml`。
- 文档：`docs/27`、`docs/23`、`docs/08`、`docs/10`、`docker/README.md`。
- 兼容性：任何不带客户端证书访问 8080–8089 的客户端都会失败；使用本地 CA 与任一已签发服务证书的客户端可通过，本批不做按调用方的授权矩阵。
