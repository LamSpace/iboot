-- ============================================================
-- 知识库菜单与权限
-- ============================================================

-- 菜单：知识库
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9802, '知识库', 9800, 'C', '/ai/knowledge', 'ai/KnowledgeView', 'ai:kb:query', 'collection', 2, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9802);

-- 按钮：知识库管理
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9803, '知识库管理', 9802, 'F', NULL, NULL, 'ai:kb:manage', NULL, 1, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9803);

-- 授权超级管理员
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9802
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9802);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9803
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9803);
