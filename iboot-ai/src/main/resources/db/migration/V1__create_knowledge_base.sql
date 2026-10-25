-- ============================================================
-- 知识库 RAG 基础表
-- ============================================================

CREATE TABLE `ai_knowledge_base`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
    `name`          VARCHAR(128) NOT NULL COMMENT '知识库名称',
    `description`   VARCHAR(512) NULL COMMENT '描述',
    `visibility`    TINYINT      NOT NULL DEFAULT 1 COMMENT '可见范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人',
    `owner_user_id` BIGINT       NOT NULL COMMENT '所有者用户ID',
    `owner_dept_id` BIGINT       NULL COMMENT '所有者部门ID',
    `created_by`    VARCHAR(64)  NULL COMMENT '创建人',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       BIGINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，删除时置为ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_owner_name` (`owner_user_id`, `name`, `deleted`),
    KEY `idx_kb_owner_dept` (`owner_dept_id`),
    KEY `idx_kb_visibility` (`visibility`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='知识库';

CREATE TABLE `ai_kb_share_dept`
(
    `kb_id`   BIGINT NOT NULL COMMENT '知识库ID',
    `dept_id` BIGINT NOT NULL COMMENT '共享部门ID',
    PRIMARY KEY (`kb_id`, `dept_id`),
    KEY `idx_kb_share_dept` (`dept_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='知识库共享部门';

CREATE TABLE `ai_document`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `kb_id`           BIGINT       NOT NULL COMMENT '知识库ID',
    `file_name`       VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_ext`        VARCHAR(16)  NULL COMMENT '扩展名',
    `file_size`       BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小',
    `mime_type`       VARCHAR(128) NULL COMMENT 'MIME类型',
    `storage_key`     VARCHAR(512) NOT NULL COMMENT '对象存储路径',
    `content_hash`    CHAR(64)     NOT NULL COMMENT '内容 SHA-256',
    `owner_user_id`   BIGINT       NOT NULL COMMENT '上传人用户ID',
    `owner_dept_id`   BIGINT       NOT NULL COMMENT '上传人部门ID',
    `status`          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/READY/FAILED',
    `error_code`      VARCHAR(32)  NULL COMMENT '失败原因码',
    `error_message`   VARCHAR(512) NULL COMMENT '失败说明',
    `chunk_count`     INT          NOT NULL DEFAULT 0 COMMENT '分块数量',
    `attempts`        INT          NOT NULL DEFAULT 0 COMMENT '已尝试次数',
    `next_attempt_at` DATETIME     NULL COMMENT '下次重试时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         BIGINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，删除时置为ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_hash` (`kb_id`, `content_hash`, `deleted`),
    KEY `idx_doc_kb_status` (`kb_id`, `status`),
    KEY `idx_doc_claim` (`status`, `next_attempt_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='知识库文档';

CREATE TABLE `ai_document_chunk`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分块ID',
    `doc_id`       BIGINT       NOT NULL COMMENT '文档ID',
    `chunk_index`  INT          NOT NULL COMMENT '分块序号',
    `page_no`      INT          NULL COMMENT '页码',
    `heading_path` VARCHAR(512) NULL COMMENT '标题路径',
    `content`      MEDIUMTEXT   NOT NULL COMMENT '分块正文',
    `token_count`  INT          NOT NULL DEFAULT 0 COMMENT '估算 token 数',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chunk` (`doc_id`, `chunk_index`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='知识库文档分块';
