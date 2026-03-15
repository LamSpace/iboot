-- Thanos Query 菜单集成

-- ===================================================================
-- 1. 菜单数据
-- ===================================================================

-- Thanos 查询
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (711, 'Thanos 查询', 7, 'C', '/monitor/thanos-query', 'monitor/thanos-query/index', 'monitor:thanos', 'trend', 12, 1, 1, 'system', NOW());

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (7111, 'Thanos 查询', 711, 'F', NULL, NULL, 'thanos:query', NULL, 1, 1, 1, 'system', NOW());

-- ===================================================================
-- 2. 参数配置（监控工具地址）
-- ===================================================================

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `deleted`, `remark`)
VALUES (63, 'Thanos 查询地址', 'sys.thanos.query.url', 'http://localhost:19192', 1, 'system', NOW(), 0, 'Thanos Query 查询页面地址');

-- ===================================================================
-- 3. 为超级管理员角色分配权限
-- ===================================================================

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 711);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 7111);
