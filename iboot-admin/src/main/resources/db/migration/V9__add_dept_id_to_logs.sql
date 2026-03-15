-- 为操作日志表添加 dept_id 字段用于数据权限过滤
ALTER TABLE sys_operate_log ADD COLUMN `dept_id` BIGINT NULL COMMENT '操作人员部门 ID' AFTER `dept_name`;
ALTER TABLE sys_operate_log ADD INDEX `idx_operate_dept_id` (`dept_id`);

-- 为登录日志表添加 dept_id 字段用于数据权限过滤
ALTER TABLE sys_login_log ADD COLUMN `dept_id` BIGINT NULL COMMENT '登录用户部门 ID' AFTER `username`;
ALTER TABLE sys_login_log ADD INDEX `idx_login_dept_id` (`dept_id`);

-- 同步历史数据的部门 ID（通过部门名称关联）
UPDATE sys_operate_log ol
SET dept_id = (SELECT id FROM sys_user WHERE username = ol.operator_name LIMIT 1)
WHERE ol.operator_name IS NOT NULL AND ol.operator_name != '';

-- 同步历史数据的部门 ID（通过用户名关联）
UPDATE sys_login_log ll
SET dept_id = (SELECT dept_id FROM sys_user WHERE username = ll.username LIMIT 1)
WHERE ll.username IS NOT NULL AND ll.username != '';
