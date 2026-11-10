-- 请假额度划转（system 所有）
-- 记录 TCC 三段操作的状态与全局事务标识，支撑幂等、空回滚与悬挂判断。
-- 只做扩展性变更：新增表与权限种子，不改动既有结构。

CREATE TABLE `sys_leave_quota_transfer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `transfer_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '划转业务标识，幂等键',
  `xid` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '全局事务 ID',
  `from_user_id` bigint NOT NULL COMMENT '划出方用户 ID',
  `to_user_id` bigint NOT NULL COMMENT '划入方用户 ID',
  `year` int NOT NULL COMMENT '年度',
  `count` int NOT NULL COMMENT '划转次数',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TRYING/CONFIRMED/CANCELLED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transfer_id` (`transfer_id`),
  UNIQUE KEY `uk_xid` (`xid`),
  KEY `idx_from_user_year` (`from_user_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假额度划转（TCC）';

-- 管理员额度划转权限，挂在审批中心（9100）下
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`,
                        `icon`, `order_num`, `is_frame`, `is_cache`, `visible`, `status`, `create_by`, `deleted`)
VALUES (9105, '额度划转', 9100, 'F', NULL, NULL, 'approval:quota:transfer',
        NULL, 5, 0, 1, 1, 1, 'system', 0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 9105);
