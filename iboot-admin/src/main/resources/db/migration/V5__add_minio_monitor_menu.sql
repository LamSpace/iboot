-- MinIO 对象存储监控菜单和配置

-- 添加 MinIO 控制台菜单
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (706, '对象存储监控', 7, 'C', '/monitor/minio-monitor', 'monitor/minio-monitor/index', 'monitor:minio', 'box', 5,
        1, 1, 'system', NOW());

-- 添加 MinIO 监控权限按钮
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`, `create_time`)
VALUES (7061, 'MinIO监控查询', 706, 'F', NULL, NULL, 'minio:query', NULL, 1, 1, 1, 'system', NOW());

-- 添加 MinIO 控制台地址配置
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`,
                          `deleted`, `remark`)
VALUES (56, 'MinIO控制台地址', 'sys.minio.console.url', 'http://localhost:9001', 1, 'system', NOW(), 0,
        'MinIO管理控制台访问地址');

-- 为超级管理员角色分配 MinIO 监控权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 706);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 7061);
