-- 门户新增“微服务治理”入口：Nacos 控制台与 Sentinel 控制台。
--
-- 开发环境默认内嵌（embed=true）；非本地部署在参数配置中把两个 embed 改为 false，
-- 门户只保留内网入口跳转。Nacos 地址必须指向 /next/，根路径的 302 带 X-Frame-Options: DENY。

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`,
                        `icon`, `order_num`, `is_frame`, `is_cache`, `visible`, `status`, `create_by`, `deleted`)
VALUES (9600, '微服务治理', 0, 'M', '/governance', NULL, NULL,
        'operation', 9, 0, 1, 1, 1, 'system', 0);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`,
                        `icon`, `order_num`, `is_frame`, `is_cache`, `visible`, `status`, `create_by`, `deleted`)
VALUES (9601, 'Nacos 控制台', 9600, 'C', '/governance/nacos', 'monitor/nacos-console/index',
        'governance:nacos', 'connection', 1, 0, 1, 1, 1, 'system', 0);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`,
                        `icon`, `order_num`, `is_frame`, `is_cache`, `visible`, `status`, `create_by`, `deleted`)
VALUES (9602, 'Sentinel 控制台', 9600, 'C', '/governance/sentinel', 'monitor/sentinel-dashboard/index',
        'governance:sentinel', 'warning-filled', 2, 0, 1, 1, 1, 'system', 0);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 9600), (1, 9601), (1, 9602);

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`)
VALUES ('Nacos 控制台地址', 'sys.nacos.console.url', '/nacos-console/', 1, 'system',
        '开发环境经门户同源代理 /nacos-console/ 内嵌；非本地部署改为内网绝对地址并关闭内嵌')
ON DUPLICATE KEY UPDATE `config_name` = 'Nacos 控制台地址',
                        `remark` = '开发环境经门户同源代理 /nacos-console/ 内嵌；非本地部署改为内网绝对地址并关闭内嵌';

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`)
VALUES ('Nacos 控制台内嵌', 'sys.nacos.console.embed', 'true', 1, 'system',
        '开发环境内嵌控制台；非本地部署改为 false，门户只保留内网入口跳转')
ON DUPLICATE KEY UPDATE `config_name` = 'Nacos 控制台内嵌',
                        `remark` = '开发环境内嵌控制台；非本地部署改为 false，门户只保留内网入口跳转';

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`)
VALUES ('Sentinel 控制台地址', 'sys.sentinel.dashboard.url', 'http://localhost:8858', 1, 'system',
        'Sentinel Dashboard 访问地址')
ON DUPLICATE KEY UPDATE `config_name` = 'Sentinel 控制台地址',
                        `remark` = 'Sentinel Dashboard 访问地址';

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`)
VALUES ('Sentinel 控制台内嵌', 'sys.sentinel.dashboard.embed', 'true', 1, 'system',
        '开发环境内嵌控制台；非本地部署改为 false，门户只保留内网入口跳转')
ON DUPLICATE KEY UPDATE `config_name` = 'Sentinel 控制台内嵌',
                        `remark` = '开发环境内嵌控制台；非本地部署改为 false，门户只保留内网入口跳转';
