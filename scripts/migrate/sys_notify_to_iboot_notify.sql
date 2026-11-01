-- ===================================================================
-- 消息与通知数据搬迁：iboot_admin -> iboot_notify
-- -------------------------------------------------------------------
-- 用途：remove-notify-from-admin 变更的一次性运维动作。
--       目标库与六张表的结构由 iboot-notify 的 Flyway V1 创建，
--       本脚本只做三件事：建库（幂等）、清掉种子行、复制数据（显式带 id）。
--
-- 约束：
--   * 只复制、不删除。源表 iboot_admin 中的六张表保持原样，
--     回退时把通知服务的数据源指回 iboot_admin 即可。
--   * 执行前必须先让通知服务启动一次，由 Flyway 建出六张表；
--     执行时必须处于停写窗口（全部后端已停止）。
--   * 重跑前先执行脚本中标注的 TRUNCATE 语句（默认注释掉），
--     否则显式 id 会撞主键。TRUNCATE 会清空目标表，只允许在停写窗口内执行。
--   * 执行顺序：停后端 -> 启动 notify 触发 Flyway -> 停 notify -> 执行本脚本
--     -> 校验 -> 启动全部服务。
--
-- 用法：
--   docker exec -i iboot-mysql mysql -uroot -proot < scripts/migrate/sys_notify_to_iboot_notify.sql
-- ===================================================================

-- 1. 建库（幂等）：两侧统一字符集与排序规则，避免比较行为漂移
CREATE DATABASE IF NOT EXISTS iboot_notify
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. 重跑前清空目标表（默认注释，确认处于停写窗口后手动执行）
-- TRUNCATE TABLE iboot_notify.sys_notification_delivery;
-- TRUNCATE TABLE iboot_notify.sys_message_receiver;
-- TRUNCATE TABLE iboot_notify.sys_message;
-- TRUNCATE TABLE iboot_notify.sys_notice_read;
-- TRUNCATE TABLE iboot_notify.sys_notice;
-- TRUNCATE TABLE iboot_notify.sys_message_template;

-- 3. 复制数据：显式带 id，MySQL 会把 AUTO_INCREMENT 推到 max(id)+1。
--    sys_message_template 的 V1 种子里已有一条 APPROVAL_TODO，先清空再按源库复制，
--    保证两侧 id 完全一致（没有外键引用模板 id）。
DELETE FROM iboot_notify.sys_message_template;

INSERT INTO iboot_notify.sys_message_template (id, template_code, template_name, template_content, message_type,
                                               status, create_by, create_time, update_by, update_time, deleted, remark)
SELECT id, template_code, template_name, template_content, message_type,
       status, create_by, create_time, update_by, update_time, deleted, remark
FROM iboot_admin.sys_message_template;

INSERT INTO iboot_notify.sys_message (id, title, content, message_type, business_type, priority, sender_type,
                                      sender_id, receiver_type, status, create_by, create_time, update_by,
                                      update_time, deleted, remark)
SELECT id, title, content, message_type, business_type, priority, sender_type,
       sender_id, receiver_type, status, create_by, create_time, update_by,
       update_time, deleted, remark
FROM iboot_admin.sys_message;

INSERT INTO iboot_notify.sys_message_receiver (id, message_id, user_id, is_read, read_time, is_deleted, update_time)
SELECT id, message_id, user_id, is_read, read_time, is_deleted, update_time
FROM iboot_admin.sys_message_receiver;

INSERT INTO iboot_notify.sys_notice (id, notice_title, notice_type, notice_content, status, is_top, create_by,
                                     create_time, update_by, update_time, deleted, remark)
SELECT id, notice_title, notice_type, notice_content, status, is_top, create_by,
       create_time, update_by, update_time, deleted, remark
FROM iboot_admin.sys_notice;

INSERT INTO iboot_notify.sys_notice_read (id, notice_id, user_id, read_time)
SELECT id, notice_id, user_id, read_time
FROM iboot_admin.sys_notice_read;

INSERT INTO iboot_notify.sys_notification_delivery (id, event_id, event_type, channel, recipient_key, recipient_id,
                                                    recipient_address, template_code, title, content, params_json,
                                                    status, attempts, max_attempts, next_retry_time, last_error,
                                                    message_id, sent_time, create_time, update_time)
SELECT id, event_id, event_type, channel, recipient_key, recipient_id,
       recipient_address, template_code, title, content, params_json,
       status, attempts, max_attempts, next_retry_time, last_error,
       message_id, sent_time, create_time, update_time
FROM iboot_admin.sys_notification_delivery;

-- 4. 校验：两侧每张表的行数与最大 id 必须一致，供执行人贴入变更记录
SELECT 'sys_message_template' AS table_name, 'source' AS side, COUNT(*) AS row_count, IFNULL(MAX(id), 0) AS max_id
FROM iboot_admin.sys_message_template
UNION ALL
SELECT 'sys_message_template', 'target', COUNT(*), IFNULL(MAX(id), 0) FROM iboot_notify.sys_message_template
UNION ALL
SELECT 'sys_message', 'source', COUNT(*), IFNULL(MAX(id), 0) FROM iboot_admin.sys_message
UNION ALL
SELECT 'sys_message', 'target', COUNT(*), IFNULL(MAX(id), 0) FROM iboot_notify.sys_message
UNION ALL
SELECT 'sys_message_receiver', 'source', COUNT(*), IFNULL(MAX(id), 0) FROM iboot_admin.sys_message_receiver
UNION ALL
SELECT 'sys_message_receiver', 'target', COUNT(*), IFNULL(MAX(id), 0) FROM iboot_notify.sys_message_receiver
UNION ALL
SELECT 'sys_notice', 'source', COUNT(*), IFNULL(MAX(id), 0) FROM iboot_admin.sys_notice
UNION ALL
SELECT 'sys_notice', 'target', COUNT(*), IFNULL(MAX(id), 0) FROM iboot_notify.sys_notice
UNION ALL
SELECT 'sys_notice_read', 'source', COUNT(*), IFNULL(MAX(id), 0) FROM iboot_admin.sys_notice_read
UNION ALL
SELECT 'sys_notice_read', 'target', COUNT(*), IFNULL(MAX(id), 0) FROM iboot_notify.sys_notice_read
UNION ALL
SELECT 'sys_notification_delivery', 'source', COUNT(*), IFNULL(MAX(id), 0) FROM iboot_admin.sys_notification_delivery
UNION ALL
SELECT 'sys_notification_delivery', 'target', COUNT(*), IFNULL(MAX(id), 0) FROM iboot_notify.sys_notification_delivery;
