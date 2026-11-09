# Design

## Context

动机见 `proposal.md`。当前相关事实：

- 日志配置只有三份：`iboot-kernel`（九个业务服务共享）、`iboot-gateway`、`iboot-ops`，内容等价，均为 `LogstashTcpSocketAppender` → `${LOGSTASH_HOST}:5000`，`ringBufferSize=8192`，`customFields {"app": "<spring.application.name>"}`。
- `docker/scripts/elk/logstash.conf` 只有 `tcp { port => 5000 }` 输入，输出到 `iboot-logs-%{+YYYY.MM.dd}`。
- `iboot-monitor` 的 `RunLogApplicationService` 直接查询 `iboot-logs-*`，依赖 `app`、`traceId` 字段——**索引名与字段形状是硬契约**。
- `logstash-logback-encoder` 9.0 的 jar 中不存在 Kafka appender（只有 TCP/UDP/Async 基类）。
- `iboot-gateway` 与 `iboot-ops` 不依赖 `iboot-kernel`；`iboot-sentinel` 是「不产生进程的小共享模块」的现成范本。
- Boot 4.0.3 BOM 托管 `kafka-clients` 4.1.1；本机 Maven 仓库与 Docker 镜像均未预装 Kafka。
- `scripts/dev-init.sh` 只启动 mysql/redis/minio/es + nacos/tempo/sentinel + rocketmq，**不启动 Logstash/Kibana**；Kibana 属于 `observability` profile。
- 测试没有 `logback-test.xml`，当前靠 TCP 连不上时静默重连存活。
- `scripts/pull-docker-images.sh` 通过华为镜像站拉取后 retag，是新增镜像的既定入口。

## Goals / Non-Goals

**Goals:**

- 把日志缓冲从 JVM 内移到 Kafka，使 Logstash 重启/抖动期间日志可在 24 小时内回放。
- 保持 ES 索引名、字段与监控服务查询接口完全不变。
- 保持「日志投递失败不影响业务、不阻塞调用线程、不阻止启动」。
- 开关有可查的默认值，宿主机调试与自动化测试不因 Kafka 缺失而变慢或产生噪声。
- 迁移过程可分两半独立验证，任一步失败都能单独回滚。

**Non-Goals:**

- 不引入 `spring-kafka`：Logback 在 Spring 上下文之前初始化，appender 只能直接持有 `KafkaProducer`。
- 不做日志去重或 exactly-once；接受至少一次与重复文档。
- 不把业务事件迁到 Kafka；事件投递仍由 RocketMQ + outbox 负责。
- 不引入 Flink、不改变 ES/Kibana 的 profile 归属、不在 `dev-init.sh` 默认启动 Kafka。
- 不暴露丢弃计数指标端点（已与用户确认）。

## Decisions

### D1：自研 Logback appender，放在新的 `iboot-logging` 模块

`logstash-logback-encoder` 9.0 没有 Kafka appender。备选与取舍：

| 方案 | 取舍 |
|---|---|
| 第三方 `logback-kafka-appender` | 最后发版 2019，绑定 `kafka-clients` 2.x，与 4.1.1 混搭风险不可控；否 |
| Filebeat 读日志文件 | 本地容器日志目录私有，需给十个容器挂共享卷并新增常驻进程；到阶段 3 K8s 才自然；否 |
| 复制 appender 到三份配置 | 三份实现漂移不可避免；否 |
| 让 gateway/ops 依赖 `iboot-kernel` | 把共享内核拖进网关，违反拆分边界；否 |
| **新模块 + 自研 appender** | 一个类 + 三个 `provided` 依赖，形态与 `iboot-sentinel` 一致；是 |

实现继承 `LoggingEventAsyncDisruptorAppender`：JSON 编码、环形缓冲、`neverBlock` 丢弃与停机 flush 均由基类提供，只实现 `start/append/stop`。`append()` 在 disruptor 线程上执行，业务线程只入队。

依赖作用域：logback 与 encoder 以 `provided` 引入（每个服务的运行期都已自带），`kafka-clients` 为普通依赖——消费方不直接声明它，必须经传递依赖进入运行期。

### D2：生产者参数以「不阻塞、有界、可恢复」为准

| 参数 | 取值 | 理由 |
|---|---|---|
| `acks` | `1` | 日志不值得为 `all` 付延迟；允许丢尾批 |
| `max.block.ms` | `1000` | 默认 60s 会让 broker 不可达时卡住投递线程，环形缓冲被迅速填满 |
| `delivery.timeout.ms` | `30000` | 界定「恢复后自动续投」的窗口，超时批次丢弃 |
| `buffer.memory` | `33554432` | 停机时堆占用有界 |
| `linger.ms` | `0` | 日志不追求批量化延迟收益 |
| `compression.type` | `lz4` | 日志重复度高，CPU 开销低 |
| `client.id` | 应用名 | broker 侧可按服务观察连接 |
| `close` 超时 | `2s` | SIGTERM 时不让 flush 拖住停机 |
| 环形缓冲 | `8192` | 与现状一致 |

`max.block.ms` 与环形缓冲共同保证：broker 停机时投递线程最多阻塞 1 秒/次，缓冲耗尽后由基类丢弃并计数，业务线程不受影响。

### D3：开关与默认值

- 属性：`iboot.logging.kafka.enabled`（默认 `false`）、`iboot.logging.kafka.bootstrap-servers`（默认 `localhost:29092`）。
- Logback 侧用 `<springProperty source="..." defaultValue="false"/>`；Spring 松弛绑定使 `IBOOT_LOGGING_KAFKA_ENABLED` 与 `-Diboot.logging.kafka.enabled` 均生效。
- compose 的 `x-app-env` 显式设为 `true` 并指向 `kafka:9092`；宿主机与测试不设置，落到默认 `false`。
- 关闭时 appender 在 `start()` 中不创建 producer、不发起连接；这是规格「输出目标在全部环境保持一致」改写后的落地方式。

### D4：Kafka 编排（KRaft 单节点、双监听、非启动依赖）

- 镜像 `apache/kafka:4.1.1`（缺失时回退 4.1.0，同属 4.1 线），经 `pull-docker-images.sh` 从华为镜像站获取。
- `KAFKA_PROCESS_ROLES=broker,controller`、单节点 quorum、`KAFKA_NODE_ID=1`，不部署 ZooKeeper。
- 监听：容器内 `PLAINTEXT://:9092`，宿主机 `PLAINTEXT_HOST://:29092`，controller `CONTROLLER://:9093`；`advertised.listeners` 分别对应 `kafka:9092` 与 `localhost:29092`，使宿主机进程与 `kafka-*.sh` 工具可用。
- 堆上限 `512MB`；数据使用命名卷；健康检查用 `kafka-broker-api-versions.sh --bootstrap-server localhost:9092`。
- `kafka-init` 一次性任务（对标 `rocketmq-init`/`minio-init`）：`--create --if-not-exists --topic iboot-logs --partitions 6 --replication-factor 1 --config retention.ms=86400000`，关闭自动建 topic。
- Kafka 与 Logstash 同级属核心编排；Kibana 仍在 `observability` profile。任何服务都不 `depends_on: kafka`。

### D5：Logstash 消费侧

- 输入换成 `kafka` 插件：`bootstrap_servers => "kafka:9092"`、`topics => ["iboot-logs"]`、`group_id => "iboot-logstash"`、`codec => json`、`auto_offset_reset => "latest"`、`consumer_threads => 2`。
- 删除 `tcp` 输入；`filter` 与 `output` 保持原样——encoder 输出的 `timestamp` 字段继续由 `date` 插件处理，索引名不变。
- 位点提交在 Kafka 的 `__consumer_offsets`；Logstash 重启从已提交位点续消费，无需重建 topic。首次启动因 `latest` 只消费新日志，与「停机期间日志允许缺失」一致。

### D6：投递语义与可观测性

- 至少一次：重试可能产生重复文档；ES 按天索引无唯一键。文档明确「允许重复、不允许阻塞」。
- 丢弃与未排空事件由基类通过 Logback status 告警（环形缓冲满时按频率节流、停机时对残留事件告警一次）；排障时按文档打开 status 读取。不新增 Micrometer 指标。
- 间接信号：Kibana/ES 按 `app` 过滤的日志量骤降；`kafka-consumer-groups.sh --describe` 查看 `iboot-logstash` 积压。

### D7：迁移分两半，每半独立验收

```
A. 基础设施（业务零改动）
   compose 加 kafka + kafka-init；logstash 加 kafka input，保留 tcp input
   验收：手工向 iboot-logs 生产一条 JSON，ES 可查到
B. 生产者切换（单次提交）
   新增 iboot-logging + 三份 logback 切换 + 删除 logstash tcp input
   验收：服务日志重新出现在 ES，app/traceId 字段不变
C. 演练、回归、文档与 ROADMAP
```

A 阶段不改变任何服务行为；B 阶段是唯一一次切换，回滚即 revert 该提交（配置 + 模块），Kafka 与 topic 可保留。

## Risks / Trade-offs

- **[`apache/kafka:4.1.1` 镜像不可从镜像站获取]** → 先执行 `pull-docker-images.sh` 验证；回退 4.1.0（同属 4.1 线，客户端 4.1.1 向下兼容）。
- **[自研 appender 引入阻塞或递归日志]** → `max.block.ms` 有界；`append()` 只在 disruptor 线程；失败通过 `addWarn` 走 status，不经过应用 logger；用 `MockProducer` 锁定「抛异常不上抛」「缓冲满即丢弃」。
- **[关闭开关后接线错误长期未被发现]** → compose 与集成测试显式开启；`scripts/regress/log-pipeline.sh` 覆盖端到端断言。
- **[重复文档被误判为缺陷]** → 文档写明至少一次语义与判定方式（同一 `traceId` + 相同 `message`）。
- **[Logstash 停机超过 24 小时]** → 超出保留期的日志不可回放；文档明确 Kafka 是缓冲、ES 是记录。
- **[测试环境意外创建 producer]** → 默认 `false`；若有人改默认值，appender 单测与启动耗时断言会先失败。
- **[本机内存 +1 常驻进程]** → Kafka 堆上限 512MB，计入 ROADMAP R4 的 26 个容器；`dev-init.sh` 默认不启动 Kafka。

## Migration Plan

1. 阶段 A：编排与 Logstash 输入先行，业务不受影响；用控制台生产者验证「Kafka → Logstash → ES」单向通路。
2. 阶段 B：新增 `iboot-logging`，三份 logback 切换，同时删除 Logstash 的 tcp 输入；单次提交，便于整体回滚。
3. 阶段 C：执行验收演练（正常链路、停 Kafka、停 Logstash、重启恢复），跑全量回归与 `scripts/regress/log-pipeline.sh`，更新文档与 ROADMAP。
4. 回滚：revert 阶段 B 提交即恢复 TCP 直投（Logstash 的 tcp 输入同时恢复）；Kafka 容器与 topic 可保留，不影响回滚后的运行。

## Open Questions

- `apache/kafka:4.1.1` 在镜像站的实际可用性，需在第一个任务中确认；只影响镜像 tag，不改规格与任务分解。
