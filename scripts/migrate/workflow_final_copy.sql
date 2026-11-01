-- 审批工作流最终增量补齐：iboot_admin 的残留行 -> iboot_workflow
--
-- 仅在 workflow_final_check.sql 出现 RESIDUAL 时执行。只复制新库中不存在的高 id 行，
-- 不覆盖新库已有记录；同 id 内容冲突需要人工判断。
--
-- 执行：
--   docker exec -i iboot-mysql mysql -uroot -proot < scripts/migrate/workflow_final_copy.sql

INSERT IGNORE INTO iboot_workflow.biz_leave_application
SELECT * FROM iboot_admin.biz_leave_application
WHERE id > (SELECT COALESCE(MAX(id), 0) FROM iboot_workflow.biz_leave_application);

-- 其它表若出现残留，按同样的 `INSERT IGNORE ... SELECT ... WHERE` 方式逐表补齐。
