# Tasks

## 1. 数据搬迁

- [x] 1.1 编写 `scripts/migrate/sys_job_to_iboot_job.sql`：四张表按主键 `INSERT IGNORE` 复制，并把 `AUTO_INCREMENT` 抬到两侧较大值；验证脚本可在同一库上重复执行而不报错
- [x] 1.2 在即将清理的环境上执行脚本；验证四张表两侧行数与 `max(id)` 一致（源表保留）
- [x] 1.3 重启 `iboot-job` 并核对 `QRTZ_TRIGGERS` 条数与 `sys_job` 中未删除任务数一致；验证迁移后任务仍按 cron 触发

## 2. 删除管理端旧实现

- [x] 2.1 删除 `iboot-admin` 中 job/backup 的接口层、应用层、领域层与基础设施实现（62 个 Java 文件）与 4 个 mapper XML；验证 `grep -rn "com.iboot.admin.domain.job\|domain.backup" iboot-admin/src` 无残留引用
- [x] 2.2 删除对应测试类（16 个）并确认 `mvn -pl iboot-admin test` 通过
- [x] 2.3 删除 `QuartzConfig`、`BackupConfig`、`BackupProperties` 等只服务于旧实现的基础设施；验证管理端启动日志不再出现 Quartz 相关输出
- [x] 2.4 移除 `spring-boot-starter-quartz` 依赖与 `spring.quartz.*`、`iboot.backup.*` 配置块；验证 `mvn -pl iboot-admin package -DskipTests` 成功且 `iboot-admin/target` 中不再含 quartz 相关 jar
- [x] 2.5 更新 `doc/21-数据备份与恢复操作指南.md`：启动方式、环境变量与排障入口改为调度服务；验证文档中的命令按原样可执行

## 3. 回归登记与基线

- [x] 3.1 `scripts/regress/migrations.txt` 两条从 `transitional` 改为 `done`；验证 `path-conservation.sh` 在管理端已无这些路径时通过
- [x] 3.2 刷新 `scripts/regress/golden/admin.json`（预期消失 17 条 job/backup 路径）；验证其余服务的基线未变
- [x] 3.3 全量 `regress.sh` 通过（契约、路径守恒、路由覆盖、冒烟 28 项、权限矩阵 25 项）
- [x] 3.4 只读等价性：`equivalence.sh --verify --service job` 仍与基线一致

## 4. 文档与状态收敛

- [x] 4.1 更新 `ROADMAP.md` §4.2 状态表（`iboot-job` 改为已完成）与调度服务小节（去掉「旧实现待删除」）
- [x] 4.2 更新 `doc/23` §11.8：把「管理端旧实现保留」改为已完成，登记源表保留周期与清理项；补充回滚方式的变化
- [x] 4.3 更新 `doc/07` 的 schema 归属表：`iboot_admin` 不再持有这四张表的写入职责
- [x] 4.4 更新 `doc/23` §5.1 基线数字（管理端路径数 99 → 83）

## 5. 集成验收

- [x] 5.1 清理运行：全栈起齐后执行 `regress.sh`，验证全部通过
- [x] 5.2 故障隔离复验：停 `iboot-job` 后 `/api/job/**` 与 `/api/monitor/backup/**` 返回可辨识的失败，管理端其余接口正常（此时已无旧实现可兜底，必须经网关降级）
- [x] 5.3 端到端：经网关完成「任务列表 → 立即执行 → 执行日志 → 备份触发 → 备份汇总」，全程不改前端
- [x] 5.4 归档前执行 `openspec validate remove-job-from-admin --strict` 与 `openspec status --change remove-job-from-admin`，确认产物齐全且校验通过
