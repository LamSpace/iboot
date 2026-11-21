# Design

## Context

见 `proposal.md` 与 B0 报告第 7 节。约束：

- Nacos 3.3 的 HTTP TLS 复用 Spring Boot `server.ssl` 环境变量；gRPC TLS 使用 `nacos.remote.server.rpc.tls.*`。
- Nacos 客户端 3.1.1 的 TLS 属性是客户端系统属性，不是 Spring 配置属性；必须在 JVM 启动前注入。
- 客户端按 `serverAddr` 端口 + 1000 连接 gRPC（8848 → 9848），端口映射必须保持偏移。
- 证书私钥必须放在容器可读的 runtime 副本；Nacos 需要 PKCS12 keystore。

## Goals / Non-Goals

**Goals:**

- Nacos API、控制台、gRPC 只接受 TLS；gRPC 要求客户端证书（mTLS），客户端校验证书与主机名。
- 宿主机与 Docker 使用同一套证书与客户端属性。
- 健康检查、Sentinel 初始化、回归与门户代理全部改用 HTTPS。

**Non-Goals:**

- Nacos 用户认证、鉴权与 mTLS（`mutualAuthEnable` 仍为 false）。
- Nacos 集群/多节点与外部数据库 TLS。
- 每服务独立 Nacos 账号。

## Decisions

### 1. Nacos 服务端使用 Spring Boot SSL 与 gRPC TLS

HTTP/控制台：

```text
SERVER_SSL_ENABLED=true
SERVER_SSL_KEY_STORE=file:/certs/nacos-keystore.p12
SERVER_SSL_KEY_STORE_PASSWORD=changeit
SERVER_SSL_KEY_STORE_TYPE=PKCS12
```

gRPC：

```text
NACOS_REMOTE_SERVER_RPC_TLS_ENABLETLS=true
NACOS_REMOTE_SERVER_RPC_TLS_CERTCHAINFILE=file:/certs/portal.pem
NACOS_REMOTE_SERVER_RPC_TLS_CERTPRIVATEKEY=file:/certs/portal-key.pem
NACOS_REMOTE_SERVER_RPC_TLS_MUTUALAUTHENABLE=true
NACOS_REMOTE_SERVER_RPC_TLS_TRUSTCOLLECTIONCERTFILE=file:/certs/rootCA.pem
```

`scripts/tls-init.sh` 生成 `runtime/nacos-keystore.p12`，由 compose 挂载到 `/certs`。

### 2. 客户端通过 JVM 系统属性启用 TLS

所有服务的启动参数加入：

```text
-Dnacos.remote.client.rpc.tls.enable=true
-Dnacos.remote.client.rpc.tls.trustCollectionChainPath=file:<cert-dir>/ca/rootCA.pem
-Dnacos.remote.client.rpc.tls.trustAll=false
-Dnacos.remote.client.rpc.tls.mutualAuth=true
-Dnacos.remote.client.rpc.tls.certChainFile=file:<cert-dir>/portal.pem
-Dnacos.remote.client.rpc.tls.certPrivateKey=file:<cert-dir>/portal-key.pem
```

宿主机由 `dev-init.sh` 注入；Docker 由 `JAVA_TOOL_OPTIONS` 注入。`serverAddr` 仍为 `host:8848`，不改成 URL scheme。

**替代方案**：尝试 `spring.cloud.nacos.*.properties`。当前版本没有该映射，且 Nacos 客户端在工厂初始化时读取系统属性，因此不采用。

### 3. 控制台与运维脚本使用 HTTPS

- 门户 Vite `/nacos-console` 目标改为 `https://localhost:8849`，依赖 `NODE_EXTRA_CA_CERTS`。
- Docker 的 `SYS_NACOS_CONSOLE_URL` 默认改为 `https://localhost:8849`。
- `dev-init.sh` 健康检查、`sentinel-init.sh` 的读写接口、回归脚本全部使用 `--cacert`。
- 健康检查不得使用 `-k`。

## Risks / Trade-offs

- **客户端属性必须在 JVM 启动前生效** → 使用 `JAVA_TOOL_OPTIONS`/显式 `-D`，不在代码里 `System.setProperty`；spike 已确认后者在类初始化后无效。
- **证书路径被当作 classpath** → 服务端与客户端路径统一加 `file:` 前缀。
- **gRPC 端口偏移** → 保留 8848→9848 的映射；9849 为集群内部端口，不映射宿主机。
- **忘记给某个服务加 JVM 参数** → 该服务注册失败；全量回归应检查所有服务在 Nacos 中可见。
- **Nacos 启动未就绪时健康检查失败** → 沿用现有启动顺序，Nacos 健康后才启动后端。

## Migration Plan

1. `scripts/tls-init.sh` 生成 Nacos keystore。
2. 滚动重启 Nacos 与全部服务；先确认 Nacos HTTPS/gRPC 健康，再启动业务服务。
3. 回滚使用上一版 compose/镜像与旧 Nacos 配置；不存在运行时明文回退开关。
