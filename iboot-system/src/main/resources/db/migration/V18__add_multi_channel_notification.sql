-- 多渠道通知：投递记录表、审批待办模板、重试任务与管理端菜单

CREATE TABLE IF NOT EXISTS `sys_notification_delivery`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '投递记录ID',
    `event_id`          VARCHAR(64)  NOT NULL COMMENT '业务事件ID',
    `event_type`        VARCHAR(64)  NOT NULL COMMENT '业务事件类型',
    `channel`           VARCHAR(20)  NOT NULL COMMENT '渠道：IN_APP/EMAIL/DINGTALK/SMS',
    `recipient_key`     VARCHAR(128) NOT NULL COMMENT '接收人唯一键，如 USER:123 或 GROUP:default',
    `recipient_id`      BIGINT       NULL COMMENT '接收用户ID',
    `recipient_address` VARCHAR(255) NULL COMMENT '接收地址（邮箱/手机号）',
    `template_code`     VARCHAR(100) NOT NULL COMMENT '模板编码',
    `title`             VARCHAR(200) NOT NULL COMMENT '消息标题',
    `content`           TEXT         NOT NULL COMMENT '渲染后的消息内容',
    `params_json`       TEXT         NULL COMMENT '模板参数JSON',
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SENDING/SUCCESS/FAILED/DEAD/SKIPPED',
    `attempts`          INT          NOT NULL DEFAULT 0 COMMENT '已尝试次数',
    `max_attempts`      INT          NOT NULL DEFAULT 5 COMMENT '最大尝试次数',
    `next_retry_time`   DATETIME     NULL COMMENT '下次自动重试时间',
    `last_error`        VARCHAR(1000) NULL COMMENT '最后一次脱敏错误',
    `message_id`        BIGINT       NULL COMMENT '站内消息ID，重试时复用',
    `sent_time`         DATETIME     NULL COMMENT '发送成功时间',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_channel_recipient` (`event_id`, `channel`, `recipient_key`),
    KEY `idx_status_retry` (`status`, `next_retry_time`),
    KEY `idx_channel_create` (`channel`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='通知投递记录';

-- 审批待办模板：模板变量使用 Flyway 转义写法，落库后为单美元占位符
INSERT INTO `sys_message_template` (`template_code`, `template_name`, `template_content`, `message_type`, `status`,
                                    `create_by`, `create_time`, `deleted`)
SELECT 'APPROVAL_TODO', '审批待办通知',
       '您有一条新的审批待办：$${taskName}，发起人：$${initiator}，申请时间：$${applyTime}。',
       '2', 1, 'system', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `sys_message_template` WHERE `template_code` = 'APPROVAL_TODO');

-- 每分钟扫描到期投递记录并重试
INSERT INTO `sys_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`,
                       `status`, `remark`, `create_by`, `create_time`, `deleted`)
SELECT '通知投递重试', 'SYSTEM', 'notificationRetryTask.retry', '0 * * * * ?', 3, 0, 1,
       '每分钟扫描到期的通知投递记录并重试，失败按指数退避', 'system', NOW(), 0
WHERE NOT EXISTS (SELECT 1
                  FROM `sys_job`
                  WHERE `invoke_target` = 'notificationRetryTask.retry'
                    AND `deleted` = 0);

-- 管理端“通知投递”页面与重试按钮
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9200, '通知投递', 7, 'C', '/notification/delivery', 'monitor/NotificationDeliveryView',
       'notification:delivery:list', 'message', 20, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9200);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9201, '手动重试', 9200, 'F', NULL, NULL, 'notification:delivery:retry', NULL, 1, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9201);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9200
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9200);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9201
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9201);
