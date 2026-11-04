-- 修复 V1 基线种子的双重编码
--
-- V1__baseline.sql 中的中文种子文本在生成时被按 Latin-1 误解码后再以 UTF-8 保存，
-- 数据库里因此出现 "ç³»ç»Ÿ" 这类乱码。MySQL 的 latin1 字符集实际是 cp1252，
-- 可以把这些字符还原为原始 UTF-8 字节，再按 utf8mb4 解释，从而恢复中文。
--
-- WHERE 条件只匹配仍含乱码前导字符的行：修复后的中文是 CJK 字符，不会再次命中。
-- 因此迁移可重复执行，也不会破坏用户后来录入的正确中文。

UPDATE `sys_config`
SET `config_name` = CONVERT(BINARY(CONVERT(`config_name` USING latin1)) USING utf8mb4)
WHERE `config_name` REGEXP '[äåæçèéïâã]';

UPDATE `sys_config`
SET `config_value` = CONVERT(BINARY(CONVERT(`config_value` USING latin1)) USING utf8mb4)
WHERE `config_value` REGEXP '[äåæçèéïâã]';

UPDATE `sys_config`
SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4)
WHERE `remark` REGEXP '[äåæçèéïâã]';

UPDATE `sys_dept`
SET `dept_name` = CONVERT(BINARY(CONVERT(`dept_name` USING latin1)) USING utf8mb4)
WHERE `dept_name` REGEXP '[äåæçèéïâã]';

UPDATE `sys_dept`
SET `leader` = CONVERT(BINARY(CONVERT(`leader` USING latin1)) USING utf8mb4)
WHERE `leader` REGEXP '[äåæçèéïâã]';

UPDATE `sys_dict_data`
SET `dict_label` = CONVERT(BINARY(CONVERT(`dict_label` USING latin1)) USING utf8mb4)
WHERE `dict_label` REGEXP '[äåæçèéïâã]';

UPDATE `sys_dict_data`
SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4)
WHERE `remark` REGEXP '[äåæçèéïâã]';

UPDATE `sys_dict_type`
SET `dict_name` = CONVERT(BINARY(CONVERT(`dict_name` USING latin1)) USING utf8mb4)
WHERE `dict_name` REGEXP '[äåæçèéïâã]';

UPDATE `sys_dict_type`
SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4)
WHERE `remark` REGEXP '[äåæçèéïâã]';

UPDATE `sys_job`
SET `job_name` = CONVERT(BINARY(CONVERT(`job_name` USING latin1)) USING utf8mb4)
WHERE `job_name` REGEXP '[äåæçèéïâã]';

UPDATE `sys_job`
SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4)
WHERE `remark` REGEXP '[äåæçèéïâã]';

UPDATE `sys_menu`
SET `menu_name` = CONVERT(BINARY(CONVERT(`menu_name` USING latin1)) USING utf8mb4)
WHERE `menu_name` REGEXP '[äåæçèéïâã]';

UPDATE `sys_menu`
SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4)
WHERE `remark` REGEXP '[äåæçèéïâã]';

UPDATE `sys_message_template`
SET `template_name` = CONVERT(BINARY(CONVERT(`template_name` USING latin1)) USING utf8mb4)
WHERE `template_name` REGEXP '[äåæçèéïâã]';

UPDATE `sys_message_template`
SET `template_content` = CONVERT(BINARY(CONVERT(`template_content` USING latin1)) USING utf8mb4)
WHERE `template_content` REGEXP '[äåæçèéïâã]';

UPDATE `sys_message_template`
SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4)
WHERE `remark` REGEXP '[äåæçèéïâã]';

UPDATE `sys_post`
SET `post_name` = CONVERT(BINARY(CONVERT(`post_name` USING latin1)) USING utf8mb4)
WHERE `post_name` REGEXP '[äåæçèéïâã]';

UPDATE `sys_post`
SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4)
WHERE `remark` REGEXP '[äåæçèéïâã]';

UPDATE `sys_role`
SET `role_name` = CONVERT(BINARY(CONVERT(`role_name` USING latin1)) USING utf8mb4)
WHERE `role_name` REGEXP '[äåæçèéïâã]';

UPDATE `sys_role`
SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4)
WHERE `remark` REGEXP '[äåæçèéïâã]';

UPDATE `sys_user`
SET `nickname` = CONVERT(BINARY(CONVERT(`nickname` USING latin1)) USING utf8mb4)
WHERE `nickname` REGEXP '[äåæçèéïâã]';

UPDATE `sys_user`
SET `remark` = CONVERT(BINARY(CONVERT(`remark` USING latin1)) USING utf8mb4)
WHERE `remark` REGEXP '[äåæçèéïâã]';
