-- Seata AT 回滚日志（所有参与方共用；单库过渡形态）
-- 只做扩展性变更：新增表与索引，不改动既有结构。

CREATE TABLE `undo_log` (
  `branch_id` bigint NOT NULL COMMENT '分支事务 ID',
  `xid` varchar(128) NOT NULL COMMENT '全局事务 ID',
  `context` varchar(128) NOT NULL COMMENT '回滚上下文',
  `rollback_info` longblob NOT NULL COMMENT '回滚信息',
  `log_status` int NOT NULL COMMENT '0-正常，1-防御状态',
  `log_created` datetime(6) NOT NULL COMMENT '创建时间',
  `log_modified` datetime(6) NOT NULL COMMENT '修改时间',
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AT 模式回滚日志';

-- 用户年度请假额度（system 所有）
CREATE TABLE `sys_user_leave_quota` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `year` int NOT NULL COMMENT '年度',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '年度总额度',
  `used_count` int NOT NULL DEFAULT '0' COMMENT '已用次数',
  `frozen_count` int NOT NULL DEFAULT '0' COMMENT '冻结次数（TCC 预留）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_year` (`user_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户年度请假额度';

-- 额度变更日志（system 所有；request_id 保证扣减幂等）
CREATE TABLE `sys_leave_quota_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `request_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '申请 ID，幂等键',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `year` int NOT NULL COMMENT '年度',
  `delta` int NOT NULL COMMENT '次数变化',
  `type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'DEDUCT/REFUND',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_id` (`request_id`),
  KEY `idx_user_year` (`user_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假额度变更日志';
