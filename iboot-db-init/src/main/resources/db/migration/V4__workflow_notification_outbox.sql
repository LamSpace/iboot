-- 审批服务通知事件 outbox（workflow 所有）
-- 只做扩展性变更：新增表与索引，不改动既有结构。

CREATE TABLE `biz_notification_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务事件 ID，如 approval-todo-<processInstanceId>',
  `event_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件类型，与消息标签一致',
  `payload` json NOT NULL COMMENT '通知命令 JSON',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/SENT/DEAD',
  `attempts` int NOT NULL DEFAULT '0' COMMENT '已尝试投递次数',
  `max_attempts` int NOT NULL DEFAULT '10' COMMENT '最大尝试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次可投递时间',
  `last_error` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后一次错误（已脱敏）',
  `message_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'broker 消息 ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批通知事件 outbox';
