# Tasks

## 1. 收敛日志数据访问层

- [x] 1.1 `LoginLogMapper.xml` 与 `OperateLogMapper.xml` 的所有表名改为 `iboot_monitor.sys_login_log` / `iboot_monitor.sys_operate_log`（含统计类聚合查询）；验证：`grep "FROM sys_\|INSERT INTO sys_" iboot-admin/src/main/resources/mapper/{LoginLogMapper,OperateLogMapper}.xml` 无输出
- [x] 1.2 `DashboardApplicationService` 改为直接使用 `LoginLogMapper` / `OperateLogMapper` 的 `count()` 与 `selectPage(0, 5)`，转换方法改为接收 PO；验证：`mvn -pl iboot-admin -am test` 通过，`/api/dashboard/summary` 经网关返回的最近登录/操作日志来自 `iboot_monitor`（与管理端表的行数差异可见）
- [x] 1.3 验证仪表板与统计页在监控服务停止时仍然可用；验证：停 monitor 后 `/api/dashboard/summary` 与 `/api/statistics/report` 返回 200 且数值非零

## 2. 删除管理端旧实现

- [x] 2.1 删除 5 个控制器：`LogController`、`OnlineUserController`、`ServerMonitorController`、`RedisMonitorController`、`MinioMonitorController`；验证：`grep -rn "api/log\|api/online\|api/monitor/server\|api/monitor/redis\|api/monitor/minio" iboot-admin/src/main/java` 无输出
- [x] 2.2 删除 7 个应用服务：`LoginLogApplicationService`、`OperateLogApplicationService`、`RunLogApplicationService`、`OnlineUserApplicationService`、`ServerMonitorApplicationService`、`RedisMonitorApplicationService`、`MinioAdminService`；验证：`mvn -pl iboot-admin -am compile` 成功
- [x] 2.3 删除 7 个 DTO/VO：`OnlineUserResponse`、`ServerInfoResponse`、`RedisMonitorResponse`、`RunLogResponse`、`LoginLogExportVO`、`OperateLogExportVO`、`OnlineUserExportVO`；验证：全仓 `grep` 无引用
- [x] 2.4 删除 2 个仓储接口与 2 个实现：`LoginLogRepository`、`OperateLogRepository`、`LoginLogRepositoryImpl`、`OperateLogRepositoryImpl`；验证：全仓 `grep` 无引用，`LoginLog` 模型仍被 dashboard 使用
- [x] 2.5 删除 `ElasticsearchConfig`，从 `iboot-admin/pom.xml` 移除 `spring-boot-starter-data-elasticsearch` 与 `oshi-core`，从 `application.yml` 移除 `spring.elasticsearch` 配置块；验证：`mvn -pl iboot-admin dependency:tree | grep -E "elasticsearch-client|oshi"` 无输出，启动日志无 ES/OSHI 相关初始化

## 3. 回归登记与基线

- [x] 3.1 `scripts/regress/migrations.txt` 的 5 条监控登记从 `transitional` 改为 `done`；验证：`path-conservation.sh` 通过，且报告 admin 不再出现这些前缀
- [x] 3.2 用 `mvn -pl iboot-admin -am clean package` 重新构建（必须 `clean`，避免已删类的 class 残留在 `target/classes`）；验证：jar 内不再包含 5 个已删控制器，直连管理端 `/api/log/login/list` 不再返回数据（由全局异常处理给出错误响应），经网关仍由监控服务返回 200
- [x] 3.3 刷新 `golden/admin.json`（预期从 76 条降到 57 条），保持其它服务的基线不变；验证：`regress.sh` 的契约与路径守恒两项通过，总路径数仍为 156

## 4. 文档

- [x] 4.1 `doc/23` §11.10 的「管理端旧实现」行改为已删除，并补一段删除清单与 dashboard/statistics 的跨库读债务；验证：章节含删除文件数与退出条件
- [x] 4.2 `ROADMAP.md` §4.2 状态表把 `iboot-monitor` 标为已完成（旧实现已删除），补一条 `remove-monitor-from-admin` 记录；验证：状态表与变更名一致
- [x] 4.3 `README.md` 项目结构中管理端的职责描述同步；`doc/07` schema 归属表中 `iboot_admin` 的备注更新；验证：两处不再宣称管理端承载日志页

## 5. 集成验收

- [x] 5.1 全量回归：`bash scripts/regress/regress.sh` 全绿，冒烟与权限矩阵条目数与拆分后一致（44 / 34）
- [x] 5.2 故障隔离双向复验：停 monitor 时日志接口返回 503 而降级，仪表板与统计仍 200；停 admin 时不做数据范围过滤的接口（如在线人数）仍 200，而带数据范围的日志查询按「失败关闭」返回错误而不是不过滤地返回全部记录
- [x] 5.3 管理端启动面收敛：记录启动耗时与 RSS，确认日志中无 ElasticsearchClient、无 oshi、无 Flowable；验证：`grep -ciE "elasticsearch|oshi|flowable" logs/iboot-admin.log` 为 0
