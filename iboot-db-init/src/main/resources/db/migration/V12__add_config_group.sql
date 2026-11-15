-- 为系统参数配置增加分组属性，并按配置键前缀回填存量数据。
--
-- 分组编码是稳定标识，显示名与排序由字典 sys_config_group 维护；
-- 未匹配的配置保留列默认值 other（未分组），不阻断迁移。

ALTER TABLE `sys_config`
    ADD COLUMN `config_group` varchar(50) NOT NULL DEFAULT 'other' AFTER `config_type`;

CREATE INDEX `idx_config_group` ON `sys_config` (`config_group`);

-- 监控与告警（排除 sba，sba 属于控制台入口）
UPDATE `sys_config`
SET `config_group` = 'monitor'
WHERE `config_key` LIKE 'monitor.%'
  AND `config_key` NOT LIKE 'monitor.sba.%';

-- 控制台入口
UPDATE `sys_config`
SET `config_group` = 'console'
WHERE `config_key` LIKE 'sys.%.monitor.url'
   OR `config_key` = 'sys.thanos.query.url'
   OR `config_key` LIKE 'sys.swagger.%'
   OR `config_key` = 'sys.druid.monitor.url'
   OR `config_key` LIKE 'monitor.sba.%'
   OR `config_key` LIKE 'sys.nacos.console.%'
   OR `config_key` LIKE 'sys.sentinel.dashboard.%';

-- 文件存储
UPDATE `sys_config`
SET `config_group` = 'storage'
WHERE `config_key` LIKE 'sys.file.%'
   OR `config_key` LIKE 'sys.silo.%';

-- 公告与消息
UPDATE `sys_config`
SET `config_group` = 'message'
WHERE `config_key` LIKE 'sys.notice.%'
   OR `config_key` LIKE 'sys.message.%';

-- 基础与账号安全
UPDATE `sys_config`
SET `config_group` = 'basic'
WHERE `config_key` = 'sys.name'
   OR `config_key` LIKE 'sys.user.%'
   OR `config_key` LIKE 'sys.account.%'
   OR `config_key` LIKE 'sys.session.%'
   OR `config_key` LIKE 'sys.login.%';

-- 监控刷新间隔不在 monitor.* 前缀下，单独归入监控
UPDATE `sys_config`
SET `config_group` = 'monitor'
WHERE `config_key` = 'sys.online.refreshInterval';

INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('参数分组', 'sys_config_group', 1, 'system', '系统参数配置的分组标签与排序')
ON DUPLICATE KEY UPDATE `dict_name` = '参数分组',
                        `remark` = '系统参数配置的分组标签与排序';

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `list_class`, `is_default`,
                             `status`, `create_by`, `remark`)
VALUES ('sys_config_group', '基础与账号安全', 'basic', 1, 'primary', 0, 1, 'system', '系统名称、账号、会话与登录安全'),
       ('sys_config_group', '公告与消息', 'message', 2, 'success', 0, 1, 'system', '公告展示与站内消息保留策略'),
       ('sys_config_group', '文件存储', 'storage', 3, 'warning', 0, 1, 'system', '本地上传、Silo 与 OSS 存储参数'),
       ('sys_config_group', '监控与告警', 'monitor', 4, 'danger', 0, 1, 'system', 'CPU、内存、JVM、磁盘与 Redis 阈值'),
       ('sys_config_group', '控制台入口', 'console', 5, 'info', 0, 1, 'system', 'Grafana、Kibana、SBA、Swagger 等入口'),
       ('sys_config_group', '未分组', 'other', 6, NULL, 1, 1, 'system', '新增或未匹配分组的配置');
