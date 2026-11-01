# Tasks

## 1. 模块骨架与接入约定

- [x] 1.1 根 `pom.xml` 增加 `<module>iboot-job</module>`，新建 `iboot-job/pom.xml`（依赖 `iboot-kernel`、`spring-boot-maven-plugin`）；验证 `mvn -s /home/lam/repo/settings.xml -pl iboot-job -am package -DskipTests` 成功，且 `unzip -l target/*.jar` 能看到 `Main-Class`
- [x] 1.2 新增启动类与 `application.yml`（端口 8085、服务名 `iboot-job`、数据源 `iboot_job`、Redis、`jwt.*`、actuator）；验证启动后 `curl localhost:8085/actuator/health` 返回 `UP`
- [x] 1.3 按 `doc/23` §11.1 补齐接入件：安全白名单、`@MapperScan`、`ApiVersionConfig`、`CloudEventResponseBodyAdvice`（`basePackages` 指向本服务 controller 包）；验证任一接口返回 CloudEvent 信封且带 `data.data` 结构，带 `Accept-Version: 1` 请求正常
- [x] 1.4 接入 Nacos 注册与 OTLP 追踪（`spring.config.import`，不用 `bootstrap.yml`），并逐项核对 §11.2 的四个隐式契约（Redis 序列化、CloudEvent 信封、API 版本策略、`jwt.secret` 逐字一致）；验证日志出现 Nacos 注册完成，且管理端签发的令牌可直接访问本服务
- [x] 1.5 更新 `scripts/dev-init.sh` 与 `.env.example`：增加 `iboot_job` schema 创建、服务启动与端口；验证 `scripts/dev-init.sh` 一键拉起后本服务健康
- [x] 1.6 在 `doc/23` 新增 §11.8「`iboot-job` 的服务状态」，登记端口、schema、临时状态与退出条件（骨架部分）；验证章节内容与实际配置逐项一致

## 2. 定时任务域迁移

- [x] 2.1 迁入 `domain/job` 与 `infrastructure/job`（`AbstractQuartzJob`、`QuartzJobExecution`、`QuartzDisallowConcurrentExecution`、`ScheduleUtils`、`JobInvokeUtil`、`ScheduleJobInit`、`SampleTask`），并迁移对应单测；验证 `mvn -pl iboot-job test -Dtest='*Job*,*Schedule*,*Cron*'` 通过
- [x] 2.2 迁入 `application/service/JobApplicationService`、`application/mapper/job/JobMapper`、`infrastructure/persistence`（`JobPO`、`JobLogPO`、mapper 接口与 XML）、`JobRepositoryImpl`、`JobLogRepositoryImpl`；验证相关单测通过
- [x] 2.3 迁入 `JobController`、请求/响应 DTO、`JobExportVO` 与导出工具；验证 `/api/job/list`、`/api/job/{id}`、`/api/job/log/list`、`/api/job/export` 的响应结构与当前管理端逐字段一致
- [x] 2.4 迁入 `infrastructure/job` 与管理端的全部 job 测试类并全部跑通；验证 `mvn -pl iboot-job test` 零失败零错误

## 3. 数据备份域迁移

- [x] 3.1 迁入 `domain/backup` 与 `application/service/backup`（`BackupApplicationService`、`RestoreDrillService`、`RestoreTargetGuard`、`BackupSummary`）；验证相关单测通过
- [x] 3.2 迁入 `infrastructure/backup`（`BackupTask`、`BackupCommandFactory`、`MysqldumpBackupExecutor`、`ProcessCommandRunner`、`BackupCipher`、`BackupErrorSanitizer`）与配套单测；验证 `mvn -pl iboot-job test -Dtest='*Backup*,*Restore*'` 通过
- [x] 3.3 迁入 `BackupController`、`backup` DTO 与 `db_backup_record` / `db_restore_record` 的 PO/mapper/RepositoryImpl；验证 `/api/monitor/backup/records`、`/restores`、`/summary` 的响应结构与当前管理端一致
- [x] 3.4 迁入存储 SPI 实现（`LocalFileStorageStrategy`、`MinioFileStorageStrategy`、`OssFileStorageStrategy`、`FileStorageConfig` 及 MinIO/OSS 配置项），存储类型以 `iboot_admin.sys_config` 的 `sys.file.storageType` 为准；验证切换 `local` 与 `minio` 两种取值各完成一次备份，产物落在对应介质的 `backup/` 前缀下
- [x] 3.5 备份启用时的启动前检查：密钥有效、存储提供方可用、`docker exec` + mysql 客户端可达，任一不满足即启动失败并指出具体项；验证分别制造三类故障，各次启动失败且错误信息包含 `IBOOT_BACKUP_ENCRYPTION_KEY` 或具体存储/命令原因

## 4. 数据库结构

- [x] 4.1 编写 `iboot-job/src/main/resources/db/migration/V1__initial_schema.sql`：`QRTZ_*`（11 张）、`sys_job`、`sys_job_log`、`db_backup_record`、`db_restore_record`；验证 `SHOW TABLES` 为 16 张，且逐表与 `iboot_admin` 的 `SHOW CREATE TABLE` 比对无结构差异（字符集与注释除外须说明）
- [x] 4.2 初始化 `sys_job` 默认任务（三个示例任务与 `backupTask.run`）与所需字典/菜单数据的迁移策略；验证新库启动后任务列表与当前管理端一致
- [x] 4.3 更新 `doc/07-数据库设计.md`，新增 `iboot_job` schema 及其表归属；验证文档中的表清单与 4.1 的迁移脚本一致

## 5. 数据权限

- [x] 5.1 实现 `PermissionScopeClient`（服务名寻址、调用者令牌、缓存、失败关闭）、`DataScopeInterceptor` 与 `DataScopeSqlBuilder`，并补单测覆盖缓存命中与下游不可用；验证单测通过，且下游不可用时拒绝而非放行
- [x] 5.2 让任务查询与导出应用数据权限（`@DataScope(deptAlias = "j")` 与 mapper XML 表别名一致）；验证受限用户仅见本部门任务，且分页总数与可见条数一致
- [x] 5.3 在 `doc/23` §11.8 补充数据权限现状与退出条件（待 `iboot-system` 拆分后改为调用系统服务）

## 6. Quartz 集群

- [x] 6.1 配置 `org.quartz.jobStore.isClustered: true`、`instanceId: AUTO` 与线程池参数；验证启动后 `QRTZ_SCHEDULER_STATE` 出现本实例记录
- [x] 6.2 双实例验收：以 8085 与 8086 启动两个实例并启用一条每分钟触发的任务，观察 5 分钟；验证每个触发时刻只有一条执行日志，强杀其中一个实例后任务继续被另一个实例触发
- [x] 6.3 在 `doc/23` §11.8 记录集群配置、数据库时钟与 `QRTZ_LOCKS` 的注意事项，以及回退到单实例的方式

## 7. AI 智能体的服务间调用

- [x] 7.1 `iboot-ai/pom.xml` 增加 `spring-cloud-starter-loadbalancer`，并在 `AgentToolConfig` 中提供 `@LoadBalanced RestClient.Builder`；验证 `mvn -pl iboot-ai dependency:tree` 中出现该依赖，且既有测试全部通过
- [x] 7.2 把 `adminRestClient` 端点改为服务名 `http://iboot-admin`（配置项改为服务名默认值），确认 `AdminApiClient` 与 `AdminOrgSnapshotClient` 均可用；验证经网关发起一次「查用户」智能体调用返回正确结果
- [x] 7.3 新增 `JobApiClient`（端点 `http://iboot-job`，方法 `backupSummary` / `triggerBackup`），`QueryBackupSummaryTool` 与 `TriggerBackupTool` 改为依赖它；验证两个工具的既有单测通过，且端到端可查询备份汇总并成功触发一次备份
- [x] 7.4 在 `doc/22-AI 工作台操作指南.md` 补充「智能体工具的服务调用约定」：按服务名经 Nacos 发现、不回穿网关、新增工具服务时的接入方式

## 8. 网关路由与回归资产

- [x] 8.1 `iboot-gateway` 增加两条路径级路由：`path=/api/monitor/backup/**` 与 `path=/api/job/**` 指向 `iboot-job`，`local` profile 用静态地址、`nacos` profile 用 `lb://iboot-job`，顺序均排在 admin 兜底之前；验证 `bash scripts/regress/route-coverage.sh` 通过
- [x] 8.2 `scripts/regress/routes.txt` 增加 `/api/monitor/backup iboot-job` 与 `/api/job iboot-job`；`scripts/regress/migrations.txt` 增加同样两条并标记 `transitional`；验证 `bash scripts/regress/path-conservation.sh` 通过
- [x] 8.3 生成 `scripts/regress/golden/job.json` 基线，并把 job 的只读接口纳入 `equivalence.sh`（任务列表、任务详情、执行日志列表、备份记录、还原记录、备份汇总）；验证 `--record` 与 `--verify` 结果归一化后完全一致
- [x] 8.4 `smoke.sh` 增加任务执行链路（登录 → 建一条停用任务 → 立即执行 → 执行日志出现记录 → 删除）与备份链路（触发备份 → 轮询变为成功 → 汇总计数变化）；验证 `bash scripts/regress/smoke.sh` 全部通过
- [x] 8.5 `permission-matrix.sh` 增加 `job:list`、`job:run`、`backup:record:list`、`backup:record:create` 的允许/拒绝用例，并增加一条受限用户的数据范围用例；验证矩阵全部通过
- [x] 8.6 更新 `scripts/regress/trace-chain.sh` 的用法示例，确认一次任务请求覆盖 `iboot-gateway → iboot-job`；验证 `bash scripts/regress/trace-chain.sh <traceId> --expect iboot-gateway,iboot-job` 通过
- [x] 8.7 更新 `doc/23` §7.3 路由表（新增两行并说明路径级登记的原因）、§12.1 的 `migrations.txt` 说明、§5 的基线数字；验证文档与 `routes.txt` / `migrations.txt` 内容一致

## 9. 集成验收

- [x] 9.1 改造前先跑一次绿色基线：全栈起齐后执行 `bash scripts/regress/regress.sh`；验证全部通过（这是后续所有比对的前提）
- [x] 9.2 故障隔离双向验收：停掉 `iboot-job` 后 `/api/job/**` 与 `/api/monitor/backup/**` 返回可辨识的失败而 `/api/user/list` 正常；停掉 `iboot-admin` 后已排定任务仍按 cron 执行并留下日志；两条结论各留证据
- [x] 9.3 前端零改动端到端：经网关完成「登录 → 打开定时任务页 → 立即执行 → 查看执行日志 → 打开数据备份页 → 触发备份 → 查看汇总」，全程不修改 `iboot-portal` 源码
- [x] 9.4 更新 `ROADMAP.md` §4.2 的业务服务拆分状态表与 `doc/23` §11.8 的验收记录表（沿用 `iboot-notify` 的验收记录形式），并登记两条已知遗留：过渡期两侧数据分叉、操作日志横切缺陷
- [x] 9.5 归档前执行 `openspec validate add-job-service --strict` 与 `openspec status --change add-job-service`，确认全部产物 `done` 且校验通过
