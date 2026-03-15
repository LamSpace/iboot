-- 告警管理菜单：Alertmanager

-- ===================================================================
-- 1. 菜单数据
-- ===================================================================

-- Alertmanager 告警管理
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (710, 'Alertmanager', 7, 'C', '/monitor/alertmanager', 'monitor/alertmanager/index', 'monitor:alertmanager', 'warning-filled', 11, 1, 1, 'system', NOW());

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (7101, 'Alertmanager 查询', 710, 'F', NULL, NULL, 'alertmanager:query', NULL, 1, 1, 1, 'system', NOW());

-- ===================================================================
-- 2. 参数配置（监控工具地址）
-- ===================================================================

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `deleted`, `remark`)
VALUES (60, 'Alertmanager 告警管理地址', 'sys.alertmanager.monitor.url', 'http://localhost:9093', 1, 'system', NOW(), 0, 'Alertmanager 告警管理页面地址');

-- ===================================================================
-- 3. 为超级管理员角色分配权限
-- ===================================================================

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 710);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 7101);
