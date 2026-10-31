-- ===================================================================
-- 文件服务初始表结构
-- -------------------------------------------------------------------
-- sys_file 从管理端 schema 迁移而来，定义与 iboot_admin.sys_file 对齐：
--   * 基线定义：iboot-admin V1__initial_schema.sql
--   * 追加列 dept_id 与索引 idx_file_dept_id：iboot-admin V11__add_dept_id_to_job_and_file.sql
-- 数据由一次性脚本 scripts/migrate/sys_file_to_iboot_file.sql 复制，
-- 迁移脚本不负责建表，避免两处 DDL 各自漂移。
-- ===================================================================

CREATE TABLE `sys_file`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `file_name`     VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_path`     VARCHAR(500) NOT NULL COMMENT '存储路径',
    `file_size`     BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `file_type`     VARCHAR(100) NULL COMMENT 'MIME类型',
    `file_ext`      VARCHAR(50) NULL COMMENT '文件扩展名',
    `file_category` VARCHAR(100) NULL COMMENT '文件分类（关联字典 sys_file_category）',
    `dept_id`       BIGINT       NULL COMMENT '上传人部门 ID',
    `upload_by`     VARCHAR(50) NULL COMMENT '上传人',
    `upload_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `create_by`     VARCHAR(50) NULL COMMENT '创建人',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(50) NULL COMMENT '更新人',
    `update_time`   DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`        VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY             `idx_file_name` (`file_name`),
    KEY             `idx_file_category` (`file_category`),
    KEY             `idx_file_dept_id` (`dept_id`),
    KEY             `idx_upload_by` (`upload_by`),
    KEY             `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';
