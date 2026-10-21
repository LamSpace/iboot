# Proposal

## Why

ROADMAP 阶段 0.3 要求业务数据可检索，且它是阶段 1 RAG 的明确前置（RAG 的 BM25 检索与索引形态直接继承本次产出）。
现状 ES 9.2.3 已随 `docker/docker-compose.yml` 部署，但只承载运行日志（`iboot-logs-*`），公告与站内消息只能按标题做数据库
分页匹配，正文内容无法检索。若不先把索引与检索能力建起来，阶段 1 的 RAG 要同时解决"建索引"和"做检索质量"两件事。

按已确认的范围决策，本变更只覆盖**公告与站内消息**；文件内容检索并入阶段 1，与 RAG 的文档解析、检索前置权限过滤
（R5）一起实现，避免解析器与五级数据权限逻辑写两遍。

## What Changes

- 新建弹性索引 `iboot-content-v1`，使用 ES 内置 `cjk` 分析器（默认发行版自带，无需装插件），与运行日志索引完全隔离。
- **公告**：仅索引 `status=1 且 deleted=0` 的公告；发布、修改、撤回、删除时同步新增或移除索引文档。
- **站内消息**：一条消息按接收人展开为一个文档（ID `message-{messageId}-{receiverId}`）；发送时批量写入，
  用户删除收件箱、撤回消息、管理员删除消息时移除对应文档。接收人隔离由索引结构保证，查询强制带 `receiverId`。
- **写路径**：`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`（复用现有虚拟线程执行器），索引失败只记日志，
  不影响业务事务。
- **对账兜底**：Quartz 任务每 5 分钟扫描最近 10 分钟变更（无状态、幂等、窗口重叠），应用启动后先做一次全量回填；
  事件丢失、进程崩溃、停机期间的变更在 5 分钟内收敛。
- 新增 Flyway `V16`：给 `sys_message_receiver` 增加 `update_time`（使对账能发现用户侧删除），并种入对账任务
  （`sys_job` 一行，可在定时任务界面查看/暂停）。
- 新增 `GET /api/search`：仅需登录，可按 `type` 过滤，返回标题、高亮片段与更新时间；本变更不做前端页面。
- 同步 `ROADMAP.md`：0.3 的验收改为公告 + 消息，文件内容检索移入阶段 1。

## Capabilities

### New Capabilities

- `content-search`: 定义业务内容（公告、站内消息）的索引范围、可见性与接收人隔离规则、事件写入与定时对账的一致性保证，
  以及 `/api/search` 的查询契约与降级行为。

### Modified Capabilities

无。现有 `file-storage` 与 `portal-design-system` 的契约不受影响。

## Impact

- 代码：
  - 新增 `infrastructure/search/`：索引事件、提交后监听器、ES 索引适配器、索引初始化与全量回填、Quartz 对账任务
  - 新增 `application/service/ContentSearchApplicationService`
  - 新增 `interfaces/controller/SearchController` 与响应 DTO
  - 修改 `NoticeApplicationService`、`MessageApplicationService`（在既有事务方法内发布索引事件）
  - 新增 Flyway `V16`；修改 `MessageReceiverMapper.xml` 的 `updateDeleted`
  - 更新 `ROADMAP.md`，并按 R12 同步 `doc/04-功能模块.md`、`doc/06-API 集成.md`、`doc/07-数据库设计.md`
- Elasticsearch：新增索引 `iboot-content-v1`；`iboot-logs-*` 的写入与查询不受影响
- API：新增 `/api/search`；现有接口路径、响应结构与权限不变
- 运行时：ES 不可用时应用仍可启动，搜索按现有 `RunLogApplicationService` 的做法降级为空结果；索引写入失败由对账兜底
- 依赖：不新增第三方依赖（`ElasticsearchClient`、Quartz、Testcontainers 均已在用）
- 测试：新增 ES Testcontainers 集成测试；容器测试沿用 `TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0`
  （本机 Docker Hub 不可达）
- 不在本次范围：文件内容索引与文件数据权限、前端搜索入口、RAG / 向量检索、跨集群与多租户
