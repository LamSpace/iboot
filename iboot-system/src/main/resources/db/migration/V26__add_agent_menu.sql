-- ============================================================
-- 智能体编排菜单与权限（1.3）
-- ============================================================

-- 菜单：智能体
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9804, '智能体', 9800, 'C', '/ai/agent', 'ai/AgentChatView', 'ai:agent:use', 'magic-stick', 3, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9804);

-- 菜单：智能体管理
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9805, '智能体管理', 9800, 'C', '/ai/agent-config', 'ai/AgentConfigView', 'ai:agent:manage', 'setting', 4, 1, 1,
       'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9805);

-- 授权超级管理员
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9804
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9804);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9805
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9805);
