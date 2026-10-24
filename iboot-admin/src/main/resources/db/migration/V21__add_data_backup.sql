-- 数据备份与恢复：备份记录表、恢复演练表、定时任务与管理端菜单

CREATE TABLE IF NOT EXISTS `db_backup_record`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '备份记录ID',
    `backup_no`        VARCHAR(64)  NOT NULL COMMENT '备份编号',
    `trigger_type`     VARCHAR(20)  NOT NULL COMMENT '触发方式：SCHEDULED/MANUAL',
    `status`           VARCHAR(20)  NOT NULL COMMENT '状态：RUNNING/SUCCESS/FAILED',
    `database_name`    VARCHAR(64)  NOT NULL COMMENT '源数据库名',
    `storage_type`     VARCHAR(20)  NULL COMMENT '存储类型：local/minio/oss',
    `storage_path`     VARCHAR(512) NULL COMMENT '加密备份对象路径',
    `ciphertext_size`  BIGINT       NULL COMMENT '密文大小（字节）',
    `plaintext_size`   BIGINT       NULL COMMENT '明文大小（字节）',
    `checksum_sha256`  CHAR(64)     NULL COMMENT '明文 SHA-256',
    `encryption`       VARCHAR(32)  NULL COMMENT '加密算法',
    `dump_tool`        VARCHAR(64)  NULL COMMENT '导出工具',
    `table_count`      INT          NULL COMMENT '表数量',
    `table_stats_json` TEXT         NULL COMMENT '备份时逐表行数快照JSON',
    `started_at`       DATETIME     NOT NULL COMMENT '开始时间',
    `finished_at`      DATETIME     NULL COMMENT '结束时间',
    `duration_ms`      BIGINT       NULL COMMENT '耗时（毫秒）',
    `error_message`    VARCHAR(1000) NULL COMMENT '脱敏后的错误信息',
    `created_by`       VARCHAR(64)  NULL COMMENT '触发人',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_backup_no` (`backup_no`),
    KEY `idx_status_started` (`status`, `started_at`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='数据库备份记录';

CREATE TABLE IF NOT EXISTS `db_restore_record`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '演练记录ID',
    `backup_id`        BIGINT       NOT NULL COMMENT '备份记录ID',
    `target_schema`    VARCHAR(64)  NOT NULL COMMENT '演练库名',
    `status`           VARCHAR(20)  NOT NULL COMMENT '状态：RUNNING/SUCCESS/MISMATCH/FAILED',
    `table_count`      INT          NULL COMMENT '比对表数量',
    `table_stats_json` TEXT         NULL COMMENT '还原后逐表行数JSON',
    `mismatch_json`    TEXT         NULL COMMENT '行数差异明细JSON',
    `started_at`       DATETIME     NOT NULL COMMENT '开始时间',
    `finished_at`      DATETIME     NULL COMMENT '结束时间',
    `duration_ms`      BIGINT       NULL COMMENT '耗时（毫秒）',
    `error_message`    VARCHAR(1000) NULL COMMENT '脱敏后的错误信息',
    `created_by`       VARCHAR(64)  NULL COMMENT '触发人',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_backup_id` (`backup_id`),
    KEY `idx_status_started` (`status`, `started_at`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='数据库恢复演练记录';

-- 每天 02:00 执行一次全量逻辑备份
INSERT INTO `sys_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`,
                       `status`, `remark`, `create_by`, `create_time`, `deleted`)
SELECT '数据库备份', 'SYSTEM', 'backupTask.run', '0 0 2 * * ?', 3, 0, 1,
       '每天凌晨全量导出数据库，加密后写入对象存储', 'system', NOW(), 0
WHERE NOT EXISTS (SELECT 1
                  FROM `sys_job`
                  WHERE `invoke_target` = 'backupTask.run'
                    AND `deleted` = 0);

-- 监控管理下的数据备份页面与操作按钮
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9300, '数据备份', 7, 'C', '/monitor/backup', 'monitor/BackupView',
       'backup:record:list', 'database', 30, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9300);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9301, '立即备份', 9300, 'F', NULL, NULL, 'backup:record:create', NULL, 1, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9301);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`,
                        `order_num`, `visible`, `status`, `create_by`)
SELECT 9302, '恢复演练', 9300, 'F', NULL, NULL, 'backup:record:restore', NULL, 2, 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 9302);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9300
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9300);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9301
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9301);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 9302
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 9302);
