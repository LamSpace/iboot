-- 阿里云OSS对象存储配置项
-- AccessKeyId / AccessKeySecret 不落库，通过环境变量 IBOOT_OSS_ACCESS_KEY_ID /
-- IBOOT_OSS_ACCESS_KEY_SECRET 提供。

INSERT IGNORE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`,
                                 `create_time`, `deleted`, `remark`)
VALUES (83, 'OSS服务端点', 'sys.file.oss.endpoint', 'https://oss-cn-hangzhou.aliyuncs.com', 1, 'system', NOW(), 0,
        '阿里云OSS Endpoint，按实际地域填写，格式：https://oss-cn-<region>.aliyuncs.com');

INSERT IGNORE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`,
                                 `create_time`, `deleted`, `remark`)
VALUES (84, 'OSS存储桶名称', 'sys.file.oss.bucketName', 'iboot-files', 1, 'system', NOW(), 0,
        '阿里云OSS Bucket名称，需预先创建，应用不会自动建桶');

INSERT IGNORE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`,
                                 `create_time`, `deleted`, `remark`)
VALUES (85, 'OSS预签名URL有效期', 'sys.file.oss.urlExpireSeconds', '3600', 1, 'system', NOW(), 0,
        'OSS预签名URL有效期（秒），默认3600秒（1小时）');
