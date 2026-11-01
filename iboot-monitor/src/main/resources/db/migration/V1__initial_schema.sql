-- 监控与日志服务初始结构
--
-- 本服务拥有审计表：sys_operate_log 由各服务经内核异步写入，
-- sys_login_log 由认证服务写入；两者都只在本 schema 中定义结构，
-- 其他服务以跨 schema 写入的方式提交记录。
--
-- DDL 由管理端 V1（基础列）+ V4（cost_time）+ V9（dept_id）+ V12（deleted）合并而成。

CREATE TABLE `sys_login_log`
(
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `username`       VARCHAR(50) NOT NULL COMMENT '用户名',
    `dept_id`        BIGINT      NULL COMMENT '登录用户部门 ID',
    `ip_address`     VARCHAR(50) NULL COMMENT '登录IP',
    `login_location` VARCHAR(255) NULL COMMENT '登录地点',
    `browser`        VARCHAR(50) NULL COMMENT '浏览器',
    `os`             VARCHAR(50) NULL COMMENT '操作系统',
    `status`         TINYINT     NOT NULL COMMENT '登录状态：0-失败，1-成功',
    `message`        VARCHAR(255) NULL COMMENT '提示消息',
    `login_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `deleted`        TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_username` (`username`),
    KEY `idx_login_time` (`login_time`),
    KEY `idx_login_dept_id` (`dept_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

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
    `dept_id`        BIGINT NULL COMMENT '操作人员部门 ID',
    `url`            VARCHAR(255) NULL COMMENT '请求URL',
    `ip`             VARCHAR(50) NULL COMMENT '主机地址',
    `location`       VARCHAR(255) NULL COMMENT '操作地点',
    `param`          TEXT NULL COMMENT '请求参数',
    `result`         TEXT NULL COMMENT '返回参数',
    `status`         TINYINT  NOT NULL COMMENT '操作状态：0-失败，1-成功',
    `error_msg`      TEXT NULL COMMENT '错误消息',
    `operate_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `deleted`        TINYINT  NOT NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    `cost_time`      BIGINT NULL COMMENT '执行耗时（毫秒）',
    PRIMARY KEY (`id`),
    KEY `idx_operator_name` (`operator_name`),
    KEY `idx_operate_time` (`operate_time`),
    KEY `idx_operate_dept_id` (`dept_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
