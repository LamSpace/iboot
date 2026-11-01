-- 监控与日志服务：切流后终检
--
-- 判定：admin_count > monitor_count 表示旧库可能仍有切流后的新行（RESIDUAL）。
-- 正常情况下，切换目标 schema 并重启全部服务后，旧表不再增长。
-- 若出现 RESIDUAL，说明还有服务把写入目标指向 iboot_admin：
--   grep -rn "operate-log-table" --include=application.yml .
--
-- 执行：
--   docker exec -i iboot-mysql mysql -uroot -proot < scripts/migrate/monitor_final_check.sql

SELECT table_name, admin_rows, monitor_rows,
       CASE WHEN admin_rows > monitor_rows THEN 'RESIDUAL' ELSE 'OK' END AS verdict
FROM (
  SELECT 'sys_login_log' AS table_name,
         (SELECT COUNT(*) FROM iboot_admin.sys_login_log)   AS admin_rows,
         (SELECT COUNT(*) FROM iboot_monitor.sys_login_log) AS monitor_rows
  UNION ALL
  SELECT 'sys_operate_log',
         (SELECT COUNT(*) FROM iboot_admin.sys_operate_log),
         (SELECT COUNT(*) FROM iboot_monitor.sys_operate_log)
) t;
