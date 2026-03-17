-- iBoot 后台管理系统 - 完整数据库表结构初始化脚本
-- MySQL 8.0+
-- 包含所有模块的表结构定义
-- 注意：Flyway 管理的迁移脚本不应包含 CREATE DATABASE / USE 语句

-- ===================================================================
-- 核心业务表
-- ===================================================================

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(30)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码',
    `nickname`    VARCHAR(30) NULL COMMENT '昵称',
    `email`       VARCHAR(50) NULL COMMENT '邮箱',
    `phone`       VARCHAR(11) NULL COMMENT '手机号码',
    `gender`      TINYINT NULL DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    `avatar`      VARCHAR(255) NULL COMMENT '头像地址',
    `user_type`   TINYINT NULL DEFAULT 2 COMMENT '用户类型：1-系统用户，2-普通用户',
    `dept_id`     BIGINT NULL COMMENT '部门ID',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `login_ip`    VARCHAR(50) NULL COMMENT '最后登录IP',
    `login_time`  DATETIME NULL COMMENT '最后登录时间',
    `create_by`   VARCHAR(50) NULL COMMENT '创建人',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(50) NULL COMMENT '更新人',
    `update_time` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`      VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY           `idx_dept_id` (`dept_id`),
    KEY           `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 2. 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name`   VARCHAR(30) NOT NULL COMMENT '角色名称',
    `role_code`   VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_sort`   INT         NOT NULL DEFAULT 0 COMMENT '角色排序',
    `data_scope`  TINYINT NULL DEFAULT 1 COMMENT '数据权限范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `create_by`   VARCHAR(50) NULL COMMENT '创建人',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(50) NULL COMMENT '更新人',
    `update_time` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`      VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----------------------------
-- 3. 菜单表
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `menu_name`   VARCHAR(50) NOT NULL COMMENT '菜单名称',
    `parent_id`   BIGINT NULL DEFAULT 0 COMMENT '父菜单ID',
    `menu_type`   CHAR(1)     NOT NULL COMMENT '菜单类型：M-目录，C-菜单，F-按钮',
    `path`        VARCHAR(200) NULL COMMENT '路由地址',
    `component`   VARCHAR(255) NULL COMMENT '组件路径',
    `permission`  VARCHAR(100) NULL COMMENT '权限标识',
    `icon`        VARCHAR(100) NULL COMMENT '菜单图标',
    `order_num`   INT         NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `is_frame`    TINYINT     NOT NULL DEFAULT 0 COMMENT '是否外链：0-否，1-是',
    `is_cache`    TINYINT     NOT NULL DEFAULT 1 COMMENT '是否缓存：0-不缓存，1-缓存',
    `visible`     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否可见：0-隐藏，1-显示',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `create_by`   VARCHAR(50) NULL COMMENT '创建人',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(50) NULL COMMENT '更新人',
    `update_time` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`      VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY           `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- ----------------------------
-- 4. 部门表
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    `parent_id`   BIGINT NULL DEFAULT 0 COMMENT '父部门ID',
    `dept_name`   VARCHAR(30) NOT NULL COMMENT '部门名称',
    `dept_code`   VARCHAR(50) NULL COMMENT '部门编码',
    `order_num`   INT         NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `leader`      VARCHAR(20) NULL COMMENT '负责人',
    `phone`       VARCHAR(11) NULL COMMENT '联系电话',
    `email`       VARCHAR(50) NULL COMMENT '邮箱',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `create_by`   VARCHAR(50) NULL COMMENT '创建人',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(50) NULL COMMENT '更新人',
    `update_time` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY           `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- ----------------------------
-- 5. 岗位表
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `post_code`   VARCHAR(50) NOT NULL COMMENT '岗位编码',
    `post_name`   VARCHAR(50) NOT NULL COMMENT '岗位名称',
    `post_sort`   INT         NOT NULL DEFAULT 0 COMMENT '岗位排序',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `create_by`   VARCHAR(50) NULL COMMENT '创建人',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(50) NULL COMMENT '更新人',
    `update_time` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`      VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_code` (`post_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';

-- ----------------------------
-- 6. 字典类型表
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典类型ID',
    `dict_name`   VARCHAR(100) NOT NULL COMMENT '字典名称',
    `dict_type`   VARCHAR(100) NOT NULL COMMENT '字典类型',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `create_by`   VARCHAR(50) NULL COMMENT '创建人',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(50) NULL COMMENT '更新人',
    `update_time` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`      VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

-- ----------------------------
-- 7. 字典数据表
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
    `dict_type`   VARCHAR(100) NOT NULL COMMENT '字典类型',
    `dict_label`  VARCHAR(100) NOT NULL COMMENT '字典标签',
    `dict_value`  VARCHAR(100) NOT NULL COMMENT '字典键值',
    `dict_sort`   INT          NOT NULL DEFAULT 0 COMMENT '字典排序',
    `css_class`   VARCHAR(100) NULL COMMENT '样式属性',
    `list_class`  VARCHAR(100) NULL COMMENT '表格回显样式',
    `is_default`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认：0-否，1-是',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `create_by`   VARCHAR(50) NULL COMMENT '创建人',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(50) NULL COMMENT '更新人',
    `update_time` DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`      VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY           `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

-- ----------------------------
-- 8. 系统配置表
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_name`  VARCHAR(100) NOT NULL COMMENT '配置名称',
    `config_key`   VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
    `config_type`  TINYINT      NOT NULL DEFAULT 0 COMMENT '配置类型：0-默认，1-系统',
    `create_by`    VARCHAR(50) NULL COMMENT '创建人',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(50) NULL COMMENT '更新人',
    `update_time`  DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`       VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ===================================================================
-- 关联关系表
-- ===================================================================

-- 用户角色关联表
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`
(
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 用户岗位关联表
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`
(
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `post_id` BIGINT NOT NULL COMMENT '岗位ID',
    PRIMARY KEY (`user_id`, `post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户岗位关联表';

-- 角色菜单关联表
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`
(
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- 角色部门关联表（数据权限）
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`
(
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `dept_id` BIGINT NOT NULL COMMENT '部门ID',
    PRIMARY KEY (`role_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色部门关联表';

-- ===================================================================
-- 日志表
-- ===================================================================

-- 登录日志表
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`
(
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `username`       VARCHAR(50) NOT NULL COMMENT '用户名',
    `ip_address`     VARCHAR(50) NULL COMMENT '登录IP',
    `login_location` VARCHAR(255) NULL COMMENT '登录地点',
    `browser`        VARCHAR(50) NULL COMMENT '浏览器',
    `os`             VARCHAR(50) NULL COMMENT '操作系统',
    `status`         TINYINT     NOT NULL COMMENT '登录状态：0-失败，1-成功',
    `message`        VARCHAR(255) NULL COMMENT '提示消息',
    `login_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY              `idx_username` (`username`),
    KEY              `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- 操作日志表
DROP TABLE IF EXISTS `sys_operate_log`;
CREATE TABLE `sys_operate_log`
(
    `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `title`          VARCHAR(50) NULL COMMENT '模块标题',
    `business_type`  TINYINT NULL COMMENT '业务类型：0-其它，1-新增，2-修改，3-删除',
    `method`         VARCHAR(100) NULL COMMENT '方法名称',
    `request_method` VARCHAR(10) NULL COMMENT '请求方式',
    `operator_type`  TINYINT NULL COMMENT '操作类别：0-其它，1-后台用户，2-手机端用户',
    `operator_name`  VARCHAR(50) NULL COMMENT '操作人员',
    `dept_name`      VARCHAR(50) NULL COMMENT '部门名称',
    `url`            VARCHAR(255) NULL COMMENT '请求URL',
    `ip`             VARCHAR(50) NULL COMMENT '主机地址',
    `location`       VARCHAR(255) NULL COMMENT '操作地点',
    `param`          TEXT NULL COMMENT '请求参数',
    `result`         TEXT NULL COMMENT '返回参数',
    `status`         TINYINT  NOT NULL COMMENT '操作状态：0-失败，1-成功',
    `error_msg`      TEXT NULL COMMENT '错误消息',
    `operate_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY              `idx_operator_name` (`operator_name`),
    KEY              `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ===================================================================
-- 系统公告模块
-- ===================================================================

-- 系统公告表
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `notice_title`   VARCHAR(200) NOT NULL COMMENT '公告标题',
    `notice_type`    CHAR(1)      NOT NULL COMMENT '公告类型（字典 sys_notice_type）',
    `notice_content` TEXT         NOT NULL COMMENT '公告内容',
    `status`         CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态（字典 sys_notice_status）',
    `is_top`         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
    `create_by`      VARCHAR(50) NULL COMMENT '创建人',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      VARCHAR(50) NULL COMMENT '更新人',
    `update_time`    DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`         VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY              `idx_status_top` (`status`, `is_top`),
    KEY              `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

-- 公告已读记录表（无逻辑删除）
DROP TABLE IF EXISTS `sys_notice_read`;
CREATE TABLE `sys_notice_read`
(
    `id`        BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `notice_id` BIGINT   NOT NULL COMMENT '公告ID',
    `user_id`   BIGINT   NOT NULL COMMENT '用户ID',
    `read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_user` (`notice_id`, `user_id`),
    KEY         `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告已读记录表';

-- ===================================================================
-- 消息中心模块
-- ===================================================================

-- 消息主表
DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `title`         VARCHAR(200) NOT NULL COMMENT '消息标题',
    `content`       TEXT         NOT NULL COMMENT '消息内容',
    `message_type`  CHAR(1)      NOT NULL COMMENT '消息类型（字典 sys_message_type）',
    `priority`      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '优先级（字典 sys_message_priority）',
    `sender_type`   CHAR(1)      NOT NULL DEFAULT '0' COMMENT '发送者类型（字典 sys_message_sender_type）',
    `sender_id`     BIGINT NULL COMMENT '发送者用户ID（管理员发送时记录）',
    `receiver_type` CHAR(1)      NOT NULL DEFAULT '1' COMMENT '接收者类型（字典 sys_message_receiver_type）',
    `status`        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态（字典 sys_message_status）',
    `create_by`     VARCHAR(50) NULL COMMENT '创建人',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(50) NULL COMMENT '更新人',
    `update_time`   DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`        VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY             `idx_status_create_time` (`status`, `create_time`),
    KEY             `idx_sender` (`sender_type`, `sender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息主表';

-- 消息接收记录表（无逻辑删除）
DROP TABLE IF EXISTS `sys_message_receiver`;
CREATE TABLE `sys_message_receiver`
(
    `id`         BIGINT  NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `message_id` BIGINT  NOT NULL COMMENT '消息ID',
    `user_id`    BIGINT  NOT NULL COMMENT '接收者用户ID',
    `is_read`    TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `read_time`  DATETIME NULL COMMENT '阅读时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '用户侧删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_user` (`message_id`, `user_id`),
    KEY          `idx_user_read` (`user_id`, `is_read`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息接收记录表';

-- 消息模板表
DROP TABLE IF EXISTS `sys_message_template`;
CREATE TABLE `sys_message_template`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_code`    VARCHAR(100) NOT NULL COMMENT '模板编码',
    `template_name`    VARCHAR(200) NOT NULL COMMENT '模板名称',
    `template_content` TEXT         NOT NULL COMMENT '模板内容（支持$${变量名}占位符）',
    `message_type`     CHAR(1)      NOT NULL COMMENT '消息类型（字典 sys_message_type）',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `create_by`        VARCHAR(50) NULL COMMENT '创建人',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(50) NULL COMMENT '更新人',
    `update_time`      DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`           VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    KEY                `idx_message_type` (`message_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息模板表';

-- ===================================================================
-- 文件管理模块
-- ===================================================================

-- 文件信息表
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `file_name`     VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_path`     VARCHAR(500) NOT NULL COMMENT '存储路径',
    `file_size`     BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `file_type`     VARCHAR(100) NULL COMMENT 'MIME类型',
    `file_ext`      VARCHAR(50) NULL COMMENT '文件扩展名',
    `file_category` VARCHAR(100) NULL COMMENT '文件分类（关联字典 sys_file_category）',
    `upload_by`     VARCHAR(50) NULL COMMENT '上传人',
    `upload_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `create_by`     VARCHAR(50) NULL COMMENT '创建人',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(50) NULL COMMENT '更新人',
    `update_time`   DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `remark`        VARCHAR(500) NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY             `idx_file_name` (`file_name`),
    KEY             `idx_file_category` (`file_category`),
    KEY             `idx_upload_by` (`upload_by`),
    KEY             `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';

-- ===================================================================
-- 定时任务模块
-- ===================================================================

-- 定时任务表
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `job_name`        VARCHAR(64)  NOT NULL COMMENT '任务名称',
    `job_group`       VARCHAR(64)  NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
    `invoke_target`   VARCHAR(500) NOT NULL COMMENT '调用目标字符串(beanName.methodName)',
    `cron_expression` VARCHAR(255)          DEFAULT '' COMMENT 'cron执行表达式',
    `misfire_policy`  TINYINT      NOT NULL DEFAULT 3 COMMENT '计划执行错误策略(1立即执行 2执行一次 3放弃执行)',
    `concurrent`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否并发执行(0禁止 1允许)',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(0暂停 1正常)',
    `remark`          VARCHAR(500)          DEFAULT '' COMMENT '备注信息',
    `create_by`       VARCHAR(64)           DEFAULT '' COMMENT '创建者',
    `create_time`     DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)           DEFAULT '' COMMENT '更新者',
    `update_time`     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志(0存在 1删除)',
    PRIMARY KEY (`id`),
    KEY               `idx_job_group` (`job_group`),
    KEY               `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务表';

-- 定时任务执行日志表
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `job_id`         BIGINT       NOT NULL COMMENT '任务ID',
    `job_name`       VARCHAR(64)  NOT NULL COMMENT '任务名称',
    `job_group`      VARCHAR(64)  NOT NULL COMMENT '任务组名',
    `invoke_target`  VARCHAR(500) NOT NULL COMMENT '调用目标字符串',
    `job_message`    VARCHAR(500)          DEFAULT '' COMMENT '日志信息',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '执行状态(0失败 1成功)',
    `exception_info` TEXT COMMENT '异常信息',
    `start_time`     DATETIME              DEFAULT NULL COMMENT '开始时间',
    `end_time`       DATETIME              DEFAULT NULL COMMENT '结束时间',
    `cost_time`      BIGINT                DEFAULT 0 COMMENT '执行耗时(毫秒)',
    `create_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY              `idx_job_id` (`job_id`),
    KEY              `idx_status` (`status`),
    KEY              `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务执行日志表';

-- Quartz 调度框架表结构（MySQL）
-- 存储每一个已配置的 Job 的详细信息
DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
CREATE TABLE `QRTZ_JOB_DETAILS`
(
    `SCHED_NAME`        VARCHAR(120) NOT NULL,
    `JOB_NAME`          VARCHAR(200) NOT NULL,
    `JOB_GROUP`         VARCHAR(200) NOT NULL,
    `DESCRIPTION`       VARCHAR(250) DEFAULT NULL,
    `JOB_CLASS_NAME`    VARCHAR(250) NOT NULL,
    `IS_DURABLE`        VARCHAR(1)   NOT NULL,
    `IS_NONCONCURRENT`  VARCHAR(1)   NOT NULL,
    `IS_UPDATE_DATA`    VARCHAR(1)   NOT NULL,
    `REQUESTS_RECOVERY` VARCHAR(1)   NOT NULL,
    `JOB_DATA`          BLOB,
    PRIMARY KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`),
    KEY                 `IDX_QRTZ_J_REQ_RECOVERY` (`SCHED_NAME`,`REQUESTS_RECOVERY`),
    KEY                 `IDX_QRTZ_J_GRP` (`SCHED_NAME`,`JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储已配置的 Trigger 的信息
DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
CREATE TABLE `QRTZ_TRIGGERS`
(
    `SCHED_NAME`     VARCHAR(120) NOT NULL,
    `TRIGGER_NAME`   VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP`  VARCHAR(200) NOT NULL,
    `JOB_NAME`       VARCHAR(200) NOT NULL,
    `JOB_GROUP`      VARCHAR(200) NOT NULL,
    `DESCRIPTION`    VARCHAR(250) DEFAULT NULL,
    `NEXT_FIRE_TIME` BIGINT       DEFAULT NULL,
    `PREV_FIRE_TIME` BIGINT       DEFAULT NULL,
    `PRIORITY`       INT          DEFAULT NULL,
    `TRIGGER_STATE`  VARCHAR(16)  NOT NULL,
    `TRIGGER_TYPE`   VARCHAR(8)   NOT NULL,
    `START_TIME`     BIGINT       NOT NULL,
    `END_TIME`       BIGINT       DEFAULT NULL,
    `CALENDAR_NAME`  VARCHAR(200) DEFAULT NULL,
    `MISFIRE_INSTR`  SMALLINT     DEFAULT NULL,
    `JOB_DATA`       BLOB,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    KEY              `IDX_QRTZ_T_J` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
    KEY              `IDX_QRTZ_T_JG` (`SCHED_NAME`,`JOB_GROUP`),
    KEY              `IDX_QRTZ_T_C` (`SCHED_NAME`,`CALENDAR_NAME`),
    KEY              `IDX_QRTZ_T_G` (`SCHED_NAME`,`TRIGGER_GROUP`),
    KEY              `IDX_QRTZ_T_STATE` (`SCHED_NAME`,`TRIGGER_STATE`),
    KEY              `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
    KEY              `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
    KEY              `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`,`NEXT_FIRE_TIME`),
    KEY              `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`,`TRIGGER_STATE`,`NEXT_FIRE_TIME`),
    KEY              `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`),
    KEY              `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_STATE`),
    KEY              `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
    CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `QRTZ_JOB_DETAILS` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储简单的 Trigger，包括重复次数，间隔，以及已触发的次数
DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPLE_TRIGGERS`
(
    `SCHED_NAME`      VARCHAR(120) NOT NULL,
    `TRIGGER_NAME`    VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP`   VARCHAR(200) NOT NULL,
    `REPEAT_COUNT`    BIGINT       NOT NULL,
    `REPEAT_INTERVAL` BIGINT       NOT NULL,
    `TIMES_TRIGGERED` BIGINT       NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储 Cron Trigger，包括 Cron 表达式和时区信息
DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
CREATE TABLE `QRTZ_CRON_TRIGGERS`
(
    `SCHED_NAME`      VARCHAR(120) NOT NULL,
    `TRIGGER_NAME`    VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP`   VARCHAR(200) NOT NULL,
    `CRON_EXPRESSION` VARCHAR(120) NOT NULL,
    `TIME_ZONE_ID`    VARCHAR(80) DEFAULT NULL,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储日历信息
DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
CREATE TABLE `QRTZ_CALENDARS`
(
    `SCHED_NAME`    VARCHAR(120) NOT NULL,
    `CALENDAR_NAME` VARCHAR(200) NOT NULL,
    `CALENDAR`      BLOB         NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `CALENDAR_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储已暂停的 Trigger 组的信息
DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS`
(
    `SCHED_NAME`    VARCHAR(120) NOT NULL,
    `TRIGGER_GROUP` VARCHAR(200) NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储与已触发的 Trigger 相关的状态信息，以及相关 Job 的执行信息
DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
CREATE TABLE `QRTZ_FIRED_TRIGGERS`
(
    `SCHED_NAME`        VARCHAR(120) NOT NULL,
    `ENTRY_ID`          VARCHAR(95)  NOT NULL,
    `TRIGGER_NAME`      VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP`     VARCHAR(200) NOT NULL,
    `INSTANCE_NAME`     VARCHAR(200) NOT NULL,
    `FIRED_TIME`        BIGINT       NOT NULL,
    `SCHED_TIME`        BIGINT       NOT NULL,
    `PRIORITY`          INT          NOT NULL,
    `STATE`             VARCHAR(16)  NOT NULL,
    `JOB_NAME`          VARCHAR(200) DEFAULT NULL,
    `JOB_GROUP`         VARCHAR(200) DEFAULT NULL,
    `IS_NONCONCURRENT`  VARCHAR(1)   DEFAULT NULL,
    `REQUESTS_RECOVERY` VARCHAR(1)   DEFAULT NULL,
    PRIMARY KEY (`SCHED_NAME`, `ENTRY_ID`),
    KEY                 `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`,`INSTANCE_NAME`),
    KEY                 `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`,`INSTANCE_NAME`,`REQUESTS_RECOVERY`),
    KEY                 `IDX_QRTZ_FT_J_G` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
    KEY                 `IDX_QRTZ_FT_JG` (`SCHED_NAME`,`JOB_GROUP`),
    KEY                 `IDX_QRTZ_FT_T_G` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
    KEY                 `IDX_QRTZ_FT_TG` (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储少量的有关 Scheduler 的状态信息，和别的 Scheduler 实例(假如是用于一个集群中)
DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
CREATE TABLE `QRTZ_SCHEDULER_STATE`
(
    `SCHED_NAME`        VARCHAR(120) NOT NULL,
    `INSTANCE_NAME`     VARCHAR(200) NOT NULL,
    `LAST_CHECKIN_TIME` BIGINT       NOT NULL,
    `CHECKIN_INTERVAL`  BIGINT       NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `INSTANCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储程序的悲观锁的信息(假如使用了悲观锁)
DROP TABLE IF EXISTS `QRTZ_LOCKS`;
CREATE TABLE `QRTZ_LOCKS`
(
    `SCHED_NAME` VARCHAR(120) NOT NULL,
    `LOCK_NAME`  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `LOCK_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储 Simprop 类型 Trigger 信息
DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
CREATE TABLE `QRTZ_SIMPROP_TRIGGERS`
(
    `SCHED_NAME`    VARCHAR(120) NOT NULL,
    `TRIGGER_NAME`  VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP` VARCHAR(200) NOT NULL,
    `STR_PROP_1`    VARCHAR(512)   DEFAULT NULL,
    `STR_PROP_2`    VARCHAR(512)   DEFAULT NULL,
    `STR_PROP_3`    VARCHAR(512)   DEFAULT NULL,
    `INT_PROP_1`    INT            DEFAULT NULL,
    `INT_PROP_2`    INT            DEFAULT NULL,
    `LONG_PROP_1`   BIGINT         DEFAULT NULL,
    `LONG_PROP_2`   BIGINT         DEFAULT NULL,
    `DEC_PROP_1`    DECIMAL(13, 4) DEFAULT NULL,
    `DEC_PROP_2`    DECIMAL(13, 4) DEFAULT NULL,
    `BOOL_PROP_1`   VARCHAR(1)     DEFAULT NULL,
    `BOOL_PROP_2`   VARCHAR(1)     DEFAULT NULL,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 存储其他类型 Trigger 信息
DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
CREATE TABLE `QRTZ_BLOB_TRIGGERS`
(
    `SCHED_NAME`    VARCHAR(120) NOT NULL,
    `TRIGGER_NAME`  VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP` VARCHAR(200) NOT NULL,
    `BLOB_DATA`     BLOB,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    KEY             `SCHED_NAME` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
    CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
