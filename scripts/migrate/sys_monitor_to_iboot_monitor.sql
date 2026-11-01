-- 监控与日志服务：审计表搬迁（停机窗口内执行）
--
-- 前提：
--   1. 已用 iboot-monitor 启动一次，由 Flyway 在 iboot_monitor 建好两张空表；
--   2. 已停止全部后端服务，否则窗口内仍有服务向旧表写入，复制后会丢行。
--
-- 两张表在 iboot_monitor 中的结构与 iboot_admin 完全一致（列顺序相同，
-- 由管理端 V1 + V4 + V9 + V12 合并而来），因此可以整表复制。
--
-- 执行：
--   docker exec -i iboot-mysql mysql --default-character-set=utf8mb4 -uroot -proot \
--     < scripts/migrate/sys_monitor_to_iboot_monitor.sql

INSERT INTO iboot_monitor.sys_login_log
    (id, username, dept_id, ip_address, login_location, browser, os, status, message, login_time, deleted)
SELECT id, username, dept_id, ip_address, login_location, browser, os, status, message, login_time, deleted
FROM iboot_admin.sys_login_log;

INSERT INTO iboot_monitor.sys_operate_log
    (id, title, business_type, method, request_method, operator_type, operator_name, dept_name, dept_id,
     url, ip, location, param, result, status, error_msg, operate_time, deleted, cost_time)
SELECT id, title, business_type, method, request_method, operator_type, operator_name, dept_name, dept_id,
       url, ip, location, param, result, status, error_msg, operate_time, deleted, cost_time
FROM iboot_admin.sys_operate_log;

-- 逐表校验：行数与最大主键必须一致，任何不一致都说明复制未完成或存在并发写入
SELECT 'sys_login_log' AS table_name,
       (SELECT COUNT(*) FROM iboot_admin.sys_login_log)   AS admin_rows,
       (SELECT COUNT(*) FROM iboot_monitor.sys_login_log) AS monitor_rows,
       (SELECT COALESCE(MAX(id), 0) FROM iboot_admin.sys_login_log)   AS admin_max_id,
       (SELECT COALESCE(MAX(id), 0) FROM iboot_monitor.sys_login_log) AS monitor_max_id
UNION ALL
SELECT 'sys_operate_log',
       (SELECT COUNT(*) FROM iboot_admin.sys_operate_log),
       (SELECT COUNT(*) FROM iboot_monitor.sys_operate_log),
       (SELECT COALESCE(MAX(id), 0) FROM iboot_admin.sys_operate_log),
       (SELECT COALESCE(MAX(id), 0) FROM iboot_monitor.sys_operate_log);
