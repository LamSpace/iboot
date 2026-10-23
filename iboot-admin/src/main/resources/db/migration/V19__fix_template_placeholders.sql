-- 修正历史模板中重复的美元前缀，使模板变量与 MessageTemplate.fillContent 的匹配规则一致。
-- 使用 CONCAT 构造占位符，避免 Flyway 将脚本中的变量写法当作配置占位符。

UPDATE `sys_message_template`
SET `template_content` = REPLACE(`template_content`, CONCAT('$', '$', '{'), CONCAT('$', '{'))
WHERE `template_content` LIKE CONCAT('%', '$', '$', '{', '%');
