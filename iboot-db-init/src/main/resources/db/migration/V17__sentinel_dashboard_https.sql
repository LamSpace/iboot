-- Sentinel Dashboard 只提供 HTTPS 并要求登录：同步门户登记地址，关闭跨源 iframe 内嵌。
--
-- 登录会话 Cookie 不能可靠地跨源 iframe 复用；门户改为说明卡 + 新窗口打开。
-- 只更新仍为历史默认值的记录，避免覆盖部署方自定义的外部地址。

UPDATE `sys_config` SET `config_value` = 'https://localhost:8858'
WHERE `config_key` = 'sys.sentinel.dashboard.url' AND `config_value` = 'http://localhost:8858';

UPDATE `sys_config` SET `config_value` = 'false'
WHERE `config_key` = 'sys.sentinel.dashboard.embed' AND `config_value` = 'true';
