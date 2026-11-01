-- iBoot 调度服务初始表结构
-- 定时任务（含 Quartz 持久化表）、执行日志、数据备份与恢复演练记录
-- 表结构与管理端 iboot_admin 中的同名表保持一致，便于拆分回退对照

DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `job_name`        VARCHAR(64)  NOT NULL COMMENT '任务名称',
    `job_group`       VARCHAR(64)  NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
    `dept_id`         BIGINT       NULL COMMENT '创建人部门 ID',
    `invoke_target`   VARCHAR(500) NOT NULL COMMENT '调用目标字符串(beanName.methodName)',
    `cron_expression` VARCHAR(255)          DEFAULT '' COMMENT 'cron执行表达式',
    `misfire_policy`  TINYINT      NOT NULL DEFAULT 3 COMMENT '计划执行错误策略(1立即执行 2执行一次 3放弃执行)',
    `concurrent`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否并发执行(0禁止 1允许)',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(0暂停 1正常)',
    `remark`          VARCHAR(500)          DEFAULT '' COMMENT '备注信息',
    `create_by`       VARCHAR(64)           DEFAULT '' COMMENT '创建者',
    `create_time`     DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)           DEFAULT '' COMMENT '更新者',
    `update_time`     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志(0存在 1删除)',
    PRIMARY KEY (`id`),
    KEY               `idx_job_group` (`job_group`),
    KEY               `idx_job_dept_id` (`dept_id`),
    KEY               `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务表';

-- 定时任务执行日志表
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `job_id`         BIGINT       NOT NULL COMMENT '任务ID',
    `job_name`       VARCHAR(64)  NOT NULL COMMENT '任务名称',
    `job_group`      VARCHAR(64)  NOT NULL COMMENT '任务组名',
    `invoke_target`  VARCHAR(500) NOT NULL COMMENT '调用目标字符串',
    `job_message`    VARCHAR(500)          DEFAULT '' COMMENT '日志信息',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '执行状态(0失败 1成功)',
    `exception_info` TEXT COMMENT '异常信息',
    `start_time`     DATETIME              DEFAULT NULL COMMENT '开始时间',
    `end_time`       DATETIME              DEFAULT NULL COMMENT '结束时间',
    `cost_time`      BIGINT                DEFAULT 0 COMMENT '执行耗时(毫秒)',
    `create_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY              `idx_job_id` (`job_id`),
    KEY              `idx_status` (`status`),
    KEY              `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务执行日志表';

-- Quartz 调度框架表结构（MySQL）
-- 存储每一个已配置的 Job 的详细信息
DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
CREATE TABLE `QRTZ_JOB_DETAILS`
(
    `SCHED_NAME`        VARCHAR(120) NOT NULL,
    `JOB_NAME`          VARCHAR(200) NOT NULL,
    `JOB_GROUP`         VARCHAR(200) NOT NULL,
    `DESCRIPTION`       VARCHAR(250) DEFAULT NULL,
    `JOB_CLASS_NAME`    VARCHAR(250) NOT NULL,
    `IS_DURABLE`        VARCHAR(1)   NOT NULL,
    `IS_NONCONCURRENT`  VARCHAR(1)   NOT NULL,
    `IS_UPDATE_DATA`    VARCHAR(1)   NOT NULL,
    `REQUESTS_RECOVERY` VARCHAR(1)   NOT NULL,
    `JOB_DATA`          BLOB,
    PRIMARY KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`),
    KEY                 `IDX_QRTZ_J_REQ_RECOVERY` (`SCHED_NAME`,`REQUESTS_RECOVERY`),
    KEY                 `IDX_QRTZ_J_GRP` (`SCHED_NAME`,`JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储已配置的 Trigger 的信息
DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
CREATE TABLE `QRTZ_TRIGGERS`
(
    `SCHED_NAME`     VARCHAR(120) NOT NULL,
    `TRIGGER_NAME`   VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP`  VARCHAR(200) NOT NULL,
    `JOB_NAME`       VARCHAR(200) NOT NULL,
    `JOB_GROUP`      VARCHAR(200) NOT NULL,
    `DESCRIPTION`    VARCHAR(250) DEFAULT NULL,
    `NEXT_FIRE_TIME` BIGINT       DEFAULT NULL,
    `PREV_FIRE_TIME` BIGINT       DEFAULT NULL,
    `PRIORITY`       INT          DEFAULT NULL,
    `TRIGGER_STATE`  VARCHAR(16)  NOT NULL,
    `TRIGGER_TYPE`   VARCHAR(8)   NOT NULL,
    `START_TIME`     BIGINT       NOT NULL,
    `END_TIME`       BIGINT       DEFAULT NULL,
    `CALENDAR_NAME`  VARCHAR(200) DEFAULT NULL,
    `MISFIRE_INSTR`  SMALLINT     DEFAULT NULL,
    `JOB_DATA`       BLOB,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    KEY              `IDX_QRTZ_T_J` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
    KEY              `IDX_QRTZ_T_JG` (`SCHED_NAME`,`JOB_GROUP`),
    KEY              `IDX_QRTZ_T_C` (`SCHED_NAME`,`CALENDAR_NAME`),
    KEY              `IDX_QRTZ_T_G` (`SCHED_NAME`,`TRIGGER_GROUP`),
    KEY              `IDX_QRTZ_T_STATE` (`SCHED_NAME`,`TRIGGER_STATE`),
    KEY              `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
    KEY              `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
    KEY              `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`,`NEXT_FIRE_TIME`),
    KEY              `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`,`TRIGGER_STATE`,`NEXT_FIRE_TIME`),
    KEY              `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`),
    KEY              `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_STATE`),
    KEY              `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
    CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `QRTZ_JOB_DETAILS` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储简单的 Trigger，包括重复次数，间隔，以及已触发的次数
DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPLE_TRIGGERS`
(
    `SCHED_NAME`      VARCHAR(120) NOT NULL,
    `TRIGGER_NAME`    VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP`   VARCHAR(200) NOT NULL,
    `REPEAT_COUNT`    BIGINT       NOT NULL,
    `REPEAT_INTERVAL` BIGINT       NOT NULL,
    `TIMES_TRIGGERED` BIGINT       NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储 Cron Trigger，包括 Cron 表达式和时区信息
DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
CREATE TABLE `QRTZ_CRON_TRIGGERS`
(
    `SCHED_NAME`      VARCHAR(120) NOT NULL,
    `TRIGGER_NAME`    VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP`   VARCHAR(200) NOT NULL,
    `CRON_EXPRESSION` VARCHAR(120) NOT NULL,
    `TIME_ZONE_ID`    VARCHAR(80) DEFAULT NULL,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储日历信息
DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
CREATE TABLE `QRTZ_CALENDARS`
(
    `SCHED_NAME`    VARCHAR(120) NOT NULL,
    `CALENDAR_NAME` VARCHAR(200) NOT NULL,
    `CALENDAR`      BLOB         NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `CALENDAR_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储已暂停的 Trigger 组的信息
DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS`
(
    `SCHED_NAME`    VARCHAR(120) NOT NULL,
    `TRIGGER_GROUP` VARCHAR(200) NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储与已触发的 Trigger 相关的状态信息，以及相关 Job 的执行信息
DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
CREATE TABLE `QRTZ_FIRED_TRIGGERS`
(
    `SCHED_NAME`        VARCHAR(120) NOT NULL,
    `ENTRY_ID`          VARCHAR(95)  NOT NULL,
    `TRIGGER_NAME`      VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP`     VARCHAR(200) NOT NULL,
    `INSTANCE_NAME`     VARCHAR(200) NOT NULL,
    `FIRED_TIME`        BIGINT       NOT NULL,
    `SCHED_TIME`        BIGINT       NOT NULL,
    `PRIORITY`          INT          NOT NULL,
    `STATE`             VARCHAR(16)  NOT NULL,
    `JOB_NAME`          VARCHAR(200) DEFAULT NULL,
    `JOB_GROUP`         VARCHAR(200) DEFAULT NULL,
    `IS_NONCONCURRENT`  VARCHAR(1)   DEFAULT NULL,
    `REQUESTS_RECOVERY` VARCHAR(1)   DEFAULT NULL,
    PRIMARY KEY (`SCHED_NAME`, `ENTRY_ID`),
    KEY                 `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`,`INSTANCE_NAME`),
    KEY                 `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`,`INSTANCE_NAME`,`REQUESTS_RECOVERY`),
    KEY                 `IDX_QRTZ_FT_J_G` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
    KEY                 `IDX_QRTZ_FT_JG` (`SCHED_NAME`,`JOB_GROUP`),
    KEY                 `IDX_QRTZ_FT_T_G` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
    KEY                 `IDX_QRTZ_FT_TG` (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储少量的有关 Scheduler 的状态信息，和别的 Scheduler 实例(假如是用于一个集群中)
DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
CREATE TABLE `QRTZ_SCHEDULER_STATE`
(
    `SCHED_NAME`        VARCHAR(120) NOT NULL,
    `INSTANCE_NAME`     VARCHAR(200) NOT NULL,
    `LAST_CHECKIN_TIME` BIGINT       NOT NULL,
    `CHECKIN_INTERVAL`  BIGINT       NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `INSTANCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储程序的悲观锁的信息(假如使用了悲观锁)
DROP TABLE IF EXISTS `QRTZ_LOCKS`;
CREATE TABLE `QRTZ_LOCKS`
(
    `SCHED_NAME` VARCHAR(120) NOT NULL,
    `LOCK_NAME`  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `LOCK_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储 Simprop 类型 Trigger 信息
DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPROP_TRIGGERS`
(
    `SCHED_NAME`    VARCHAR(120) NOT NULL,
    `TRIGGER_NAME`  VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP` VARCHAR(200) NOT NULL,
    `STR_PROP_1`    VARCHAR(512)   DEFAULT NULL,
    `STR_PROP_2`    VARCHAR(512)   DEFAULT NULL,
    `STR_PROP_3`    VARCHAR(512)   DEFAULT NULL,
    `INT_PROP_1`    INT            DEFAULT NULL,
    `INT_PROP_2`    INT            DEFAULT NULL,
    `LONG_PROP_1`   BIGINT         DEFAULT NULL,
    `LONG_PROP_2`   BIGINT         DEFAULT NULL,
    `DEC_PROP_1`    DECIMAL(13, 4) DEFAULT NULL,
    `DEC_PROP_2`    DECIMAL(13, 4) DEFAULT NULL,
    `BOOL_PROP_1`   VARCHAR(1)     DEFAULT NULL,
    `BOOL_PROP_2`   VARCHAR(1)     DEFAULT NULL,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储其他类型 Trigger 信息
DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
CREATE TABLE `QRTZ_BLOB_TRIGGERS`
(
    `SCHED_NAME`    VARCHAR(120) NOT NULL,
    `TRIGGER_NAME`  VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP` VARCHAR(200) NOT NULL,
    `BLOB_DATA`     BLOB,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    KEY             `SCHED_NAME` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

-- 默认任务：三个示例任务默认暂停，供任务管理页演示调用目标格式
INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`,
                       `concurrent`, `status`, `remark`, `create_by`)
VALUES (1, '系统默认（无参）', 'DEFAULT', 'sampleTask.noParams', '0/10 * * * * ?', 3, 0, 0, '系统默认示例任务（无参数）',
        'system');
INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`,
                       `concurrent`, `status`, `remark`, `create_by`)
VALUES (2, '系统默认（有参）', 'DEFAULT', 'sampleTask.withParams(''iboot'')', '0/15 * * * * ?', 3, 0, 0,
        '系统默认示例任务（有参数）', 'system');
INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`,
                       `concurrent`, `status`, `remark`, `create_by`)
VALUES (3, '系统默认（多参）', 'DEFAULT', 'sampleTask.multipleParams(''iboot'', true, 2000L, 316.50D, 100)',
        '0/20 * * * * ?', 3, 0, 0, '系统默认示例任务（多参数）', 'system');

-- 每天 02:00 执行一次全量逻辑备份（备份功能未启用时任务体直接返回）
INSERT INTO `sys_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`,
                       `status`, `remark`, `create_by`, `create_time`, `deleted`)
VALUES ('数据库备份', 'SYSTEM', 'backupTask.run', '0 0 2 * * ?', 3, 0, 1,
        '每天凌晨全量导出数据库，加密后写入对象存储', 'system', NOW(), 0);
