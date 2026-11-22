# Proposal

## Why

Kafka 目前只启用 SSL：任何能进入 `data` 网络并持有本地 CA 的客户端都能生产或消费日志，服务端不校验调用方身份。Redis 虽然要求口令，但所有服务共用默认用户，拥有全部命令权限。两者让“数据网络内仍需凭据”的最低边界不成立。

## What Changes

- Kafka 客户端监听从 `SSL` 改为 `SASL_SSL`，使用 SASL/PLAIN 账号按用途区分：
  `iboot_log`（业务服务日志生产）、`iboot_logstash`（日志消费）、`iboot_tools`（运维与初始化）、`broker`（节点间）。
- 启用 `StandardAuthorizer` 并关闭“无 ACL 即放行”：生产端只能写 `iboot-logs`，消费端只能读该 topic 与自己的消费组；初始化任务以超级用户创建 topic 与 ACL。
- 日志 appender 增加 SASL 机制、用户名与口令属性；Logstash 与 `kafka-init` 使用对应账号。
- Redis 关闭 `default` 用户，新增 `iboot_app`（服务）与 `iboot_exporter`（指标导出，受限命令集）；服务通过 `spring.data.redis.username` 使用命名账号。
- 更新 `.env.example`、`dev-init.sh`、CI 与回归脚本；新增 Kafka 无凭据、错误口令、越权 Topic 的负向检查，以及 Redis 默认用户禁用检查。
- 修正 `docs/27` 中已过期的 Nacos 鉴权描述与宿主机监听边界表述。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `middleware-auth`: 新增 Kafka SASL 账号与 Topic 授权要求、Redis 命名账号与默认用户禁用要求。

## Impact

- 编排：`docker/docker-compose.yml` 的 Kafka 监听、JAAS、Authorizer、Redis 启动参数与 exporter。
- Java：`iboot-logging` 的 `KafkaAppender` 增加 SASL 配置；三份 `logback-spring.xml` 增加属性。
- 配置：`.env.example`、CI、`dev-init.sh`、九个服务的 `spring.data.redis.username`。
- 脚本：`kafka/init-topics.sh`、Logstash 配置、`scripts/regress/middleware-auth.sh`、`log-search-tls.sh`。
- 运行时：Kafka 与 Redis 需要重建容器；Kafka 数据卷保留，ACL 存储在元数据中；Redis ACL 不持久化到卷，由启动参数恢复。
