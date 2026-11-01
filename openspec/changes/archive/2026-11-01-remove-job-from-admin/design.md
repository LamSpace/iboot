# Design

## Context

见 `proposal.md` 的 Why。设计需要额外知道的现状：

- `iboot-job` 已独立运行，`iboot_job` 中有 4 条种子任务与拆分后产生的备份记录；管理端 `iboot_admin` 中也有同结构的 4 条种子任务。
- 管理端的 Quartz 由 `QuartzConfig` 注入 DataSource 与事务管理器，`ScheduleJobInit` 在启动时把 `sys_job` 同步进调度器。
- 备份在管理端的入口是 `BackupConfig`（密钥校验）与 `BackupProperties`（数据库与命令配置）。
- 管理端 DDD 四层中 job/backup 是自包含的：`grep` 确认除自身包外无其它管理端代码引用。
- 项目尚未对外发布，`add-job-service` 与 `remove-notify-from-admin` 都是当日完成，不存在需要保护的在线旧版本。

## Goals / Non-Goals

**Goals:**

- 单一实现：job/backup 只存在于 `iboot-job`。
- 管理端不再启动调度器、不再持有备份密钥。
- 数据搬迁可重复执行、可核对。

**Non-Goals:**

- 不删除 `iboot_admin` 中的 `QRTZ_*`、`sys_job` 等表（保留一个发布周期）。
- 不修复已拆服务操作日志静默失败的问题（见 proposal 的已知遗留）。
- 不改 `iboot-job` 的任何行为。

## Decisions

### D1 迁移四张业务表，不迁移 `QRTZ_*`

**决策**：`scripts/migrate/sys_job_to_iboot_job.sql` 只处理 `sys_job`、`sys_job_log`、`db_backup_record`、`db_restore_record`。

**理由**：`QRTZ_*` 是派生状态，`ScheduleJobInit` 在启动时按 `sys_job` 重建 JobDetail 与 Trigger；搬迁它反而会把管理端调度器的运行痕迹带进新库，制造「任务存在但定义不存在」的孤儿。

**代价**：搬迁后需要重启一次 `iboot-job`，让它按新数据重建触发器。

### D2 冲突策略：目标库已有同 id 行视为更新版本

**决策**：`INSERT IGNORE` 语义，按主键跳过已存在的行。

**理由**：拆分后新建的任务与备份只写新库，管理端侧不会产生新数据；反之管理端可能残留拆分前的历史记录。以目标库为准可以避免把过期行覆盖回新库。

**替代方案**：先清空新库再整表搬迁——否决，会丢掉拆分后产生的执行日志与备份记录。

**自增回填**：搬迁后把 `AUTO_INCREMENT` 抬到两侧 `max(id)` 的较大值，避免新插入撞主键。

### D3 删除边界：保留菜单与权限码

**决策**：只删代码与配置，不删 `sys_menu` 中「定时任务」「数据备份」的菜单与按钮行。

**理由**：菜单与权限码属于系统域，随 `iboot-system` 迁移；`iboot-job` 的 `@PreAuthorize` 仍依赖这些码存在于令牌中。

### D4 管理端移除 Quartz 依赖

**决策**：删除 `spring-boot-starter-quartz`、`spring.quartz.*` 配置与 `QuartzConfig`。

**理由**：删除 job 实现后管理端没有任何 `Job` 实现类，保留调度器只会每个实例白占一个线程池并反复扫描空的 `QRTZ_*` 表。

**替代方案**：保留依赖以备将来——否决，YAGNI；将来需要时再加回来成本很低。

### D5 备份配置只留在调度服务

**决策**：删除管理端 `iboot.backup.*` 与 `IBOOT_BACKUP_*` 的读取路径。

**理由**：密钥同时存在于两处时，运维修改其中一处会得到「改了没生效」的困惑，而这恰恰是备份这种「出事才用」的能力最危险的失效方式。

## Risks / Trade-offs

- **搬迁遗漏数据** → 脚本按四张表逐表核对行数与 `max(id)`，并在验收记录中列出两侧数值；源表不删除，可随时重新搬迁。
- **管理端删代码后仍被引用** → 删除前用 `grep` 确认无外部引用；编译期即会暴露遗漏。
- **`QRTZ_*` 表残留** → 表保留但不再被写入，记录在 `doc/23` §11.8 的退出条件里，随清理项一并处理。
- **回滚方式变化** → `git revert` + 数据源指回 `iboot_admin`；因为源表保留，回滚不需要反向搬迁数据。

## Migration Plan

1. 执行 `scripts/migrate/sys_job_to_iboot_job.sql`，核对行数与 `max(id)`。
2. 重启 `iboot-job`（重建触发器）。
3. 删除管理端旧实现与相关配置，重新构建并启动管理端。
4. `migrations.txt` 两条改 `done`，刷新 `golden/admin.json`，跑全量回归。
5. 回滚：`git revert` 本次提交，把管理端数据源指回 `iboot_admin`（源表仍在）。
