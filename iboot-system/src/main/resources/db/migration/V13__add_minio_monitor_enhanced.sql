-- MinIO监控功能增强：添加监控开关配置

-- 启用MinIO监控面板配置（使用 IGNORE 避免重复）
INSERT
IGNORE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `deleted`, `remark`)
VALUES (60, '启用MinIO监控', 'sys.minio.monitor.enabled', 'true', 1, 'system', NOW(), 0, '是否在监控面板显示MinIO状态');

-- MinIO控制台地址配置（用于在新窗口打开）
INSERT
IGNORE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `deleted`, `remark`)
VALUES (62, 'MinIO控制台地址', 'sys.minio.console.url', 'http://localhost:9001', 1, 'system', NOW(), 0, 'MinIO管理控制台访问地址，用于在新窗口打开');