# 27-TLS 与零信任操作指南

> 适用版本：iBoot 1.0.0。本文是本地 TLS 的唯一操作权威：证书生成、公开来源、
> 客户端信任、验收、排障与回滚。B0 的实测结论见
> [TLS 与零信任 Spike 报告](./spike/tls-zero-trust-spike-report.md)。

## 1. 范围与边界

当前已完成的加密范围：

- 浏览器/CLI → 门户（L1 边缘 TLS）；门户 → 网关；服务间调用与 JWKS。
- MySQL、Redis、Silo、Neo4j 等核心数据面；Elasticsearch/Kafka/Logstash 日志与检索面；Tempo 的 OTLP 上报与查询面；Kafka SASL 与 Redis 命名 ACL。
- Nacos 的 HTTP/控制台 HTTPS 与 gRPC mTLS；RocketMQ 只有加密通道（见 §9.4）。
- Docker 全量部署中，门户是唯一公开入口；宿主机侧只有 80/443 跨网卡监听，其余容器发布端口只绑定回环地址（§12）。
- 不引入本机服务网格、SPIFFE/OPA；每服务工作负载身份与完整 mTLS 留给 B5。

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
- 证书沿用 B1 的 `portal.pem` / `portal-key.pem`，SAN 已覆盖服务名、`localhost`、
  `127.0.0.1`；容器使用 `docker/certs/runtime/` 中容器用户可读的副本。

### 8.2 信任链

- Spring 客户端使用 SSL bundle（`spring.http.clients.ssl.bundle=iboot`）。
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

`internal-tls.sh` 覆盖逐端口 TLS、明文拒绝、无 CA 失败、网关→服务、JWKS、
MySQL cipher、Redis/Silo 明文拒绝与 Neo4j（运行 graph profile 时）。

### 8.5 边界与回退

- Nacos 的 TLS 在 B4 单独完成，见第 10 节；B2 本身不宣称完整零信任。
- **不提供运行期内部明文回退**：`IBOOT_INTERNAL_TLS_ENABLED=false` 会让
  `dev-init.sh` 在启动前失败。内网半开 TLS 会同时破坏服务间、JWKS 与数据面，
  定位成本高于直接回退。
- 回滚使用上一版 commit/镜像与 compose，或 `git revert`。B1 的边缘回退
  `IBOOT_TLS_ENABLED=false` 仍只影响公开入口，不改变内部 TLS。

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

### 9.4 RocketMQ 加密边界

- Proxy 加载本地证书并开启 TLS；workflow 与 notify 设置
  `IBOOT_ROCKETMQ_SSL_ENABLED=true`。
- **已知边界**：同一端口仍接受明文；客户端使用 `InsecureTrustManagerFactory`，
  不校验服务端证书。该链路只提供被动窃听防护，不提供身份认证或强制 TLS。
- 文档与回归输出统一使用“加密通道/已知边界”，不得写成零信任或 mTLS。
- 强制 TLS 与客户端认证留待 B4/B5 或前置 TLS 终止代理。

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
- B5：服务间 mTLS、每服务工作负载身份、RocketMQ 强制 TLS 与 Seata 例外决策。

## 12. 暴露面与网络分段

TLS 只保护「连上之后」的通道，不回答「谁允许连接」。本节把容器部署的宿主机暴露面与
容器网络边界固化下来；它仍不等于零信任身份，服务端目前不校验调用方身份（B5）。

### 12.1 宿主机端口

| 类别 | 端口 | 绑定 |
|---|---|---|
| 门户 | 80 / 443 | `0.0.0.0` |
| 网关与业务服务 | 8080–8089 | `127.0.0.1` |
| 数据面 | 3306、6379、9000/9001、9200、8848/9848/8849、8091、29092、18080/18081 | `127.0.0.1` |
| 治理与可观测 UI | 8858、9090、3000、9093、5601、18082、3200 | `127.0.0.1` |
| 图数据库（`graph` profile） | 7474、7687 | `127.0.0.1` |
| Logstash、Thanos、exporter、SkyWalking Banyandb/OAP admin | 不发布 | — |

Docker 的端口发布会绕过常见主机防火墙，因此不能采用「先发布到 `0.0.0.0` 再靠 ufw 限制」
的做法。需要从其它主机临时访问时，用 SSH 端口转发，不修改 compose。

### 12.2 容器网络

| 网络 | 成员 | 边界 |
|---|---|---|
| `edge` | 门户、网关、系统服务、运维控制台 | 门户唯一入口 |
| `app` | 网关、系统服务、运维控制台、八个业务服务、Prometheus | 服务间调用与指标抓取 |
| `data` | 中间件、业务与初始化任务、Logstash/Kibana/Grafana/Thanos/exporter | 数据面 |
| `obs` | Prometheus、Grafana、Alertmanager、Thanos、SkyWalking、exporter | 可观测组件 |

门户只加入 `edge`，无法解析 `mysql`、`redis`、`elasticsearch`、`nacos`。这不是文档约定，
而是由回归脚本检查的运行时边界。

### 12.3 验收

```bash
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/exposure.sh
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/regress.sh
```

`exposure.sh` 分三层：

1. 用 `docker inspect` 断言只有门户 80/443 绑定 `0.0.0.0`，其余已发布端口绑定 `127.0.0.1`，
   并断言已移除的映射不再出现。
2. 用宿主机非回环 IPv4 连接 3306、6379、9200、8848、29092，必须失败；回环地址必须成功。
3. 容器矩阵：门户访问网关成功，访问 MySQL/Redis/ES/Nacos 失败；业务服务访问 MySQL/Nacos 成功。
   容器未运行时明确跳过矩阵，但端口检查仍执行。

### 12.4 边界

- 只收敛 Docker 编排发布的网络暴露面，不改变各组件内部的 TLS 配置。
- 宿主机开发模式下由 `dev-init.sh` 直接启动的 Java 进程仍按 Spring Boot 默认监听所有网卡；
  它与 Prometheus 的 `host-gateway` 采集方式耦合，其绑定地址留待 B5 收敛。
- 中间件已启用账号认证，但仍是共享账号；服务间没有客户端证书。
- 可观测 UI 仍为 HTTP，但只监听回环地址。
- 完整身份与工作负载 mTLS 属于 B5，不在本节的宣称范围内。

## 13. 中间件账号与最小权限（B4.3）

B4.3 在 TLS 与网络边界之上补账号认证。它是**共享账号**方案：同一个服务账号被多个服务使用，
因此不能证明调用方是哪个服务；每服务身份仍属于 B5。

### 13.1 账号矩阵

| 组件 | 账号 | 用途 | 权限 |
|---|---|---|---|
| Redis | `iboot_app` | 九个业务服务 | 命名 ACL 账号，全部命令；口令由 `REDIS_PASSWORD` 注入 |
| Redis | `iboot_exporter` | redis-exporter | 只读监控命令集；口令由 `REDIS_EXPORTER_PASSWORD` 注入 |
| MySQL | `root` | 账号初始化与迁移 | 管理权限；不注入业务服务 |
| MySQL | `iboot_app` | 八个业务服务 | `iboot` 库 `SELECT/INSERT/UPDATE/DELETE` |
| MySQL | `iboot_exporter` | mysqld-exporter | `PROCESS`、`REPLICATION CLIENT`、`performance_schema` 只读 |
| Silo | 根账号 | 服务、`silo-init`、Thanos | 由 `SILO_ROOT_USER/PASSWORD` 注入 |
| Nacos | `iboot` | 服务注册/配置与管理脚本 | 配置读写；管理员 `nacos` 只用于初始化脚本 |
| Neo4j | `neo4j` | AI 图能力 | 社区版单账号；口令由 `NEO4J_PASSWORD` 注入，缺失时编排失败 |

### 13.2 初始化

```bash
# 宿主机开发
scripts/dev-init.sh
# 或单独执行
docker compose -f docker/docker-compose.yml run --rm --no-deps mysql-users-init
docker compose -f docker/docker-compose.yml run --rm --no-deps nacos-init
scripts/sentinel-init.sh --verify
```

- `mysql-users-init` 幂等创建/更新 `iboot_app` 与 `iboot_exporter`；`db-init` 与业务服务依赖它完成。
- `nacos-init` 首次设置 `nacos` 管理员密码，随后创建/更新 `iboot` 服务账号并授权。
- Docker 全量部署中，`db-init` 依赖上述两个一次性任务；宿主机开发由 `dev-init.sh` 显式执行。

### 13.3 凭据

根 `.env` 必须提供：

```text
MYSQL_ROOT_PASSWORD=
MYSQL_APP_PASSWORD=
MYSQL_EXPORTER_PASSWORD=
REDIS_PASSWORD=
SILO_ROOT_USER=
SILO_ROOT_PASSWORD=
NACOS_USERNAME=
NACOS_PASSWORD=
NACOS_ADMIN_PASSWORD=
NACOS_AUTH_TOKEN=
```

- 缺失任一变量时，`dev-init.sh` 或 `docker compose` 在启动前失败。
- 口令不写入镜像、compose 默认值或版本管理；回归输出不打印口令。
- `NACOS_SERVER_ADDR` 必须带 `https://`，否则 Nacos 客户端会用明文调用 TLS 端口并登录失败。

### 13.4 验收

```bash
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/middleware-auth.sh
REG_CACERT="$PWD/docker/certs/ca/rootCA.pem" scripts/regress/regress.sh
```

检查覆盖：

- MySQL：业务账号可查询、执行 DDL 被拒绝；exporter 不能写业务库；错误口令失败。
- Redis：未认证、`default` 用户与错误口令被拒绝；`iboot_app` 可 `PING`；`iboot_exporter` 可读 `INFO`、不能写。
- Silo：`minioadmin` 被拒绝；受管凭据可列举 bucket。
- Nacos：匿名管理请求被拒绝；服务账号可登录；`sentinel-init` 缺少凭据时失败。

### 13.5 边界

- MySQL root 仍允许从编排网络连接，仅用于初始化与迁移；后续可改为专用迁移账号并关闭远程 root。
- Redis 使用 `iboot_app` 共享业务账号与受限 `iboot_exporter`，仍不是每服务 ACL 用户。
- Silo 使用受管根凭据，尚未创建按桶授权的应用用户。
- Nacos 控制台鉴权在本地保持关闭以支持门户内嵌；非本地必须开启，且门户不代填凭据。
- Neo4j 使用环境注入的单一账号；社区版不支持按服务划分用户，不能据此声称工作负载身份。
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
- 所有服务当前共用一张叶子证书，因此一个探针即可覆盖；B5 拆分证书后必须增加目标。

验证：

```bash
scripts/regress/tls-cert-monitor.sh
curl -s 'http://127.0.0.1:9090/api/v1/query?query=probe_ssl_earliest_cert_expiry{job="tls-cert"}'
```

### 14.4 明文默认地址审计

`scripts/regress/plaintext-audit.sh` 扫描应用配置与 Grafana 数据源；命中 `http://`
且未登记在 `scripts/regress/plaintext-allowlist.txt` 时失败。当前例外：

- Grafana→Prometheus / Thanos（本地后端只提供 HTTP）。
- CLI 回环 OAuth 回调（RFC 8252，不承载业务数据）。

允许清单只允许缩小；新增条目必须在评审中说明用途、不可加密原因与后续处理去向。

### 14.5 边界

- Prometheus、Thanos 的明文链路仍未加密，属于已登记边界；Tempo 已在 B4.5 完成 TLS。
- Grafana 匿名只读依赖 Viewer 角色；若 Explore 权限不可用，按 `design.md` 的退路处理，
  不得回退匿名 Admin。
- 本批次不包含服务间 mTLS、RocketMQ 强制 TLS、Seata 认证与 Kafka SASL。

## 15. Tempo TLS（B4.5）

Tempo 的 OTLP/HTTP（4318）、OTLP/gRPC（4317）接收端口与查询 HTTP（3200）只接受 TLS，证书复用本地叶子证书。

- `docker/tempo/tempo.yaml` 用 `server.http_tls_config` 为 3200 提供 HTTPS，并在两个 OTLP receiver 上配置 `tls`。
- 9095 是 Tempo 进程内 frontend↔querier 的 gRPC 端口，不发布到宿主机；开启 `grpc_tls_config` 会打断内部调用，因此保留明文并登记为边界。
- 应用 `TEMPO_OTLP` 与 Grafana `Tempo` 数据源均为 `https://`；Grafana 通过 `tlsAuthWithCACert` 使用 `/certs/rootCA.pem` 校验证书。
- 明文允许清单移除 Tempo 两条，仅保留 CLI 回环回调与 Grafana→Prometheus/Thanos。

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
| `iboot_app` | 九个业务服务 | 全部命令 |
| `iboot_exporter` | redis-exporter | 只读与监控命令，不能写 |

- `default` 用户关闭；服务通过 `spring.data.redis.username=iboot_app` 认证。
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
