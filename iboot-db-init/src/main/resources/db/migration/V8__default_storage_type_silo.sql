-- 默认文件存储切到 Silo，使 Docker Compose 全量部署无需手工改参数。
--
-- 这里不尝试区分“种子默认 local”和“管理员显式 local”：V3 修种子编码、
-- V7 改 MinIO 命名都会触发 update_time 的 ON UPDATE CURRENT_TIMESTAMP，
-- 迁移之后已无法用 update_time 判断是否被人工编辑过。
-- 因此统一切换；需要保留本地存储的环境通过 IBOOT_FILE_STORAGE_TYPE=local
-- 覆盖，或在升级后把该配置改回 local。

UPDATE `sys_config`
SET `config_value` = 'silo',
    `remark` = '文件存储方式：local-本地存储，silo-Silo对象存储，oss-OSS对象存储，默认silo'
WHERE `config_key` = 'sys.file.storageType'
  AND `config_value` = 'local';
