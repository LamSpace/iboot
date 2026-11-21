# Proposal

## Why

B2 已把服务端口、服务间调用与 MySQL / Redis / Silo / Neo4j 切到 TLS，但集中日志与检索面仍是明文：服务到 Kafka、Kafka 到 Logstash、Logstash 到 Elasticsearch、Spring 客户端到 Elasticsearch 的日志与查询内容（含 traceId、用户行为、异常栈）都可被抓包读取。B0 明确把 ES 9、Kafka SSL 与 Logstash 全链路列为“未验证，B3 单独 spike”，因此本变更先验证再落地，并同时把 RocketMQ 只能做到“加密通道”的边界写死，避免把它误算进零信任范围。

## What Changes

- **Elasticsearch 9.5**：从匿名 HTTP 改为 security + HTTPS；HTTP 与 transport 都加载本地证书；使用环境注入的凭据；健康检查、索引初始化与查询全部走 HTTPS 与本地 CA。
- **ES 客户端全链路**：`iboot-ai`、`iboot-monitor`、`iboot-notify` 使用 `spring.elasticsearch.restclient.ssl.bundle=iboot` 与凭据；Logstash 输出、Kibana、Grafana 数据源、elasticsearch-exporter 全部改为 HTTPS + CA + 凭据。
- **Kafka**：容器内 9092、宿主机 29092 与 controller 监听改为 SSL-only；`tls-init.sh` 增加 Kafka 使用的 PKCS12 keystore；broker 健康检查、`kafka-init`、`log-pipeline.sh` 与宿主机工具改用 SSL 客户端配置。
- **Kafka 日志投递**：`KafkaAppender` 增加 security protocol、truststore 位置/密码/类型配置，默认 SSL；Kafka 不可用时仍保持“只降级本地日志、不阻塞业务”的语义。
- **Logstash**：Kafka 输入启用 SSL，ES 输出启用 HTTPS、CA 校验与认证。
- **RocketMQ“仅加密”边界**：proxy 挂载证书并开启 TLS，workflow 客户端 `enableSsl=true`；但同一端口仍接受明文、客户端不校验服务端证书，因此只登记为“加密通道”，不宣称认证、不宣称强制 TLS、不纳入零信任身份。
- **BREAKING**：Elasticsearch 不再接受匿名明文请求，客户端必须带凭据与 CA；Kafka 客户端必须带 truststore，旧的明文生产/消费命令与健康检查失效；已有 `es_data` 卷从无 security 迁移时需要重置 `elastic` 密码。
- **回归与文档**：新增日志/检索面 TLS 正负向检查并接入全量回归与 CI；更新 TLS 指南、可观测性、微服务底座、配置详解、快速开始、容器与脚本说明、`.env.example` 与 ROADMAP。

## Capabilities

### New Capabilities

无。传输安全已在 `transport-security` 建立，日志与检索属于其覆盖面扩展。

### Modified Capabilities

- `transport-security`: 增加 Elasticsearch、Kafka、日志管道与 RocketMQ 加密通道的要求；把负向门禁扩展到日志与检索面。
- `container-deployment`: Kafka 只提供 SSL 监听；ES、Kafka、Logstash、Kibana、Grafana、exporter 与 RocketMQ proxy 的证书挂载、健康检查与启动隔离按 TLS 调整。

## Impact

- **共享内核与日志模块**：`iboot-kernel` 的共享 ES SSL bundle 配置、`iboot-logging` 的 `KafkaAppender` 与单元/集成测试。
- **业务服务**：`iboot-ai`、`iboot-monitor`、`iboot-notify` 的 ES 地址、SSL bundle 与凭据；`iboot-workflow` 的 RocketMQ SSL 开关。
- **数据面与编排**：`docker/scripts/elk/elasticsearch.yml`、`docker/scripts/elk/logstash.conf`、`docker/kafka/init-topics.sh`、`docker/rocketmq/rmq-proxy.json`、`docker/scripts/grafana/provisioning/datasources/elasticsearch.yaml`、`docker/docker-compose.yml`。
- **证书与脚本**：`scripts/tls-init.sh`（Kafka keystore）、`scripts/dev-init.sh`（ES/Kafka 等待与凭据）、`scripts/regress/*`（log-pipeline、internal-tls 或新增 log-search-tls）、CI `regress.yml`。
- **配置与文档**：`.env.example`、`docs/27`、`docs/11`、`docs/23`、`docs/10`、`docs/05`、`docker/README.md`、`scripts/README.md`、`ROADMAP.md`。
- **兼容性**：所有直连 Elasticsearch 的明文客户端、所有不带 SSL 配置的 Kafka 客户端与运维命令失效；Kibana/Grafana 自身监听端口的 TLS、Kafka mTLS、ES 每服务账号与 RocketMQ 强制 TLS 不在本变更范围。
