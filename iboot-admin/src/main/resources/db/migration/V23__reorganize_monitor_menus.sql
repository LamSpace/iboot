-- 重构监控菜单：拆分“运行时监控”与“可观测平台”，归位 Kibana 与 Swagger
-- 说明：前端路由取 path 的最后一段，调整路径前缀不影响现有路由。

-- ============================================================
-- 1. 一级菜单顺序与命名
--    最终顺序：系统/组织/日志/消息/文件/审批/运行时监控/可观测平台/系统运维/统计分析
-- ============================================================
UPDATE `sys_menu`
SET `order_num` = 6
WHERE `id` = 9100; -- 审批中心，与文件管理解耦

UPDATE `sys_menu`
SET `menu_name` = '运行时监控',
    `order_num` = 7
WHERE `id` = 7; -- 原“监控管理”

UPDATE `sys_menu`
SET `order_num` = 9
WHERE `id` = 9500; -- 系统运维

UPDATE `sys_menu`
SET `order_num` = 10
WHERE `id` = 9; -- 统计分析

-- 新建一级菜单“可观测平台”，收纳 Prometheus/Grafana/Alertmanager/Thanos
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9700, '可观测平台', 0, 'M', '/observability', NULL, NULL, 'chart', 8, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9700);

-- ============================================================
-- 2. 日志管理：路径统一为 /log，接收 Kibana
-- ============================================================
UPDATE `sys_menu`
SET `path` = '/log'
WHERE `id` = 3;

UPDATE `sys_menu`
SET `path` = '/log/login-log'
WHERE `id` = 300;

UPDATE `sys_menu`
SET `path` = '/log/operate-log'
WHERE `id` = 301;

UPDATE `sys_menu`
SET `path` = '/log/audit-log'
WHERE `id` = 302;

UPDATE `sys_menu`
SET `parent_id` = 3,
    `menu_name` = 'Kibana 日志检索',
    `path`      = '/log/kibana-monitor',
    `order_num` = 4
WHERE `id` = 709;

-- ============================================================
-- 3. 运行时监控：只保留应用与中间件运行状态，修正重复排序
-- ============================================================
UPDATE `sys_menu`
SET `menu_name` = '在线用户监控',
    `order_num` = 1
WHERE `id` = 700;

UPDATE `sys_menu`
SET `menu_name` = '系统性能监控',
    `order_num` = 2
WHERE `id` = 704;

UPDATE `sys_menu`
SET `menu_name` = '服务健康监控（SBA）',
    `order_num` = 3
WHERE `id` = 701;

UPDATE `sys_menu`
SET `menu_name` = '数据库监控（Druid）',
    `order_num` = 4
WHERE `id` = 703;

UPDATE `sys_menu`
SET `menu_name` = '缓存监控（Redis）',
    `order_num` = 5
WHERE `id` = 702;

UPDATE `sys_menu`
SET `menu_name` = '对象存储监控（MinIO）',
    `order_num` = 6
WHERE `id` = 706;

-- ============================================================
-- 4. 可观测平台：迁入外部可观测性工具
-- ============================================================
UPDATE `sys_menu`
SET `parent_id` = 9700,
    `menu_name` = 'Grafana 看板',
    `path`      = '/observability/grafana-monitor',
    `order_num` = 1
WHERE `id` = 708;

UPDATE `sys_menu`
SET `parent_id` = 9700,
    `menu_name` = 'Prometheus',
    `path`      = '/observability/prometheus-monitor',
    `order_num` = 2
WHERE `id` = 707;

UPDATE `sys_menu`
SET `parent_id` = 9700,
    `menu_name` = 'Alertmanager',
    `path`      = '/observability/alertmanager',
    `order_num` = 3
WHERE `id` = 710;

UPDATE `sys_menu`
SET `parent_id` = 9700,
    `menu_name` = 'Thanos 查询',
    `path`      = '/observability/thanos-query',
    `order_num` = 4
WHERE `id` = 711;

-- ============================================================
-- 5. Swagger UI 属于开发工具，移入系统运维
-- ============================================================
UPDATE `sys_menu`
SET `parent_id` = 9500,
    `path`      = '/ops/swagger',
    `order_num` = 3
WHERE `id` = 705;

-- ============================================================
-- 6. 角色菜单：为新父菜单及迁入目标补齐父级授权，避免子菜单失去父节点
-- ============================================================
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 9700
FROM `sys_role_menu` rm
WHERE rm.menu_id IN (707, 708, 710, 711)
  AND NOT EXISTS (SELECT 1
                  FROM `sys_role_menu` x
                  WHERE x.role_id = rm.role_id
                    AND x.menu_id = 9700);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 3
FROM `sys_role_menu` rm
WHERE rm.menu_id = 709
  AND NOT EXISTS (SELECT 1
                  FROM `sys_role_menu` x
                  WHERE x.role_id = rm.role_id
                    AND x.menu_id = 3);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 9500
FROM `sys_role_menu` rm
WHERE rm.menu_id = 705
  AND NOT EXISTS (SELECT 1
                  FROM `sys_role_menu` x
                  WHERE x.role_id = rm.role_id
                    AND x.menu_id = 9500);
