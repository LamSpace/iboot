-- 通知重试与内容索引对账已迁入 iboot-notify 的进程内调度（@Scheduled + Redis 锁）
-- 管理端不再持有这两个任务的实现，保留行会成为指向不存在 Bean 的死配置。
-- 采用逻辑删除，保留变更痕迹；任务管理页不再展示。

UPDATE `sys_job`
SET `deleted` = 1
WHERE `deleted` = 0
  AND `invoke_target` IN ('notificationRetryTask.retry', 'contentIndexReconcileTask.reconcile');
