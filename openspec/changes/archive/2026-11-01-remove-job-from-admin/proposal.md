# Proposal

## Why

`add-job-service` 已交付：定时任务与数据备份的全部外部流量已经网关切到 `iboot-job`，新服务在双实例下跑过集群调度、两种存储介质各完成一次备份、故障隔离与链路追踪均已验收。管理端仍保留整套旧实现（62 个 Java 文件、16 个测试类、4 个 mapper XML），并继续持有 Quartz 与备份配置——这是绞杀者过渡期的临时状态：

- 同一份业务存在两套代码与两套测试，任何相关改动都要改两处，且极易漏改；
- 四张业务表仍留在 `iboot_admin` schema，与「每服务独立 schema」的路线图目标冲突；
- 管理端仍在启动 Quartz 调度器、加载 `QRTZ_*` 表，而这些任务已全部由新服务承接；
- 备份的密钥与命令配置同时存在于两个服务，误配一处就会得到「改了没生效」的困惑。

过渡期两侧数据源不同，任务列表已经出现分叉（管理端读 `iboot_admin`，新服务读 `iboot_job`）。收敛越晚，搬迁时要合并的差异越多。

## What Changes

- **数据搬迁**：`sys_job`、`sys_job_log`、`db_backup_record`、`db_restore_record` 四张表从 `iboot_admin` 复制到 `iboot_job`。策略是「目标库已有的同 id 行视为更新版本跳过」，因为拆分后新建的任务与备份只写在新库。`QRTZ_*` 不搬迁——它是派生状态，新服务启动时按 `sys_job` 重建触发器。
- **删除管理端旧实现**：62 个 Java 文件、16 个测试类、4 个 mapper XML；连同 `QuartzConfig`、`BackupConfig`、`BackupProperties`、`ScheduleJobInit` 等只服务于旧实现的基础设施。
- **管理端移除 Quartz**：删除 `spring-boot-starter-quartz` 依赖、`spring.quartz.*` 配置块与管理端 schema 中 `QRTZ_*` 表的写入路径。表本身保留一个发布周期，不做删除。
- **管理端移除备份配置**：`iboot.backup.*` 配置块与其测试；备份密钥只存在于调度服务。
- **回归登记收敛**：`migrations.txt` 两条从 `transitional` 改为 `done`，刷新 `golden/admin.json`（预期消失 17 条路径）。
- **文档**：`ROADMAP` 状态表、`doc/23` §11.8、`doc/21` 数据备份操作指南（启动方式改为调度服务）、`doc/07` 的 schema 归属表。

**对外契约不变**：`/api/**` 路径、方法、响应结构、权限码均无变化，前端零改动。

**BREAKING（仅内部）**：回滚方式从「改回网关路由」变为 `git revert` + 数据源指回 `iboot_admin`。项目尚未对外发布，不执行「观察一个发布周期」的收敛。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。本变更是部署归属与数据归属的收敛，不改变任何能力的外部行为。`job-scheduling` 与 `data-backup` 的既有需求继续成立，由契约基线、冒烟与权限矩阵强制。

## Impact

**代码**

- 删除：`iboot-admin` 中 job/backup 的接口、应用、领域与基础设施实现，`infrastructure/job`、`infrastructure/backup`、`QuartzConfig`、`BackupConfig`，以及对应的 DTO、导出工具与 mapper XML。
- 保留：`iboot_admin` 中的菜单与权限码（`job:*`、`backup:record:*`）——它们属于系统域，随 `iboot-system` 迁移。

**数据**

- 四张表复制到 `iboot_job`（`scripts/migrate/sys_job_to_iboot_job.sql`），源表保留一个发布周期。
- `QRTZ_*` 不搬迁；管理端 schema 的同名表保留但不再使用。

**依赖与配置**

- `iboot-admin` 移除 `spring-boot-starter-quartz` 与 `spring.quartz.*`、`iboot.backup.*`。
- `iboot-job` 的配置不变。

**回归资产**

- `migrations.txt` 两条改 `done`；`golden/admin.json` 刷新；冒烟与权限矩阵全部经网关，路径不变。

**已知遗留**

- 已拆服务（file / notify / job）的 `@Log` 操作日志仍会因各自 schema 中没有 `sys_operate_log` 而静默失败。本变更不修复；它是拆分 `iboot-monitor` 之前必须定下来的前置决策，已记录在 `add-job-service` 的归档材料中。
