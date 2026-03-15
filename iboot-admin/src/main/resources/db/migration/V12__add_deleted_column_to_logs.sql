-- V12__add_deleted_column_to_logs.sql
-- 为日志表添加逻辑删除字段

-- 为操作日志表添加 deleted 字段
ALTER TABLE sys_operate_log ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除' AFTER `operate_time`;
ALTER TABLE sys_operate_log ADD INDEX `idx_deleted` (`deleted`);

-- 为登录日志表添加 deleted 字段
ALTER TABLE sys_login_log ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除' AFTER `login_time`;
ALTER TABLE sys_login_log ADD INDEX `idx_login_deleted` (`deleted`);
