-- ===================================================================
-- sys_file 数据搬迁：iboot_admin -> iboot_file
-- -------------------------------------------------------------------
-- 用途：remove-file-from-admin 变更的一次性运维动作。
--       目标库与 sys_file 表结构由 iboot-file 的 Flyway V1 创建，
--       本脚本只做两件事：建库（幂等）与复制数据（显式带 id）。
--
-- 约束：
--   * 只复制、不删除。源表 iboot_admin.sys_file 保持原样，
--     回退时把文件服务的数据源指回 iboot_admin 即可。
--   * 重跑前先执行脚本中标注的 TRUNCATE 语句（默认注释掉），
--     否则显式 id 会撞主键。TRUNCATE 会清空目标表，
--     只允许在「后端已停止、切流窗口内」执行。
--   * 执行顺序：停后端 -> 执行本脚本 -> 校验 -> 启动服务。
--
-- 用法：
--   docker exec -i iboot-mysql mysql -uroot -proot < scripts/migrate/sys_file_to_iboot_file.sql
-- ===================================================================

-- 1. 建库（幂等）：两侧统一字符集与排序规则，避免比较行为漂移
CREATE DATABASE IF NOT EXISTS iboot_file
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. 重跑前清空目标表（默认注释，确认处于停写窗口后手动执行）
-- TRUNCATE TABLE iboot_file.sys_file;

-- 3. 复制数据：显式带 id，MySQL 会把 AUTO_INCREMENT 推到 max(id)+1
INSERT INTO iboot_file.sys_file (id, file_name, file_path, file_size, file_type, file_ext,
                                 file_category, upload_by, upload_time, create_by, create_time,
                                 update_by, update_time, deleted, remark)
SELECT id, file_name, file_path, file_size, file_type, file_ext,
       file_category, upload_by, upload_time, create_by, create_time,
       update_by, update_time, deleted, remark
FROM iboot_admin.sys_file;

-- 4. 校验：两侧行数与最大 id 必须一致，供执行人贴入变更记录
SELECT 'source' AS side, COUNT(*) AS row_count, IFNULL(MAX(id), 0) AS max_id
FROM iboot_admin.sys_file
UNION ALL
SELECT 'target' AS side, COUNT(*) AS row_count, IFNULL(MAX(id), 0) AS max_id
FROM iboot_file.sys_file;
