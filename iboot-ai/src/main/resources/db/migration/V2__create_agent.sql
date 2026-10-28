-- ============================================================
-- 智能体编排基础表（1.3）
-- ============================================================

CREATE TABLE `ai_agent`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '智能体ID',
    `name`                VARCHAR(64)  NOT NULL COMMENT '智能体名称',
    `description`         VARCHAR(512) NULL COMMENT '描述',
    `system_prompt`       TEXT         NOT NULL COMMENT '系统提示词',
    `model`               VARCHAR(64)  NULL COMMENT '模型标识，空表示使用服务默认模型',
    `tool_allowlist_json` TEXT         NOT NULL COMMENT '工具白名单 JSON 数组',
    `status`              TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    `created_by`          VARCHAR(64)  NULL COMMENT '创建人',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             BIGINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，删除时置为ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_name` (`name`, `deleted`),
    KEY `idx_agent_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='智能体定义';

CREATE TABLE `ai_agent_run`
(
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '运行ID',
    `conversation_id`      VARCHAR(64)  NOT NULL COMMENT '会话ID',
    `agent_id`             BIGINT       NOT NULL COMMENT '智能体ID',
    `user_id`              BIGINT       NOT NULL COMMENT '发起用户ID',
    `status`               VARCHAR(24)  NOT NULL COMMENT 'RUNNING/PENDING_APPROVAL/EXECUTING/COMPLETED/REJECTED/EXPIRED/FAILED',
    `pending_tool`         VARCHAR(64)  NULL COMMENT '待确认工具名',
    `pending_args_json`    MEDIUMTEXT   NULL COMMENT '待确认参数 JSON',
    `pending_args_digest`  CHAR(64)     NULL COMMENT '待确认参数 SHA-256',
    `pending_expires_at`   DATETIME     NULL COMMENT '待确认过期时间',
    `approval_granted_at`  DATETIME     NULL COMMENT '确认通过时间',
    `step_count`           INT          NOT NULL DEFAULT 0 COMMENT '模型调用步数',
    `steps_json`           MEDIUMTEXT   NULL COMMENT '步骤轨迹 JSON（只存摘要）',
    `input_tokens`         BIGINT       NOT NULL DEFAULT 0 COMMENT '输入 token 累计',
    `output_tokens`        BIGINT       NOT NULL DEFAULT 0 COMMENT '输出 token 累计',
    `error_code`           VARCHAR(32)  NULL COMMENT '失败原因码',
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `finished_at`          DATETIME     NULL COMMENT '结束时间',
    PRIMARY KEY (`id`),
    KEY `idx_run_user_time` (`user_id`, `create_time`),
    KEY `idx_run_conversation` (`conversation_id`),
    KEY `idx_run_pending` (`status`, `pending_expires_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='智能体运行记录';
