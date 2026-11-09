# Tasks

## 1. 镜像与 Kafka 编排

- [x] 1.1 在 `scripts/pull-docker-images.sh` 增加 `apache/kafka` 条目（优先 4.1.1，不可用时回退 4.1.0）并 retag 到 Dockerfile/compose 引用的名字；验证：执行脚本后 `docker images` 存在与编排引用一致的固定标签，重复执行不失败
- [x] 1.2 在 `docker/docker-compose.yml` 新增 `kafka` 服务（KRaft 单节点、`kafka:9092` + `localhost:29092` 双监听、堆上限 512MB、命名卷、`kafka-broker-api-versions.sh` 健康检查），不加入任何服务的 `depends_on`；验证：`docker compose up -d kafka` 后容器 healthy，宿主机 `kafka-broker-api-versions.sh --bootstrap-server localhost:29092` 成功
- [x] 1.3 新增 `kafka-init` 一次性任务，`--if-not-exists` 建 `iboot-logs`（6 分区、副本 1、`retention.ms=86400000`），并关闭自动建 topic；验证：连续执行两次均以 0 退出，`kafka-topics.sh --describe` 的分区数与保留期保持不变

## 2. Logstash 消费侧（阶段 A）

- [x] 2.1 把 `docker/scripts/elk/logstash.conf` 的 `tcp` 输入替换为 `kafka` 输入（`iboot-logs`、组 `iboot-logstash`、`codec => json`、`auto_offset_reset => latest`），`filter`/`output` 保持不变；本任务先保留 tcp 输入直到任务 4.2；验证：`kafka-console-producer` 灌入一条带 `app`/`traceId` 的 JSON 后，ES 中可按 `app` 与 `traceId` 查到该文档
- [x] 2.2 更新 `docs/11-可观测性设计.md` 的架构图、数据流与 Logstash 配置小节，并修正「5044 接收 Beats」的存量漂移；验证：按文档中的命令可复现任务 2.1 的验证结果

## 3. 生产者模块 `iboot-logging`

- [x] 3.1 新建 `iboot-logging` 模块（logback 与 encoder 以 `provided` 引入，`kafka-clients` 为普通依赖以传递到消费方运行期），在根 `pom.xml` 登记，并由 `iboot-kernel`、`iboot-gateway`、`iboot-ops` 依赖；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-logging -am compile` 通过，`dependency:tree` 显示 `kafka-clients:4.1.1`
- [x] 3.2 实现 `KafkaAppender`（继承 `LoggingEventAsyncDisruptorAppender`，实现 `start/append/stop`，`enabled=false` 时不建 producer，发送失败不上抛，`close` 超时 2 秒），按项目规范补 Javadoc；验证：`MockProducer` 单测覆盖「编码含 app/traceId」「发送失败不影响调用方」「环形缓冲满即丢弃并计数」「关闭时释放 producer」四条断言
- [x] 3.3 增加 Testcontainers 集成测试（`apache/kafka` KRaft），真实生产者到消费者往返；验证：`mvn -s /home/lam/repo/settings.xml -pl iboot-logging test` 通过，且未启动 Kafka 的普通单测不创建 producer、总耗时与改造前相当
- [x] 3.4 在 `docs/03-架构设计.md` 模块表新增 `iboot-logging` 一行；验证：文档模块清单与根 `pom.xml` 的 `<module>` 列表一致

## 4. 生产者切换（阶段 B）

- [x] 4.1 三份 `logback-spring.xml`（kernel、gateway、ops）把 `LOGSTASH` appender 换成 `KAFKA` appender，绑定 `iboot.logging.kafka.enabled`（默认 false）与 `iboot.logging.kafka.bootstrap-servers`（默认 `localhost:29092`），环绕缓冲保持 8192；验证：以开关开启方式启动任一服务，日志同时出现在 `iboot-logs` topic 与 ES，且 `app`/`traceId` 字段与改造前一致
- [x] 4.2 `docker/docker-compose.yml` 的 `x-app-env` 删除 `LOGSTASH_HOST`、增加两个开关变量，并删除 Logstash 的 `tcp` 输入；验证：全栈启动后 ES 中同时出现九个业务服务与 `iboot-ops` 共十个 `app` 的日志，且 `docker logs iboot-logstash` 无 tcp 监听
- [x] 4.3 在 `docs/11-可观测性设计.md` 与 `docs/23-微服务底座操作指南.md` 登记开关的四类场景（compose、dev-init 默认关闭、dev-init 显式开启、测试）、默认值、优先级与关闭时的行为，并在 `.env.example` 增加占位；验证：按文档配置在 compose 与宿主机两种方式下各复现一次，行为与表格一致

## 5. 验收、回归与运维文档

- [x] 5.1 新增 `scripts/regress/log-pipeline.sh`：断言十个 `app` 均进 ES、按 `traceId` 可检索、停 Kafka 期间请求成功率与延迟基线、恢复后自动续投、停 Logstash 后回放；验证：脚本在全栈环境一次跑通，且每条断言失败时输出可定位信息
- [x] 5.2 执行回归矩阵：全量 `mvn -s /home/lam/repo/settings.xml test`、`dev-init.sh` 启动耗时对比、`IBOOT_LOGGING_KAFKA_ENABLED=false` 时全栈健康、`iboot-monitor` 运行日志接口、登录/列表/审批冒烟；验证：全部通过且启动耗时与改造前偏差在可接受范围内
- [x] 5.3 补齐 `docs/23-微服务底座操作指南.md` 的 Kafka 运维章节（组件与端口、`kafka-topics.sh`/`kafka-consumer-groups.sh` 排障、停 Kafka 与停 Logstash 两种演练、回滚步骤），更新 `docs/05`/`docs/08`/`README.md` 的中间件清单；验证：文档中的命令逐条可执行，演练步骤与任务 5.1 的脚本一致
- [x] 5.4 更新 `ROADMAP.md`：把 Kafka 日志收集从阶段 2 收尾移入能力索引，R4 容器数改为 26，新增决策记录「日志走 Kafka、事件仍走 RocketMQ」；验证：`grep` 不再命中「Kafka 日志收集（新增项）」段落，R4 计数与 compose 常驻服务数一致
- [x] 5.5 确认前端零改动并做一次端到端冒烟（门户登录 → 列表 → 发起审批 → 按 `traceId` 在 ES 关联到三个服务的日志）；验证：`git status` 中 `iboot-portal` 无改动，冒烟链路日志可串联

## Workflow follow-up

- 完成评审后归档本变更，并核对归档后的 `service-runtime` 与 `container-deployment` 主规格。
- 如需在阶段 4 接入 Flink 消费日志 topic，另起变更，不在本次范围内。
