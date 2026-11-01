# Proposal

## Why

阶段 2 的拆分顺序里，`iboot-job` 排在 `iboot-file` 与 `iboot-notify` 之后。选它不是因为"依赖少"，而是因为它引入了一个前三个服务都没有的形态：**没有请求也会自己干活**。`file` 是无状态 HTTP、`auth` 是协议级服务、`notify` 是被别人触发的入站服务，而 `job` 是**时间驱动**的，且它的任务实现是 JVM 内反射调用 Spring Bean（`JobInvokeUtil` → `SpringUtils.getBean`）。这个形态会直接决定后面 `monitor`（索引对账、指标采集）与 `workflow`（超时升级）怎么处理自己的后台任务，必须先解决一次。

第二个原因是故障半径。数据备份现在与业务主体同进程：一次 `mysqldump` 经 `docker exec` 阻塞、一次全量备份的内存峰值，都会连带管理端不可用。而备份恰恰是"业务最脆弱的时候最需要它"的能力，不该和业务共享同一个 JVM。

业务量是薄的，这一点必须诚实：`sys_job` 里真正在跑的任务只有一条 `backupTask.run`，其余是三个示例任务（通知与索引两个任务已在 V28 逻辑删除，改由各自服务的进程内调度承担）。因此本变更的价值不在"拆出一个庞大的调度服务"，而在于**把时间驱动的执行模型、集群下的单实例语义、以及与宿主机耦合的备份能力一次性从单体里切干净**。

## What Changes

- **新建 `iboot-job` 服务（端口 8085）**，承载两个能力域：
  - 定时任务管理与执行（`/api/job/**`：CRUD、启停、立即执行、cron 校验、执行日志、导出）
  - 数据备份与恢复演练（`/api/monitor/backup/**`：记录、手动触发、恢复演练、汇总）
- **独立 schema `iboot_job`**：`QRTZ_*`（11 张）、`sys_job`、`sys_job_log`、`db_backup_record`、`db_restore_record` 由本服务 Flyway 管理。`iboot_admin` 中的同名表保留一个发布周期供回退对照。
- **Quartz 由 `isClustered: false` 改为 `true`**：任务在多实例下只有一个实例执行，避免拆分后多副本重复触发。
- **数据权限经跨服务获取**：`JobMapper` 上有 4 处 `@DataScope`，新服务沿用 `iboot-file` 已确立的模式——带调用者令牌请求 admin 的 `GET /api/auth/scope`，本地缓存，失败关闭。
- **AI 智能体工具改指向 `iboot-job`**：`QueryBackupSummaryTool` 与 `TriggerBackupTool` 不再经 `AdminApiClient` 访问备份端点，改为新增的 `JobApiClient`。这是项目里**第一处服务间按名调用**：使用 `@LoadBalanced` 客户端与服务名 `http://iboot-job`，而不是静态地址或回穿网关。
- **网关新增两条路径级路由**：`/api/monitor/backup` 与 `/api/job` 指向 `iboot-job`，均排在 admin 兜底之前。`/api/monitor/**` 的其余部分仍归 admin（monitor 尚未拆分），因此路由必须是**路径级**而非前缀级。
- **管理端旧实现保留**（绞杀者过渡期）：`iboot-admin` 中 job/backup 代码原样不动，改回路由即可回滚。删除旧实现与数据搬迁由后续变更 `remove-job-from-admin` 承担。
- **对外契约不变**：`/api/**` 路径、方法、响应结构与权限码全部保持原样，前端零改动。本变更不含 **BREAKING** 变更。

不在本变更范围内（显式记录，避免被当成遗漏）：

- 不解决"其他服务的任务如何登记到本调度器"。当前三个服务的后台任务都用各自进程内的 `@Scheduled` + 锁，`iboot-job` 只调度自己进程里的 Bean。跨服务调度需要 worker 注册或回调模型，是独立课题。
- 不处理 `iboot-file` / `iboot-notify` 调用 admin 的 scope 仍是静态地址这一不一致——它们不动，留作后续统一。

## Capabilities

### New Capabilities

- `job-scheduling`: 定时任务的管理与执行语义——任务定义与 cron 校验、启停与立即执行、执行日志、多实例下的单实例执行保证、任务列表的数据权限范围，以及调度服务独立启停时对业务功能的隔离。

### Modified Capabilities

- `data-backup`: 两处需求变化。(1) 启用与密钥校验的"应用启动失败"现在指的是**调度服务**的启动，管理端不再持有该配置，因此管理端带错误密钥也能正常启动；(2) 定时触发由调度服务自己的 Quartz 承担，备份的并发唯一性约束必须在集群多实例下仍然成立，而不只是在单一进程内成立。

## Impact

**代码**

- 新增模块 `iboot-job`：`JobController`、`BackupController`、`domain/job`、`domain/backup`、`infrastructure/job`（Quartz 执行链与 `JobInvokeUtil`）、`infrastructure/backup`（mysqldump 命令、AES-GCM 加解密、存储、恢复演练）、对应 application service、mapper、DTO、导出工具。
- `iboot-admin` 保留旧实现，本变更不删任何文件；`remove-job-from-admin` 负责删除。
- 随代码迁移的测试类：`JobApplicationService`、`ScheduleUtils`/`CronUtils`、`BackupTask`、`BackupCommandFactory`、`BackupCipher`、`RestoreTargetGuard` 等。
- `iboot-ai` 新增 `JobApiClient`，两个备份工具切换依赖；`AgentToolConfig` 增加负载均衡客户端。

**接口**

- 对外路径零变化，前端零改动。
- 内部新增：`iboot-ai` → `iboot-job` 的服务间调用（Nacos 服务发现）；`iboot-job` → admin 的 `GET /api/auth/scope`。

**依赖**

- 新增 1 个 JVM（本机内存约束 R4 再 +512MB）。
- `iboot-ai` 新增 `spring-cloud-starter-loadbalancer`（当前不在其 classpath 上）。
- 调度服务需要与备份相关的容器访问能力：`docker exec` 调用 `iboot-mysql` 中的 `mysqldump` / `mysql` 客户端，以及 `IBOOT_BACKUP_ENCRYPTION_KEY` 环境变量。
- Nacos 注册、OTLP 追踪、Redis 会话按 `doc/23` §11.1 接入；四个隐式契约（Redis 序列化、CloudEvent 信封、API 版本策略、`jwt.secret` 逐字一致）必须逐项核对。

**数据**

- 新 schema `iboot_job`，16 张表由本服务 Flyway 管理。
- 本变更只建结构，不搬数据；运行期数据搬迁与源表清理在 `remove-job-from-admin` 中完成。

**回归资产**

- `scripts/regress/golden/job.json` 新增基线；`routes.txt` 两条；`migrations.txt` 两条登记为 `transitional`；`equivalence` 扩展到 job 的只读接口；冒烟新增任务执行链路；权限矩阵新增 job/backup 用例；`trace-chain` 增加 `gateway → job`。

**已知遗留**

- 已拆服务（file / notify，以及本变更后的 job）的 `@Log` 操作日志写入会因各自 schema 中没有 `sys_operate_log` 而静默失败（`logs/iboot-file.log` 已可观察到 18 次 `bad SQL grammar` 告警）。这是拆分前就存在的横切缺陷，本变更不引入也不修复，但**必须在拆分 `iboot-monitor` 之前定下操作日志的归属**，因为 monitor 正是该数据的查询方。
