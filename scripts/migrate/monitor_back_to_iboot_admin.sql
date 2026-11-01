-- 监控与日志服务：回滚时把切换后的审计记录复制回管理端
--
-- 使用场景：切换目标 schema 之后验收未通过，决定把写入目标改回 iboot_admin。
-- 管理端的旧实现仍然保留（绞杀者过渡期），两张表也还在，因此只需补齐切换后新增的行。
--
-- 执行前必须先停止全部后端服务，否则复制期间仍会有新行写入。
-- 使用 INSERT IGNORE：主键已存在的行跳过，只补切换后新增的记录。
--
-- 执行：
--   docker exec -i iboot-mysql mysql --default-character-set=utf8mb4 -uroot -proot \
--     < scripts/migrate/monitor_back_to_iboot_admin.sql

INSERT IGNORE INTO iboot_admin.sys_login_log
    (id, username, dept_id, ip_address, login_location, browser, os, status, message, login_time, deleted)
SELECT id, username, dept_id, ip_address, login_location, browser, os, status, message, login_time, deleted
FROM iboot_monitor.sys_login_log;

INSERT IGNORE INTO iboot_admin.sys_operate_log
    (id, title, business_type, method, request_method, operator_type, operator_name, dept_name, dept_id,
     url, ip, location, param, result, status, error_msg, operate_time, deleted, cost_time)
SELECT id, title, business_type, method, request_method, operator_type, operator_name, dept_name, dept_id,
       url, ip, location, param, result, status, error_msg, operate_time, deleted, cost_time
FROM iboot_monitor.sys_operate_log;

SELECT 'sys_login_log' AS table_name,
       (SELECT COUNT(*) FROM iboot_monitor.sys_login_log) AS monitor_rows,
       (SELECT COUNT(*) FROM iboot_admin.sys_login_log)   AS admin_rows
UNION ALL
SELECT 'sys_operate_log',
       (SELECT COUNT(*) FROM iboot_monitor.sys_operate_log),
       (SELECT COUNT(*) FROM iboot_admin.sys_operate_log);
