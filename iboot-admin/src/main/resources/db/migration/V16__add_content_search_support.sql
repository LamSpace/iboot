-- 业务全文检索支持
-- 1. 消息接收记录增加更新时间：对账任务靠它发现用户侧删除
-- 2. 种入内容索引对账定时任务（每 5 分钟重放最近 10 分钟变更）

ALTER TABLE `sys_message_receiver`
    ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `is_deleted`;

ALTER TABLE `sys_message_receiver`
    ADD INDEX `idx_receiver_update_time` (`update_time`);

INSERT INTO `sys_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`,
                       `status`, `remark`, `create_by`, `create_time`, `deleted`)
SELECT '内容索引对账', 'SYSTEM', 'contentIndexReconcileTask.reconcile', '0 */5 * * * ?', 3, 0, 1,
       '每 5 分钟重放最近 10 分钟的内容变更，保证全文检索索引最终一致', 'system', NOW(), 0
WHERE NOT EXISTS (SELECT 1
                  FROM `sys_job`
                  WHERE `invoke_target` = 'contentIndexReconcileTask.reconcile'
                    AND `deleted` = 0);
