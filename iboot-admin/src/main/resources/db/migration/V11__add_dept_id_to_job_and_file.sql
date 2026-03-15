-- 为定时任务表添加 dept_id 字段用于数据权限过滤
ALTER TABLE sys_job ADD COLUMN `dept_id` BIGINT NULL COMMENT '创建人部门 ID' AFTER `job_group`;
ALTER TABLE sys_job ADD INDEX `idx_job_dept_id` (`dept_id`);

-- 为文件表添加 dept_id 字段用于数据权限过滤
ALTER TABLE sys_file ADD COLUMN `dept_id` BIGINT NULL COMMENT '上传人部门 ID' AFTER `file_category`;
ALTER TABLE sys_file ADD INDEX `idx_file_dept_id` (`dept_id`);

-- 同步定时任务的部门 ID（根据创建人关联）
UPDATE sys_job j
SET dept_id = (SELECT dept_id FROM sys_user WHERE username = j.create_by LIMIT 1)
WHERE j.create_by IS NOT NULL AND j.create_by != '';

-- 同步文件的部门 ID（根据上传人关联）
UPDATE sys_file f
SET dept_id = (SELECT dept_id FROM sys_user WHERE username = f.upload_by LIMIT 1)
WHERE f.upload_by IS NOT NULL AND f.upload_by != '';
