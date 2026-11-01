# Tasks

## 1. 模块骨架与服务接入

- [x] 1.1 根 `pom.xml` 增加 `iboot-notify` 模块，模块 pom 依赖 `iboot-kernel` 并声明 `spring-boot-maven-plugin`；验证：`mvn -pl iboot-notify -am package -DskipTests` 成功且产物 jar 含主清单
- [x] 1.2 启动类与 `application.yml`：端口 8084、`spring.application.name=iboot-notify`、数据源指向现有库、Redis、`jwt.*`、`iboot.security.whitelist`、`@MapperScan`、`spring.config.import` 接入 Nacos、OTLP 追踪；验证：服务启动后 `/actuator/health` 为 `UP`，日志出现注册与追踪导出
- [x] 1.3 复制 `ApiVersionConfig` 与 `CloudEventResponseBodyAdvice`（`basePackages` 指向 `com.iboot.notify.interfaces.controller`）；验证：任一接口经网关返回 `{type, source, data:{code, data}}` 两层结构
- [x] 1.4 网关新增 `notify-sse`（`/api/push/**`，继承 `response-timeout: -1` 与 `connect-timeout: 2000`）与 `notify`（`/api/message/**,/api/notice/**,/api/notification/**,/api/search`）路由，order 均小于 admin 兜底；验证：`bash scripts/regress/route-coverage.sh` 通过
- [x] 1.5 `routes.txt` 增加五行、`migrations.txt` 增加五条 `transitional` 登记；验证：`bash scripts/regress/path-conservation.sh` 通过
- [x] 1.6 `doc/23-微服务底座操作指南.md` 新增 §11.7 登记临时状态（共享表、无 Flyway、旧实现保留）与退出条件；验证：文档中可查到五条路由、回滚步骤与退出条件

## 2. 消息、公告、模板与投递接口迁移

- [x] 2.1 迁移消息域 `domain` / `application` / `infrastructure` / `interfaces` 四层到 `com.iboot.notify`，包名整体替换且不改类名；验证：`mvn -pl iboot-notify -am compile` 无循环依赖
- [x] 2.2 补齐跨 schema 只读：`SysConfigReader` 读 `iboot_admin.sys_config`、用户查询以 `iboot_admin.sys_user` 限定名读取；验证：公告上限、消息接收人枚举与拆分前一致
- [x] 2.3 迁移 13 个既有测试类到新模块并保持断言不变；验证：`mvn -pl iboot-notify test` 全绿
- [x] 2.4 经网关验证四组接口：`/api/message` 建/发/撤回/收件箱/未读/已读、`/api/message/template` 增删改查、`/api/notice` 发布/撤回/已读、`/api/notification/delivery` 分页与重试；验证：`bash scripts/regress/smoke.sh` 相关段落全部 PASS

## 3. SSE 实时推送与内容检索迁移

- [x] 3.1 迁移 `infrastructure/push`（`SseEmitterManager`、`RedisPushEventBroadcaster`、`PushEndpoint`、`PushProperties`）与 `MessageSentEvent` / `MessageReadEvent`；验证：经网关 `/api/push/connect` 建立连接后发送消息，客户端收到事件
- [x] 3.2 迁移 `infrastructure/search` 全量（索引写入、`ContentIndexListener`、`ContentIndexUpdater`、`ContentIndexInitializer`、`ContentIndexReconcileTask`）与 `SearchController`；验证：新建/撤回公告后 `/api/search` 命中结果与拆分前一致
- [x] 3.3 迁移 5 个内容检索测试类；验证：`mvn -pl iboot-notify test` 全绿
- [x] 3.4 验证重复写入幂等：管理端对账与 notify 索引写入同时运行，文档不重复；验证：ES 中同一 `docType+sourceId(+receiverId)` 只有一条文档

## 4. 审批触发解耦

- [x] 4.1 `iboot-admin` 定义端口 `NotificationPort` 与命令对象，`ApprovalApplicationService` 改为依赖端口；验证：审批相关单测通过，管理端不再直接引用 `NotificationApplicationService`
- [x] 4.2 `iboot-notify` 暴露 `POST /internal/notifications`，校验 `X-Internal-Token`（环境变量注入、缺失即 401），不经网关；验证：直连携带正确令牌返回 200，错误/缺失令牌返回 401，网关访问该路径返回 404
- [x] 4.3 管理端 HTTP 适配器实现端口：事务提交后异步调用、3 次指数退避、失败记录 `notification_trigger_failed_total` 与结构化错误，不向上抛出；验证：停掉 notify 提交请假，审批返回成功且指标计数增加
- [x] 4.4 幂等验证：同一事件 ID 重复调用内部端点，投递记录与站内消息均不重复；验证：对应单测断言 `(eventId, channel, recipient)` 唯一
- [x] 4.5 更新 `doc/20-多渠道通知操作指南.md`：触发方式、内部端点契约、失败补偿与人工重试路径；验证：按文档步骤可从零复现一次审批待办通知

## 5. 回归资产

- [x] 5.1 生成 `scripts/regress/golden/notify.json` 基线并纳入 `regress.sh` 的快照循环；验证：连续导出两次归一化后完全相同
- [x] 5.2 把 `equivalence.sh` / `equivalence.py` 参数化为按服务执行，新增 notify 只读用例（`/api/message/list`、`/api/message/inbox`、`/api/notice/list`、`/api/notice/published`、`/api/search`）；验证：`--record` 后 `--verify` 经网关一致
- [x] 5.3 `smoke.sh` 的实时推送段落改为断言经网关的 SSE 事件内容；验证：停掉 notify 时该用例失败，恢复后通过
- [x] 5.4 `permission-matrix.sh` 增加 `message:list` 与 `notice:list` 两条受限用户用例；验证：无权限用户返回 403
- [x] 5.5 冒烟增加 `/api/notification/delivery` 分页与人工重试用例；验证：重试后状态从 `DEAD` 变为 `PENDING`
- [x] 5.6 `trace-chain.sh` 增加 `--expect iboot-gateway,iboot-notify` 用例；验证：一次消息发送返回贯穿两跳的 TraceID

## 6. 全量回归

- [x] 6.1 启动全部服务与中间件后执行 `bash scripts/regress/regress.sh`；验证：快照、路径守恒、路由覆盖、冒烟、权限矩阵全部 PASS
- [x] 6.2 执行 `bash scripts/regress/equivalence.sh --verify` 与 notify 等价性校验；验证：无差异输出
- [x] 6.3 记录回归结果与已知差异到 `doc/23` 的验收记录小节；验证：文档包含本次回归的执行日期与结论

## 7. 集成验收与回滚演练

- [x] 7.1 验收判据一：登录 → 建消息 → 发送 → SSE 收到 → 已读，全链路经网关完成；验证：手工走查记录
- [x] 7.2 验收判据二：停掉 `iboot-notify`，业务接口返回明确降级、审批提交仍成功；验证：`curl` 输出与告警记录
- [x] 7.3 验收判据三：把五条网关路由改回 `iboot-admin`，切换期间写入的消息在回滚后仍可查询；验证：回滚后列表包含切换后创建的数据
- [x] 7.4 更新 `ROADMAP.md` §4.2 服务状态表（notify 已完成，变更名 `add-notify-service` / `remove-notify-from-admin`）；验证：表格与 `doc/23` 一致
- [x] 7.5 更新 `doc/03-架构设计.md`、`doc/04-功能模块.md`、`doc/10-后端配置详解.md`、`doc/15-SSE 推送集成.md` 的模块归属、配置项与连接地址；验证：文档中的端口、服务名、前缀与实际 `application.yml`、网关路由一致
