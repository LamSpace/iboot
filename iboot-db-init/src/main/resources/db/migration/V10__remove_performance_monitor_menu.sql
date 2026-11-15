-- 删除“运行时监控 > 系统性能监控”菜单及其按钮权限。
-- 该页面读取宿主机指标，容器化与上云后语义失真；服务健康监控统一由 SBA 承担。
DELETE FROM sys_role_menu WHERE menu_id IN (704, 7041);
DELETE FROM sys_menu WHERE id IN (704, 7041);
