-- iBoot 后台管理系统 - 添加导出功能权限
-- 为所有模块添加导出权限菜单项

-- ===================================================================
-- 添加导出权限菜单项
-- ===================================================================

-- 用户导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (1006, '用户导出', 100, 'F', NULL, NULL, 'user:export', NULL, 6, 1, 1, 'system');

-- 角色导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (1015, '角色导出', 101, 'F', NULL, NULL, 'role:export', NULL, 5, 1, 1, 'system');

-- 菜单导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (1025, '菜单导出', 102, 'F', NULL, NULL, 'menu:export', NULL, 5, 1, 1, 'system');

-- 字典导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (1035, '字典导出', 103, 'F', NULL, NULL, 'dict:export', NULL, 5, 1, 1, 'system');

-- 参数配置导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (1045, '参数导出', 104, 'F', NULL, NULL, 'config:export', NULL, 5, 1, 1, 'system');

-- 部门导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (2005, '部门导出', 200, 'F', NULL, NULL, 'dept:export', NULL, 5, 1, 1, 'system');

-- 岗位导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (2015, '岗位导出', 201, 'F', NULL, NULL, 'post:export', NULL, 5, 1, 1, 'system');

-- 登录日志导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (3003, '登录日志导出', 300, 'F', NULL, NULL, 'loginlog:export', NULL, 3, 1, 1, 'system');

-- 操作日志导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (3013, '操作日志导出', 301, 'F', NULL, NULL, 'operatelog:export', NULL, 3, 1, 1, 'system');

-- 公告导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (4006, '公告导出', 400, 'F', NULL, NULL, 'notice:export', NULL, 6, 1, 1, 'system');

-- 消息导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (4020, '消息导出', 401, 'F', NULL, NULL, 'message:export', NULL, 11, 1, 1, 'system');

-- 文件导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (504, '文件导出', 500, 'F', NULL, NULL, 'file:export', NULL, 4, 1, 1, 'system');

-- 在线用户导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (7003, '在线用户导出', 700, 'F', NULL, NULL, 'online:export', NULL, 3, 1, 1, 'system');

-- 定时任务导出
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
VALUES (808, '任务导出', 8, 'F', NULL, NULL, 'job:export', NULL, 8, 1, 1, 'system');

-- ===================================================================
-- 为超级管理员角色分配导出权限
-- ===================================================================

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 1006);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 1015);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 1025);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 1035);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 1045);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 2005);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 2015);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 3003);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 3013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 4006);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 4020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 504);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 7003);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 808);
