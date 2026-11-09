# Proposal

## Why

当前集中日志链路是「服务 → Logstash TCP → Elasticsearch」的直连模式：缓冲只存在于每个 JVM 内的 8192 槽环形缓冲，Logstash 重启或抖动期间，九个业务服务加运维控制台的日志会直接丢弃，且没有任何可回放的中间层。ROADMAP 阶段 2 收尾已登记「Kafka 日志收集」，阶段 4 的流处理条目也以「Kafka 日志 topic 已就位」为前提。

## What Changes

- 服务日志投递从「Logstash TCP 直投」改为「Kafka 缓冲 → Logstash 消费 → Elasticsearch」。ES 索引名（`iboot-logs-*`）与文档字段（`app`、`traceId`）保持不变，监控服务的日志查询接口零改动。
- 新增共享模块 `iboot-logging`，承载自定义 Logback appender（`logstash-logback-encoder` 9.0 未提供 Kafka appender）。`iboot-kernel`、`iboot-gateway`、`iboot-ops` 依赖该模块，三份 `logback-spring.xml` 同步切换。运维控制台一并接入，日志投递范围变为「九个业务服务 + 运维控制台」。
- 编排新增 KRaft 单进程 Kafka 与一次性 topic 初始化（topic `iboot-logs`，分区 6，副本 1，保留 24 小时）。Kafka 不是任何服务的启动依赖。
- 新增投递开关 `iboot.logging.kafka.enabled`，默认 `false`；compose 部署开启，宿主机调试与自动化测试默认关闭。开关与默认值必须写入文档。
- **BREAKING**：Logstash 删除 TCP 5000 输入，不再接受任何 TCP 日志来源；仍按 TCP 投递的客户端将失去集中日志。
- 规格调整：`service-runtime` 变更集中日志投递的传输语义与开关合规性；`container-deployment` 新增 Kafka 日志栈的编排与隔离契约。

## Capabilities

### New Capabilities

（无。本变更不引入新的能力边界：投递契约归 `service-runtime`，编排契约归 `container-deployment`。）

### Modified Capabilities

- `service-runtime`: 结构化日志投递改为经消息队列缓冲后进入集中日志系统，并允许以文档登记的开关在宿主机调试与测试环境关闭投递。
- `container-deployment`: 新增 Kafka 日志缓冲栈的编排、主题初始化、启动隔离与宿主机可达契约。

## Impact

- 新增模块：`iboot-logging`（appender + 测试），根 `pom.xml` 登记。
- 依赖变更：`iboot-kernel`、`iboot-gateway`、`iboot-ops` 增加对 `iboot-logging` 的依赖；`iboot-logging` 以 `provided` 作用域引用 logback、encoder 与 `kafka-clients`（版本由 Boot 4.0.3 托管为 4.1.1）。
- 配置：三份 `logback-spring.xml`；`docker/docker-compose.yml`（kafka、kafka-init、`x-app-env`）；`docker/scripts/elk/logstash.conf`；`.env.example`。
- 脚本：`scripts/pull-docker-images.sh`（新增 Kafka 镜像）、`scripts/regress/log-pipeline.sh`（验收与故障演练）。
- 文档：`docs/03`、`docs/05`、`docs/08`、`docs/11`、`docs/23`、`README.md`、`ROADMAP.md`。
- 不变：任何 HTTP API、前端代码、数据库结构、ES 索引与字段、监控服务查询实现。
