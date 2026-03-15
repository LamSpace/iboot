-- MinIO对象存储配置项
-- 添加MinIO相关系统配置

-- 文件存储方式（核心开关）
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `deleted`, `remark`) 
VALUES (50, '文件存储方式', 'sys.file.storageType', 'local', 1, 'system', NOW(), 0, '文件存储方式：local-本地存储，minio-MinIO对象存储，默认local');

-- MinIO服务端点
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `deleted`, `remark`) 
VALUES (51, 'MinIO服务端点', 'sys.file.minio.endpoint', 'http://localhost:9000', 1, 'system', NOW(), 0, 'MinIO服务器地址，格式：http://host:port 或 https://host:port');

-- MinIO访问密钥
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `deleted`, `remark`) 
VALUES (52, 'MinIO访问密钥', 'sys.file.minio.accessKey', 'minioadmin', 1, 'system', NOW(), 0, 'MinIO访问密钥ID（Access Key）');

-- MinIO秘密密钥
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `deleted`, `remark`) 
VALUES (53, 'MinIO秘密密钥', 'sys.file.minio.secretKey', 'minioadmin', 1, 'system', NOW(), 0, 'MinIO秘密访问密钥（Secret Key），生产环境请使用强密码');

-- MinIO存储桶名称
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `deleted`, `remark`) 
VALUES (54, 'MinIO存储桶名称', 'sys.file.minio.bucketName', 'iboot-files', 1, 'system', NOW(), 0, 'MinIO存储桶名称，必须符合DNS命名规范（小写字母、数字、连字符）');

-- MinIO预签名URL有效期
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `deleted`, `remark`) 
VALUES (55, 'MinIO预签名URL有效期', 'sys.file.minio.urlExpireSeconds', '3600', 1, 'system', NOW(), 0, '预签名URL有效期（秒），默认3600秒（1小时），范围60-604800');
