# RocketMQ 客户端校验服务端证书 — 设计

## 1. 上游限制

`rocketmq-client-java:5.2.2` 的 `RpcClientImpl` 在 SSL 分支执行：

```java
SslContextBuilder builder = GrpcSslContexts.forClient();
builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
```

最新发行版仍是 5.2.2，不能通过配置项启用校验。`ClientConfiguration` 也没有信任库字段。

## 2. 兼容层

在 `iboot-workflow` 与 `iboot-notify` 的源码树中加入同 FQCN 类：

```text
org.apache.rocketmq.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
```

该类只暴露客户端引用的字段：

```java
public static final TrustManagerFactory INSTANCE = create();
```

`create()`：

1. 读取 `iboot.rocketmq.trustStore` 系统属性或 `IBOOT_ROCKETMQ_TRUSTSTORE` 环境变量；
2. 有路径时按 `PKCS12` 加载，口令取 `iboot.rocketmq.trustStorePassword` /
   `IBOOT_ROCKETMQ_TRUSTSTORE_PASSWORD`，默认 `changeit`；
3. 没有路径时调用 `factory.init((KeyStore) null)`，使用 JVM 默认信任库；
4. 任一步失败抛 `ExceptionInInitializerError`，客户端失败关闭。

Spring Boot fat jar 的 `BOOT-INF/classes` 在类路径中先于 `BOOT-INF/lib`，因此该实现覆盖客户端 jar 中的
同名类；客户端字节码只引用 `INSTANCE`，不需要复制 `RpcClientImpl`。

## 3. 主机名校验

客户端 shaded gRPC 的 `ProtocolNegotiators$ClientTlsHandler` 已对 `SSLEngine` 设置
`endpointIdentificationAlgorithm=HTTPS`。兼容层只替换信任管理器，不触碰该逻辑。

`portal.pem` 当前 SAN 包含 `localhost`、`127.0.0.1`、`::1`、`host.docker.internal` 与
`rocketmq-proxy`，不包含应用实际连接的 `rocketmq-tls`。`tls-init.sh` 的 `SAN_NAMES` 增加
`rocketmq-tls`，`--check` 与 `--force` 会重签现有证书。

## 4. 测试

单元测试 `RocketMqServerTrustTest`（两个模块各一份，逻辑相同）：

- 用本地 CA 签发的 `services/iboot-workflow.pem` 调用 `checkServerTrusted`，应通过；
- 用内嵌的自签名测试证书调用，应抛 `CertificateException`；
- 断言不是“信任所有”实现。

测试通过 surefire 的 `argLine` 指定 `javax.net.ssl.trustStore=docker/certs/truststore.p12`。
错误 CA 场景由回归用 JDK `cacerts` 再跑一次，并期望集成投递失败。

集成投递由 `RocketMqServerTrustTest` 的可选部分完成：设置
`IBOOT_ROCKETMQ_INTEGRATION=true`、endpoint 与 workflow 凭据后发送一条测试消息；
未设置时跳过，不阻塞普通单元测试。

## 5. 回归

`scripts/regress/rocketmq-server-auth.sh`：

1. `openssl x509 -checkhost rocketmq-tls` 检查门户证书 SAN；
2. `unzip -l` 检查两个 fat jar 的 `BOOT-INF/classes` 包含兼容层；
3. 用本地信任库运行单元测试；
4. 用 JDK `cacerts` 运行同一测试，期望失败；
5. 运行 `rocketmq-acl.sh` 证明 workflow 发布与 notify 消费仍成功。

## 6. 升级与回滚

- 升级 `rocketmq-client-java` 后，先检查 `RpcClientImpl` 是否仍硬编码不安全信任管理器；
  已修复则删除两个兼容层与相关测试，并保留“错误 CA 被拒绝”的回归。
- 回滚：删除兼容层源码即可恢复旧行为；SAN 新增不影响其它客户端。
