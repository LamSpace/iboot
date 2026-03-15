-- iBoot 后台管理系统 - 完整数据库初始数据脚本
-- MySQL 8.0+
-- 包含所有模块的初始化数据
-- 注意：Flyway 管理的迁移脚本不应包含 USE 语句

-- ===================================================================
-- 核心基础数据
-- ===================================================================

-- ----------------------------
-- 1. 初始化用户数据
-- ----------------------------
-- 密码：admin123（BCrypt 加密）
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `gender`, `user_type`, `dept_id`, `status`, `create_by`, `remark`) 
VALUES (1, 'admin', '$2a$10$uAKzTVTib3zNyrwLAZt5UemckqbOMOaBBQfewspf01jKbkhs/ENpW', '管理员', 'admin@iboot.com', '18888888888', 1, 1, 1, 1, 'system', '系统管理员');

-- ----------------------------
-- 2. 初始化角色数据
-- ----------------------------
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `data_scope`, `status`, `create_by`, `remark`) 
VALUES (1, '超级管理员', 'ROLE_ADMIN', 1, 1, 1, 'system', '超级管理员');

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `data_scope`, `status`, `create_by`, `remark`) 
VALUES (2, '普通用户', 'ROLE_USER', 2, 5, 1, 'system', '普通用户');

-- ----------------------------
-- 3. 初始化菜单数据（完整菜单结构）
-- ----------------------------
-- 系统管理
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1, '系统管理', 0, 'M', '/system', NULL, NULL, 'setting', 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (100, '用户管理', 1, 'C', '/system/user', 'system/user/index', 'user:list', 'user', 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1001, '用户查询', 100, 'F', NULL, NULL, 'user:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1002, '用户新增', 100, 'F', NULL, NULL, 'user:add', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1003, '用户修改', 100, 'F', NULL, NULL, 'user:edit', NULL, 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1004, '用户删除', 100, 'F', NULL, NULL, 'user:remove', NULL, 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1005, '用户重置密码', 100, 'F', NULL, NULL, 'user:resetPwd', NULL, 5, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (101, '角色管理', 1, 'C', '/system/role', 'system/role/index', 'role:list', 'peoples', 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1011, '角色查询', 101, 'F', NULL, NULL, 'role:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1012, '角色新增', 101, 'F', NULL, NULL, 'role:add', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1013, '角色修改', 101, 'F', NULL, NULL, 'role:edit', NULL, 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1014, '角色删除', 101, 'F', NULL, NULL, 'role:remove', NULL, 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (102, '菜单管理', 1, 'C', '/system/menu', 'system/menu/index', 'menu:list', 'tree-table', 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1021, '菜单查询', 102, 'F', NULL, NULL, 'menu:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1022, '菜单新增', 102, 'F', NULL, NULL, 'menu:add', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1023, '菜单修改', 102, 'F', NULL, NULL, 'menu:edit', NULL, 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1024, '菜单删除', 102, 'F', NULL, NULL, 'menu:remove', NULL, 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (103, '字典管理', 1, 'C', '/system/dict', 'system/dict/index', 'dict:list', 'dict', 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1031, '字典查询', 103, 'F', NULL, NULL, 'dict:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1032, '字典新增', 103, 'F', NULL, NULL, 'dict:add', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1033, '字典修改', 103, 'F', NULL, NULL, 'dict:edit', NULL, 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1034, '字典删除', 103, 'F', NULL, NULL, 'dict:remove', NULL, 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (104, '参数配置', 1, 'C', '/system/config', 'system/config/index', 'config:list', 'edit', 5, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1041, '参数查询', 104, 'F', NULL, NULL, 'config:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1042, '参数新增', 104, 'F', NULL, NULL, 'config:add', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1043, '参数修改', 104, 'F', NULL, NULL, 'config:edit', NULL, 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (1044, '参数删除', 104, 'F', NULL, NULL, 'config:remove', NULL, 4, 1, 1, 'system');

-- 组织管理
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (2, '组织管理', 0, 'M', '/organization', NULL, NULL, 'tree', 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (200, '部门管理', 2, 'C', '/organization/dept', 'organization/dept/index', 'dept:list', 'tree', 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (2001, '部门查询', 200, 'F', NULL, NULL, 'dept:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (2002, '部门新增', 200, 'F', NULL, NULL, 'dept:add', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (2003, '部门修改', 200, 'F', NULL, NULL, 'dept:edit', NULL, 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (2004, '部门删除', 200, 'F', NULL, NULL, 'dept:remove', NULL, 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (201, '岗位管理', 2, 'C', '/organization/post', 'organization/post/index', 'post:list', 'post', 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (2011, '岗位查询', 201, 'F', NULL, NULL, 'post:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (2012, '岗位新增', 201, 'F', NULL, NULL, 'post:add', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (2013, '岗位修改', 201, 'F', NULL, NULL, 'post:edit', NULL, 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (2014, '岗位删除', 201, 'F', NULL, NULL, 'post:remove', NULL, 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (202, '组织架构图', 2, 'C', '/organization/org-chart', 'organization/org-chart/index', 'orgchart:view', 'chart', 3, 1, 1, 'system');

-- 日志管理
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (3, '日志管理', 0, 'M', '/security', NULL, NULL, 'log', 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (300, '登录日志', 3, 'C', '/security/login-log', 'security/login-log/index', 'loginlog:list', 'logininfor', 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (3001, '登录日志查询', 300, 'F', NULL, NULL, 'loginlog:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (3002, '登录日志删除', 300, 'F', NULL, NULL, 'loginlog:remove', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (301, '操作日志', 3, 'C', '/security/operate-log', 'security/operate-log/index', 'operatelog:list', 'form', 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (3011, '操作日志查询', 301, 'F', NULL, NULL, 'operatelog:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (3012, '操作日志删除', 301, 'F', NULL, NULL, 'operatelog:remove', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (302, '运行日志', 3, 'C', '/security/audit-log', 'security/audit-log/index', 'auditlog:list', 'bug', 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (3021, '运行日志查询', 302, 'F', NULL, NULL, 'auditlog:query', NULL, 1, 1, 1, 'system');

-- 消息管理
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4, '消息管理', 0, 'M', '/message', NULL, NULL, 'message', 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (400, '系统公告', 4, 'C', '/message/notice', 'message/notice/index', 'notice:list', 'notification', 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4001, '公告查询', 400, 'F', NULL, NULL, 'notice:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4002, '公告新增', 400, 'F', NULL, NULL, 'notice:add', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4003, '公告修改', 400, 'F', NULL, NULL, 'notice:edit', NULL, 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4004, '公告删除', 400, 'F', NULL, NULL, 'notice:remove', NULL, 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4005, '公告发布', 400, 'F', NULL, NULL, 'notice:publish', NULL, 5, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (401, '消息中心', 4, 'C', '/message/message-center', 'message/message-center/index', 'message:list', 'chat-dot-round', 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4010, '消息查询', 401, 'F', NULL, NULL, 'message:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4011, '消息新增', 401, 'F', NULL, NULL, 'message:add', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4012, '消息修改', 401, 'F', NULL, NULL, 'message:edit', NULL, 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4013, '消息删除', 401, 'F', NULL, NULL, 'message:remove', NULL, 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4014, '消息发送', 401, 'F', NULL, NULL, 'message:send', NULL, 5, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4015, '模板查询', 401, 'F', NULL, NULL, 'message:template:list', NULL, 6, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4016, '模板详情', 401, 'F', NULL, NULL, 'message:template:query', NULL, 7, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4017, '模板新增', 401, 'F', NULL, NULL, 'message:template:add', NULL, 8, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4018, '模板修改', 401, 'F', NULL, NULL, 'message:template:edit', NULL, 9, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (4019, '模板删除', 401, 'F', NULL, NULL, 'message:template:remove', NULL, 10, 1, 1, 'system');

-- 文件管理
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (5, '文件管理', 0, 'M', '/file', NULL, NULL, 'upload', 5, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (500, '文件管理', 5, 'C', '/file/file-manage', 'file/file-manage/index', 'file:list', 'folder', 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (501, '文件上传', 500, 'F', NULL, NULL, 'file:upload', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (502, '文件下载', 500, 'F', NULL, NULL, 'file:download', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (503, '文件删除', 500, 'F', NULL, NULL, 'file:remove', NULL, 3, 1, 1, 'system');

-- 监控管理
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`)
VALUES (7, '监控管理', 0, 'M', '/monitor', NULL, NULL, 'monitor', 6, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (700, '在线用户监控', 7, 'C', '/monitor/online-user', 'monitor/online-user/index', 'online:list', 'online', 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (7001, '在线用户查询', 700, 'F', NULL, NULL, 'online:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (7002, '在线用户强退', 700, 'F', NULL, NULL, 'online:forceLogout', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (701, '服务监控检查', 7, 'C', '/monitor/server-monitor', 'monitor/server-monitor/index', 'server:list', 'server', 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (7011, '服务监控查询', 701, 'F', NULL, NULL, 'server:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (702, 'Redis缓存监控', 7, 'C', '/monitor/redis-monitor', 'monitor/redis-monitor/index', 'redis:list', 'redis', 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (7021, 'Redis监控查询', 702, 'F', NULL, NULL, 'redis:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (7022, '缓存键删除', 702, 'F', NULL, NULL, 'redis:delete', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (7023, '缓存清空', 702, 'F', NULL, NULL, 'redis:clear', NULL, 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (703, 'SQL执行监控', 7, 'C', '/monitor/sql-monitor', 'monitor/sql-monitor/index', 'monitor:sql', 'sql', 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (7031, 'SQL监控查询', 703, 'F', NULL, NULL, 'sql:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (704, '系统性能监控', 7, 'C', '/monitor/performance', 'monitor/performance/index', 'monitor:performance', 'dashboard', 5, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (7041, '性能监控查询', 704, 'F', NULL, NULL, 'performance:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (705, 'Swagger UI', 7, 'C', '/monitor/swagger', 'monitor/swagger/index', 'monitor:swagger', 'api', 6, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (8, '定时任务管理', 7, 'C', '/monitor/job', 'monitor/job/index', 'job:list', 'job', 7, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (801, '任务查询', 8, 'F', NULL, NULL, 'job:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (802, '任务新增', 8, 'F', NULL, NULL, 'job:add', NULL, 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (803, '任务修改', 8, 'F', NULL, NULL, 'job:edit', NULL, 3, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (804, '任务删除', 8, 'F', NULL, NULL, 'job:remove', NULL, 4, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (805, '任务执行', 8, 'F', NULL, NULL, 'job:run', NULL, 5, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (806, '状态修改', 8, 'F', NULL, NULL, 'job:changeStatus', NULL, 6, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (807, '任务日志', 8, 'F', NULL, NULL, 'job:log', NULL, 7, 1, 1, 'system');

-- 统计分析
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`)
VALUES (9, '统计分析', 0, 'M', '/analysis', NULL, NULL, 'chart', 7, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (900, '数据统计报表', 9, 'C', '/analysis/report', 'analysis/report/index', 'report:view', 'pie-chart', 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (9001, '报表查询', 900, 'F', NULL, NULL, 'report:query', NULL, 1, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (901, '系统使用分析', 9, 'C', '/analysis/usage', 'analysis/usage/index', 'usage:view', 'data-analysis', 2, 1, 1, 'system');

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `order_num`, `visible`, `status`, `create_by`) 
VALUES (9011, '使用分析查询', 901, 'F', NULL, NULL, 'usage:query', NULL, 1, 1, 1, 'system');

-- ----------------------------
-- 4. 初始化部门数据
-- ----------------------------
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `dept_code`, `order_num`, `leader`, `phone`, `email`, `status`, `create_by`) 
VALUES (1, 0, 'iBoot科技', 'IBOOT', 0, 'iBoot', '15888888888', 'iboot@iboot.com', 1, 'system');

INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `dept_code`, `order_num`, `leader`, `status`, `create_by`) 
VALUES (2, 1, '研发部门', 'DEV', 1, '研发经理', 1, 'system');

INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `dept_code`, `order_num`, `leader`, `status`, `create_by`) 
VALUES (3, 1, '市场部门', 'MARKET', 2, '市场经理', 1, 'system');

-- ----------------------------
-- 5. 初始化岗位数据
-- ----------------------------
INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `remark`) 
VALUES (1, 'CEO', '董事长', 1, 1, 'system', '董事长岗位');

INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `remark`) 
VALUES (2, 'MANAGER', '项目经理', 2, 1, 'system', '项目经理岗位');

INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `remark`) 
VALUES (3, 'STAFF', '普通员工', 3, 1, 'system', '普通员工岗位');

-- ===================================================================
-- 字典数据（完整）
-- ===================================================================

-- ----------------------------
-- 用户性别
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (1, '用户性别', 'sys_user_gender', 1, 'system', '用户性别列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `is_default`, `status`, `create_by`) 
VALUES (1, 'sys_user_gender', '未知', '0', 1, 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `is_default`, `status`, `create_by`) 
VALUES (2, 'sys_user_gender', '男', '1', 2, 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `is_default`, `status`, `create_by`) 
VALUES (3, 'sys_user_gender', '女', '2', 3, 0, 1, 'system');

-- ----------------------------
-- 菜单状态
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (2, '菜单状态', 'sys_show_hide', 1, 'system', '菜单状态列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `is_default`, `status`, `create_by`) 
VALUES (4, 'sys_show_hide', '显示', '1', 1, 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `is_default`, `status`, `create_by`) 
VALUES (5, 'sys_show_hide', '隐藏', '0', 2, 0, 1, 'system');

-- ----------------------------
-- 系统开关
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (3, '系统开关', 'sys_normal_disable', 1, 'system', '系统开关列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `is_default`, `status`, `create_by`) 
VALUES (6, 'sys_normal_disable', '正常', '1', 1, 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `is_default`, `status`, `create_by`) 
VALUES (7, 'sys_normal_disable', '停用', '0', 2, 0, 1, 'system');

-- ----------------------------
-- 用户状态
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (4, '用户状态', 'sys_user_status', 1, 'system', '用户状态列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `is_default`, `status`, `create_by`) 
VALUES (8, 'sys_user_status', '正常', '1', 1, 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `is_default`, `status`, `create_by`) 
VALUES (9, 'sys_user_status', '停用', '0', 2, 0, 1, 'system');

-- ----------------------------
-- 角色状态
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (5, '角色状态', 'sys_role_status', 1, 'system', '角色状态列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `is_default`, `status`, `create_by`) 
VALUES (10, 'sys_role_status', '正常', '1', 1, 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `is_default`, `status`, `create_by`) 
VALUES (11, 'sys_role_status', '停用', '0', 2, 0, 1, 'system');

-- ----------------------------
-- 公告类型
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (6, '公告类型', 'sys_notice_type', 1, 'system', '系统公告类型列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (12, 'sys_notice_type', '通知', '1', 1, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (13, 'sys_notice_type', '公告', '2', 2, 'success', 0, 1, 'system');

-- ----------------------------
-- 公告状态
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (7, '公告状态', 'sys_notice_status', 1, 'system', '系统公告状态列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (14, 'sys_notice_status', '草稿', '0', 1, 'info', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (15, 'sys_notice_status', '已发布', '1', 2, 'success', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (16, 'sys_notice_status', '已撤回', '2', 3, 'warning', 0, 1, 'system');

-- ----------------------------
-- 消息类型
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (8, '消息类型', 'sys_message_type', 1, 'system', '消息中心消息类型列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (17, 'sys_message_type', '系统消息', '1', 1, 'primary', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (18, 'sys_message_type', '待办提醒', '2', 2, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (19, 'sys_message_type', '审批通知', '3', 3, 'success', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (20, 'sys_message_type', '预警通知', '4', 4, 'danger', 0, 1, 'system');

-- ----------------------------
-- 消息优先级
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (9, '消息优先级', 'sys_message_priority', 1, 'system', '消息中心消息优先级列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (21, 'sys_message_priority', '普通', '0', 1, 'info', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (22, 'sys_message_priority', '重要', '1', 2, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (23, 'sys_message_priority', '紧急', '2', 3, 'danger', 0, 1, 'system');

-- ----------------------------
-- 消息状态
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (10, '消息状态', 'sys_message_status', 1, 'system', '消息中心消息状态列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (24, 'sys_message_status', '草稿', '0', 1, 'info', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (25, 'sys_message_status', '已发送', '1', 2, 'success', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (26, 'sys_message_status', '已撤回', '2', 3, 'warning', 0, 1, 'system');

-- ----------------------------
-- 发送者类型
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (11, '消息发送者类型', 'sys_message_sender_type', 1, 'system', '消息中心发送者类型列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (27, 'sys_message_sender_type', '系统', '0', 1, 'primary', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (28, 'sys_message_sender_type', '管理员', '1', 2, 'success', 0, 1, 'system');

-- ----------------------------
-- 接收者类型
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (12, '消息接收者类型', 'sys_message_receiver_type', 1, 'system', '消息中心接收者类型列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (29, 'sys_message_receiver_type', '全部用户', '0', 1, 'primary', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (30, 'sys_message_receiver_type', '指定用户', '1', 2, 'success', 1, 1, 'system');

-- ----------------------------
-- 文件分类
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (13, '文件分类', 'sys_file_category', 1, 'system', '文件分类列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (31, 'sys_file_category', '文档', 'document', 1, '', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (32, 'sys_file_category', '图片', 'image', 2, 'success', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (33, 'sys_file_category', '视频', 'video', 3, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (34, 'sys_file_category', '音频', 'audio', 4, 'info', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (35, 'sys_file_category', '压缩包', 'archive', 5, 'danger', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (36, 'sys_file_category', '其他', 'other', 6, 'info', 0, 1, 'system');

-- ----------------------------
-- 任务组名
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (14, '任务组名', 'sys_job_group', 1, 'system', '定时任务的任务组名');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (37, 'sys_job_group', '默认', 'DEFAULT', 1, 'primary', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (38, 'sys_job_group', '系统', 'SYSTEM', 2, 'warning', 0, 1, 'system');

-- ----------------------------
-- 任务状态
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (15, '任务状态', 'sys_job_status', 1, 'system', '定时任务的运行状态');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (39, 'sys_job_status', '暂停', '0', 1, 'danger', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (40, 'sys_job_status', '正常', '1', 2, 'success', 1, 1, 'system');

-- ----------------------------
-- 计划执行错误策略
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (16, '任务错过执行策略', 'sys_job_misfire', 1, 'system', '定时任务错过执行时间的处理策略');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (41, 'sys_job_misfire', '立即执行', '1', 1, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (42, 'sys_job_misfire', '执行一次', '2', 2, 'info', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (43, 'sys_job_misfire', '放弃执行', '3', 3, 'primary', 1, 1, 'system');

-- ----------------------------
-- 是否并发执行
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (17, '任务并发执行', 'sys_job_concurrent', 1, 'system', '定时任务是否允许并发执行');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (44, 'sys_job_concurrent', '禁止', '0', 1, 'danger', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (45, 'sys_job_concurrent', '允许', '1', 2, 'success', 0, 1, 'system');

-- ===================================================================
-- 新增字典数据（原初始化脚本中缺失）
-- ===================================================================

-- ----------------------------
-- 配置类型（前端 ConfigView.vue 使用）
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (18, '配置类型', 'sys_config_type', 1, 'system', '系统参数配置的类型分类');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (46, 'sys_config_type', '默认', '0', 1, 'info', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (47, 'sys_config_type', '系统', '1', 2, 'primary', 0, 1, 'system');

-- ----------------------------
-- 通用状态（前端 LoginLogView.vue / OperateLogView.vue 使用）
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (19, '通用状态', 'sys_common_status', 1, 'system', '通用操作状态列表，用于登录日志和操作日志');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (48, 'sys_common_status', '失败', '0', 1, 'danger', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (49, 'sys_common_status', '成功', '1', 2, 'success', 1, 1, 'system');

-- ----------------------------
-- 业务类型（前端 OperateLogView.vue 使用，对应后端 BusinessTypeEnum）
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (20, '业务类型', 'sys_business_type', 1, 'system', '操作日志业务操作类型列表');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (50, 'sys_business_type', '其它', '0', 1, 'info', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (51, 'sys_business_type', '新增', '1', 2, 'primary', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (52, 'sys_business_type', '修改', '2', 3, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (53, 'sys_business_type', '删除', '3', 4, 'danger', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (54, 'sys_business_type', '授权', '4', 5, 'success', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (55, 'sys_business_type', '导出', '5', 6, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (56, 'sys_business_type', '导入', '6', 7, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (57, 'sys_business_type', '强退', '7', 8, 'danger', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (58, 'sys_business_type', '清空', '8', 9, 'danger', 0, 1, 'system');

-- ----------------------------
-- 监控状态（后端 ServerMonitorApplicationService / RedisMonitorApplicationService 使用，
--          前端 PerformanceView.vue / RedisMonitorView.vue 使用）
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (21, '监控状态', 'sys_monitor_status', 1, 'system', '系统监控检查项的运行状态');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (59, 'sys_monitor_status', '正常', 'UP', 1, 'success', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (60, 'sys_monitor_status', '警告', 'WARN', 2, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (61, 'sys_monitor_status', '异常', 'DOWN', 3, 'danger', 0, 1, 'system');

-- ----------------------------
-- 监控项类型（后端 ServerMonitorApplicationService / ServerInfoResponse 引用）
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (22, '监控项类型', 'sys_monitor_type', 1, 'system', '系统监控检查项的类型分类');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (62, 'sys_monitor_type', '数据库', 'db', 1, 'primary', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (63, 'sys_monitor_type', 'Redis缓存', 'redis', 2, 'danger', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (64, 'sys_monitor_type', 'CPU', 'cpu', 3, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (65, 'sys_monitor_type', '物理内存', 'memory', 4, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (66, 'sys_monitor_type', 'JVM内存', 'jvm', 5, 'info', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (67, 'sys_monitor_type', '磁盘空间', 'disk', 6, 'danger', 0, 1, 'system');

-- ----------------------------
-- 浏览器类型（前端 OnlineUserView.vue 使用）
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (23, '浏览器类型', 'sys_browser_type', 1, 'system', '在线用户监控的浏览器类型');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (68, 'sys_browser_type', 'Chrome', 'Chrome', 1, 'success', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (69, 'sys_browser_type', 'Firefox', 'Firefox', 2, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (70, 'sys_browser_type', 'Safari', 'Safari', 3, 'primary', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (71, 'sys_browser_type', 'Edge', 'Edge', 4, 'info', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (72, 'sys_browser_type', '其他', 'Other', 5, '', 0, 1, 'system');

-- ----------------------------
-- 操作系统类型（前端 OnlineUserView.vue 使用）
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (24, '操作系统类型', 'sys_os_type', 1, 'system', '在线用户监控的操作系统类型');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (73, 'sys_os_type', 'Windows', 'Windows', 1, 'primary', 1, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (74, 'sys_os_type', 'macOS', 'macOS', 2, 'info', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (75, 'sys_os_type', 'Linux', 'Linux', 3, 'success', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (76, 'sys_os_type', 'Android', 'Android', 4, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (77, 'sys_os_type', 'iOS', 'iOS', 5, '', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (78, 'sys_os_type', '其他', 'Other', 6, '', 0, 1, 'system');

-- ----------------------------
-- Redis 信息分类（前端 RedisMonitorView.vue 使用）
-- ----------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `create_by`, `remark`) 
VALUES (25, 'Redis信息分类', 'sys_redis_info_category', 1, 'system', 'Redis监控信息的分类标签');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (79, 'sys_redis_info_category', '服务器信息', 'server', 1, 'primary', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (80, 'sys_redis_info_category', '内存信息', 'memory', 2, 'warning', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (81, 'sys_redis_info_category', '统计信息', 'stats', 3, 'info', 0, 1, 'system');

INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`, `status`, `create_by`) 
VALUES (82, 'sys_redis_info_category', '键空间信息', 'keyspace', 4, 'danger', 0, 1, 'system');

-- ===================================================================
-- 系统配置数据（完整）
-- ===================================================================

-- ----------------------------
-- 基础系统配置（原有）
-- ----------------------------
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (1, '系统名称', 'sys.name', 'iBoot 后台管理系统', 1, 'system', '系统名称');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (2, '用户默认密码', 'sys.user.initPassword', '123456', 1, 'system', '用户默认密码');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (3, '账号自助注册', 'sys.account.registerUser', 'false', 1, 'system', '是否开启用户注册功能');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (4, '登录验证码开关', 'sys.account.captchaEnabled', 'true', 1, 'system', '是否开启登录验证码');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (5, '公告最大展示数量', 'sys.notice.maxDisplay', '5', 1, 'system', '弹窗和首页最大展示公告数量');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (6, '公告置顶数量限制', 'sys.notice.topCount', '3', 1, 'system', '允许同时置顶的最大公告数量');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (7, '消息保留天数', 'sys.message.retentionDays', '90', 1, 'system', '站内消息保留天数，超过该天数的消息将被清理');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (8, '收件箱最大展示数量', 'sys.message.maxDisplay', '50', 1, 'system', '用户收件箱单页最大展示消息数量');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (9, '文件上传路径', 'sys.file.uploadPath', './uploads', 1, 'system', '文件上传的存储根目录，支持相对路径和绝对路径');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (10, '文件上传大小限制', 'sys.file.maxSize', '50', 1, 'system', '单个文件上传最大大小，单位MB');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (11, '允许上传的文件扩展名', 'sys.file.allowedExtensions', 'doc,docx,xls,xlsx,ppt,pptx,pdf,txt,md,jpg,jpeg,png,gif,bmp,svg,mp4,avi,mov,mp3,wav,zip,rar,7z,tar,gz', 1, 'system', '允许上传的文件扩展名列表，逗号分隔');

-- ----------------------------
-- 新增会话和认证配置（原初始化脚本中缺失，AuthApplicationService 使用）
-- ----------------------------
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (12, '会话超时时间', 'sys.session.timeout', '30', 1, 'system', '用户会话超时时间，单位分钟');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (13, '独占会话开关', 'sys.session.exclusive', 'true', 1, 'system', '是否开启会话互斥，同一用户同时只能保持一个活跃会话');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (14, '登录最大重试次数', 'sys.login.maxRetry', '5', 1, 'system', '连续登录失败达到该次数后锁定账户');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (15, '登录锁定时间', 'sys.login.lockTime', '10', 1, 'system', '登录失败锁定的持续时间，单位分钟');

-- ----------------------------
-- 新增监控阈值配置（原初始化脚本中缺失，ServerMonitorApplicationService / RedisMonitorApplicationService 使用）
-- ----------------------------
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (16, 'Redis内存警告阈值', 'monitor.redis.memory.warn.threshold', '70', 1, 'system', 'Redis内存使用率达到该百分比时显示警告');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (17, 'Redis内存错误阈值', 'monitor.redis.memory.error.threshold', '85', 1, 'system', 'Redis内存使用率达到该百分比时显示错误');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (18, 'CPU警告阈值', 'monitor.cpu.warn.threshold', '80', 1, 'system', 'CPU使用率达到该百分比时显示警告');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (19, 'CPU错误阈值', 'monitor.cpu.error.threshold', '90', 1, 'system', 'CPU使用率达到该百分比时显示错误');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (20, '物理内存警告阈值', 'monitor.memory.warn.threshold', '80', 1, 'system', '物理内存使用率达到该百分比时显示警告');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (21, '物理内存错误阈值', 'monitor.memory.error.threshold', '90', 1, 'system', '物理内存使用率达到该百分比时显示错误');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (22, 'JVM内存警告阈值', 'monitor.jvm.warn.threshold', '80', 1, 'system', 'JVM堆内存使用率达到该百分比时显示警告');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (23, 'JVM内存错误阈值', 'monitor.jvm.error.threshold', '90', 1, 'system', 'JVM堆内存使用率达到该百分比时显示错误');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (24, '磁盘空间警告阈值', 'monitor.disk.warn.threshold', '80', 1, 'system', '磁盘空间使用率达到该百分比时显示警告');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (25, '磁盘空间错误阈值', 'monitor.disk.error.threshold', '90', 1, 'system', '磁盘空间使用率达到该百分比时显示错误');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (26, 'Spring Boot Admin地址', 'monitor.sba.url', '/sba', 1, 'system', '监控页面中的Spring Boot Admin控制台链接地址');

-- ----------------------------
-- 新增前端监控页面配置（前端页面通过 getConfigByKey 获取）
-- ----------------------------
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (27, 'Swagger UI地址', 'sys.swagger.ui.url', '/swagger-ui.html', 1, 'system', 'Swagger API文档的访问地址');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (28, 'Swagger UI开关', 'sys.swagger.enabled', 'true', 1, 'system', '是否启用Swagger API文档，生产环境建议关闭');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (29, 'API文档地址', 'sys.swagger.api-docs.url', '/v3/api-docs', 1, 'system', 'OpenAPI接口文档的基础路径，对应springdoc.api-docs.path配置');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (30, 'Druid监控地址', 'sys.druid.monitor.url', '/druid', 1, 'system', 'Druid数据库连接池监控页面地址');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (31, 'Redis监控自动刷新间隔', 'monitor.redis.refreshInterval', '30', 1, 'system', 'Redis缓存监控页面自动刷新间隔，单位秒');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (32, '在线用户监控刷新间隔', 'sys.online.refreshInterval', '30', 1, 'system', '在线用户监控页面自动刷新间隔，单位秒');

INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`) 
VALUES (33, '性能监控自动刷新间隔', 'monitor.performance.refreshInterval', '30', 1, 'system', '系统性能监控页面自动刷新间隔，单位秒');

-- ===================================================================
-- 关联关系数据
-- ===================================================================

-- 用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 用户岗位关联
INSERT INTO `sys_user_post` (`user_id`, `post_id`) VALUES (1, 1);

-- 角色菜单关联（超级管理员拥有所有菜单权限）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 1, id FROM sys_menu WHERE deleted = 0;

-- 角色部门关联（超级管理员拥有所有部门数据权限）
INSERT INTO `sys_role_dept` (`role_id`, `dept_id`) 
SELECT 1, id FROM sys_dept WHERE deleted = 0;

-- ===================================================================
-- 示例数据
-- ===================================================================

-- 示例消息模板数据
INSERT INTO `sys_message_template` (`id`, `template_code`, `template_name`, `template_content`, `message_type`, `status`, `create_by`)
VALUES (1, 'WELCOME_TEMPLATE', '欢迎消息模板', '亲爱的$${userName}，欢迎加入$${companyName}！您的账号已创建成功，初始密码为：$${password}，请及时登录系统修改密码。', '1', 1, 'system');

-- 示例定时任务数据
INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`)
VALUES (1, '系统默认（无参）', 'DEFAULT', 'sampleTask.noParams', '0/10 * * * * ?', 3, 0, 0, '系统默认示例任务（无参数）', 'system');

INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`)
VALUES (2, '系统默认（有参）', 'DEFAULT', 'sampleTask.withParams(\'iboot\')', '0/15 * * * * ?', 3, 0, 0, '系统默认示例任务（有参数）', 'system');

INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`)
VALUES (3, '系统默认（多参）', 'DEFAULT', 'sampleTask.multipleParams(\'iboot\', true, 2000L, 316.50D, 100)', '0/20 * * * * ?', 3, 0, 0, '系统默认示例任务（多参数）', 'system');
