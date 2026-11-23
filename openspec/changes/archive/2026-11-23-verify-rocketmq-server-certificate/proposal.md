# RocketMQ 客户端校验服务端证书

## Why

RocketMQ 应用入口已经只接受 TLS 并启用 ACL，但官方 Java 客户端 5.2.2 在 `RpcClientImpl` 中硬编码
`InsecureTrustManagerFactory.INSTANCE`：客户端不校验 Proxy 证书链，也不抵御中间人。文档因此只能把
该链路描述为“加密通道”，不能纳入零信任。上游最新发行版仍是 5.2.2，短期内没有可等待的修复版本。

## What Changes

- 在 workflow 与 notify 的 `src/main/java` 中加入与客户端 shaded 类同名的兼容层，
  把 `InsecureTrustManagerFactory.INSTANCE` 替换为使用 JVM 本地信任库（或显式配置的信任库）的安全
  `TrustManagerFactory`。Spring Boot fat jar 的 `BOOT-INF/classes` 优先于 `BOOT-INF/lib`，因此该实现会覆盖客户端内置类。
- 信任库来源：默认使用 JVM 的 `javax.net.ssl.trustStore`；也支持
  `-Diboot.rocketmq.trustStore=/path/truststore.p12` 或 `IBOOT_ROCKETMQ_TRUSTSTORE`。
  初始化失败 MUST 让客户端失败关闭，MUST NOT 回退到信任所有证书。
- `tls-init.sh` 的门户证书 SAN 增加 `rocketmq-tls`，满足 gRPC 客户端已启用的 HTTPS 主机名校验。
- 新增单元测试与回归：
  - 正确本地 CA 签发的 Proxy 证书通过；
  - 不受信任的自签名证书被拒绝；
  - 使用不含本地 CA 的 JDK `cacerts` 时投递失败；
  - fat jar 的 `BOOT-INF/classes` 中确实包含覆盖类。
- 文档明确这是上游兼容层，客户端升级修复后必须删除。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `transport-security`：
  - 修改“RocketMQ 应用入口只接受 TLS”，删除“客户端不校验服务端证书”的边界表述；
  - 新增“RocketMQ 客户端校验服务端证书”要求。

## Impact

- 修改：`iboot-workflow/src/main/java`、`iboot-notify/src/main/java`、`scripts/tls-init.sh`、
  `scripts/regress/regress.sh`、文档与路线图。
- 新增：两个模块的兼容层与测试、`scripts/regress/rocketmq-server-auth.sh`。
- 不改变 RocketMQ 服务端、ACL、代理配置或客户端依赖版本。
- 风险：覆盖 shaded 类依赖客户端内部实现；仅覆盖 `INSTANCE` 常量，且客户端升级后立即删除。
