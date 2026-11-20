# Design

## Context

See `proposal.md` for motivation, the archived `add-edge-tls` design for the B1 boundary, and `docs/spike/tls-zero-trust-spike-report.md` for the B0 measurements. Current state that shapes B2:

- 所有业务服务、网关与运维控制台仍以 HTTP 监听；`server.ssl` 未配置。
- 网关 `application-local.yml` 的 `IBOOT_*_URI` 默认是 `http://localhost:808x`；`application-nacos.yml` 使用 `lb://iboot-*`。
- 服务间客户端有两类：Spring 自动配置的 `RestClient`，以及 `JwkSetProvider` / `ServiceClientConfig` / `AgentToolConfig` 中手工构造的 `RestClient` 和 `SimpleClientHttpRequestFactory`。
- MySQL URL 显式 `useSSL=false`；Redis 只有明文端口；Silo endpoint 为 `http://silo:9000`；Neo4j 默认 `bolt://`。
- B1 的 `tls-init.sh` 已生成覆盖全部服务名的叶子证书与 PKCS12 信任库；B0 已验证 MySQL、Redis、Silo 的 TLS 与 Java/curl/Node 信任链。
- Nacos、ES、Kafka、RocketMQ、Seata 不在本变更范围；Nacos 注册流量在 B2 结束后仍为明文，这是显式边界。

## Goals / Non-Goals

**Goals:**

- 网关、九个业务服务与运维控制台的端口只接受 TLS；门户到网关、服务间、JWKS 全部 HTTPS。
- MySQL、Redis、Silo、Neo4j 的连接只走 TLS，明文被拒绝。
- 用一份共享信任链覆盖 Spring 服务、JDBC、Redis、对象存储与 Neo4j 客户端。
- 回归与 CI 能证明“去掉 CA / 使用明文必须失败”。

**Non-Goals:**

- Nacos 注册/配置中心 TLS；ES/Kafka（B3）；RocketMQ/Seata（B3/B5）。
- mTLS、SPIFFE/工作负载身份、端口收敛、网络分段、每服务 DB 账号（B4/B5）。
- 生产公网 CA；不改变任何业务接口路径或响应结构。
- 不为数据面引入新的常驻组件。

## Decisions

### D1. 网关端口保持 8088，直接改为 HTTPS

网关继续监听 `8088`，只把 scheme 从 `http` 改为 `https`；门户 nginx 与 Vite 的上游从 `http://iboot-gateway:8088` / `http://localhost:8088` 改为 `https://...`。

理由：端口不变可以把改动收敛到 scheme 与证书配置，避免同时迁移端口、脚本、健康检查与文档。B1 已经把门户设为唯一公开入口，`8088` 不再是浏览器入口；B4 做端口收敛时再决定是否停止发布。备选方案是新增 `8443` 并保留 `8088` 过渡，但那会留下一个明文监听，且需要两套上游配置，收益不值得。

### D2. 共享 Spring SSL bundle，客户端用 JVM 信任库兜底

在 `iboot-kernel` 的 classpath 放一份共享 `iboot-tls.yml`，由各服务通过 `spring.config.import` 引入：

```yaml
spring:
  ssl:
    bundle:
      pem:
        iboot:
          keystore:
            certificate: file:${IBOOT_TLS_CERT_DIR}/portal.pem
            private-key: file:${IBOOT_TLS_CERT_DIR}/portal-key.pem
          truststore:
            certificate: file:${IBOOT_TLS_CERT_DIR}/ca/rootCA.pem
server:
  ssl:
    bundle: iboot
```

- 服务端统一使用 `server.ssl.bundle=iboot`；证书沿用 B1 的 `portal.pem`（SAN 已覆盖服务名、`localhost` 与 `127.0.0.1`）。
- Spring 的 `RestClient`/`WebClient` 使用 SSL bundle；`JwkSetProvider`、`ServiceClientConfig`、`AgentToolConfig` 改为注入自动配置的 `RestClient.Builder` 或 `ClientHttpRequestFactoryBuilder`，不再手工构造不感知 SSL bundle 的工厂。
- Druid/Connector-J、MinIO SDK、Neo4j 驱动不一定读取 Spring SSL bundle，因此所有服务额外注入 JVM 信任库：`-Djavax.net.ssl.trustStore=${IBOOT_TLS_CERT_DIR}/truststore.p12`。B0 已验证 JDK 默认信任库路径对 Java HttpClient 与 Druid 生效。
- 共享配置用 classpath 引入，避免在 10 个 `application.yml` 中重复证书路径；若 Spring Config 的 classpath import 在某个服务中不生效，退化为逐服务重复配置，由 v0 验证。

**Alternatives considered:** 每个服务手写 `server.ssl.*`（重复、易漂移）；只用 JVM 全局 truststore 而不配 SSL bundle（服务端证书仍要配置，且客户端无法按 bundle 统一）。

### D3. 网关到服务的 HTTPS scheme 由 v0 验证后固化

`application-local.yml` 的默认 URI 改为 `https://localhost:808x`。`application-nacos.yml` 的 `lb://iboot-*` 需要确认 Spring Cloud Gateway 如何选择 HTTPS scheme：

- 首选 `lb:https://iboot-*` 或等价的 secure URI 形式；
- 若当前版本不支持，则用显式 `https://` 路由或一个只负责 scheme 的小过滤器，保留 Nacos 动态实例；
- 退路是 local profile 显式 HTTPS、Nacos profile 暂时保留 `lb://` + HTTP——但这会让“网关到服务”仍明文，不能作为 B2 验收状态。

该验证放在 v0，结果写入设计；不接受“配置了 truststore 但实际仍走 HTTP”的假通过。

### D4. 数据面按组件配置，服务端与客户端同时收紧

- **MySQL**：服务端挂载 `rootCA.pem` / `portal.pem` / `portal-key.pem`，启动参数 `--ssl-ca`、`--ssl-cert`、`--ssl-key`、`--require_secure_transport=ON`；所有服务与 `iboot-db-init` 的 JDBC URL 改为 `sslMode=VERIFY_IDENTITY&requireSSL=true`，去掉 `useSSL=false` 与不必要的 `allowPublicKeyRetrieval`；Druid 使用 JVM 信任库。健康检查改用带 CA 的 `mysqladmin` 或 TLS 连接。
- **Redis**：`--port 0 --tls-port 6379 --tls-cert-file ... --tls-key-file ... --tls-ca-cert-file ... --tls-auth-clients no`；Spring `spring.data.redis.ssl.enabled=true` 并使用 SSL bundle；`redis-cli --tls --cacert` 做健康检查；运维脚本与 `redis-cli` 文档同步。
- **Silo**：`--certs-dir /certs`，证书以 `public.crt` / `private.key` 挂载；内部与公开 endpoint 改为 `https://`；`SiloFileStorageStrategy` 的 MinIO 客户端通过 JVM 信任库校验；预签名 URL 使用 HTTPS 公开地址；`silo-init` 与健康检查使用 CA。
- **Neo4j**：`graph` profile 下挂载证书、启用 Bolt TLS，URI 改为 `bolt+s://neo4j:7687`；驱动使用 JVM 信任库；明文 `bolt://` 健康检查或客户端连接必须失败。该组件是可选 profile，v0 先验证再决定是否纳入本批验收。
- **运维控制台**：`iboot-ops` 自身启用 TLS；SBA 从 Nacos 发现的服务实例必须使用 HTTPS。若 SBA 的实例 scheme 无法从注册元数据推导，先用 `secure=true` 元数据或显式实例 URL 修复；无法修复时在文档中登记为已知限制，不能让门户 `/sba` 静默空白。

### D5. 证书复用与容器权限

- 复用 B1 的叶子证书与 PKCS12 信任库，不新增 CA；`tls-init.sh` 的 SAN 已包含全部服务名。
- 宿主机开发的服务以开发者身份运行，可直接读取 0600 私钥；Docker 运行期镜像以非 root 用户运行，不能直接读取宿主 0600 私钥。compose 用只读证书卷挂载，并在启动脚本中把证书复制到应用用户可读的路径（或使用一个一次性证书初始化容器），**不通过放宽宿主私钥权限解决**。
- 证书缺失、信任库不匹配或私钥不可读时，服务启动必须失败；不以明文继续。

### D6. 健康检查与内部脚本同步

- `dev-init.sh` 的服务健康等待改为 HTTPS + `IBOOT_TLS_CERT_DIR`；`wait_http` 已具备 CA 参数。
- `scripts/regress/api-snapshot.sh` 增加 `SVC_CACERT` 与 `--cacert`；`regress.sh` 的 `SVC_*` 改为 HTTPS。
- `scripts/regress/log-pipeline.sh`、`scripts/apm/*`、`scripts/regress/equivalence.sh` 中指向网关的地址改为 HTTPS 并带 CA；`equivalence.sh` 仍是迁移期脚本，至少不能以明文默认值误导。
- MySQL/Redis/Silo/Neo4j 的容器健康检查改用 CA；应用容器的 TCP 健康检查不变。
- 新增 `scripts/regress/internal-tls.sh`：逐端口 TLS、明文失败、CA 失败、数据面 cipher/明文拒绝、JWKS HTTPS、网关→服务与 SSE。

### D7. 回归与 CI

- `SVC_*` 直连地址改为 HTTPS，`SVC_CACERT` 默认 `docker/certs/ca/rootCA.pem`；快照、路径守恒、路由覆盖与冒烟继续运行。
- 门户回归 `REG_*` 不变，仍经公开入口；内部 TLS 通过网关与直连服务两条路径验证。
- CI 全栈任务在 B1 证书基础上启用内部 TLS，执行 `internal-tls.sh` 与全量回归；PR 静态任务不依赖证书。
- 单元测试聚焦 `JwkSetProvider` 的 HTTPS 拉取、Spring SSL bundle 装配与网关 HTTPS 上游；数据面以全栈负向用例为主。

### D8. 回退边界

- 宿主机与 Docker 都不提供运行期内部明文回退；`IBOOT_INTERNAL_TLS_ENABLED=false` 会让
  `dev-init.sh` 在启动前失败并提示回退到上一版 commit/镜像。内网明文模式会同时破坏
  服务间、JWKS 与数据面 TLS，保留半开状态反而制造难以归因的故障。
- 回滚使用上一版 commit/镜像与 compose，或 `git revert` 本变更。
- B1 的边缘回退 `IBOOT_TLS_ENABLED=false` 仍只影响公开入口，不改变内部 TLS。
- Nacos 注册流量仍为明文；若 B2 后需要继续收紧，应单独立项 `add-nacos-tls`，不在本变更内扩展。

## Risks / Trade-offs

- [网关 `lb://` 能否选择 HTTPS 未验证] → v0 先验证；不支持则用显式 HTTPS 路由或小过滤器，不以 HTTP 假通过。
- [SBA 从 Nacos 推导 HTTPS 实例可能失败] → v0 验证；用 `secure=true` 元数据或显式实例 URL 修复；无法修复则登记限制并保证门户 `/sba` 不空白。
- [MinIO SDK / Neo4j 驱动 / Druid 对信任库的读取方式不同] → 统一 JVM truststore；v0 对每个客户端各跑一次最小连接。
- [容器内非 root 用户读不到 0600 私钥] → 证书初始化/复制到应用用户路径，不放宽宿主私钥权限。
- [MySQL `allowPublicKeyRetrieval` 与 TLS 的组合] → v0 验证 Druid 初始化；优先移除该参数。
- [Nacos 仍是明文] → 在 ROADMAP 与 docs/27 显式登记，不把 B2 宣称为完整零信任。
- [端口仍发布到宿主机] → B4 端口收敛；B2 只保证服务只接受 TLS，不改变可达性。
- [ES/Kafka/Seata/RocketMQ 仍明文] → B3/B5；B2 的验收不覆盖这些链路。

## Migration Plan

1. **v0**：在单服务 + 网关 + MySQL + Redis + Silo 上验证 SSL bundle、JVM truststore、网关 HTTPS 上游、SBA 实例 scheme、Druid/MinIO/Neo4j 客户端；记录可用配置。
2. **服务端 TLS**：先启用网关与全部服务 `server.ssl`，同步 `dev-init.sh` 健康等待、门户/Vite 上游、服务间客户端与 JWKS；执行内部 TLS 负向矩阵。
3. **数据面 TLS**：按 MySQL → Redis → Silo → Neo4j 顺序启用，每步跑备份、文件、会话、图同步与全量回归。
4. **Docker**：挂载证书、更新健康检查，跑 Docker 全量回归与 `internal-tls.sh`。
5. **回滚**：宿主机设置 `IBOOT_INTERNAL_TLS_ENABLED=false` 并恢复 HTTP 上游；Docker 回退上一版镜像/compose。两者都只作为排障/回滚，不作验收状态。

## Open Questions

无。网关 `lb://` HTTPS、SBA scheme、客户端信任库读取方式是 v0 的实现验证项，不改变本设计的范围或验收判据；若其中一项无法满足，按 Risks 中的回退方案处理并在文档登记。
