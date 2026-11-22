-- 可观测控制台只提供 HTTPS：同步门户中登记的控制台地址。
--
-- 仅更新仍为历史 HTTP 默认值的记录，避免覆盖部署方自定义的外部地址。

UPDATE `sys_config` SET `config_value` = 'https://localhost:9090'
WHERE `config_key` = 'sys.prometheus.monitor.url' AND `config_value` = 'http://localhost:9090';

UPDATE `sys_config` SET `config_value` = 'https://localhost:3000'
WHERE `config_key` = 'sys.grafana.monitor.url' AND `config_value` = 'http://localhost:3000';

UPDATE `sys_config` SET `config_value` = 'https://localhost:5601'
WHERE `config_key` = 'sys.kibana.monitor.url' AND `config_value` = 'http://localhost:5601';

UPDATE `sys_config` SET `config_value` = 'https://localhost:9093'
WHERE `config_key` = 'sys.alertmanager.monitor.url' AND `config_value` = 'http://localhost:9093';

UPDATE `sys_config` SET `config_value` = 'https://localhost:19192'
WHERE `config_key` = 'sys.thanos.query.url' AND `config_value` = 'http://localhost:19192';
