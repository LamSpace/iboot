# Design

## Context

见 `proposal.md`。当前实现约束：

- B0 已验证 mkcert 本地 CA 可用于 curl、Node、Java 与容器，叶子证书的 SAN 已包含 `elasticsearch`、`kafka`、`rocketmq-proxy` 等名字，因此 B3 复用同一份叶子证书，不为每个组件引入独立 CA。
- 各服务通过 `iboot-kernel` 的 `iboot-tls.yml` 共享 Spring SSL bundle；ES 客户端是 Spring Boot 自动配置的 `ElasticsearchClient`，不是普通的 `RestClient`。
- `KafkaAppender` 在 Spring 上下文之前初始化，不能依赖 Spring 的 SSL bundle，只能直接持有 Kafka 客户端配置。
- 集中日志不是启动依赖：Kafka、Logstash 不可用时业务服务必须继续运行。
- RocketMQ 5.5 客户端的 SSL 实现不校验服务端证书，proxy 同端口同时接受明文与 TLS；这是上游能力边界，不是配置遗漏。

## Goals / Non-Goals

**Goals:**

- ES 的 HTTP 与 transport、Kafka 的三个监听、日志管道两端都只使用 TLS，客户端通过本地 CA 校验。
- ES 访问增加凭据认证，凭据只从环境注入。
- 宿主机开发与 Docker 全量部署使用同一套证书与客户端配置语义。
- 把 RocketMQ 的“仅加密、不认证、不强制”边界写成可验证的回归断言与文档，而不是隐含假设。

**Non-Goals:**

- Kibana、Grafana 自身监听端口的 TLS；它们在 B3 只作为 ES 客户端。
- Kafka 客户端证书认证（mTLS）、ES 每服务独立账号与角色隔离。
- RocketMQ 强制 TLS、服务端身份校验与客户端证书认证。
- ES 的跨集群/多节点 transport 发现调优；当前只覆盖单节点本地栈。

## Decisions

### 1. 复用同一份叶子证书，Kafka 额外生成 PKCS12 keystore

`scripts/tls-init.sh` 继续用 mkcert 生成一张包含全部 SAN 的叶子证书。Kafka 需要一个同时含私钥与证书链的 keystore，因此新增：

```text
docker/certs/runtime/kafka-keystore.p12   # 密码 changeit，仅供本地开发
```

由 `portal.pem` + `portal-key.pem` + `rootCA.pem` 通过 `openssl pkcs12 -export` 生成，放入 runtime 副本并设为 0644；宿主私钥仍保持 0600。ES 直接使用 PEM 证书与私钥，不额外生成 keystore。

**替代方案**：为每个组件生成独立证书。更接近生产，但当前所有 Spring 服务与 Silo 已共用一张叶子证书，单独拆分会扩大 B3 范围且不改变验收结论；留待 B4/B5 的工作负载身份。

### 2. Elasticsearch 采用 security + HTTPS，而不是只包一层 TLS 代理

ES 9 的 HTTP SSL 与 security 绑定；仅关闭 security 再开 HTTPS 不是受支持组合。选择：

- `xpack.security.enabled: true`
- `xpack.security.http.ssl.enabled: true`，使用 `/usr/share/elasticsearch/config/certs/portal.pem` 与 `portal-key.pem`（spike 确认 ES 9.5 的 entitlement 策略只允许读取配置目录内的证书）
- `xpack.security.transport.ssl.enabled: true`，使用同一证书与 CA，避免 9300 明文
- 用户名 `elastic`，密码由 `ELASTIC_PASSWORD` 注入

这样同时获得传输加密与最基本的访问控制，避免“只加密、谁都能读”的假安全。

**替代方案**：在 ES 前放 nginx stream TLS 终止器。可保留匿名访问，但多一跳、多一份配置，且仍然没有认证；仅作为 security 无法落地时的退路，不在本设计默认路径。

已有 `es_data` 卷从无 security 启动过时，spike 确认用 security 配置加 `ELASTIC_PASSWORD` 重启即可自动创建 `.security-7` 并正常认证；若旧卷状态异常，再执行 `elasticsearch-reset-password` 或重建本地卷作为兜底。spike 已覆盖新卷与旧卷两条路径。

### 3. Spring ES 客户端通过 `spring.elasticsearch.restclient.ssl.bundle` 接入

Spring Boot 4.0.3 的 ES 自动配置支持 `spring.elasticsearch.restclient.ssl.bundle`。在 `iboot-tls.yml` 增加：

```yaml
spring:
  elasticsearch:
    restclient:
      ssl:
        bundle: iboot
```

三个使用 ES 的服务增加 `spring.elasticsearch.username/password`，默认用户名 `elastic`，密码从 `ELASTICSEARCH_PASSWORD` 读取。这样不需要为每个服务手写 `ElasticsearchClient` Bean，也不会绕过 Boot 的 SSL bundle 装配。

**替代方案**：自建 `Rest5Client` 或自定义 `ElasticsearchClient` Bean。可控但重复，且三个服务都要维护；只有在 Boot 自动配置不支持 bundle 时才采用。

### 4. Kafka 三个监听全部 SSL，客户端显式配置 truststore

broker 保留 9092（容器内）、29092（宿主机）与 9093（controller）端口号，但协议映射全部为 SSL：

```text
SSL://:9092, SSL_HOST://:29092, CONTROLLER://:9093
```

keystore 使用 `kafka-keystore.p12`，truststore 使用 `truststore.p12`，`ssl.client.auth=none`（本批只做加密，不做客户端认证）。

`KafkaAppender` 增加 `securityProtocol`、`truststoreLocation`、`truststorePassword`、`truststoreType` 字段，默认 SSL + PKCS12；宿主机默认信任库为 `docker/certs/truststore.p12`，容器内由环境覆盖为 `/certs/truststore.p12`。创建 producer 时写入 `security.protocol`、`ssl.truststore.*`。不引入 Spring 依赖。

`kafka-init`、broker 健康检查、`log-pipeline.sh` 的 console producer 与宿主机工具统一通过 `--command-config` 或 `--producer-property` 传入同一组 SSL 配置。

**替代方案**：保留 PLAINTEXT 监听给宿主机工具。会直接违背“日志面只接受 TLS”的验收，且让明文回归无法失败，因此不采用。

### 5. Logstash、Kibana、Grafana 与 exporter 作为纯客户端改造

- Logstash：Kafka input 使用 `security_protocol=SSL` + truststore；ES output 使用 HTTPS + CA + 凭据，证书校验保持开启。
- Kibana：`ELASTICSEARCH_HOSTS=https://elasticsearch:9200`，注入用户名/密码与 CA 路径。
- Grafana：ES 数据源改 HTTPS，开启 basic auth 与 `tlsAuthWithCACert`；CA 通过挂载文件供给。若 provisioning 的 `$__file{}` 在当前版本不可用，则内联公开 CA 文本（CA 不是私钥）。
- exporter：使用 HTTPS URI、`--es.ca` 与凭据；若该版本不支持独立密码参数，则使用 URI 内嵌凭据，并在文档标注仅限本地开发。

这些组件的自身监听端口不在本批改 TLS，避免把日志栈 UI 与 B4 的端口收敛混在一起。

### 6. RocketMQ 只开加密，不做强制

proxy 配置增加 `tlsKeyPath` / `tlsCertPath`，挂载 runtime 证书；workflow 通过 `IBOOT_ROCKETMQ_SSL_ENABLED=true` 使用 `enableSsl(true)`。不引入前置 TLS 终止器。

回归分两类输出：

- **通过/失败**：TLS 握手成功、加密客户端可投递通知。
- **边界信息**：同一端口明文仍可连接、客户端不校验服务端证书。后者不计入 TLS 通过，也不伪装成失败修复项。

**替代方案**：用 nginx stream/envoy 强制只接受 TLS。能解决“不强制”，但客户端仍不认证服务端，收益有限且引入新组件；留到 B5 与 mTLS 一起决策。

### 7. 回归拆分：配置检查与端到端探针分离

- `scripts/regress/internal-tls.sh` 继续覆盖服务端口与 MySQL/Redis/Silo/Neo4j。
- 新增 `scripts/regress/log-search-tls.sh`：ES 正负向、Kafka 正负向、Logstash 两端配置、RocketMQ 握手与边界信息。
- `scripts/regress/log-pipeline.sh` 改为 HTTPS + 凭据 + SSL 客户端，保留十个进程覆盖与停 Kafka/停 Logstash 演练。
- `regress.sh` 在“内部 TLS”之后增加“日志与检索 TLS”步骤；CI full-stack 注入 ES 凭据并执行同一脚本。

## Risks / Trade-offs

- **ES 旧卷 security 迁移失败** → spike 同时验证新卷与旧卷；文档给出 `elasticsearch-reset-password` 步骤，失败时明确要求重建本地 `es_data` 而不是静默继续。
- **ES 证书私钥权限** → 容器使用 runtime 0644 副本，宿主私钥保持 0600；spike 验证 ES 非 root 用户可读。
- **Kafka controller SSL 在 KRaft 单节点下启动失败** → spike 先验证；若失败，回到提案修改 spec 并显式登记 controller 明文例外，不在实现中静默保留 PLAINTEXT。
- **Logstash 插件键名随版本差异** → spike 用实际镜像验证 `ssl_enabled` / `ssl_certificate_authorities` / `security_protocol` 等键，再写配置。
- **Grafana CA 注入方式差异** → 优先 `$__file{}`，不可用则内联公开 CA；不关闭校验。
- **`KafkaAppender` 初始化失败会静默降级** → 验收必须断言探针进入 ES，不能只看进程健康；单测补充 SSL 属性断言。
- **exporter 凭据进入进程参数** → 仅限本地开发；文档标注，生产需专用 secret 机制。
- **RocketMQ 给人“已安全”的错觉** → 命名、文档与回归输出统一使用“加密通道/已知边界”，禁止“零信任/认证”措辞。

## Migration Plan

1. 执行 `scripts/tls-init.sh --force` 生成 Kafka keystore 与 runtime 副本；所有使用证书的容器需要重建。
2. 在 `.env` 增加 `ELASTIC_PASSWORD`（本地默认值），确认 `.env` 仍在 `.gitignore` 中。
3. 对已有 `es_data`：启动 ES 后执行 `elasticsearch-reset-password -u elastic`，或按文档重建本地卷；新环境由 `ELASTIC_PASSWORD` 初始化。
4. 先启动数据面（ES、Kafka、kafka-init、Logstash）并跑 `log-search-tls.sh`，再启动宿主机服务或 Docker 全栈。
5. 回滚：切回上一提交/镜像并恢复旧的 compose 与 ES/Kafka 配置；ES 关闭 security 需要同时回滚配置与凭据，Kafka 恢复 PLAINTEXT 需要重建 broker 容器。不存在运行时自动明文回退开关。
