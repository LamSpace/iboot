-- ============================================================
-- 知识图谱菜单与权限（1.4）
-- ============================================================

-- 功能权限：知识图谱查询。1.4 不新增独立视图，此处只登记权限点
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9806, '知识图谱', 9800, 'F', NULL, NULL, 'ai:graph:use', NULL, 5, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9806);

-- 授权超级管理员
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9806
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9806);
