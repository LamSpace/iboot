-- ============================================================
-- AI 工作台菜单
-- ============================================================

-- 一级目录：AI 工作台
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9800, 'AI 工作台', 0, 'M', '/ai', NULL, NULL, 'chat-dot-round', 9, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9800);

-- 菜单：智能对话
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9801, '智能对话', 9800, 'C', '/ai/chat', 'ai/ChatView', 'ai:chat', 'chat-dot-round', 1, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9801);

-- 授权超级管理员
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9800
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9800);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9801
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9801);
