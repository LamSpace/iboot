-- 为操作日志表添加执行耗时字段
ALTER TABLE `sys_operate_log` ADD COLUMN `cost_time` BIGINT NULL COMMENT '执行耗时（毫秒）' AFTER `operate_time`;
