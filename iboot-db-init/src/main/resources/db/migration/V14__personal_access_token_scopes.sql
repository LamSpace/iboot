-- 个人访问令牌的权限范围。
--
-- 逗号分隔的权限码列表；NULL 表示继承用户全部权限（兼容已签发令牌）。
-- scope 必须是用户当前权限的子集，由认证服务在签发时校验。

ALTER TABLE `sys_user_pat`
    ADD COLUMN `scopes` varchar(2000) DEFAULT NULL
        COMMENT '权限码范围，逗号分隔；NULL 表示继承用户全部权限' AFTER `token_id`;
