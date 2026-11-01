-- ===================================================================
-- 调度与备份数据搬迁：iboot_admin -> iboot_job
-- -------------------------------------------------------------------
-- 用途：remove-job-from-admin 变更的一次性运维动作。
--       目标库与四张表的结构由 iboot-job 的 Flyway V1 创建，
--       本脚本只做一件事：把源库的数据复制过来。
--
-- 约束：
--   * 只复制、不删除。源表 iboot_admin 中的四张表保持原样，
--     回退时把调度服务的数据源指回 iboot_admin 即可。
--   * QRTZ_* 不搬迁：它是派生状态，调度服务启动时按 sys_job 重建触发器。
--     搬迁它只会把管理端调度器的运行痕迹带进新库，制造「触发器存在但任务定义不存在」的孤儿。
--   * 冲突策略：
--     - sys_job 按「调用目标」判重。两个 schema 的种子行自增起点不同，同一条逻辑任务
--       （如 backupTask.run）在两侧的 id 可能不同；只按 id 去重会复制出第二条同样的任务。
--       因此仅当目标库没有同 invoke_target 的未删除任务时才复制。
--     - 其余三张表是追加型数据，按 id 去重（INSERT IGNORE）。拆分后新建的记录只写新库，
--       覆盖回去等于丢数据。
--   * 已知代价：sys_job_log.job_id 以源库 id 为准，与目标库中同 invoke_target 的任务 id
--     可能不一致。这是过渡期数据分叉的既知代价，执行日志以时间为准，不影响调度。
--   * 执行前必须先让调度服务启动一次，由 Flyway 建出四张表；
--     执行时必须处于停写窗口（全部后端已停止）。
--   * 执行顺序：停后端 -> 启动 job 触发 Flyway -> 停 job -> 执行本脚本
--     -> 校验 -> 启动全部服务（job 启动时会按新数据重建触发器）。
--
-- 用法：
--   docker exec -i iboot-mysql mysql -uroot -proot < scripts/migrate/sys_job_to_iboot_job.sql
-- ===================================================================

-- 1. 建库（幂等）：两侧统一字符集与排序规则，避免比较行为漂移
CREATE DATABASE IF NOT EXISTS iboot_job
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. 复制数据：显式带 id 列顺序两侧一致，MySQL 会把 AUTO_INCREMENT 推到 max(id)+1
--    sys_job 按调用目标判重，避免同一条任务在两库中以不同 id 各存一份
INSERT IGNORE INTO iboot_job.sys_job
SELECT s.*
FROM iboot_admin.sys_job s
WHERE s.deleted = 1
   OR NOT EXISTS (SELECT 1
                  FROM iboot_job.sys_job j
                  WHERE j.invoke_target = s.invoke_target
                    AND j.deleted = 0);

INSERT IGNORE INTO iboot_job.sys_job_log SELECT * FROM iboot_admin.sys_job_log;

INSERT IGNORE INTO iboot_job.db_backup_record SELECT * FROM iboot_admin.db_backup_record;

INSERT IGNORE INTO iboot_job.db_restore_record SELECT * FROM iboot_admin.db_restore_record;

-- 3. 校验：两侧行数应一致（同 id 冲突的行只算一次），迁移后记录在验收表中
SELECT 'sys_job' AS table_name,
       (SELECT COUNT(*) FROM iboot_admin.sys_job) AS src_rows,
       (SELECT COUNT(*) FROM iboot_job.sys_job) AS dst_rows,
       (SELECT COALESCE(MAX(id), 0) FROM iboot_admin.sys_job) AS src_max_id,
       (SELECT COALESCE(MAX(id), 0) FROM iboot_job.sys_job) AS dst_max_id
UNION ALL
SELECT 'sys_job_log',
       (SELECT COUNT(*) FROM iboot_admin.sys_job_log),
       (SELECT COUNT(*) FROM iboot_job.sys_job_log),
       (SELECT COALESCE(MAX(id), 0) FROM iboot_admin.sys_job_log),
       (SELECT COALESCE(MAX(id), 0) FROM iboot_job.sys_job_log)
UNION ALL
SELECT 'db_backup_record',
       (SELECT COUNT(*) FROM iboot_admin.db_backup_record),
       (SELECT COUNT(*) FROM iboot_job.db_backup_record),
       (SELECT COALESCE(MAX(id), 0) FROM iboot_admin.db_backup_record),
       (SELECT COALESCE(MAX(id), 0) FROM iboot_job.db_backup_record)
UNION ALL
SELECT 'db_restore_record',
       (SELECT COUNT(*) FROM iboot_admin.db_restore_record),
       (SELECT COUNT(*) FROM iboot_job.db_restore_record),
       (SELECT COALESCE(MAX(id), 0) FROM iboot_admin.db_restore_record),
       (SELECT COALESCE(MAX(id), 0) FROM iboot_job.db_restore_record);
