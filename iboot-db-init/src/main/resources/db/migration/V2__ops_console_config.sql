-- 运维控制台：门户同源入口与访问开关
--
-- monitor.sba.url 保持相对路径 /sba：开发环境由 Vite 代理转发到 iboot-ops，
-- 生产环境由同源反向代理或内网入口提供。使用绝对地址会让 iframe 跨源，
-- 与 SBA 的 SAMEORIGIN 头冲突。
UPDATE `sys_config`
SET `config_value` = '/sba',
    `remark` = '运维控制台在门户同源路径下的入口'
WHERE `config_key` = 'monitor.sba.url';

-- 生产环境应将该开关置为 false，页面改为内网访问提示
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `remark`)
VALUES ('运维控制台开关', 'monitor.sba.enabled', 'true', 1, 'system',
        '服务器监控页是否内嵌运维控制台；生产环境置为 false，仅保留内网访问')
ON DUPLICATE KEY UPDATE `config_name` = '运维控制台开关',
                        `remark` = '服务器监控页是否内嵌运维控制台；生产环境置为 false，仅保留内网访问';
