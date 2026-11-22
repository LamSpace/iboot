# Design

## Context

Kafka 4.4 KRaft 单节点当前使用三个 SSL 监听：9092 容器内、29092 宿主机、9093 controller；`KAFKA_SSL_CLIENT_AUTH=none`，服务端只出示证书，不识别客户端。日志通过自定义 `com.iboot.logging.KafkaAppender` 经 logback 投递，配置项来自 `iboot.logging.kafka.*`；Logstash 与 `kafka-init` 直接使用 Kafka 命令行工具。

Redis 8 通过 `--requirepass` 保护默认用户，所有服务、exporter、健康检查与回归脚本共用该用户与全部命令权限。

## Goals / Non-Goals

**Goals:**

- Kafka 客户端监听只接受通过 SASL 认证的连接，并按账号限制 topic / consumer group 权限。
- Redis 关闭默认用户，业务与 exporter 使用命名 ACL 账号。
- 保持现有 TLS 信任链与日志降级行为：认证失败只影响日志投递，不阻塞业务请求。

**Non-Goals:**

- 不引入 SCRAM 或外部身份提供方；本地使用 SASL/PLAIN over TLS，密码由环境注入。
- 不实现 Kafka 按服务的细粒度账号（九个服务共用 `iboot_log`）；每服务身份属于 B5。
- 不改变 controller 监听的发布范围（仍不发布到宿主机），但 controller 也使用 SASL_SSL 与 `broker` 账号。
- 不把 Redis ACL 扩展到按服务命令集隔离，`iboot_app` 仍为共享业务账号。

## Decisions

### Kafka 使用 SASL/PLAIN over TLS

SCRAM 在 KRaft 首次启动前需要在 `kafka-storage format` 时写入凭据，当前 compose 依赖镜像自动格式化，改造面更大。PLAIN 的监听级 JAAS 可以直接通过环境变量注入，密码由 TLS 通道保护，适合本地可重复部署。密码限制为不含引号、反斜杠与空白的字符集，避免 JAAS 解析歧义；SCRAM 作为后续独立变更。

### 监听改名为 INTERNAL / HOST / CONTROLLER

Apache Kafka 镜像把 `KAFKA_` 环境变量按“下划线转点”映射为配置项，监听名中的下划线会被拆开，
`SSL_HOST` 因而无法映射到 `listener.name.ssl_host.*`。监听名改为不含下划线的
`INTERNAL`、`HOST`、`CONTROLLER`，三个监听都使用 SASL_SSL；controller 仍不发布到宿主机，
节点内使用 `broker` 账号。

镜像只在 advertised listeners 含字面量 `SSL://` 时自动注入 keystore；改名后需显式提供
`KAFKA_SSL_KEYSTORE_LOCATION`、`KAFKA_SSL_KEYSTORE_PASSWORD` 与 `KAFKA_SSL_KEY_PASSWORD`。

### 授权采用 StandardAuthorizer 并默认拒绝

`KAFKA_AUTHORIZER_CLASS_NAME=org.apache.kafka.metadata.authorizer.StandardAuthorizer`，`KAFKA_ALLOW_EVERYONE_IF_NO_ACL_FOUND=false`，超级用户为 `broker` 与 `iboot_tools`。`kafka-init` 以 `iboot_tools` 创建 topic 和 ACL：

- `User:iboot_log`：`Write`、`Describe` on topic `iboot-logs`。
- `User:iboot_logstash`：`Read`、`Describe` on topic `iboot-logs`；`Read` on group `iboot-logstash`。

服务可能先于 ACL 就绪启动；KafkaAppender 已有失败降级与重试窗口，ACL 写入后会恢复投递。

### KafkaAppender 增加 SASL 三属性

在 `KafkaAppender` 增加 `saslMechanism`、`saslUsername`、`saslPassword`，在 `applySecurityProperties` 中写入 `sasl.mechanism` 与构造后的 `sasl.jaas.config`。JAAS 字符串对用户名/口令做引号与反斜杠转义，避免配置注入。logback 通过 `iboot.logging.kafka.sasl-*` 读取，环境变量为 `IBOOT_LOGGING_KAFKA_SASL_MECHANISM/USERNAME/PASSWORD`。

### Redis 使用命令行的命名 ACL

启动参数在 `--requirepass` 之外增加：

- `--user default off`
- `--user iboot_app on ><REDIS_PASSWORD> ~* +@all`
- `--user iboot_exporter on ><REDIS_EXPORTER_PASSWORD> ~* +@read +info +dbsize +slowlog +memory +client|list +config|get +command +ping +cluster|info`

九个服务在 `spring.data.redis.username` 使用 `iboot_app`；健康检查与回归使用 `--user iboot_app --pass`。exporter 使用 `rediss://`、`iboot_exporter` 与本地 CA，避免 TLS-only 端口上的明文客户端。ACL 不落盘，每次容器启动由参数恢复，避免引入 ACL 文件与证书目录的耦合。

## Risks / Trade-offs

- **Kafka 口令包含引号或反斜杠导致 JAAS 解析失败** → `.env.example` 与文档限定字符集；KafkaAppender 做转义；CI 使用简单口令。
- **ACL 未就绪期间日志投递失败** → 保留既有丢弃与告警语义，业务不受影响；`kafka-init` 在 broker 健康后立即执行。
- **旧 Kafka 数据卷中的 ACL 元数据** → 首次启用 Authorizer 时 ACL 为空且默认拒绝；初始化任务补写 ACL，无需清空日志卷。
- **Redis ACL 未持久化** → 以启动参数为唯一来源；容器重建后自动恢复，不需要额外卷。
- **共享 `iboot_log` / `iboot_app` 账号不能证明服务身份** → 在文档中登记为共享账号边界，每服务身份留给 B5。

## Migration Plan

1. 更新 `.env.example` 与本地 `.env` 新增 Kafka 四个口令与 Redis exporter 口令。
2. 修改 KafkaAppender 与三份 logback 配置，重新构建镜像。
3. 重建 Kafka、Logstash、Redis、exporter 与九个服务。
4. `kafka-init` 创建 topic 与 ACL；验证正确账号、无凭据、越权 topic、Redis 默认用户与 exporter 命令集。
5. 回滚：恢复旧 compose 与镜像重建 Kafka/Redis；Kafka ACL 元数据保留但 Authorizer 关闭后不再生效；Redis 启动参数恢复 `--requirepass`。

## Open Questions

无。
