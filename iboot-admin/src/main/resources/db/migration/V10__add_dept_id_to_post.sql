-- 为岗位表添加 dept_id 字段用于数据权限过滤
ALTER TABLE sys_post
    ADD COLUMN `dept_id` BIGINT NULL COMMENT '所属部门 ID' AFTER `post_name`;
ALTER TABLE sys_post
    ADD INDEX `idx_post_dept_id` (`dept_id`);

-- 同步历史数据的部门 ID（设置为默认部门或根据创建人关联）
UPDATE sys_post p
SET dept_id = (SELECT dept_id FROM sys_user WHERE username = p.create_by LIMIT 1)
WHERE p.create_by IS NOT NULL AND p.create_by != '';
