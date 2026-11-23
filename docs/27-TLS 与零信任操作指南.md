# 27-TLS 与零信任操作指南

> 适用版本：iBoot 1.0.0。本文是本地 TLS 的唯一操作权威：证书生成、公开来源、
> 客户端信任、验收、排障与回滚。B0 的实测结论见
> [TLS 与零信任 Spike 报告](./spike/tls-zero-trust-spike-report.md)。

## 1. 范围与边界

当前已完成的加密范围：

- 浏览器/CLI → 门户（L1 边缘 TLS）；门户 → 网关；服务间调用与 JWKS。
- MySQL、Redis、Silo、Neo4j 等核心数据面；Elasticsearch/Kafka/Logstash 日志与检索面；Tempo 的 OTLP 上报与查询面；Kafka SASL 与 Redis 命名 ACL；Grafana/Prometheus/Alertmanager/Thanos/Kibana 控制台。
- Nacos 的 HTTP/控制台 HTTPS 与 gRPC mTLS；RocketMQ 应用入口只接受 TLS、启用 ACL，客户端通过兼容层校验服务端证书与主机名（见 §19/§22/§26）。
- Seata 无原生 TLS/认证，作为显式例外放在 `tx` 网络，仅 workflow/system 可访问（见 §19）。
- 十个业务服务的 HTTPS 端口要求客户端证书；网关、服务间调用、门户与 Prometheus/blackbox 各自出示工作负载证书（B5.2，见 §18）。
- Docker 全量部署中，门户是唯一公开入口；宿主机侧只有 80/443 跨网卡监听，其余容器发布端口只绑定回环地址（§12）。
  宿主机开发模式下 Java 进程也只监听回环地址（B5.10）。
- 服务端以客户端证书的 SPIFFE URI SAN（CN 回退）做路径级服务间授权（B5.4/B5.8）；仍不引入本机服务网格或 OPA，方法级业务权限继续由 JWT 承担。

## 2. 证书

### 2.1 生成

```bash
scripts/tls-init.sh          # 不存在或剩余有效期 < 30 天时生成/重签
scripts/tls-init.sh --force  # 强制重签
scripts/tls-init.sh --check  # 只检查有效期与证书用途
```

证书写入 `docker/certs/`（已被 `.gitignore` 忽略）：

| 文件 | 用途 |
|---|---|
| `ca/rootCA.pem` | 本地 CA，供 nginx / Node / curl |
| `ca/rootCA-key.pem` | CA 私钥，权限 0600，不入仓 |
| `portal.pem` / `portal-key.pem` | 门户、Nacos、Tempo 与可观测控制台的共享叶子证书；门户也用它作为访问上游的客户端证书 |
| `services/<service>.pem` / `<service>-key.pem` | 十个业务服务各自的叶子证书与私钥，同时用于服务端与客户端身份 |
| `clients/prometheus.pem` / `clients/blackbox.pem` 等 | Prometheus 与 blackbox 的探针客户端证书 |
| `truststore.p12` | Java 信任库，口令 `changeit` |

共享门户证书的 SAN 覆盖 `localhost`、`127.0.0.1`、`::1`、`host.docker.internal`、
`*.iboot.local` 与全部服务名；每个服务证书的 SAN 覆盖自身服务名、`localhost`、
`127.0.0.1`、`::1` 与 `host.docker.internal`。服务与门户叶子证书的 EKU 同时包含
`serverAuth` 与 `clientAuth`；探针客户端证书只包含 `clientAuth`。B5.2 起 `--check`
会拒绝缺少 `clientAuth` 的旧证书，并自动重签。

首次生成由 `mkcert` 创建本地 CA，之后叶子证书由 `openssl` 使用同一 CA 签发；
`truststore.p12` 与浏览器信任链保持不变。脚本依赖 `mkcert`、`openssl` 与 `keytool`。mkcert 缺失时：

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

- 叶子证书剩余有效期小于 30 天或缺少 `clientAuth` 用途时，`tls-init.sh` 会重签，
  `scripts/regress/tls.sh` 会失败并提示。
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
| 服务间调用返回 500 或“数据权限范围不可用” | 确认 Nacos 实例 `secure=true`，且实例地址与证书 SAN 一致（宿主机 127.0.0.1 / Docker 服务名） |
| JDBC 报证书或主机名错误 | 确认 `MYSQL_SSL_PARAMS=sslMode=VERIFY_IDENTITY...` 与 JVM truststore；不要改回 `useSSL=false` |
| Redis 连接被关闭 | 确认服务端只开 TLS 端口，客户端 `REDIS_SSL_ENABLED=true` |
| Silo 连接失败 | 确认 endpoint 为 `https://`，Silo 使用 `--certs-dir`，客户端 JVM 信任库有效 |
| 网关上游 502 | 确认网关路由使用 HTTPS 且信任库生效；`lb://` 实例 `secure=true` |
| Neo4j 连接失败 | 确认 URI 为 `bolt+s://`，驱动使用 JVM 信任库，Bolt TLS policy 已启用 |

## 7. 明文回退与回滚

- **宿主机开发**：`IBOOT_TLS_ENABLED=false scripts/dev-init.sh --frontend` 会以 HTTP 启动
  Vite，并在日志中输出明文模式警告。该模式仅用于排障，回归与验收不得判定通过。
- **Docker**：B1 不提供明文回退；回滚使用上一版镜像与 compose，或 `git revert` 本次变更。
  不要通过关闭 nginx 证书校验或把 443 改回 80 来“绕过”问题。
- 回滚后旧的 `http://localhost:8088` 客户端入口会重新可用，但需要同步恢复认证 issuer、
  门户回调、CLI context 与回归地址；不要在新旧来源之间混用令牌。

## 8. 内部 TLS（B2）

B2 在不改变公开入口的前提下，把门户到网关、服务间、JWKS 与核心数据面切到 TLS。

### 8.1 服务端

- 网关保持 `8088`，但只提供 HTTPS；门户 nginx 与 Vite 的上游改为 `https://`。
- 九个业务服务与运维控制台通过共享 `iboot-tls.yml` 启用 `server.ssl.bundle=iboot`：
  - `iboot-kernel` 提供文件，八个业务服务通过 `spring.config.import` 引入；
  - 网关与运维控制台不依赖 kernel，各自在 `src/main/resources` 放同一份文件。
- 十个业务服务通过 `IBOOT_TLS_CERT_FILE` / `IBOOT_TLS_KEY_FILE` 加载各自的
  `services/<service>.pem` / `<service>-key.pem`；SAN 含服务名、`localhost`、`127.0.0.1`。
- B5.2 起 `iboot-tls.yml` 同时设置 `server.ssl.client-auth=need`：服务端口只接受由
  本地 CA 签发的客户端证书；不带证书的 TLS 连接在握手阶段失败。
- 门户、Nacos、Tempo 与可观测控制台继续使用共享的 `portal.pem` / `portal-key.pem`；
  两者都由同一本地 CA 签发，信任链不变。门户用它作为访问上游的客户端证书。

### 8.2 信任链

- Spring 客户端使用 SSL bundle（`spring.http.clients.ssl.bundle=iboot`），并自动把本服务证书
  作为客户端证书发出；网关额外设置
  `spring.cloud.gateway.server.webflux.httpclient.ssl.ssl-bundle=iboot`。
- 门户 nginx 用 `proxy_ssl_certificate` 出示门户证书；宿主机 Vite 代理设置
  `ca` / `cert` / `key`。Prometheus 与 blackbox 分别使用 `clients/prometheus.pem` 与
  `clients/blackbox.pem`。
- Druid、MinIO SDK、Neo4j 驱动等非 Spring 客户端使用 JVM 信任库：
  `-Djavax.net.ssl.trustStore=docker/certs/truststore.p12`。
- 宿主机由 `dev-init.sh` 为每个 Java 进程注入；Docker 由 compose 的
  `JAVA_TOOL_OPTIONS` 注入。
- Nacos 注册实例的 `secure=true`，网关的 `lb://` 据此选择 HTTPS。宿主机服务以
  `127.0.0.1` 注册（证书 SAN 覆盖），Docker 以服务名注册（Docker DNS 解析）。

### 8.3 核心数据面

| 组件 | 服务端 | 客户端 |
|---|---|---|
| MySQL | `--ssl-ca/--ssl-cert/--ssl-key` + `require_secure_transport=ON` | `sslMode=VERIFY_IDENTITY&requireSSL=true`，Druid + JVM 信任库 |
| Redis | 只监听 TLS 端口 | `spring.data.redis.ssl.enabled=true`，Lettuce + JVM 信任库 |
| Silo | `--certs-dir /certs` | 内部/公开 endpoint 使用 `https://`，MinIO SDK + JVM 信任库 |
| Neo4j（graph） | Bolt TLS policy | `bolt+s://`，驱动 + JVM 信任库 |

### 8.4 验收

```bash
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/tls.sh
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/internal-tls.sh
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/regress.sh
```

`internal-tls.sh` 覆盖逐端口 TLS、无客户端证书拒绝、每服务证书的 `serverAuth`/`clientAuth`
用途、证书指纹唯一、门户证书可作为客户端、网关→服务、JWKS、MySQL cipher、Redis/Silo
明文拒绝与 Neo4j（运行 graph profile 时）。

### 8.5 边界与回退

- Nacos 的 TLS 在 B4 单独完成，见第 10 节；B2 本身不宣称完整零信任。
- **不提供运行期内部明文回退**：`IBOOT_INTERNAL_TLS_ENABLED=false` 会让
  `dev-init.sh` 在启动前失败。内网半开 TLS 会同时破坏服务间、JWKS 与数据面，
  定位成本高于直接回退。
- 回滚使用上一版 commit/镜像与 compose，或 `git revert`。B1 的边缘回退
  `IBOOT_TLS_ENABLED=false` 仍只影响公开入口，不改变内部 TLS。
- 如只需回退 B5.2 的双向认证，移除 `server.ssl.client-auth` 并重建服务即可；证书仍可保留，
  单向 TLS 客户端恢复可用。

## 9. 日志与检索面 TLS（B3）

B3 在 B2 基础上把集中日志与检索面切到 TLS，并把 RocketMQ 只能做到“加密通道”的边界写死。

### 9.1 Elasticsearch

- ES 9.5 启用 `xpack.security.enabled=true`，HTTP 与 transport 都开启 TLS。
- 证书必须放在 ES 配置目录内（9.5 的 entitlement 限制）：
  `/usr/share/elasticsearch/config/certs/`，不能复用其他组件的 `/certs` 挂载。
- 凭据由 `.env` 的 `ELASTIC_PASSWORD` 注入；Kibana 使用 `kibana_system`，
  由一次性 `elasticsearch-init` 幂等同步密码，不能使用 `elastic` 超级用户。
- Spring 客户端使用共享 `iboot-tls.yml` 中的
  `spring.elasticsearch.restclient.ssl.bundle=iboot`，地址必须是 `https://`。
- Logstash、Kibana、Grafana 与 elasticsearch-exporter 都通过本地 CA 校验证书，
  Grafana 使用 `$__file{/certs/rootCA.pem}` 注入 CA。

验证：

```bash
curl --cacert docker/certs/ca/rootCA.pem -u elastic:"$ELASTIC_PASSWORD" \
  https://localhost:9200/_cluster/health
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:9200/_cluster/health   # 应为 000
```

### 9.2 Kafka

- 三个监听都使用 SASL_SSL：`INTERNAL://kafka:9092`、`HOST://localhost:29092`、
  `CONTROLLER://:9093`；controller 不发布到宿主机，节点内使用 `broker` 账号。
- `tls-init.sh` 额外生成 `runtime/kafka-keystore.p12` 与密码文件。
- 镜像的 configure 脚本要求 `KAFKA_SSL_KEYSTORE_FILENAME`、`KAFKA_SSL_KEY_CREDENTIALS`、
  `KAFKA_SSL_KEYSTORE_CREDENTIALS`，文件放在 `/etc/kafka/secrets`。
- client auth 为 `none` 时镜像不会自动注入 truststore；controller 出站仍需显式传
  `KAFKA_SSL_TRUSTSTORE_LOCATION`，否则启动时报 PKIX 失败。监听名改为不含下划线的
  `INTERNAL`/`HOST` 后，镜像不再自动注入 keystore，需显式设置
  `KAFKA_SSL_KEYSTORE_LOCATION` 与口令。
- SASL/PLAIN 账号按用途区分：`iboot_log`（写日志）、`iboot_logstash`（读日志与消费组）、
  `iboot_tools`（初始化/运维超级用户）、`broker`（节点间）。`StandardAuthorizer` 在无 ACL 时默认拒绝。
- `KafkaAppender` 默认 `security.protocol=SASL_SSL`，宿主机信任库为
  `docker/certs/truststore.p12`，容器内为 `/certs/truststore.p12`。

验证：

```bash
scripts/regress/log-search-tls.sh
scripts/regress/log-pipeline.sh --drills
```

### 9.3 Logstash 与日志管道

- Kafka 输入使用 `security_protocol=SASL_SSL`、`iboot_logstash` 账号与 PKCS12 truststore；
  ES 输出使用 `https://`、`ssl_certificate_authorities` 与凭据。
- 端到端探针必须能从 Kafka 经 Logstash 进入 ES，并按 `traceId` 检索到。
- Kafka 或 Logstash 不可用时业务仍正常运行；恢复后日志自动续传。

### 9.4 RocketMQ TLS-only 入口

- 应用入口为 `rocketmq-tls`（nginx stream + `ssl_preread`）：只转发 TLS ClientHello，
  非 TLS 连接被关闭；TLS 仍在客户端与 Proxy 之间端到端完成。
- workflow 与 notify 连接 `rocketmq-tls:8081` 并设置 `IBOOT_ROCKETMQ_SSL_ENABLED=true`；
  宿主机开发使用 `localhost:18081`。Proxy 原生 8080/8081 不再发布到宿主机。
- 客户端使用 `enableSsl=true`，并通过 B5.12 兼容层校验服务端证书链与 `rocketmq-tls`
  主机名；上游 5.2.2 的硬编码限制由该兼容层修补（见 §26）。
- B5.6 已补 ACL 与调用方账号。该链路仍不是 mTLS：客户端没有证书身份，
  调用方身份来自 ACL 凭据；文档与回归输出不得把 ACL 写成 mTLS。

### 9.5 验收与回滚

```bash
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/log-search-tls.sh
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/log-pipeline.sh
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/log-pipeline.sh --drills
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/regress.sh
```

回滚使用上一版 commit/镜像与 compose，并恢复 ES/Kafka 旧配置。ES 已有卷可以用
`ELASTIC_PASSWORD` 直接启用 security；若旧卷状态异常，执行
`docker exec iboot-elasticsearch bin/elasticsearch-reset-password -u elastic` 或重建本地卷。
Kafka 回到 PLAINTEXT 需要重建 broker 容器；不存在运行时自动明文回退开关。

## 10. Nacos TLS（B4）

Nacos 3.3 的 HTTP API、控制台与 gRPC 通道已切到 TLS；gRPC 进一步要求客户端证书（mTLS）。

- `tls-init.sh` 生成 `runtime/nacos-keystore.p12`，compose 挂载到 `/certs`。
- HTTP/控制台通过 Spring Boot `server.ssl` 启用 HTTPS；gRPC 通过 JVM 系统属性启用 TLS：
  `nacos.remote.server.rpc.tls.enableTls=true` 等。纯环境变量无法映射 camelCase，必须用
  `JAVA_TOOL_OPTIONS` 或 `application.properties`。
- 客户端在 JVM 启动前注入：
  `-Dnacos.remote.client.rpc.tls.enable=true`、
  `-Dnacos.remote.client.rpc.tls.trustCollectionChainPath=file:<cert-dir>/ca/rootCA.pem`、
  `-Dnacos.remote.client.rpc.tls.trustAll=false`、
  `-Dnacos.remote.client.rpc.tls.mutualAuth=true`、
  `-Dnacos.remote.client.rpc.tls.certChainFile=file:<cert-dir>/portal.pem`、
  `-Dnacos.remote.client.rpc.tls.certPrivateKey=file:<cert-dir>/portal-key.pem`；
  `serverAddr` 仍为 `host:8848`。
- gRPC 端口偏移保持不变：8848 → 9848；9849 是集群内部端口，不映射宿主机。
- 门户 Vite 的 `/nacos-console` 代理、`sentinel-init.sh`、`dev-init.sh` 健康检查与 Docker
  控制台 URL 都改为 HTTPS + 本地 CA。
- Nacos 的 HTTP 与 gRPC 已启用服务账号认证与鉴权（B4.3）；gRPC 已启用 mTLS，但所有服务共用同一张客户端证书，每服务身份留待 B5。本地控制台鉴权仍关闭（见 §13.5）。

验证：

```bash
curl --cacert docker/certs/ca/rootCA.pem https://localhost:8848/nacos/v1/ns/operator/metrics
curl --cacert docker/certs/ca/rootCA.pem https://localhost:8849/v3/console/health/readiness
echo | openssl s_client -connect localhost:9848 -tls1_2 -CAfile docker/certs/ca/rootCA.pem \
  -cert docker/certs/portal.pem -key docker/certs/portal-key.pem
scripts/regress/internal-tls.sh
```

回滚：恢复上一版 compose/镜像与 Nacos 明文配置；不存在运行时明文回退开关。

## 11. 与后续批次的关系

- B2：门户到网关、服务间、JWKS、MySQL/Redis/Silo/Neo4j 的 TLS；已完成。
- B3：ES/Kafka/Logstash 日志与检索面 TLS；已完成；RocketMQ 只作为加密通道，
  不能宣称服务端认证或强制 TLS。
- B4：Nacos TLS、端口收敛与网络分段已完成；中间件账号鉴权由 `harden-middleware-auth`
  完成（MySQL/Redis/Silo/Nacos）；默认值与运维收尾由 `harden-operational-defaults`
  完成（ES 专用账号、Grafana/Neo4j 默认值、证书告警、明文审计）。
- B4.5：Tempo 的 OTLP 上报与查询链路 TLS 已完成（`add-tempo-tls`）。
- B4.6：Kafka SASL/ACL 与 Redis 命名 ACL 已完成（`harden-kafka-redis-auth`）。
- B4.7：可观测查询与控制台 TLS 已完成（`harden-observability-ui-tls`）。
- B5.1：每服务独立服务端证书已完成（`add-service-certificates`）。
- B5.2：服务间 mTLS、每服务工作负载身份与探针客户端身份已完成（`enforce-service-mtls`），见 §18。
- B5.3：RocketMQ TLS-only 入口与 Seata 网络隔离/例外登记已完成（`harden-rocketmq-tls-and-isolate-seata`），见 §19；RocketMQ ACL 与服务端校验仍待后续。
- B5.4：服务间路径级授权已完成（`enforce-service-authorization`），见 §20；中间件每服务身份与 SPIFFE/服务网格仍待后续。

## 12. 暴露面与网络分段

TLS 只保护「连上之后」的通道，不回答「谁允许连接」。本节把容器部署的宿主机暴露面与
容器网络边界固化下来；它仍不等于零信任身份，服务端目前不校验调用方身份（B5）。

### 12.1 宿主机端口

| 类别 | 端口 | 绑定 |
|---|---|---|
| 门户 | 80 / 443 | `0.0.0.0` |
| 网关与业务服务 | 8080–8089 | `127.0.0.1` |
| 数据面 | 3306、6379、9000/9001、9200、8848/9848/8849、8091、29092、18081（RocketMQ TLS） | `127.0.0.1` |
| 治理与可观测 UI | 8858、9090、3000、9093、5601、19192、18082、3200 | `127.0.0.1`；均为 HTTPS |
| 图数据库（`graph` profile） | 7474、7687 | `127.0.0.1` |
| Logstash、Thanos、exporter、SkyWalking Banyandb/OAP admin | 不发布 | — |

Docker 的端口发布会绕过常见主机防火墙，因此不能采用「先发布到 `0.0.0.0` 再靠 ufw 限制」
的做法。需要从其它主机临时访问时，用 SSH 端口转发，不修改 compose。

### 12.2 容器网络

| 网络 | 成员 | 边界 |
|---|---|---|
| `edge` | 门户、网关、系统服务、运维控制台 | 门户唯一入口 |
| `app` | 网关、系统服务、运维控制台、八个业务服务、Prometheus、`rocketmq-tls` | 服务间调用、指标抓取与 RocketMQ TLS 入口 |
| `data` | 中间件（不含 Seata 与 RocketMQ 集群）、业务与初始化任务、Logstash/Kibana/Grafana/Thanos/exporter | 数据面 |
| `mq` | RocketMQ NameServer/Broker/Proxy/初始化任务、`rocketmq-tls` | 仅 RocketMQ 集群与 TLS 前置代理 |
| `tx` | `seata-server`、`iboot-workflow`、`iboot-system` | 仅事务协调与两个事务参与方 |
| `obs` | Prometheus、Grafana、Alertmanager、Thanos、SkyWalking、exporter | 可观测组件 |

门户只加入 `edge`，无法解析 `mysql`、`redis`、`elasticsearch`、`nacos`。这不是文档约定，
而是由回归脚本检查的运行时边界。

### 12.3 验收

```bash
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/exposure.sh
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/regress.sh
```

`exposure.sh` 分四组：

1. 用 `docker inspect` 断言只有门户 80/443 绑定 `0.0.0.0`，其余已发布端口绑定 `127.0.0.1`，
   并断言已移除的映射不再出现。
2. 用宿主机非回环 IPv4 连接 3306、6379、9200、8848、29092、18081，必须失败；回环地址必须成功。
3. 容器矩阵：门户访问网关成功，访问 MySQL/Redis/ES/Nacos 失败；业务服务访问 MySQL/Nacos 成功。
4. mq/tx 隔离：workflow/system 可访问 Seata，其它服务不可；应用无法直连 RocketMQ Proxy，
   但可访问 `rocketmq-tls`。容器未运行时明确跳过矩阵，但端口检查仍执行。

### 12.4 边界

- 只收敛 Docker 编排发布的网络暴露面，不改变各组件内部的 TLS 配置。
- 宿主机开发模式下由 `dev-init.sh` 启动的 Java 进程只监听 `127.0.0.1`，不再按 Spring Boot 默认绑定全部网卡。
  Prometheus/blackbox 通过 `host.docker.internal` 访问宿主机回环：Docker Desktop 自带代理；
  原生 Docker 无法直连时，`dev-init.sh` 启动只绑定 Docker 网桥网关的 TCP relay（8080-8089），
  不扩大 LAN 暴露面。relay 随 `--stop/down/purge` 停止。
- Sentinel 客户端命令端口 8719 仍由 Sentinel 自身绑定策略决定，不承载业务请求，作为残余边界登记。
- 中间件已按服务拆分账号（B5.5）：MySQL、Redis、Nacos 每服务独立，Silo 由文件/备份/AI 三个桶级账号隔离；
  权限边界仍保持共享库 DML，表级授权不在本节范围内。
- RocketMQ 入口只接受 TLS、启用 ACL，并由 B5.12 兼容层校验服务端证书；Seata 无原生 TLS/认证，仅靠 `tx` 网络与回环发布隔离，均为显式例外。
- 可观测 UI 均为 HTTPS；Sentinel Dashboard 在 B5.11 完成 TLS 与登录。
- 工作负载 mTLS 的证书身份已落地；SPIFFE/服务网格与按调用方授权不在本节的宣称范围内。

## 13. 中间件账号与最小权限（B4.3 → B5.5）

B4.3 在 TLS 与网络边界之上补账号认证，但当时是共享账号方案。B5.5 已把 MySQL、Redis、Nacos 与 Silo
拆成每服务独立身份；本节描述当前矩阵，历史共享账号仅保留在归档变更中。

### 13.1 账号矩阵

| 组件 | 账号 | 用途 | 权限 |
|---|---|---|---|
| MySQL | `iboot_<service>` | 八个数据服务（system/workflow/file/notify/job/monitor/auth/ai） | 共享表 `SELECT` + 本服务写表 `INSERT/UPDATE/DELETE`；无库级 DML、无 DDL |
| MySQL | `root` | 账号初始化与迁移 | 管理权限；不注入业务服务 |
| MySQL | `iboot_exporter` | mysqld-exporter | `PROCESS`、`REPLICATION CLIENT`、`performance_schema` 只读 |
| Redis | `iboot_<service>` | 八个数据服务 | 命名 ACL 账号，按登记键/频道模式限制；monitor 键空间浏览例外 |
| Redis | `iboot_exporter` | redis-exporter 与健康检查 | 只读监控命令集 |
| Nacos | `iboot_<service>` | 十个服务（含 gateway/ops） | 绑定 `iboot_role`，配置读写 |
| Nacos | `iboot` | 管理脚本 | 运维账号；不注入业务服务 |
| Silo | `iboot_file` | 文件服务 | 仅 `iboot-files` 桶 |
| Silo | `iboot_job` | 备份任务 | 仅 `iboot-files` 桶 |
| Silo | `iboot_ai` | AI 服务 | 仅 `iboot-ai` 桶 |
| Silo | 根账号 | `silo-init`、Thanos、运维 | 不注入业务服务 |
| Neo4j | `neo4j` | AI 图能力 | 社区版单账号；口令由 `NEO4J_PASSWORD` 注入，缺失时编排失败 |

共享账号 `iboot_app` 已删除；服务账号名由服务名把 `-` 换成 `_` 得到（如 `iboot-system` → `iboot_system`）。

B5.13 起，MySQL 写权限由 `scripts/mysql-table-grants.tsv` 与 `scripts/mysql-grants-init.sh` 在建表后授予；
账号创建脚本不再授予库级 DML。Redis 键模式在 compose 的 ACL 用户定义中维护。

### 13.2 初始化

```bash
# 宿主机开发
scripts/dev-init.sh
# 或单独执行
docker compose -f docker/docker-compose.yml run --rm --no-deps mysql-users-init
docker compose -f docker/docker-compose.yml run --rm --no-deps nacos-init
docker compose -f docker/docker-compose.yml run --rm --no-deps silo-init
scripts/sentinel-init.sh --verify
```

- `mysql-users-init` 幂等创建/更新八个服务账号与 `iboot_exporter`，并删除共享 `iboot_app`。
- `nacos-init` 首次设置 `nacos` 管理员密码，随后创建/更新 `iboot` 运维账号与十个服务账号。
- `silo-init` 预创建 `thanos`、`iboot-files`、`iboot-ai` 三个桶，并创建三个桶级服务账号。
- Docker 全量部署中，`db-init` 依赖上述初始化任务；宿主机开发由 `dev-init.sh` 显式执行。

### 13.3 凭据

根 `.env` 必须提供：

```text
MIDDLEWARE_SYSTEM_PASSWORD=
MIDDLEWARE_WORKFLOW_PASSWORD=
MIDDLEWARE_FILE_PASSWORD=
MIDDLEWARE_NOTIFY_PASSWORD=
MIDDLEWARE_JOB_PASSWORD=
MIDDLEWARE_MONITOR_PASSWORD=
MIDDLEWARE_AUTH_PASSWORD=
MIDDLEWARE_AI_PASSWORD=
MIDDLEWARE_GATEWAY_PASSWORD=
MIDDLEWARE_OPS_PASSWORD=
MYSQL_ROOT_PASSWORD=
MYSQL_EXPORTER_PASSWORD=
REDIS_EXPORTER_PASSWORD=
SILO_ROOT_USER=
SILO_ROOT_PASSWORD=
SILO_FILE_ACCESS_KEY=
SILO_FILE_SECRET_KEY=
SILO_JOB_ACCESS_KEY=
SILO_JOB_SECRET_KEY=
SILO_AI_ACCESS_KEY=
SILO_AI_SECRET_KEY=
NACOS_USERNAME=
NACOS_PASSWORD=
NACOS_ADMIN_PASSWORD=
NACOS_AUTH_TOKEN=
```

- 每服务口令同时用于该服务的 MySQL、Redis 与 Nacos；生产可分别覆盖，本地开发不拆三套。
- 缺失任一变量时，`dev-init.sh` 或 `docker compose` 在启动前失败。
- 口令不写入镜像、compose 默认值或版本管理；回归输出不打印口令。
- `NACOS_SERVER_ADDR` 必须带 `https://`，否则 Nacos 客户端会用明文调用 TLS 端口并登录失败。

### 13.4 验收

```bash
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/middleware-auth.sh
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/regress.sh
```

`middleware-auth.sh` 从中间件侧取证：MySQL `information_schema.processlist` 必须出现八个服务账号，
Redis `CLIENT LIST` 必须按服务区分，Nacos 逐账号登录并读取配置，Silo 用文件/AI 账号做跨桶拒绝。
同时检查每个服务进程只持有自己的 `MYSQL_*`/`REDIS_*`/`NACOS_*` 身份，且不含根或管理口令。

检查覆盖：

- MySQL：八个服务账号各自可查询、执行 DDL 被拒绝；exporter 不能写业务库；错误口令失败。
- Redis：八个服务账号各自可 `PING`；未认证、`default` 用户与错误口令被拒绝；exporter 可读 `INFO`、不能写。
- Silo：`minioadmin` 被拒绝；文件/备份账号可读文件桶、AI 账号可读 AI 桶，交叉访问失败。
- Nacos：十个服务账号逐一登录并读配置；匿名管理请求被拒绝；`sentinel-init` 缺少凭据时失败。
- 进程环境：每个服务只持有自己的 `MYSQL_*`/`REDIS_*`/`NACOS_*` 与专属 Silo 凭据。

### 13.5 边界

- MySQL root 仍允许从编排网络连接，仅用于初始化与迁移；后续可改为专用迁移账号并关闭远程 root。
- Redis 的每服务用户权限等价（`~*`、`+@all`），身份可区分但不是键级隔离。
- Nacos 的十个服务账号权限等价（同绑 `iboot_role`），身份可区分但不是资源级隔离。
- Nacos 控制台鉴权在本地保持关闭以支持门户内嵌；非本地必须开启，且门户不代填凭据。
- Neo4j 使用环境注入的单一账号；社区版不支持按服务划分用户，不能据此声称工作负载身份。
- Seata 2.6.0 无原生 TLS/认证；只有 workflow/system 加入 `tx` 网络，8091 仅回环发布。该链路是显式例外，不计入 mTLS 覆盖。
- Neo4j 的 `NEO4J_AUTH` 只在空数据卷首次启动时生效。已有 `neo4j_data` 卷时，修改
  `NEO4J_PASSWORD` 不会改数据库内口令，需先停止 Neo4j 后执行
  `neo4j-admin dbms set-initial-password <新口令>`，或删除 graph profile 的数据卷重建
  （图数据可由业务重新同步）。
- Sentinel 控制台登录、RocketMQ ACL 与 Seata 认证不在 B4.3 范围；Kafka SASL 与 Redis ACL 已在 B4.6 完成。

## 14. 默认值、账号与证书告警收尾（B4.4）

B4.4 处理 B4.3 审计中剩下的默认值和运维缺口，不改变 TLS 拓扑。

### 14.1 Elasticsearch 专用账号

`es-users-init` 在 ES 健康后幂等创建六个账号，业务容器不再持有 `elastic` 口令：

| 账号 | 用途 | 权限 |
|---|---|---|
| `iboot_ai` | AI 知识库 | `iboot-kb-chunk-*` 读写、建索引 |
| `iboot_notify` | 通知内容索引 | `iboot-content-*` 读写、建索引 |
| `iboot_monitor` | 运行日志查询 | `iboot-logs-*` 只读 |
| `iboot_grafana` | Grafana 日志数据源 | `iboot-logs-*` 只读 |
| `iboot_logstash` | 日志写入 | `iboot-logs-*` 写入、建索引 |
| `iboot_exporter` | 指标导出 | 集群与索引只读监控 |

- 口令由 `ES_*_PASSWORD` 注入；`ELASTIC_PASSWORD` 只进入 `es-users-init`、ES/Kibana
  初始化与运维脚本。
- 编排在业务服务中显式清空 `ELASTIC_PASSWORD` 与其它服务的 ES 口令，避免 `.env`
  整文件挂载后越权可见。

### 14.2 Grafana 与 Neo4j 默认值

- Grafana 匿名角色从 `Admin` 降为 `Viewer`，管理员口令改为
  `GRAFANA_ADMIN_PASSWORD` 必填；匿名用户不能改数据源或调用管理接口。
  已有 `grafana_data` 卷时环境变量不会覆盖数据库中的旧管理员口令，需执行
  `docker exec iboot-grafana grafana cli admin reset-admin-password <新口令>`。
- Neo4j 口令改为 `NEO4J_PASSWORD` 必填，`.env.example` 只保留本地占位值。
  已有数据卷的改口令方式见 §13.5。

### 14.3 证书过期告警

- `blackbox-exporter` 用本地 CA 探测 `iboot-system:8080`（宿主机模式经
  `host.docker.internal`），暴露 `probe_ssl_earliest_cert_expiry`。
- `tls-alerts` 规则：剩余 30 天 warning、7 天 critical、探针失败 warning。
- 服务端证书已按服务拆分，但十张证书同批生成、到期时间一致，因此证书探针仍只需一个目标；
  后续若引入不同有效期的证书，必须按证书追加探针目标。

验证：

```bash
scripts/regress/tls-cert-monitor.sh
curl -s 'http://127.0.0.1:9090/api/v1/query?query=probe_ssl_earliest_cert_expiry{job="tls-cert"}'
```

### 14.4 明文默认地址审计

`scripts/regress/plaintext-audit.sh` 扫描应用配置与 Grafana 数据源；命中 `http://`
且未登记在 `scripts/regress/plaintext-allowlist.txt` 时失败。当前例外：

- CLI 回环 OAuth 回调（RFC 8252，不承载业务数据）。

允许清单只允许缩小；新增条目必须在评审中说明用途、不可加密原因与后续处理去向。

### 14.5 边界

- Prometheus、Thanos 的明文链路已在 B4.7 收敛为 TLS；Sentinel Dashboard 已在 B5.11 收敛为 HTTPS。
- Grafana 匿名只读依赖 Viewer 角色；若 Explore 权限不可用，按 `design.md` 的退路处理，
  不得回退匿名 Admin。
- 本批次不包含服务间 mTLS、RocketMQ 强制 TLS、Seata 认证与 Kafka SASL。

## 15. Tempo TLS（B4.5）

Tempo 的 OTLP/HTTP（4318）、OTLP/gRPC（4317）接收端口与查询 HTTP（3200）只接受 TLS，证书复用本地叶子证书。

- `docker/tempo/tempo.yaml` 用 `server.http_tls_config` 为 3200 提供 HTTPS，并在两个 OTLP receiver 上配置 `tls`。
- 9095 是 Tempo 进程内 frontend↔querier 的 gRPC 端口，不发布到宿主机；开启 `grpc_tls_config` 会打断内部调用，因此保留明文并登记为边界。
- 应用 `TEMPO_OTLP` 与 Grafana `Tempo` 数据源均为 `https://`；Grafana 通过 `tlsAuthWithCACert` 使用 `/certs/rootCA.pem` 校验证书。
- 明文允许清单移除 Tempo 两条；B4.7 完成后仅保留 CLI 回环回调。

验证：

```bash
echo | openssl s_client -connect localhost:3200 -CAfile docker/certs/ca/rootCA.pem
curl --cacert docker/certs/ca/rootCA.pem https://localhost:3200/status
scripts/regress/internal-tls.sh
scripts/regress/trace-chain.sh <traceId> --tempo https://localhost:3200
```

回滚：恢复上一版 Tempo 配置与 compose，重建 Tempo，并把 `TEMPO_OTLP` 与 Grafana 数据源改回 HTTP；不存在运行时明文回退开关。

## 16. Kafka 与 Redis 账号鉴权（B4.6）

B4.6 在 TLS 之上补客户端身份：Kafka 从“只加密”变为“认证 + Topic 授权”，Redis 从默认用户变为命名 ACL 账号。

### 16.1 Kafka SASL 与 ACL

| 账号 | 用途 | 权限 |
|---|---|---|
| `iboot_log` | 九个服务与运维控制台的日志生产 | 写 `iboot-logs` |
| `iboot_logstash` | Logstash 日志消费 | 读 `iboot-logs` 与 `iboot-logstash` 消费组 |
| `iboot_tools` | `kafka-init`、健康检查与运维 | 超级用户 |
| `broker` | 节点间通信 | 超级用户 |

- 两个客户端监听使用 `SASL_SSL` + `PLAIN`，`StandardAuthorizer` 在无 ACL 时默认拒绝。
- `kafka-init` 以 `iboot_tools` 创建 topic，并写入生产/消费的最小 ACL；重复执行幂等。
- 口令只允许字母、数字、`-`、`_`，避免 JAAS 解析歧义；口令由 `KAFKA_*_PASSWORD` 注入。

### 16.2 Redis ACL

| 账号 | 用途 | 权限 |
|---|---|---|
| `iboot_<service>` | 八个数据服务 | 全部命令；每个服务独立口令 |
| `iboot_exporter` | redis-exporter 与健康检查 | 只读与监控命令，不能写 |

- `default` 用户关闭；服务通过 `spring.data.redis.username=${REDIS_USERNAME}` 认证，值为自己的 `iboot_<service>`。
- exporter 使用 `REDIS_USER=iboot_exporter` 与独立口令。
- ACL 由启动参数恢复，不依赖额外文件；容器重建后自动生效。

验证：

```bash
scripts/regress/middleware-auth.sh
scripts/regress/log-search-tls.sh
scripts/regress/log-pipeline.sh
```

回滚：恢复旧 compose 与镜像重建 Kafka/Redis；Kafka ACL 元数据保留但在 Authorizer 关闭后不生效；
Redis 启动参数恢复为 `--requirepass` 即回到默认用户模式。

## 17. 可观测控制台 TLS（B4.7）

Grafana、Prometheus、Alertmanager、Thanos Query 与 Kibana 的 HTTP 入口只提供 HTTPS，证书复用本地叶子证书。

- Prometheus/Alertmanager 通过 `--web.config.file`、Thanos Query 通过 `--http.config` 加载 `tls_server_config`。
- Grafana 使用 `[server] protocol=https`；Kibana 使用 `SERVER_SSL_ENABLED=true`。
- Grafana 的 Prometheus/Thanos 数据源、Thanos sidecar 到 Prometheus、Prometheus 到 Alertmanager 均使用 HTTPS + 本地 CA。
- `sys_config` 中五个控制台地址由 `V16__observability_https_urls.sql` 迁移为 `https://`。
- Sentinel Dashboard 使用 HTTPS 并要求登录（B5.11）。

验证：

```bash
scripts/regress/observability-tls.sh
scripts/regress/tls-cert-monitor.sh
scripts/regress/regress.sh
```

回滚：恢复上一版 Compose/配置与迁移前的控制台地址，重建可观测组件；不存在运行时明文回退开关。

## 18. 服务间 mTLS（B5.2）

B5.2 在 B5.1 的每服务独立证书之上启用双向 TLS：服务端口要求客户端证书，调用方以各自的
工作负载证书证明身份。

### 18.1 服务端

- 三份 `iboot-tls.yml`（kernel、gateway、ops）设置 `server.ssl.client-auth=need`；
  truststore 仍来自 `iboot` bundle 的本地 CA。
- 服务证书 EKU 为 `serverAuth,clientAuth`；旧证书缺少 `clientAuth` 时 `tls-init.sh`
  会自动重签。
- 服务容器的健康检查仍只做 TCP 探活，不发起 TLS 握手，避免因缺证书误判。

### 18.2 调用方身份

| 调用方 | 客户端证书 |
|---|---|
| 十个业务服务 | 自己的 `services/<service>.pem`（Spring HTTP 客户端自动使用 `iboot` bundle） |
| 网关 | `services/iboot-gateway.pem`；额外设置 Gateway `httpclient.ssl.ssl-bundle` |
| 门户 nginx | `portal.pem`，通过 `proxy_ssl_certificate` |
| 门户 Vite（宿主机） | `portal.pem`，通过 `http-proxy` 的 `cert` / `key` |
| Prometheus | `clients/prometheus.pem`，抓取 job 的 `tls_config` |
| blackbox | `clients/blackbox.pem`，`https_cert` 模块的 `tls_config` |
| 回归脚本 | `portal.pem`（扮演门户这一合法调用方） |

### 18.3 验收

```bash
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/internal-tls.sh
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/regress.sh
```

`internal-tls.sh` 新增：

- 每个服务端口不带客户端证书必须失败；
- 每个服务证书同时具备 `SSL server` 与 `SSL client` 用途；
- 每张服务证书都能作为客户端完成握手；
- 门户证书可作为客户端访问服务端口。

### 18.4 排障

```bash
# 带客户端证书访问服务健康检查
curl --cacert docker/certs/ca/rootCA.pem \
  --cert docker/certs/portal.pem --key docker/certs/portal-key.pem \
  https://localhost:8080/actuator/health

# 查看服务端证书用途
openssl x509 -in docker/certs/services/iboot-system.pem -noout -purpose
```

出现 `certificate required`、`handshake failure` 或 curl `000` 时，先确认
客户端证书由本地 CA 签发且未过期，再检查 `IBOOT_TLS_CERT_FILE` / `IBOOT_TLS_KEY_FILE`
是否指向本服务文件。

### 18.5 边界

- 服务端已按 SPIFFE ID（CN 回退）做路径级授权（B5.4/B5.8）；方法级业务权限仍由 JWT 决定，SPIFFE Workload API 与服务网格留待后续。
- Nacos 注册客户端仍使用共享 `portal.pem`；注册通道不是业务服务端口。
- 证书探针仍以一个服务端点反映同批签发的叶子证书到期时间；拆分不同有效期前必须扩展探针。

## 19. RocketMQ TLS-only 与 Seata 例外（B5.3）

B5.3 收掉两条未闭环链路：RocketMQ 应用入口只接受 TLS；Seata 因无原生 TLS/认证，登记为显式例外并做专用网络隔离。

### 19.1 RocketMQ

- 新增 `rocketmq-tls`（nginx `stream` + `ssl_preread`）作为唯一应用入口：TLS ClientHello 透传到
  `rocketmq-proxy:8081`，非 TLS 连接被关闭。TLS 仍在客户端与 Proxy 之间端到端完成。
- RocketMQ NameServer、Broker、Proxy 与初始化任务只加入 `mq` 网络；`rocketmq-tls` 同时加入
  `app` 与 `mq`。应用连接 `rocketmq-tls:8081`，宿主机连接 `localhost:18081`。
- Proxy 原生 8080/8081 不再发布到宿主机；`18080` 映射已移除。
- 客户端服务端证书校验由 B5.12 兼容层补齐（见 §26）；ACL 与调用方身份见 §22。
  该链路仍不是 mTLS：客户端没有证书身份。

验证：

```bash
scripts/regress/log-search-tls.sh
scripts/regress/exposure.sh
```

应看到 RocketMQ TLS 握手成功、明文连接失败，以及应用容器无法直连 `rocketmq-proxy:8081`。

### 19.2 Seata

Seata 2.6.0 没有原生传输层 TLS/认证，本批不做 TLS 终止器，改为：

- `seata-server` 只加入 `tx` 网络；只有 `iboot-workflow` 与 `iboot-system` 加入 `tx`。
- 8091 继续只绑定宿主机 `127.0.0.1`；其余服务无法解析或连接 `seata-server:8091`。
- 业务侧调用仍由服务 mTLS（B5.2）与 `X-Internal-Token` 保护；Seata TC 控制面本身是例外。
- 残余风险：被攻陷的 workflow/system 仍可干扰 TC；复审触发为 Seata 原生 TLS/认证或服务网格引入。
- 该链路不计入 mTLS 覆盖率，文档与回归不得写成零信任。

验证：

```bash
scripts/regress/seata.sh
scripts/regress/seata-tcc.sh
scripts/regress/exposure.sh
```

`exposure.sh` 应看到 workflow/system 可访问 Seata 8091，而 `iboot-file`/`iboot-ai` 不可访问。

### 19.3 回滚

- RocketMQ：恢复 Proxy 的 `18080/18081` 映射与 `rocketmq-proxy:8081` 端点，移除 `rocketmq-tls`；
  网络可保留。
- Seata：把 `seata-server` 放回 `data`，移除 system/workflow 的 `tx` 网络；8091 回环发布不变。

## 20. 服务间授权（B5.4）

B5.2 解决了“调用方是谁”，B5.4 进一步限制“这个调用方能访问哪些路径”。授权只作用于服务间请求，
用户 API 的权限仍由 JWT 与方法级权限决定。

### 20.1 身份与过滤器

- 身份优先取自已由本地 CA 校验的客户端证书 URI SAN（`spiffe://iboot.local/service/<name>` 或 `/client/<name>`），没有 SPIFFE ID 时回退 Subject CN：服务证书 CN 为服务名，门户为
  `iboot-portal`，探针为 `prometheus` / `blackbox`。
- 共享模块 `iboot-service-authz` 提供 Servlet 与 WebFlux 过滤器：
  - 8 个 Servlet 服务通过 `iboot-kernel` 以最高优先级注册，早于 Spring Security；
  - 网关注册 WebFlux 过滤器，在路由前执行；
  - 运维控制台注册 Servlet 过滤器，在 Basic 认证前执行。
- 命中规则且调用方不在白名单时返回 403 `{"code":403,"message":"调用方无权访问"}`，不进入业务逻辑。

### 20.2 调用矩阵

公共规则（三份 `iboot-tls.yml`）：

| 路径 | 允许调用方 |
|---|---|
| `/actuator/**` | `iboot-ops`、`prometheus`、`blackbox`、`iboot-portal` |
| `/oauth2/jwks` | 全部 `iboot-*` 服务、网关、门户 |
| `/oauth2/**`、`/.well-known/**`、`/login`、`/connect/**` | 网关、门户 |
| `/sba/**`、`/instances/**`、`/applications/**` | 门户 |
| `/druid/**`、Swagger/OpenAPI 路径 | 门户 |

服务专属规则（`application.yml` 的 `extra-rules`，先于公共规则）：

| 服务 | 规则 |
|---|---|
| system | `/internal/leave/quota/**` → workflow；`/api/auth/scope` → file/monitor/job/ai/gateway；`/api/user/**`、`/api/log/login/**`、`/api/dept/**`、`/api/menu/**`、`/api/role/**` → ai/gateway；`/api/**` → gateway |
| notify | `/internal/notifications/**` → workflow；`/api/**` → gateway |
| job | `/api/monitor/backup/**` → ai/gateway；`/api/**` → gateway |
| file/monitor/workflow/auth/ai | `/api/**` → gateway |
| gateway | `/api/**` → portal |
| ops | 无额外规则，依赖公共 `/sba`、`/actuator` |

### 20.3 开关与默认值

```yaml
iboot:
  security:
    service-authorization:
      enabled: ${IBOOT_SERVICE_AUTHORIZATION_ENABLED:false}
```

- 默认关闭，避免单元测试切片因缺少客户端证书失败。
- Docker 公共环境与 `dev-init.sh` 显式设为 `true`；宿主机明文排障模式默认随内部 TLS 关闭。
- 规则未覆盖的路径保持既有 JWT/白名单语义。

### 20.4 验收

```bash
scripts/regress/service-authz.sh
scripts/regress/regress.sh
```

`service-authz.sh` 覆盖：

- 允许：门户→网关、门户→Actuator/文档、Prometheus/blackbox→Actuator、SBA→Actuator、
  AI→system 用户接口、file→scope、workflow→internal；
- 拒绝：file→网关 API、file→system 用户接口/Actuator/文档/internal、file→网关 Actuator。

### 20.5 边界与回滚

- 只做路径级工作负载授权，不替代用户权限、方法级 `@PreAuthorize` 或数据权限。
- 不基于来源 IP、网段或请求头；证书 SPIFFE ID 是首选身份，CN 仅作旧证书兼容回退。
- 回滚：把 `IBOOT_SERVICE_AUTHORIZATION_ENABLED` 设为 `false` 并重建服务；规则与代码可保留。
- 新增服务或调用方时，必须同步更新 `extra-rules` 与 `service-authz.sh` 的最小用例。

## 21. 中间件每服务身份（B5.5）

B4.3 解决“中间件需要认证”，B5.5 解决“认证的是哪个服务”。此前 MySQL/Redis 共用 `iboot_app`、
Nacos 共用 `iboot`、文件与 AI 共用 Silo 根账号，中间件的连接列表与审计无法归因到服务；
工作负载的 mTLS 身份到数据面就断开了。

### 21.1 身份矩阵

| 层 | 身份来源 | 权限 |
|---|---|---|
| HTTP 服务间 | 客户端证书 SPIFFE ID（CN 回退，B5.2/B5.4/B5.8） | 路径级调用方白名单 |
| MySQL | `iboot_<service>` | `iboot` 库 DML；无 DDL |
| Redis | `iboot_<service>` | 命名 ACL，全部命令 |
| Nacos | `iboot_<service>` | `iboot_role` 配置读写 |
| Silo | `iboot_file` / `iboot_job` / `iboot_ai` | 各自桶的读写 |

服务名到账号名的映射固定为把 `-` 换成 `_`，例如 `iboot-workflow` → `iboot_workflow`。

### 21.2 注入方式

- Docker：`x-app-env` 先清空所有每服务口令与根凭据，各服务块只注入自己的 `MYSQL_*`、`REDIS_*`、
  `NACOS_*`；文件、备份与 AI 分别注入 `SILO_FILE_*`、`SILO_JOB_*`、`SILO_AI_*`。
- 宿主机：`dev-init.sh` 用 `env -u` 移除其它服务的 `MIDDLEWARE_*_PASSWORD` 与根凭据，再注入本服务身份。
- 迁移期可保留旧共享账号口令变量，但必须保持未使用；回归会断言服务进程环境不含非本服务凭据。

### 21.3 验收

```bash
bash scripts/regress/middleware-auth.sh
bash scripts/regress/regress.sh
```

身份矩阵用例覆盖：

- MySQL processlist 出现八个 `iboot_<service>`，且不存在 `iboot_app` 连接或账号；
- Redis `CLIENT LIST` 的用户标签按服务区分，且无 `iboot_app`；
- Nacos 十个服务账号逐一登录并读取配置；
- Silo 文件/备份账号可读文件桶、AI 账号可读 AI 桶，交叉访问被拒绝；
- 每个服务进程环境只含自己的身份，不含根或其它服务口令。

### 21.4 边界

- B5.13 已完成表级/键级授权：MySQL 写权限按表授予，Redis 按前缀限制；monitor 的键空间浏览是显式例外。
- 每服务口令默认在 MySQL/Redis/Nacos 间复用；生产应通过独立变量覆盖后分别轮换。
- Kafka 日志账号保持按用途（生产/消费/运维）区分；Neo4j 社区版仍是单账号。
- 共享库表级只读契约不变，见 [docs/23](<./23-微服务底座操作指南.md>) §11.12。

## 22. RocketMQ ACL（B5.6）

B5.3 只保证入口加密，没有回答“谁能发布、谁能消费”。B5.6 在 Broker 与 Proxy 上启用 ACL 2.0，
让 workflow 与 notify 使用各自的 access key，并把运维账号限制在初始化与排障路径。

### 22.1 账号与权限

| 账号 | 用途 | 权限 |
|---|---|---|
| `iboot_rocketmq` | Broker/Proxy 初始化、`mqadmin`、排障 | SUPER；不注入业务服务 |
| `iboot_workflow` | 审批通知生产 | `Topic:iboot-notification` 的 `PUB` |
| `iboot_notify` | 通知消费 | `Topic:iboot-notification`、`%RETRY%iboot-notify-consumer` 的 `SUB`；`Group:iboot-notify-consumer` 的 `SUB` |

- Broker 启用 `aclEnable`、认证与授权；元数据使用 Local Provider 并持久化到 `rocketmq_auth_data` 卷。
- Proxy 通过 Broker 元数据校验调用方；`enableAclRpcHookForClusterMode=true` 用于集群模式 gRPC 校验。
- 凭据由环境注入，入口脚本生成 `broker.conf`、`rmq-proxy.json` 与 `tools.yml`；仓库不保存口令。
- topic 初始化与 ACL 初始化都使用运维账号生成的 `tools.yml`，不依赖镜像默认凭据。

### 22.2 验收

```bash
bash scripts/regress/rocketmq-acl.sh
bash scripts/regress/regress.sh
```

`rocketmq-acl.sh` 覆盖：

- 运维账号可列出用户与 ACL；workflow/notify 账号存在且授权项正确；
- workflow 可发布 `iboot-notification`，notify 不能发布；
- notify 拥有消费组与 topic 授权，workflow 不能消费；
- 无凭据与错误凭据被拒绝；
- 镜像默认 `tools.yml` 无法管理 Broker。

### 22.3 边界与回滚

- 服务端证书校验由 B5.12 兼容层提供；ACL 仍只负责调用方身份，不构成 mTLS。
- NameServer 未启用认证；它只服务 Broker/Proxy 的路由查询，不在应用入口路径上。
- 回滚：移除 Broker/Proxy 的 ACL 配置与 `rocketmq-acl-init`，客户端凭据字段可保留为空。

## 23. Exporter mTLS 与证书轮换（B5.7）

B5.7 把指标采集链路从“容器网络内明文”收紧为 HTTPS + 客户端证书，并给出可重复的证书轮换入口。

### 23.1 Exporter mTLS

- `mysql-exporter`、`node-exporter`、`elasticsearch-exporter` 使用 exporter-toolkit
  `--web.config.file`：`client_auth_type: RequireAndVerifyClientCert`，CA 为本地根证书。
- `redis-exporter` 使用原生 `--tls-server-cert-file/key-file/ca-cert-file`，最低 TLS 1.2。
- 四个 exporter 各用独立服务端证书（`docker/certs/services/<name>.pem`，仅 serverAuth）。
- Prometheus 用 `clients/prometheus.pem` 作为客户端身份，抓取配置为 HTTPS + CA + 客户端证书 +
  `server_name`。
- exporter 不发布到宿主机；回归通过临时 curl 容器在对应 Docker 网络内取证。

验收：

```bash
scripts/regress/exporter-mtls.sh
scripts/regress/regress.sh
```

应看到四个 exporter 的携带证书请求 200、明文 400、无客户端证书与错误 CA 失败，以及 Prometheus
四个抓取目标 `up=1`。

### 23.2 证书轮换

```bash
scripts/tls-rotate.sh --check                  # 只检查，失败退出
scripts/tls-rotate.sh --rotate --restart none  # 显式重签，不重启
scripts/tls-rotate.sh --rotate --restart docker
scripts/tls-rotate.sh --rotate --restart host
```

- 普通启动只补发缺失的 exporter 证书，不重签既有服务证书；`--rotate` 才调用 `tls-init.sh --force`。
- Docker 重启对当前运行的 compose 服务执行 `up -d --force-recreate`；宿主机重启调用 `dev-init.sh --stop` 后再启动。
- 周期调度由独立入口安装，不与普通启动耦合：

```bash
scripts/tls-rotate-schedule.sh --install --restart=docker  # 每日检查，失败才重签并重建当前 Docker 服务
scripts/tls-rotate-schedule.sh --install --restart=host    # 宿主机进程场景
scripts/tls-rotate-schedule.sh --status
scripts/tls-rotate-schedule.sh --uninstall
scripts/tls-rotate-schedule.sh --print                     # 只输出将安装的内容
```

- 优先安装 systemd user timer；当前会话不可用时回退到当前用户 crontab。默认每日 `03:30` 执行。
- 定时任务先跑 `--check`：通过则只写日志，不碰证书；失败才执行 `--rotate --restart=<scope>`。
- 默认 `--restart=none`，需要自动生效时必须显式选择 `docker` 或 `host`。
- 日志写入 `logs/tls-rotate.log`，目录锁防止并发重签。

### 23.3 边界

- 只覆盖 Prometheus → exporter；Grafana/Thanos/Kibana 的内网调用仍使用既有 HTTPS。
- 调度只负责检查、重签与按范围重启；CA 轮换、跨主机调度与滚动发布不在本批范围内。
- Dashboard 的反向探测命令端口 8719 仍为 HTTP，只在 Docker 网络内使用，不发布到宿主机。

## 24. SPIFFE 工作负载身份（B5.8）

B5.8 把工作负载身份从展示字段 CN 迁移到 SPIFFE URI SAN：证书本身携带结构化身份，授权层优先读取它，
CN 仅作为旧证书的兼容回退。

### 24.1 身份格式

| 证书 | SPIFFE ID |
|---|---|
| 服务 | `spiffe://iboot.local/service/<service>` |
| 探针客户端 | `spiffe://iboot.local/client/<name>` |
| portal / exporter | `spiffe://iboot.local/service/<name>` |

- URI SAN 与 CN 必须一致；`scripts/tls-init.sh --check` 会同时校验。
- 信任域固定为 `iboot.local`；切换时必须同时修改 `tls-init.sh` 与 Java 解析器。
- 授权规则仍使用服务名，不绑定证书序列号或有效期。

### 24.2 解析顺序

1. 遍历证书 URI SAN，匹配 `spiffe://iboot.local/service/<name>` 或 `/client/<name>`；
2. 没有匹配时回退 Subject CN；
3. 两者都不可用时视为匿名调用方，命中规则即 403。

网关、八个 Servlet 服务、运维控制台与 WebFlux 网关共用同一解析器；过滤器与策略签名不变。

### 24.3 验收

```bash
mvn -pl iboot-service-authz test
bash scripts/regress/internal-tls.sh
bash scripts/regress/service-authz.sh
```

`service-authz.sh` 用本地 CA 临时签发“CN=iboot-file、SPIFFE ID=iboot-gateway”的证书：访问
`/api/user/**` 应按 gateway 身份通过授权（返回 401 等待 JWT），而只有 CN 的证书仍按 file 身份被 403 拒绝。

### 24.4 边界

- 不部署 SPIRE/SPIFFE Workload API；证书仍由本地 CA 直接签发。
- 不引入 OPA 或服务网格；策略仍由 `ServiceAuthorizationPolicy` 执行。
- 信任域切换需要同时改 `tls-init.sh` 与 `ClientCertificateIdentity`，当前不支持多信任域。

## 25. Sentinel Dashboard TLS 与登录（B5.11）

Sentinel Dashboard 是最后收敛的可观测控制台：此前是回环 HTTP 且本地关闭登录。B5.11 让它与
Grafana/Kibana 使用同一条基线。

### 25.1 HTTPS

- `tls-init.sh` 在门户证书 SAN 中增加 `sentinel-dashboard`，并生成
  `docker/certs/runtime/sentinel-keystore.p12`（alias `sentinel`，口令 `changeit`）。
- Dashboard 通过 Spring Boot 属性开启 HTTPS：

```text
-Dserver.ssl.enabled=true
-Dserver.ssl.key-store=file:/certs/sentinel-keystore.p12
-Dserver.ssl.key-store-password=changeit
-Dserver.ssl.key-store-type=PKCS12
-Dserver.ssl.key-alias=sentinel
```

- 宿主机入口 `https://localhost:8858`；Docker 内客户端使用 `https://sentinel-dashboard:8858`。
- 健康检查与 `dev-init.sh` 都使用本地 CA 校验，不使用 `-k`。

### 25.2 登录

- 本地与生产都开启 `auth.enabled=true`；基础 compose 默认 `sentinel` / `sentinel123`。
- 生产叠加 `docker-compose.prod.yml` 时必须显式提供 `SENTINEL_DASHBOARD_USER` 与
  `SENTINEL_DASHBOARD_PASSWORD`，缺失时 compose 直接失败。
- 门户 `sys.sentinel.dashboard.url` 改为 HTTPS，`sys.sentinel.dashboard.embed` 改为 `false`；
  登录会话不能可靠跨源 iframe 复用，页面改为新窗口打开。

### 25.3 验收

```bash
scripts/tls-init.sh --check
bash scripts/regress/observability-tls.sh
```

应看到 Dashboard TLS 握手通过、明文失败、不带 CA 失败，以及匿名访问
`/app/briefinfos.json` 被重定向或拒绝。

### 25.4 边界

- Dashboard 反向探测服务命令端口 8719 仍是 HTTP，只在 Docker 网络内使用，不发布到宿主机。
- 规则仍从 Nacos 下发；Dashboard 重启不影响运行中的限流规则。

## 26. RocketMQ 服务端证书校验（B5.12）

官方 `rocketmq-client-java:5.2.2` 在 `RpcClientImpl` 中硬编码
`InsecureTrustManagerFactory.INSTANCE`，TLS 只加密、不校验服务端证书；最新发行版仍是 5.2.2。
B5.12 用一个最小兼容层修补该限制。

### 26.1 兼容层

- workflow 与 notify 的 `src/main/java` 中各有一个同 FQCN 的
  `org.apache.rocketmq.shaded...InsecureTrustManagerFactory`，只暴露客户端引用的 `INSTANCE`。
- Spring Boot fat jar 的 `BOOT-INF/classes` 优先于 `BOOT-INF/lib`，因此兼容层覆盖客户端 jar 内的
  不安全实现；客户端其余逻辑不变。
- gRPC 的 `ClientTlsHandler` 已启用 `HTTPS` 主机名校验；兼容层只替换信任管理器，不关闭主机名校验。
- 初始化失败抛出 `ExceptionInInitializerError`，客户端失败关闭，不回退到信任所有证书。

### 26.2 信任库

- 默认使用 JVM 的 `javax.net.ssl.trustStore`；本项目的宿主机与容器进程都指向
  `docker/certs/truststore.p12`。
- 可选覆盖：

```text
-Diboot.rocketmq.trustStore=/path/truststore.p12
-Diboot.rocketmq.trustStorePassword=changeit
-Diboot.rocketmq.trustStoreType=PKCS12
```

或对应环境变量 `IBOOT_ROCKETMQ_TRUSTSTORE`、`IBOOT_ROCKETMQ_TRUSTSTORE_PASSWORD`、
`IBOOT_ROCKETMQ_TRUSTSTORE_TYPE`。

### 26.3 SAN

应用连接 `rocketmq-tls:8081`，因此 `tls-init.sh` 的门户证书 SAN 增加 `rocketmq-tls`。
`--check` 会校验该 SAN；旧证书需要执行 `scripts/tls-init.sh --force` 或
`scripts/tls-rotate.sh --rotate --restart docker`。

### 26.4 验收

```bash
scripts/tls-init.sh --check
bash scripts/regress/rocketmq-server-auth.sh
bash scripts/regress/rocketmq-acl.sh
```

`rocketmq-server-auth.sh` 覆盖 SAN、fat jar 打包、正确 CA 投递、错误 CA（JDK cacerts）失败；
`rocketmq-acl.sh` 继续覆盖 workflow 发布与 notify 消费。

### 26.5 边界与升级

- 兼容层依赖客户端内部类路径，属于上游临时补丁；升级 `rocketmq-client-java` 后先确认
  `RpcClientImpl` 已不再引用不安全信任管理器，然后删除两个兼容层与相关测试。
- RocketMQ 仍不是 mTLS：客户端没有证书身份，调用方身份由 ACL access key 提供。
