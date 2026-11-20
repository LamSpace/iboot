# Tasks

## 1. v0 最小验证

- [x] 1.1 在 `iboot-system` 上验证共享 SSL bundle 与 JVM 信任库：服务端 HTTPS 健康检查成功，去掉信任库后客户端调用失败。验证：`curl --cacert` 返回 200；无 CA 请求失败。
- [x] 1.2 验证网关 HTTPS 服务端与到 `iboot-system` 的 HTTPS 上游：分别验证 `lb:https://` 或显式 HTTPS 路由，记录可用写法。验证：经网关请求系统服务成功；网关日志确认上游为 HTTPS；不支持时给出设计中的回退写法。
- [x] 1.3 验证数据面客户端信任：Druid 连 MySQL、Lettuce 连 Redis、MinIO SDK 连 Silo、Neo4j 驱动连图库各跑通一次最小连接。验证：四类连接均使用本地 CA；去掉信任库分别失败。
- [x] 1.4 验证 SBA/运维控制台能从 Nacos 发现 HTTPS 服务实例；若失败，确定 `secure=true` 元数据或显式实例 URL 的修复方式。验证：门户 `/sba` 能看到至少一个服务实例。
- [x] 1.5 把 v0 实际可用的网关 scheme、SSL bundle 与客户端信任配置回写到 design.md 或 docs/27。验证：文档与实测命令一致。

## 2. 服务端 TLS

- [x] 2.1 在 `iboot-kernel` 增加共享 `iboot-tls.yml`（PEM keystore + truststore），各服务通过 `spring.config.import` 引入并设置 `server.ssl.bundle=iboot`。验证：每个服务启动后 `openssl s_client` 协商 TLS 1.2/1.3。
- [x] 2.2 在 `dev-init.sh` 与 compose 中为所有 Java 进程注入 `-Djavax.net.ssl.trustStore` / `trustStorePassword` / `trustStoreType`，并确保容器内路径存在。验证：服务启动无证书错误；Druid/Redis/MinIO/Neo4j 连接成功。
- [x] 2.3 网关 `8088` 启用 `server.ssl`；门户 nginx 与 Vite 的网关上游改为 `https://` 并信任本地 CA。验证：门户登录与 `/api` 正常；网关明文请求失败。
- [x] 2.4 `scripts/dev-init.sh` 的服务健康等待与 `show_status` 改用 HTTPS + CA；`wait_http` 对 HTTPS 服务生效。验证：`dev-init.sh --status` 显示所有服务健康，且不使用明文健康检查。
- [x] 2.5 处理容器内非 root 用户读取私钥：通过启动脚本复制到应用用户路径或一次性证书初始化，不修改宿主私钥为 0644。验证：容器内应用用户能读取证书，宿主私钥仍为 0600。

## 3. 内部客户端与 JWKS

- [x] 3.1 让 `JwkSetProvider` 使用感知 SSL bundle 的 `RestClient`；更新 `KernelSecurityAutoConfiguration` 的装配。验证：单元测试覆盖 HTTPS 拉取成功与无 CA 失败。
- [x] 3.2 替换 `ServiceClientConfig`、`AgentToolConfig` 与负载均衡 `RestClient.Builder` 中的 `SimpleClientHttpRequestFactory` / 裸 builder，统一使用 SSL bundle。验证：文件、任务、AI 工具的服务间调用成功；去掉 CA 失败。
- [x] 3.3 把所有服务间地址与 JWKS 地址改为 HTTPS（workflow→system/notify、ai→system/job、`JWT_JWK_SET_URI`、`IBOOT_*_BASE_URL`）。验证：审批、通知、AI 智能体、资源服务验签均正常；明文 JWKS 不可用。
- [x] 3.4 运维控制台与门户 `/sba` 上游改 HTTPS；按 v0 结论配置 SBA 实例 scheme。验证：`/sba` 页面可打开，实例状态可见；无法修复的项登记为已知限制。

## 4. 核心数据面

- [x] 4.1 MySQL：服务端挂载证书并设置 `--require_secure_transport=ON`；全部服务与 `iboot-db-init` 改为 `sslMode=VERIFY_IDENTITY&requireSSL=true`；Druid 使用 JVM 信任库；健康检查走 TLS。验证：`SHOW STATUS LIKE 'Ssl_cipher'` 非空；`sslMode=DISABLED` 被拒绝；全量回归通过。
- [x] 4.2 Redis：只监听 TLS 端口，挂载证书；Spring 启用 Redis SSL bundle；健康检查与脚本使用 `--tls --cacert`。验证：Lettuce `PING` 成功；明文连接失败。
- [x] 4.3 Silo：挂载 `public.crt` / `private.key`，启用 `--certs-dir`；内部/公开 endpoint 改 HTTPS；MinIO SDK 使用 JVM 信任库；预签名 URL 与健康检查使用 CA。验证：HTTPS 读写与预签名下载成功；HTTP 请求不返回对象内容。
- [x] 4.4 Neo4j（`graph` profile）：挂载证书并启用 Bolt TLS，URI 改 `bolt+s://`；驱动使用 JVM 信任库。验证：图同步成功；明文 `bolt://` 连接失败。若 v0 证明配置不可行，按设计登记限制并从本批验收中明确排除。
- [x] 4.5 验证 `iboot-monitor` 的 MySQL/Redis/Silo 资源检查、备份链路、`silo-init` 与 `iboot-db-init` 在 TLS 下仍可用。验证：监控页面、备份触发、初始化任务均成功。

## 5. 回归与持续集成

- [x] 5.1 `scripts/regress/regress.sh` 的 `SVC_*` 改为 HTTPS 并导出 `SVC_CACERT`；`api-snapshot.sh` 为所有请求添加 `--cacert`。验证：契约快照、路径守恒、路由覆盖在内部 TLS 下通过。
- [x] 5.2 新增 `scripts/regress/internal-tls.sh`：逐端口 TLS、明文拒绝、无 CA/错误 CA 失败、JWKS HTTPS、网关→服务与 SSE、MySQL cipher、Redis 明文拒绝、Silo HTTPS、Neo4j（条件）。验证：启用内部 TLS 时全部通过；移除任一 CA 或改回明文时对应用例失败。
- [x] 5.3 更新 `scripts/regress/log-pipeline.sh`、`scripts/apm/*`、`scripts/regress/equivalence.sh` 中指向网关的地址与 CA；迁移期脚本不清除历史用途。验证：日志链路与 APM 脚本不再默认明文访问网关。
- [x] 5.4 更新 CI 全栈任务：启用内部 TLS，执行 `regress.sh`、`internal-tls.sh` 与 `cli.sh`；PR 静态任务不依赖证书。验证：本地按工作流命令逐条执行成功。

## 6. 容器编排

- [x] 6.1 为所有后端服务挂载只读证书目录，注入 `IBOOT_TLS_CERT_DIR=/certs` 与 JVM 信任库；应用容器以非 root 用户可读。验证：compose 启动后服务健康，容器内证书可读。
- [x] 6.2 MySQL/Redis/Silo/Neo4j 挂载证书并更新启动参数与健康检查；数据面端口只提供 TLS。验证：`docker compose ps` 全部健康；数据面明文请求失败。
- [x] 6.3 `docker-deploy.sh` 在内部证书缺失或不可读时 fail-fast，并输出生成/修复命令。验证：移除证书后部署在启动前失败；恢复后部署成功。

## 7. 文档与进度

- [x] 7.1 扩展 `docs/27-TLS 与零信任操作指南.md`：B2 内部 TLS、共享 SSL bundle、JVM 信任库、网关上游、数据面组件、负向矩阵、回滚。验证：文档命令在本地按原文执行成功。
- [x] 7.2 更新 `docs/10-后端配置详解.md`、`docs/08-部署指南.md`、`docs/23-微服务底座操作指南.md`、`docs/05-快速开始.md`、`docs/21-数据备份与恢复操作指南.md`、`docs/22-AI 工作台操作指南.md`、`docker/README.md`、`scripts/README.md` 与 `.env.example`；不重复维护证书步骤。验证：内部服务地址、scheme 与证书路径不再以明文为默认。
- [x] 7.3 在 `ROADMAP.md` 更新 B2 状态与判据，明确 Nacos/ES/Kafka/RocketMQ/Seata 的边界。验证：ROADMAP 与 docs/27、本变更不矛盾。

## 8. 集成验收

- [x] 8.1 宿主机开发端到端：`dev-init.sh --frontend` 启动内部 TLS；完成登录、SSE、文件上传/下载、审批、AI 对话、备份与图同步（若启用）；执行 `regress.sh`、`cli.sh`、`internal-tls.sh`。验证：全部通过，抓包或负向用例证明内部流量为 TLS。
- [x] 8.2 Docker 全量端到端：`docker-deploy.sh up` 后完成登录、SSE、文件链路与全量回归；执行 `internal-tls.sh`。验证：门户 443、服务与数据面只接受 TLS；备份用例按既有部署限制处理。
- [x] 8.3 回退演练：确认不提供运行期内部明文回退——`IBOOT_INTERNAL_TLS_ENABLED=false` 启动时 fail-fast；记录回退上一版 commit/镜像与 compose 的步骤。验证：fail-fast 文案与回退步骤写入 docs/27 与 design D8。
- [x] 8.4 构建与契约门禁：`openspec validate add-internal-tls --strict`、相关 Maven 测试、`docker compose config`、`git ls-files` 不含私钥。验证：全部通过。

## Workflow follow-up

- 实现完成后按项目约定走功能分支、PR 与 `gh pr merge`。
- 归档前运行 `openspec validate add-internal-tls --strict`，同步主 spec，并确认 docs/27 与 ROADMAP 一致。
