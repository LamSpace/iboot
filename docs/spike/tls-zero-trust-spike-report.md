# TLS 与零信任 Spike 报告（B0）

> **日期**：2026-11-20
> **定位**：TLS 与零信任落地的 B0 前置验证。只回答“能不能做、坑在哪”，不产出生产代码。
> **对应变更**：无。B0 不是 OpenSpec 变更；第一个可交付变更在 B1 启动时创建 `add-edge-tls`。
> **环境**：Ubuntu 26.04 / JDK 25.0.3 / Node 22.23.1 / OpenSSL 3.5.5 / Docker 29.9.3 / mkcert 1.4.4。

## 1. 结论

**B0 门禁通过（有条件）**：L1 传输加密在本地可行，但数据面必须按组件分级，不能按“全部配置一下”估算。

| 组件 / 能力 | 结论 | 关键约束 |
|---|---|---|
| mkcert 信任链（curl / Node / Java / 容器） | **Go** | JDK 用 PKCS12 truststore；Node 用 `NODE_EXTRA_CA_CERTS`；浏览器信任需一次 sudo |
| Vite HTTPS + HTTPS 上游 + HMR WSS | **Go** | 上游证书必须被 Node 信任；HMR 走 `wss://` |
| MySQL 8.4 + Druid | **Go** | `sslMode=VERIFY_IDENTITY` + `require_secure_transport=ON` |
| Redis 8 + Lettuce | **Go** | `rediss://` + JDK truststore；明文被拒 |
| Silo | **Go** | `--certs-dir` + `public.crt` / `private.key`；预签名 URL 需浏览器信任 CA |
| Nacos 3.3 HTTP + gRPC TLS | **Go（有条件）** | 证书路径必须加 `file:`；gRPC TLS 必须用 `JAVA_TOOL_OPTIONS`，`JAVA_OPT_EXT` 无效 |
| RocketMQ 5.5 Proxy TLS | **Go（仅加密）** | 客户端不校验证书；同一端口仍接受明文，TLS 不强制 |
| Seata 2.6 | **No-Go** | 服务端与客户端均无原生传输层 TLS；只能前置 TCP TLS 终止器或暂缓 |

## 2. 信任链

### 2.1 本地 CA 与证书

用 mkcert 生成一套本地 CA 与叶子证书，叶子证书 SAN 覆盖：

```text
localhost, 127.0.0.1, ::1, host.docker.internal,
iboot.local, *.iboot.local,
iboot-system, iboot-gateway, iboot-auth,
mysql, redis, elasticsearch, nacos, seata-server, rocketmq-proxy
```

mkcert 通过 `go install filippo.io/mkcert@v1.4.4` 构建；apt 也有 `mkcert 1.4.4-1ubuntu2`，但本机 `sudo apt-get` 需要终端认证，未能直接安装。

`mkcert -install` 在写入系统信任库时失败：

```text
ERROR: failed to execute "tee": exit status 1
sudo: A terminal is required to authenticate
```

CA 本身已生成，非浏览器信任链全部验证通过；**浏览器信任安装是 B1 的一次性特权步骤**，需要用户执行 `! sudo mkcert -install`（或先安装 apt 包）。

### 2.2 验证结果

| 客户端 | 带 CA | 不带 CA |
|---|---|---|
| curl | `200 ok` | `unable to get local issuer certificate (20)` |
| Node | `200 ok` | `UNABLE_TO_VERIFY_LEAF_SIGNATURE` |
| Java 25 (`HttpClient`) | `200 ok` | `PKIX path building failed` |
| 容器内 curl | `200 ok` | `unable to get local issuer certificate (20)` |

Java 信任库用：

```bash
keytool -importcert -noprompt -alias iboot-local-ca \
  -file rootCA.pem -keystore truststore.p12 \
  -storetype PKCS12 -storepass changeit
java -Djavax.net.ssl.trustStore=/path/truststore.p12 \
     -Djavax.net.ssl.trustStorePassword=changeit ...
```

### 2.3 容器相关约束

- Docker 文件共享不允许挂载 `/tmp`；`/home/lam/.cache` 可挂载。证书目录应放在仓库内（`.gitignore`）或用户目录，不要放 `/tmp`。
- 运行期镜像 `eclipse-temurin:25.0.5_8-jre` **没有 curl / wget**。应用容器的健康检查目前都是 TCP 探测，不受影响；中间件健康检查使用 curl，开启 TLS 后必须挂载 CA 并加 `--cacert`，不能用 `-k` 绕过。

## 3. Vite

最小 Vite 5.4.21 工程验证：

```ts
server: {
  https: { cert: fs.readFileSync('server.pem'), key: fs.readFileSync('server-key.pem') },
  proxy: {
    '/iboot/api': { target: 'https://localhost:8443', changeOrigin: true }
  }
}
```

以 `NODE_EXTRA_CA_CERTS=rootCA.pem` 启动后：

| 用例 | 结果 |
|---|---|
| `curl --cacert` 访问页面 | 200 |
| 经 Vite 代理访问 HTTPS 上游 | `ok` |
| HMR WebSocket `wss://`（`vite-hmr`） | `WSS OPEN` |
| 不带 CA 访问 | 证书校验失败 |

结论：Vite 开发服务器与 HTTPS 上游、HMR WSS 均可用；B1 只需给 `vite.config.ts` 增加 `server.https`，并把代理目标切到 HTTPS。

## 4. MySQL 8.4 + Druid

服务端参数：

```bash
--ssl-ca=/tls/rootCA.pem --ssl-cert=/tls/server.pem --ssl-key=/tls/server-key.pem \
--require_secure_transport=ON
```

验证结果：

| 用例 | 结果 |
|---|---|
| 服务端 `SHOW STATUS LIKE 'Ssl_cipher'` | `TLS_AES_128_GCM_SHA256` |
| 客户端 `--ssl-mode=VERIFY_IDENTITY --ssl-ca` | 成功 |
| 客户端 `--ssl-mode=DISABLED` | `ERROR 3159: Connections using insecure transport are prohibited` |
| Druid + Connector/J `sslMode=VERIFY_IDENTITY&requireSSL=true` | 连接成功，`cipher=TLS_AES_256_GCM_SHA384` |
| Druid + `sslMode=DISABLED` | 初始化失败，原因同上 |

结论：数据库层可直接落地；B2 需要把 Druid URL、JVM truststore 与证书挂载串起来。

## 5. Redis 8 + Lettuce

服务端参数：

```bash
--port 0 --tls-port 16379 \
--tls-cert-file /tls/server.pem --tls-key-file /tls/server-key.pem \
--tls-ca-cert-file /tls/rootCA.pem --tls-auth-clients no
```

| 用例 | 结果 |
|---|---|
| `redis-cli --tls --cacert` | `PONG` |
| 明文 `redis-cli` | `Server closed the connection` |
| Lettuce `rediss://` + JDK truststore | `PING=PONG` |
| Lettuce `redis://` | 连接失败 |

结论：Redis 可直接落地；B4 需要客户端证书时把 `--tls-auth-clients` 改为 `yes`。

## 6. Silo

Silo 保留 MinIO 的证书目录契约：`public.crt` + `private.key`，启动加 `--certs-dir /certs`。

| 用例 | 结果 |
|---|---|
| `https://.../minio/health/live` + CA | 200 |
| 明文 HTTP | 400（Silo 拒绝 HTTP 请求） |

预签名 URL 会变成 `https://localhost:9000/...`，浏览器必须信任本地 CA；B2 需同步修改 `IBOOT_FILE_SILO_PUBLIC_ENDPOINT` 与健康检查（`mcli ready local` 的别名地址）。

## 7. Nacos 3.3

### 7.1 HTTP / 控制台

通过环境变量启用 Spring Boot 的 `server.ssl`：

```text
SERVER_SSL_ENABLED=true
SERVER_SSL_KEY_STORE=file:/certs/keystore.p12
SERVER_SSL_KEY_STORE_PASSWORD=changeit
SERVER_SSL_KEY_STORE_TYPE=PKCS12
```

| 用例 | 结果 |
|---|---|
| API `https://...:8848/nacos/v1/ns/operator/metrics` + CA | 200 |
| 同一地址走 HTTP | 400 |
| 控制台 `https://...:8080/v3/console/health/readiness` + CA | 200 |

### 7.2 gRPC（9848）

服务端配置：

```text
nacos.remote.server.rpc.tls.enableTls=true
nacos.remote.server.rpc.tls.certChainFile=file:/certs/server.pem
nacos.remote.server.rpc.tls.certPrivateKey=file:/certs/server-key.pem
nacos.remote.server.rpc.tls.mutualAuthEnable=false
nacos.remote.server.rpc.tls.trustCollectionCertFile=file:/certs/rootCA.pem
```

客户端配置（Nacos client 3.1.1，连 3.3.0 服务端）：

```text
nacos.remote.client.rpc.tls.enable=true
nacos.remote.client.rpc.tls.trustCollectionChainPath=file:/path/rootCA.pem
nacos.remote.client.rpc.tls.trustAll=false
```

验证结果：

- `openssl s_client` 连 9848：`TLSv1.3`，`Verify return code: 0`；
- Nacos client 启动后 `status=UP`，`registerInstance` 请求成功；`getAllInstances` 的即时返回受客户端缓存影响，复现时可能先看到空列表，稍后重查即可；
- `trustAll=false` 在 SAN 为 `localhost` 时可用；若通过容器 IP 或未列入 SAN 的主机名连接，会失败。

### 7.3 三个必须写进 B2 的坑

1. **证书路径必须带 `file:`**。不加前缀会被当作 classpath 资源，报 `class path resource [certs/server.pem] cannot be opened`。服务端与客户端的 trust 路径都一样。
2. **`JAVA_OPT_EXT` 不能传 JVM 属性**。Nacos 镜像把 `JAVA_OPT_EXT` 追加在 `-jar nacos-server.jar` 之后，`-D...` 变成应用参数，TLS 不会生效。必须用 `JAVA_TOOL_OPTIONS`，或写进 `application.properties`。
3. **gRPC 端口偏移**。客户端按 `serverAddr` 端口 + 1000 连接（8848 → 9848）；做端口映射时必须保持该偏移，否则客户端连不上。

Spring Cloud Alibaba 的端到端接入尚未在 B0 验证：客户端的 TLS 属性是 Nacos 客户端属性，不是 Spring 属性，需要通过 JVM/system properties 注入；B2 需要单独跑一次服务注册/发现。

## 8. RocketMQ 5.5

Proxy 配置 `rmq-proxy.json`：

```json
{
  "rocketMQClusterName": "DefaultCluster",
  "useEndpointPortFromRequest": true,
  "tlsTestModeEnable": false,
  "tlsKeyPath": "/certs/server-key.pem",
  "tlsCertPath": "/certs/server.pem"
}
```

验证结果：

| 用例 | 结果 |
|---|---|
| `openssl s_client` 连 gRPC 8081 | `TLSv1.3`，`Verify return code: 0` |
| Java client 5.2.2 `enableSsl(true)` 发送消息 | 成功 |
| 同一客户端 `enableSsl(false)` 发送消息 | **也成功** |

两个关键限制：

1. **TLS 不强制**。Proxy 使用 `ProxyAndTlsProtocolNegotiator` 在同一端口做协议探测，明文与 TLS 都接受。开启 TLS 只是“可用”，不是“只允许 TLS”。
2. **客户端不认证服务端**。`RocketMQ client 5.2.2` 的 `RpcClientImpl` 在 SSL 分支使用 `GrpcSslContexts.forClient()` 后直接设置 `InsecureTrustManagerFactory.INSTANCE`，不校验 CA 与主机名。它只能防止被动窃听，不能防止中间人。

结论：RocketMQ 链路不能纳入“零信任身份”叙事，只能标注为**加密通道**。若要强制 TLS，需要前置只接受 TLS 的 TCP 代理（nginx stream / envoy）或依赖网络分段；客户端认证在本版本不可得。

## 9. Seata 2.6

检查了服务端 `seata-core-2.6.0.jar`、`seata-common-2.6.0.jar`、`seata-config-core-2.6.0.jar` 与客户端 2.5.0 系列 jar：

- 没有 TLS / SSL 相关类；
- 没有 `tls` / `ssl` 配置键；
- 默认 `application.yml` 也无传输层 TLS 配置。

结论：**Seata 2.6.0 没有原生传输层 TLS**。可选处置：

1. **暂缓**，在文档与验收判据中明确“Seata 链路不参与本轮 TLS”（推荐）；
2. 在 8091 前置 TCP TLS 终止器（stunnel / nginx stream），客户端连终止器；终止器到 Seata 的最后一跳仍是明文，属于“局部加密”，不是端到端。

不要让 Seata 阻塞 B2；它应作为 B5 的独立事项或明确的例外。

## 10. 对后续批次的影响

### B1 边缘 TLS

- 采用单一 443 入口（`https://localhost`），避免 issuer 二次切换；网关 8443 只作内部/调试。
- B1 开工前需要一次特权操作安装本地 CA 到系统/浏览器信任库；否则浏览器仍会告警。
- CLI 需要支持 HTTPS base URL 与 JDK truststore；`bin/iboot` 或文档给出 `-Djavax.net.ssl.trustStore` 的用法。
- 回归脚本的 `REG_BASE` / `REG_AUTH_BASE` / `REG_OIDC_REDIRECT` 与所有 `curl` 都要支持 `IBOOT_SCHEME` 与 `--cacert`。

### B2 服务间与核心数据面

- MySQL / Redis / Silo / JWKS / 网关出站按本报告结论推进。
- Nacos 可行，但必须把三条坑写进设计：`file:` 路径、`JAVA_TOOL_OPTIONS`、端口偏移；并补一次 Spring Cloud Alibaba 端到端验证。
- Seata 从 B2 范围移除。
- 网关 WebFlux 到 HTTPS 上游、Spring Boot SSL bundle 的装配尚未在 B0 验证，放在 B2 的 v0 小验证里。

### B3 日志与检索面

- ES、Kafka、Logstash、Kibana、exporter 本轮未做，仍需单独 spike；不要因为 B0 通过就默认它们“配置一下就好”。
- RocketMQ 可以开启 TLS，但按“仅加密、不认证、不强制”记录；如果验收要求明文必须失败，需要额外的前置 TLS 代理。

### B4 / B5 身份与收敛

- Spring 服务之间的 mTLS 仍可做；Nacos 的 mTLS（`mutualAuthEnable=true`）可在 B4/B5 再验证。
- RocketMQ 与 Seata 无法参与工作负载身份；在零信任边界图中显式标注为例外。

## 11. 未验证与剩余风险

| 项 | 状态 | 处置 |
|---|---|---|
| ES 9 开启 security + HTTPS，Logstash / Kibana / exporter 全链路 | 未验证 | B3 单独 spike |
| Kafka SSL 监听、双监听、`KafkaAppender` | 未验证 | B3 单独 spike |
| Neo4j `bolt+s://` | 未验证 | B2/B3 按需验证（`graph` profile） |
| 网关 WebFlux 出站 HTTPS + 信任库 | 未验证 | B2 最小验证 |
| nginx → 网关 HTTPS 上游 | 未验证 | B1/B2 验证 `proxy_ssl_trusted_certificate` |
| Spring Boot `spring.ssl.bundle.*` 在 8 个服务的装配 | 未验证 | B2 统一验证 |
| Nacos + Spring Cloud Alibaba 端到端 | 未验证 | B2 必须补 |
| 浏览器实际信任本地 CA | 未验证 | B1 开工时执行 `mkcert -install` 后在浏览器验收 |
| 证书轮换 / 过期回归 | 未验证 | B1 加入“剩余有效期 < 30 天失败” |
| macOS / Windows 开发机 | 未验证 | 文档标注以 Linux 为准 |

## 12. 复现与清理

本次验证全部在临时目录与临时容器中完成，结束后已删除：

- 临时 CA、叶子证书、PKCS12 信任库与 mkcert 二进制；
- MySQL / Redis / Silo / Nacos / RocketMQ 临时容器与自定义网络；
- HTTPS stub 与 Vite 临时进程。

复现时的关键命令已在上文各节给出。需要注意：

- `mkcert -install` 与 apt 安装需要终端认证，建议在会话中用 `! sudo ...` 执行；
- 证书私钥不要入仓；临时验证目录不要放在 `/tmp`（Docker 文件共享会拒绝挂载）；
- 容器销毁后 Docker VM 可能仍保留内存，重负载前先确认资源。

## 13. B0 退出结论

- **L1 边缘 TLS 与服务间 TLS：可行**，按 B1 → B2 推进。
- **数据面按组件分级**：MySQL / Redis / Silo 直接做；Nacos 做但带三个配置约束；RocketMQ 仅加密；Seata 暂缓或前置终止器。
- **不能把“开了 TLS”当成“零信任”**：RocketMQ 客户端不认证服务端、Seata 无 TLS，这两条必须在 B4/B5 的边界图中明确为例外。
- B1 的第一件事不是写 nginx，而是把本地 CA 的信任链与 `tls-init.sh` 的幂等生成跑通。
