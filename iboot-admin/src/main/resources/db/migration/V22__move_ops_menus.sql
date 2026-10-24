-- 新建一级菜单“系统运维”，收纳定时任务与数据备份等运维工具

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9500, '系统运维', 0, 'M', '/ops', NULL, NULL, 'operation', 7, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9500);

-- 定时任务管理与数据备份移入“系统运维”；路径前缀改为 /ops，末段保持不变
UPDATE `sys_menu`
SET `parent_id` = 9500,
    `path`      = '/ops/job',
    `order_num` = 1
WHERE `id` = 8;

UPDATE `sys_menu`
SET `parent_id` = 9500,
    `path`      = '/ops/backup',
    `order_num` = 2
WHERE `id` = 9300;

-- 统计分析后移，保持一级菜单顺序
UPDATE `sys_menu`
SET `order_num` = 8
WHERE `id` = 9;

-- 为原本拥有定时任务或备份菜单的角色补上“系统运维”父菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 9500
FROM (SELECT `role_id` FROM `sys_role_menu` WHERE `menu_id` IN (8, 9300)) rm
WHERE NOT EXISTS (SELECT 1
                  FROM `sys_role_menu` x
                  WHERE x.role_id = rm.role_id
                    AND x.menu_id = 9500);
