-- 将 MinIO 命名的配置、菜单与备份记录迁移到 Silo。
--
-- Silo 是 MinIO 的兼容 fork，本迁移只改对外命名，不改桶名、对象键与磁盘格式。
-- 迁移在 V3 修复种子编码之后执行，因此可以直接写入正确的中文文本。

UPDATE `sys_config`
SET `config_key` = REPLACE(`config_key`, 'sys.file.minio.', 'sys.file.silo.')
WHERE `config_key` LIKE 'sys.file.minio.%';

UPDATE `sys_config`
SET `config_key` = REPLACE(`config_key`, 'sys.minio.', 'sys.silo.')
WHERE `config_key` LIKE 'sys.minio.%';

UPDATE `sys_config`
SET `config_value` = 'silo'
WHERE `config_key` = 'sys.file.storageType' AND `config_value` = 'minio';

UPDATE `sys_config`
SET `remark` = REPLACE(`remark`, 'minio-MinIO', 'silo-Silo')
WHERE `config_key` = 'sys.file.storageType';

UPDATE `sys_config`
SET `config_name` = REPLACE(`config_name`, 'MinIO', 'Silo'),
    `remark` = REPLACE(`remark`, 'MinIO', 'Silo')
WHERE `config_name` LIKE '%MinIO%' OR `remark` LIKE '%MinIO%';

UPDATE `sys_menu`
SET `menu_name` = '对象存储监控（Silo）',
    `path` = '/monitor/silo-monitor',
    `component` = 'monitor/silo-monitor/index',
    `permission` = 'monitor:silo'
WHERE `id` = 706;

UPDATE `sys_menu`
SET `menu_name` = 'Silo监控查询',
    `permission` = 'silo:query'
WHERE `id` = 7061;

UPDATE `db_backup_record`
SET `storage_type` = 'silo'
WHERE `storage_type` = 'minio';

ALTER TABLE `db_backup_record`
  MODIFY COLUMN `storage_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '存储类型：local/silo/oss';
