-- 可观测性监控菜单：Prometh监控、Grafana看板、Kibana UI

-- ===================================================================
-- 1. 菜单数据
-- ===================================================================

-- Prometh监控
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (707, 'Prometh监控', 7, 'C', '/monitor/prometheus-monitor', 'monitor/prometheus-monitor/index',
        'monitor:prometheus', 'trend', 8, 1, 1, 'system', NOW());

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (7071, 'Prometh监控查询', 707, 'F', NULL, NULL, 'prometheus:query', NULL, 1, 1, 1, 'system', NOW());

-- Grafana看板
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (708, 'Grafana看板', 7, 'C', '/monitor/grafana-monitor', 'monitor/grafana-monitor/index', 'monitor:grafana',
        'histogram', 9, 1, 1, 'system', NOW());

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (7081, 'Grafana看板查询', 708, 'F', NULL, NULL, 'grafana:query', NULL, 1, 1, 1, 'system', NOW());

-- Kibana UI
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (709, 'Kibana UI', 7, 'C', '/monitor/kibana-monitor', 'monitor/kibana-monitor/index', 'monitor:kibana',
        'data-analysis', 10, 1, 1, 'system', NOW());

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (7091, 'Kibana查询', 709, 'F', NULL, NULL, 'kibana:query', NULL, 1, 1, 1, 'system', NOW());

-- ===================================================================
-- 2. 参数配置（监控工具地址）
-- ===================================================================

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`,
                          `deleted`, `remark`)
VALUES (57, 'Prometheus监控地址', 'sys.prometheus.monitor.url', 'http://localhost:9090', 1, 'system', NOW(), 0,
        'Prometheus监控页面地址');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`,
                          `deleted`, `remark`)
VALUES (58, 'Grafana看板地址', 'sys.grafana.monitor.url', 'http://localhost:3000', 1, 'system', NOW(), 0,
        'Grafana可视化看板地址');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`,
                          `deleted`, `remark`)
VALUES (59, 'Kibana UI地址', 'sys.kibana.monitor.url', 'http://localhost:5601', 1, 'system', NOW(), 0,
        'Kibana日志分析界面地址');

-- ===================================================================
-- 3. 为超级管理员角色分配权限
-- ===================================================================

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 707);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 7071);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 708);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 7081);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 709);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 7091);
