# Tasks

## 1. 环境与范围同步

- [x] 1.1 启动中间件并确认健康：`docker compose -f docker/docker-compose.yml up -d mysql redis elasticsearch minio`，验证 `curl -s localhost:9200/_cluster/health` 返回 green/yellow，且 `iboot-mysql` 健康（空数据卷由 `docker/init.sql` 建库）
- [x] 1.2 建立测试基线：`TESTCONTAINERS_RYUK_CONTAINER_IMAGE=testcontainers/ryuk:0.12.0 mvn -s /home/lam/repo/settings.xml clean verify`，验证现有 9 个测试全绿并记录结果
- [x] 1.3 更新 `ROADMAP.md` 阶段 0.3 的验收判据（公告 + 消息，文件内容检索移入阶段 1），验证文档中不再把文件内容列为 0.3 的完成条件

## 2. 数据库迁移与增量查询

- [x] 2.1 新增 Flyway `V16`：`sys_message_receiver` 增加 `update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP` 与索引，并插入对账任务行（`invoke_target = contentIndexReconcileTask.reconcile`，cron `0 */5 * * * ?`），验证新库启动后 `flyway_schema_history` 含 V16 且 `sys_job` 中存在该任务
- [x] 2.2 修改 `MessageReceiverMapper.xml` 的 `updateDeleted`，显式写入 `update_time = NOW()`（不使用 `ON UPDATE`，避免已读回执触发重放），验证执行收件箱删除后该行 `update_time` 发生变化
- [x] 2.3 为 `NoticeMapper`、`MessageMapper`、`MessageReceiverMapper` 新增 `selectChangedSince` 查询（包含 `deleted = 1` 的行，窗口在 SQL 内用 `NOW() - INTERVAL 10 MINUTE` 计算），验证对账调用能取到已逻辑删除的记录
- [x] 2.4 同步 `doc/07-数据库设计.md` 记录 `sys_message_receiver.update_time` 与 V16 用途，验证文档描述与实际表结构一致

## 3. 索引与写入路径

- [x] 3.1 实现索引文档组装（公告 → 单文档、消息 → 按接收人展开；未发布/已删除不产出文档）并编写单元测试，验证各状态映射与文档 ID 规则
- [x] 3.2 实现 `ElasticsearchContentIndex`（幂等建索引与 mapping、批量 upsert、按文档 ID 删除、按 `sourceId` 删除）并编写 Testcontainers ES 集成测试，验证索引幂等创建、中文关键字能命中、重复写入不产生重复结果
- [x] 3.3 实现 `ContentIndexEvent` 与 `ContentIndexListener`（`AFTER_COMMIT` + `@Async`，异常只记日志不外抛）并编写单元测试，验证 ES 抛异常时监听器不向调用方传播
- [x] 3.4 在 `NoticeApplicationService` 与 `MessageApplicationService` 的所有变更路径（发布/修改/撤回/删除/发送/收件箱删除）发布对应事件，验证单元测试断言每个路径都发布了正确动作
- [x] 3.5 在 `doc/04-功能模块.md` 增加检索能力说明，验证文档覆盖公告与消息的可见性规则

## 4. 检索 API

- [x] 4.1 实现 `ContentSearchApplicationService`（空白关键字返回空页、非法 `type` 报错、消息强制 `receiverId`、高亮片段、ES 故障降级为空结果）并编写单元测试，验证上述分支
- [x] 4.2 实现 `SearchController` 与响应 DTO，沿用 `Result` / `PageResult` / `@GetMapping(version = "1")`，`pageSize` 上限 100；验证未登录返回未认证错误、非法类型返回参数错误、合法请求返回分页结构
- [x] 4.3 补充检索集成测试（Testcontainers ES），验证类型过滤、接收人隔离（用户 A 搜不到用户 B 的消息）、分页与高亮片段
- [x] 4.4 同步 `doc/06-API 集成.md` 记录 `/api/search` 的参数与响应，验证文档示例可直接调用

## 5. 对账与回填

- [x] 5.1 实现 `ContentIndexReconcileTask`（每轮重放最近 10 分钟变更，逐条 upsert/删除；串行处理），编写单元测试验证公告、消息、收件箱删除三类变更都能被正确重放
- [x] 5.2 实现启动全量回填与失败重试（失败不阻塞启动，下一轮对账继续重试），编写单元测试验证首次失败后后续轮次会重试
- [x] 5.3 验证事件丢失自愈：直接修改数据库中的公告正文（不经过应用），等待一个对账窗口后检索新内容能命中

## 6. 端到端验收与回归

- [x] 6.1 按 `specs/content-search/spec.md` 逐条执行验收场景（公告发布/撤回/删除、消息多接收人隔离、收件箱删除、中文/英文命中、空关键字、未登录、非法类型），记录每条结果
- [x] 6.2 执行故障演练：停止 ES 后发布公告应成功且检索返回空结果；恢复 ES 后 5 分钟内可检索
- [x] 6.3 执行回归：全量 `mvn verify` 绿；运行日志检索（`/api/log/**`）行为不变；应用在已有库上启动完成 V16 升级；`sys_job` 原有任务仍可触发
