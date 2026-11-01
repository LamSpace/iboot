-- ===================================================================
-- 消息与通知服务：独立 schema 的初始结构
-- -------------------------------------------------------------------
-- 结构来源：iboot_admin 的 V1（基础表）、V16（接收记录更新时间）、
--           V18（投递表）、V20（消息业务类型），已合并为终态。
-- 数据搬迁见 scripts/migrate/sys_*_to_iboot_notify.sql。
-- ===================================================================

-- 系统公告表
CREATE TABLE `sys_notice`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `notice_title`   VARCHAR(200) NOT NULL COMMENT '公告标题',
    `notice_type`    CHAR(1)      NOT NULL COMMENT '公告类型（字典 sys_notice_type）',
    `notice_content` TEXT         NOT NULL COMMENT '公告内容',
    `status`         CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态（字典 sys_notice_status）',
    `is_top`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
    `create_by`      VARCHAR(50) NULL COMMENT '创建人',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      VARCHAR(50) NULL COMMENT '更新人',
    `update_time`    DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`         VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY              `idx_status_top` (`status`, `is_top`),
    KEY              `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='系统公告表';

-- 公告已读记录表（无逻辑删除）
CREATE TABLE `sys_notice_read`
(
    `id`        BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `notice_id` BIGINT   NOT NULL COMMENT '公告ID',
    `user_id`   BIGINT   NOT NULL COMMENT '用户ID',
    `read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`),
    KEY         `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='公告已读记录表';

-- 消息主表
CREATE TABLE `sys_message`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `title`         VARCHAR(200) NOT NULL COMMENT '消息标题',
    `content`       TEXT         NOT NULL COMMENT '消息内容',
    `message_type`  CHAR(1)      NOT NULL COMMENT '消息类型（字典 sys_message_type）',
    `business_type` VARCHAR(50) NULL COMMENT '业务类型：APPROVAL_TODO 等',
    `priority`      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '优先级（字典 sys_message_priority）',
    `sender_type`   CHAR(1)      NOT NULL DEFAULT '0' COMMENT '发送者类型（字典 sys_message_sender_type）',
    `sender_id`     BIGINT NULL COMMENT '发送者用户ID（管理员发送时记录）',
    `receiver_type` CHAR(1)      NOT NULL DEFAULT '1' COMMENT '接收者类型（字典 sys_message_receiver_type）',
    `status`        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态（字典 sys_message_status）',
    `create_by`     VARCHAR(50) NULL COMMENT '创建人',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(50) NULL COMMENT '更新人',
    `update_time`   DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`        VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY             `idx_status_create_time` (`status`, `create_time`),
    KEY             `idx_sender` (`sender_type`, `sender_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='消息主表';

-- 消息接收记录表（无逻辑删除；update_time 供索引对账发现变更）
CREATE TABLE `sys_message_receiver`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `message_id`  BIGINT   NOT NULL COMMENT '消息ID',
    `user_id`     BIGINT   NOT NULL COMMENT '接收者用户ID',
    `is_read`     TINYINT  NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `read_time`   DATETIME NULL COMMENT '阅读时间',
    `is_deleted`  TINYINT  NOT NULL DEFAULT 0 COMMENT '用户侧删除：0-未删除，1-已删除',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_user` (`message_id`, `user_id`),
    KEY           `idx_user_read` (`user_id`, `is_read`, `is_deleted`),
    KEY           `idx_receiver_update_time` (`update_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='消息接收记录表';

-- 消息模板表
CREATE TABLE `sys_message_template`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_code`    VARCHAR(100) NOT NULL COMMENT '模板编码',
    `template_name`    VARCHAR(200) NOT NULL COMMENT '模板名称',
    `template_content` TEXT         NOT NULL COMMENT '模板内容（支持$${变量名}占位符）',
    `message_type`     CHAR(1)      NOT NULL COMMENT '消息类型（字典 sys_message_type）',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `create_by`        VARCHAR(50) NULL COMMENT '创建人',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(50) NULL COMMENT '更新人',
    `update_time`      DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`           VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    KEY                `idx_message_type` (`message_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='消息模板表';

-- 通知投递记录表
CREATE TABLE `sys_notification_delivery`
(
    `id`                BIGINT   NOT NULL AUTO_INCREMENT COMMENT '投递记录ID',
    `event_id`          VARCHAR(64)  NOT NULL COMMENT '业务事件ID',
    `event_type`        VARCHAR(64)  NOT NULL COMMENT '业务事件类型',
    `channel`           VARCHAR(20)  NOT NULL COMMENT '渠道：IN_APP/EMAIL/DINGTALK/SMS',
    `recipient_key`     VARCHAR(128) NOT NULL COMMENT '接收人唯一键，如 USER:123 或 GROUP:default',
    `recipient_id`      BIGINT NULL COMMENT '接收用户ID',
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
    `message_id`        BIGINT NULL COMMENT '站内消息ID，重试时复用',
    `sent_time`         DATETIME     NULL COMMENT '发送成功时间',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_channel_recipient` (`event_id`, `channel`, `recipient_key`),
    KEY                 `idx_status_retry` (`status`, `next_retry_time`),
    KEY                 `idx_channel_create` (`channel`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='通知投递记录';

-- 种子数据：审批待办模板（模板变量使用 Flyway 转义写法，落库后为单美元占位符）
INSERT INTO `sys_message_template` (`template_code`, `template_name`, `template_content`, `message_type`, `status`,
                                    `create_by`, `create_time`, `deleted`)
VALUES ('APPROVAL_TODO', '审批待办通知',
        '您有一条新的审批待办：$${taskName}，发起人：$${initiator}，申请时间：$${applyTime}。',
        '2', 1, 'system', NOW(), 0);
