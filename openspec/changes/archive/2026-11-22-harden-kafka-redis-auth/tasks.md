# Tasks

## 1. 凭据与日志客户端

- [x] 1.1 在 `.env.example` 增加 `KAFKA_BROKER_PASSWORD`、`KAFKA_LOG_PASSWORD`、`KAFKA_LOGSTASH_PASSWORD`、`KAFKA_TOOLS_PASSWORD`、`REDIS_EXPORTER_PASSWORD`，并说明字符集限制；验证：`dev-init.sh` 缺失变量时失败关闭
- [x] 1.2 扩展 `iboot-logging` 的 `KafkaAppender`，增加 SASL 机制/用户名/口令属性并安全构造 JAAS 配置；验证：单元测试覆盖 SASL 属性与转义
- [x] 1.3 在 `iboot-kernel`、`iboot-gateway`、`iboot-ops` 的 `logback-spring.xml` 注入 SASL 属性，编排为九个服务提供 `IBOOT_LOGGING_KAFKA_SASL_*`；验证：构建通过且 logback 解析无未知属性

## 2. Kafka SASL 与授权

- [x] 2.1 将 Kafka 两个客户端监听切换为 `SASL_SSL`，配置 PLAIN JAAS、`StandardAuthorizer`、默认拒绝与超级用户；验证：无凭据连接失败，`iboot_log` 可生产
- [x] 2.2 更新 `docker/kafka/init-topics.sh`，使用 `iboot_tools` 创建 topic，并幂等写入 `iboot_log` 写权限与 `iboot_logstash` 读权限/消费组权限；验证：重复执行成功且 ACL 列表符合预期
- [x] 2.3 更新 Logstash Kafka 输入为 SASL_SSL 与 `iboot_logstash`；验证：日志探针仍能从服务到达 Elasticsearch

## 3. Redis ACL

- [x] 3.1 修改 Redis 启动参数：关闭 `default`，新增 `iboot_app` 与受限的 `iboot_exporter`；更新 exporter 与健康检查；验证：命名账号 PING 成功、默认用户失败
- [x] 3.2 在九个服务的 Redis 配置中增加 `username`（默认 `iboot_app`），并同步 `dev-init.sh`/CI 所需环境；验证：服务启动后 Redis 读写正常
- [x] 3.3 扩展 `scripts/regress/middleware-auth.sh`：检查默认用户禁用、业务账号可用、exporter 命令集；验证：该脚本全部通过

## 4. 文档与运维

- [x] 4.1 更新 `docs/27-TLS 与零信任操作指南.md`、`docs/11-可观测性设计.md`、`docker/README.md` 的 Kafka/Redis 账号、ACL 与验证命令；修正 Nacos 鉴权与宿主机监听的过期表述
- [x] 4.2 更新 Kafka/Redis 排障与回滚说明；验证：文档中的命令可执行，且不包含可直接使用的仓库内口令

## 5. 集成验证

- [x] 5.1 重建 Kafka/Redis 与全部服务后，检查日志管道、业务读写与 exporter 指标；验证：`log-pipeline.sh` 11/11、`middleware-auth.sh` 33/33、Redis exporter `redis_up=1`
- [x] 5.2 执行全栈回归；验证：TLS 11/11、内部 TLS 53/53、中间件认证 33/33、日志检索 TLS 12/12、暴露面 46/46、明文审计通过、权限矩阵 34/34；冒烟 43/44，唯一失败为既有 `IBOOT_BACKUP_ENABLED=false` 备份用例
