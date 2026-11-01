-- 站内消息业务跳转类型；通知投递菜单归入消息管理
ALTER TABLE `sys_message`
    ADD COLUMN `business_type` VARCHAR(50) NULL COMMENT '业务类型：APPROVAL_TODO 等' AFTER `message_type`;

-- 历史审批待办消息补上业务类型，使已存在的收件箱消息也能跳转
UPDATE `sys_message`
SET `business_type` = 'APPROVAL_TODO'
WHERE `message_type` = '2'
  AND `title` = '审批待办通知'
  AND `business_type` IS NULL;

-- 通知投递属于消息/通知运维，不放在监控管理下
UPDATE `sys_menu`
SET `parent_id`  = 4,
    `component`  = 'message/NotificationDeliveryView',
    `order_num`  = 3
WHERE `id` = 9200;
