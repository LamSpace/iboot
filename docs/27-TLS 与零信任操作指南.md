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

- Nacos 注册/配置中心仍为 HTTP；B2 不宣称完整零信任。
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

- 三个监听全部 SSL：`SSL://kafka:9092`、`SSL_HOST://localhost:29092`、
  `CONTROLLER://:9093`；明文客户端被拒绝。
- `tls-init.sh` 额外生成 `runtime/kafka-keystore.p12` 与密码文件。
- 镜像的 configure 脚本要求 `KAFKA_SSL_KEYSTORE_FILENAME`、`KAFKA_SSL_KEY_CREDENTIALS`、
  `KAFKA_SSL_KEYSTORE_CREDENTIALS`，文件放在 `/etc/kafka/secrets`。
- client auth 为 `none` 时镜像不会自动注入 truststore；controller 出站 SSL 仍需显式传
  `KAFKA_SSL_TRUSTSTORE_LOCATION`，否则启动时报 PKIX 失败。
- `KafkaAppender` 默认 `security.protocol=SSL`，宿主机信任库为
  `docker/certs/truststore.p12`，容器内为 `/certs/truststore.p12`。

验证：

```bash
scripts/regress/log-search-tls.sh
scripts/regress/log-pipeline.sh --drills
```

### 9.3 Logstash 与日志管道

- Kafka 输入使用 `security_protocol=SSL` 与 PKCS12 truststore；
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

## 10. 与后续批次的关系

- B2：门户到网关、服务间、JWKS、MySQL/Redis/Silo/Neo4j 的 TLS；已完成，Nacos 仍是明文。
- B3：ES/Kafka/Logstash 日志与检索面 TLS；已完成；RocketMQ 只作为加密通道，
  不能宣称服务端认证或强制 TLS。
- B4/B5：mTLS、工作负载身份、端口收敛与网络分段；Seata 无原生 TLS，需要单独决策。
