-- 门户新增 Tempo 链路追踪入口，挂在可观测平台（9700）下。
--
-- 界面由 Grafana Explore 提供，数据源 uid 固定为 tempo；菜单路径最后一段
-- 必须与门户路由 tempo-monitor 一致。

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`,
                        `icon`, `order_num`, `is_frame`, `is_cache`, `visible`, `status`, `create_by`, `deleted`)
VALUES (712, 'Tempo 链路追踪', 9700, 'C', '/observability/tempo-monitor', 'monitor/tempo-monitor/index',
        'monitor:tempo', 'connection', 5, 0, 1, 1, 1, 'system', 0);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`,
                        `icon`, `order_num`, `is_frame`, `is_cache`, `visible`, `status`, `create_by`, `deleted`)
VALUES (7121, 'Tempo 链路查询', 712, 'F', NULL, NULL, 'tempo:query',
        NULL, 1, 0, 1, 1, 1, 'system', 0);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 712), (1, 7121);
