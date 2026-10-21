# Design

## Context

见 `proposal.md` — Why。与本设计相关的现状：

- ES 9.2.3 已在 compose 中运行（`xpack.security.enabled: false`，`localhost:9200`），目前只有 Logstash 写入的
  `iboot-logs-*`；应用侧用 Spring Boot 自动配置的 `ElasticsearchClient` 裸客户端查询（`RunLogApplicationService`），
  没有引入 Spring Data ES（`repositories.enabled: false`）。
- `@Async` 已启用，默认 `taskExecutor` 是虚拟线程执行器（`AsyncConfig`）；`MessagePushListener` 已示范
  `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 的用法。
- 公告/消息/文件的所有写方法均为 `@Transactional`；三张主表的逻辑删除 SQL 都会 `update_time = NOW()`。
- `sys_message_receiver`（V1 建表）没有 `update_time`，用户侧删除只改 `is_deleted`。
- 定时任务由 `sys_job` 数据驱动，`ScheduleJobInit` 在启动时 `scheduler.clear()` 后重新加载；目前没有种子任务。
- Flyway 已到 `V15`；`sys_file` 有 `@DataScope` 五级部门权限，公告/消息没有。

## Goals / Non-Goals

**Goals:**

- 公告与站内消息的正文可被中文关键字检索，结果按用户可见性隔离。
- 源数据变更在 5 分钟内收敛到索引，事件丢失与停机都能自愈。
- 检索后端故障不影响业务写入，应用仍能启动。
- 为阶段 1 的 BM25 检索留下可复用的索引与查询形态。

**Non-Goals:**

- 文件内容索引、文件数据权限（并入阶段 1）。
- 前端搜索入口（本变更只交付后端 API）。
- 向量检索、RAG、同义词/拼音/繁简转换等检索质量增强。
- 索引分片、副本、ILM 等运维参数调优（单节点开发环境）。

## Decisions

### D1. 单索引 + `docType` 字段

索引名 `iboot-content-v1`，公告与消息共用一套 mapping，用 `docType` 区分。

- **为什么**：统一检索入口只需一次查询；mapping、初始化、对账只有一份代码。
- **备选**：按类型建三个索引——mapping 与对账代码翻三倍，跨类型检索要 multi-index 查询，收益不明显。
- 阶段 1 的 RAG 分块索引是不同粒度（`dense_vector`），会单独建索引，不与本索引冲突。

### D2. 中文分析器用内置 `cjk`

- **为什么**：`cjk` 属于 `analysis-common` 模块，默认发行版自带，零运维；`standard` 会把中文切成单字，召回质量太差。
- **备选**：IK（分词更准，但要自定义 ES 镜像并维护插件生命周期）、`smartcn`（同样是插件）。
- **代价**：bigram 的召回偏高、精确短语能力弱于 IK。先接受，后续若 RAG 检索质量不达标再升级到 IK，届时通过新索引名重灌。

### D3. 文档粒度：公告 1:1，消息按接收人展开

- 公告：一条已发布公告一个文档，ID `notice-{id}`。
- 消息：一条消息 × 一个接收人一个文档，ID `message-{messageId}-{receiverId}`。
- **为什么**：接收人过滤退化为一个 `term(receiverId)`，越权在索引结构上就不可能；不需要把接收人 ID 列表拼进查询。
- **备选**：一条消息一个文档 + `terms(receiverIds)` 过滤——广播消息会产生巨大的 ID 列表；或检索后回库过滤——多一次 DB
  往返，且分页总数会不准。
- **代价**：广播消息按用户数放大文档量。当前用户规模下可接受，作为已知成本记录。

### D4. 写路径：提交后异步，事件只带标识

- 应用服务在既有事务方法内发布 `ContentIndexEvent`，监听器用 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`
  执行，按 ID 重新读库后写入 ES。
- 事件动作只有三种：`REFRESH_SOURCE`（发布/更新/发送）、`DELETE_SOURCE`（撤回/删除整条）、`DELETE_RECEIVER`
  （用户删除收件箱）。
- **为什么**：不会索引未提交的数据；事件体积小；与 `MessagePushListener` 的既有模式一致。
- **备选**：事务内同步写 ES——拉长请求、回滚后留下脏文档；本地消息表/outbox——可靠性更强，但超出路线图对 0.3 的要求，
  且对账已能兜住丢失。
- 监听器捕获所有异常并记录 WARN，**绝不向业务调用方抛出**。

### D5. 对账：无状态窗口 + 启动全量回填

- Quartz 每 5 分钟执行，数据库侧用 `update_time >= NOW() - INTERVAL 10 MINUTE` 取增量（重叠一个窗口），逐条重放
  `REFRESH` / `DELETE`。不持久化 checkpoint。
- 启动后先做一次全量回填；失败则每轮对账重试，直到成功。
- **为什么**：无状态使对账天然幂等、无缓存失效与时钟漂移问题；全量回填覆盖停机期间的变更。
- **备选**：Redis/`sys_config` 存 checkpoint——多一份状态，且 `sys_config` 每次写入会触发二级缓存失效；每 5 分钟全量扫
  ——数据量增长后成本不可控；binlog/Canal——路线图明确不引入。
- **时区坑**：compose 的 MySQL 默认 UTC、JDBC 用 `serverTimezone=UTC`，而 JVM 是本地时区。窗口必须在 SQL 内用 `NOW()`
  计算，不能用 `LocalDateTime.now()` 传参。
- 对账查询必须能读到 `deleted=1` 的行，因此需要新增 `selectChangedSince(...)`（不带 `deleted=0` 条件）。

### D6. 删除感知

- 公告/消息的逻辑删除已更新 `update_time`，窗口对账可直接发现。
- `sys_message_receiver` 新增 `update_time`（V16，`DEFAULT CURRENT_TIMESTAMP`），并只在 `updateDeleted` 里显式赋值。
- **为什么**不直接加 `ON UPDATE CURRENT_TIMESTAMP`：已读回执（`updateRead`）也会触发行更新，会给对账制造大量无意义重放。

### D7. 查询与可见性

- 公告：索引侧只保留已发布公告，查询不需要额外权限条件。
- 消息：查询强制 `docType=message AND receiverId=当前用户`，用户 ID 取自 `SecurityUtils.getCurrentUserId()`。
- 不新增权限码：`/api/notice/published`、`/api/message/inbox` 已经对所有登录用户开放，检索与之保持一致。
- 空白关键字直接返回空页，不执行 `match_all`；非法 `type` 返回 400。

### D8. 降级行为

- 索引初始化与全量回填不阻塞启动；ES 不可用时只记日志。
- 检索捕获 `ElasticsearchException` / `IOException`，返回空结果页，与 `RunLogApplicationService` 的既有做法一致。
- 索引监听器失败后不重试，交给 5 分钟对账。

### D9. 代码落位

- `infrastructure/search/`：`ContentIndexEvent`、`ContentIndexListener`、`ElasticsearchContentIndex`、
  `ContentIndexInitializer`、`ContentIndexReconcileTask`。
- 事件放在 infrastructure 与现有 `infrastructure/push/MessageSentEvent` 一致；应用服务已 import 该层，不新增反向依赖方向。
- `application/service/ContentSearchApplicationService` 负责查询组装与可见性；`interfaces/controller/SearchController`
  只做参数校验与响应包装，沿用 `Result` / `PageResult` / `@GetMapping(version = "1")`。

### D10. 对账任务通过 `sys_job` 注册

- V16 插入一行 `sys_job`，`invoke_target = contentIndexReconcileTask.reconcile`，cron `0 */5 * * * ?`。
- **为什么**：与项目现有的数据驱动任务管理一致，可在界面里查看和暂停；`@Scheduled` 无法从 UI 管理。

### D11. 幂等与乱序

- 确定性文档 ID + 覆盖写，使重复处理不产生重复结果。
- 事件与对账可能乱序到达，采用"最后写入者胜"；偏差由下一轮对账修正，最坏一个窗口（≤5 分钟）。
- 不使用 ES 外部版本号：它要求可比较的版本值并会改变删除语义，当前收益不抵复杂度。

### D12. 接口形态

```
GET /api/search?keyword=绩效&type=notice&pageNum=1&pageSize=10
-> Result<PageResult<SearchHitResponse>>
   SearchHitResponse { docType, sourceId, title, highlights[], updatedAt }
```

- `type` 可省略（公告 + 消息），取值仅 `notice` / `message`；`pageSize` 上限 100。
- 高亮只返回片段数组，不返回整段正文，避免无前端时的大响应。

## Risks / Trade-offs

- **`cjk` 相关性不足** → 先用测试集验证中文命中；不足时评估 IK（新索引名 + 重灌，不改 API）。
- **广播消息文档放大** → 记录文档量与对账耗时；用户规模上升后再考虑是否只为活跃用户建文档。
- **ES 启动前应用已就绪** → 回填重试 + 检索降级；不阻塞业务。
- **事件乱序/丢失** → 对账兜底，最坏 5 分钟偏差。
- **JVM 与 MySQL 时区不一致** → 对账窗口在 SQL 内计算。
- **虚拟线程异步任务无界** → 单次事件只做批量写；对账串行处理，避免压垮单节点 ES。
- **容器测试依赖本机镜像** → 使用 `TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0`，ES 9.2.3 镜像已在本机。

## Migration Plan

1. 启动中间件：`docker compose -f docker/docker-compose.yml up -d mysql redis elasticsearch minio`；空数据卷由
   `docker/init.sql` 建库，表结构交给 Flyway。
2. 部署应用：Flyway 自动执行 V16（新增列可向后兼容）；启动时幂等创建 `iboot-content-v1` 并触发全量回填。
3. 验证：按 `specs/content-search/spec.md` 的场景逐条验证；先看全量回填后的公告/消息命中，再验证删除与接收人隔离。
4. 回滚：停用 `sys_job` 中的对账任务并回退应用版本即可；新列与索引均不影响旧代码。必要时删除 `iboot-content-v1`。

## Open Questions

无。检索质量是否升级到 IK 属于阶段 1 的评估项，不改变本次 spec 与任务拆分。
