-- 操作审计的认证方式。
--
-- SESSION 表示交互式登录会话，PAT 表示个人访问令牌。
-- 旧记录使用默认值 SESSION；滚动发布期间未升级实例写入的记录也按默认值解释。

ALTER TABLE `sys_operate_log`
    ADD COLUMN `auth_type` varchar(16) NOT NULL DEFAULT 'SESSION'
        COMMENT '认证方式：SESSION/PAT' AFTER `cost_time`;
