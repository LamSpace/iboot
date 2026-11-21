# Tasks

## 1. B3-0 可行性 Spike

- [x] 1.1 用独立 compose 覆盖验证 ES 9.5：security + HTTP/transport TLS、全新 `es_data` 与已有卷密码迁移两种路径；验证 `curl --cacert` 成功、`http://` 失败、错误凭据失败
- [x] 1.2 验证 Kafka KRaft 单节点三个监听（9092/29092/9093）全部 SSL，含 broker 健康检查、`kafka-init` 与宿主机客户端；验证明文客户端失败
- [x] 1.3 验证 Logstash Kafka 输入 SSL、ES 输出 HTTPS、Kibana、Grafana 数据源与 elasticsearch-exporter 的实际配置键；验证一条探针从 Kafka 进入 ES
- [x] 1.4 验证 RocketMQ Proxy TLS 握手、workflow `enableSsl=true` 投递通知，并确认同端口明文仍可连接；记录客户端不校验服务端证书的证据
- [x] 1.5 把可用的精确配置键、失败点与边界写入 `docs/spike/log-search-tls-spike-report.md`，并确认报告中的命令可在本机复现

## 2. 证书与共享配置

- [x] 2.1 扩展 `scripts/tls-init.sh` 生成 `runtime/kafka-keystore.p12` 并加入 runtime 副本与权限处理；执行 `scripts/tls-init.sh --force` 后 `keytool -list` 能读出私钥条目
- [x] 2.2 在 `iboot-tls.yml` 增加 `spring.elasticsearch.restclient.ssl.bundle=iboot`；启动任一使用 ES 的服务，确认 SSL bundle 被解析且健康检查通过
- [x] 2.3 更新 `.env.example` 的 `ELASTIC_PASSWORD`、Kafka SSL 与 ES 地址项，并确认 `.env`、`docker/certs` 仍被 `.gitignore` 覆盖
- [x] 2.4 更新 `scripts/dev-init.sh` 的 ES 等待、Kafka 等待、凭据与 truststore 注入；宿主机启动后 ES 与 Kafka 健康检查均通过

## 3. Elasticsearch 与检索面

- [x] 3.1 修改 `docker/scripts/elk/elasticsearch.yml` 与 compose：启用 security、HTTP/transport TLS、证书与凭据挂载、HTTPS 健康检查；验证 `https://localhost:9200` 带 CA 与凭据返回 200，明文与无 CA 失败
- [x] 3.2 修改 `iboot-ai`、`iboot-monitor`、`iboot-notify` 的 ES 地址、SSL bundle 与凭据；验证三个服务启动、运行日志查询与知识库索引/召回正常
- [x] 3.3 修改 Logstash 输出、Kibana、Grafana 数据源与 elasticsearch-exporter；验证日志探针可在 Kibana/Grafana 数据源中检索，exporter 指标不因 TLS 变空
- [x] 3.4 更新 `docs/11-可观测性设计.md`、`docs/10-后端配置详解.md`、`docs/23-微服务底座操作指南.md` 中的 ES 地址、凭据、TLS 与排障；按文档命令逐条执行验证

## 4. Kafka 与日志投递

- [x] 4.1 修改 compose 的 Kafka listener、keystore/truststore、健康检查与 `kafka-init`；验证 SSL 生产/消费成功，PLAINTEXT 客户端失败，topic 初始化幂等
- [x] 4.2 扩展 `KafkaAppender` 的 SSL 配置与单测，更新集成测试的协议设置；运行 `mvn -s /home/lam/repo/settings.xml -pl iboot-logging test` 通过
- [x] 4.3 更新 `scripts/regress/log-pipeline.sh` 与宿主机 Kafka 工具用法，改用 SSL 与 CA；验证十个进程日志覆盖、traceId 探针、停 Kafka 与停 Logstash 演练全部通过
- [x] 4.4 更新 `docs/11-可观测性设计.md`、`docs/23-微服务底座操作指南.md`、`docs/10-后端配置详解.md` 的 Kafka 监听、客户端配置与命令；按文档命令验证

## 5. RocketMQ 仅加密边界

- [x] 5.1 修改 `docker/rocketmq/rmq-proxy.json` 与 compose，挂载证书并开启 TLS；设置 workflow `IBOOT_ROCKETMQ_SSL_ENABLED=true`，验证审批通知事件可投递
- [x] 5.2 新增 RocketMQ 边界回归输出：TLS 握手成功计入通过，明文仍可连接作为边界信息而非 TLS 通过；验证输出不会把明文结果误报为安全
- [x] 5.3 更新 `docs/27-TLS 与零信任操作指南.md` 与 `docs/23` 的 RocketMQ 边界说明；检查文档中不出现“零信任/认证/强制 TLS”等超出能力的表述

## 6. 回归、CI 与文档收口

- [x] 6.1 新增 `scripts/regress/log-search-tls.sh` 并接入 `regress.sh`；脚本覆盖 ES 明文/无 CA/错误凭据、Kafka 明文/无 truststore、Logstash 两端校验与 RocketMQ 边界
- [x] 6.2 更新 `.github/workflows/regress.yml` 注入 ES 凭据与 Kafka SSL 环境；全栈 CI 步骤能执行新增检查
- [x] 6.3 更新 `docs/27` 的 B3 章节、`docs/05-快速开始.md`、`docker/README.md`、`scripts/README.md`、`ROADMAP.md`；确认所有 `http://localhost:9200`、无 SSL 的 Kafka 命令已清除或标注为历史
- [x] 6.4 在宿主机与 Docker 全量栈各执行一次 `scripts/regress/regress.sh`；记录公开 TLS、内部 TLS、日志/检索 TLS、冒烟、权限矩阵与 CLI 的结果，失败项必须回到对应任务组修复
  - 宿主机：公开 TLS 11/11、内部 TLS 40/40、日志/检索 TLS 12/12、冒烟 44/44、权限矩阵 34/34。
  - Docker：公开 TLS 11/11、内部 TLS 40/40、日志/检索 TLS 12/12、冒烟 43/44（唯一失败为 compose 固定的 `IBOOT_BACKUP_ENABLED=false`，B2 已有部署默认，不属于本变更）、权限矩阵 34/34、CLI 38/38；日志管道演练 16/16。

## Workflow follow-up

- 代码评审与回归全部通过后，按项目流程提交功能分支、创建 PR、合并到 `master`。
- 合并后同步 delta spec 到主规格并归档本变更。
