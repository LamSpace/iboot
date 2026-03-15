/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.admin.common.cloudevent;

/**
 * CloudEvents 事件类型常量
 * <p>
 * 定义系统中所有业务事件类型，遵循 CloudEvents 规范
 * 命名格式：com.iboot.{domain}.{action}
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class CloudEventTypes {

    private CloudEventTypes() {
        // 防止实例化
    }

    // ==================== 通用响应事件 ====================

    /**
     * 通用成功响应
     */
    public static final String RESPONSE_SUCCESS = "com.iboot.response.success";

    /**
     * 通用错误响应
     */
    public static final String RESPONSE_ERROR = "com.iboot.response.error";

    // ==================== 认证授权事件 ====================

    /**
     * 用户登录成功
     */
    public static final String AUTH_LOGIN_SUCCESS = "com.iboot.auth.login.success";

    /**
     * 用户登录失败
     */
    public static final String AUTH_LOGIN_FAILED = "com.iboot.auth.login.failed";

    /**
     * 用户登出成功
     */
    public static final String AUTH_LOGOUT_SUCCESS = "com.iboot.auth.logout.success";

    /**
     * 用户注册成功
     */
    public static final String AUTH_REGISTER_SUCCESS = "com.iboot.auth.register.success";

    // ==================== 用户管理事件 ====================

    /**
     * 用户创建
     */
    public static final String USER_CREATED = "com.iboot.user.created";

    /**
     * 用户更新
     */
    public static final String USER_UPDATED = "com.iboot.user.updated";

    /**
     * 用户删除
     */
    public static final String USER_DELETED = "com.iboot.user.deleted";

    /**
     * 用户密码重置
     */
    public static final String USER_PASSWORD_RESET = "com.iboot.user.password.reset";

    /**
     * 用户状态变更
     */
    public static final String USER_STATUS_CHANGED = "com.iboot.user.status.changed";

    // ==================== 角色管理事件 ====================

    /**
     * 角色创建
     */
    public static final String ROLE_CREATED = "com.iboot.role.created";

    /**
     * 角色更新
     */
    public static final String ROLE_UPDATED = "com.iboot.role.updated";

    /**
     * 角色删除
     */
    public static final String ROLE_DELETED = "com.iboot.role.deleted";

    /**
     * 角色权限分配
     */
    public static final String ROLE_PERMISSION_ASSIGNED = "com.iboot.role.permission.assigned";

    // ==================== 部门管理事件 ====================

    /**
     * 部门创建
     */
    public static final String DEPT_CREATED = "com.iboot.dept.created";

    /**
     * 部门更新
     */
    public static final String DEPT_UPDATED = "com.iboot.dept.updated";

    /**
     * 部门删除
     */
    public static final String DEPT_DELETED = "com.iboot.dept.deleted";

    // ==================== 岗位管理事件 ====================

    /**
     * 岗位创建
     */
    public static final String POST_CREATED = "com.iboot.post.created";

    /**
     * 岗位更新
     */
    public static final String POST_UPDATED = "com.iboot.post.updated";

    /**
     * 岗位删除
     */
    public static final String POST_DELETED = "com.iboot.post.deleted";

    // ==================== 菜单管理事件 ====================

    /**
     * 菜单创建
     */
    public static final String MENU_CREATED = "com.iboot.menu.created";

    /**
     * 菜单更新
     */
    public static final String MENU_UPDATED = "com.iboot.menu.updated";

    /**
     * 菜单删除
     */
    public static final String MENU_DELETED = "com.iboot.menu.deleted";

    // ==================== 字典管理事件 ====================

    /**
     * 字典类型创建
     */
    public static final String DICT_TYPE_CREATED = "com.iboot.dict.type.created";

    /**
     * 字典类型更新
     */
    public static final String DICT_TYPE_UPDATED = "com.iboot.dict.type.updated";

    /**
     * 字典类型删除
     */
    public static final String DICT_TYPE_DELETED = "com.iboot.dict.type.deleted";

    /**
     * 字典数据创建
     */
    public static final String DICT_DATA_CREATED = "com.iboot.dict.data.created";

    /**
     * 字典数据更新
     */
    public static final String DICT_DATA_UPDATED = "com.iboot.dict.data.updated";

    /**
     * 字典数据删除
     */
    public static final String DICT_DATA_DELETED = "com.iboot.dict.data.deleted";

    // ==================== 参数配置事件 ====================

    /**
     * 参数创建
     */
    public static final String CONFIG_CREATED = "com.iboot.config.created";

    /**
     * 参数更新
     */
    public static final String CONFIG_UPDATED = "com.iboot.config.updated";

    /**
     * 参数删除
     */
    public static final String CONFIG_DELETED = "com.iboot.config.deleted";

    // ==================== 系统公告事件 ====================

    /**
     * 公告创建
     */
    public static final String NOTICE_CREATED = "com.iboot.notice.created";

    /**
     * 公告更新
     */
    public static final String NOTICE_UPDATED = "com.iboot.notice.updated";

    /**
     * 公告删除
     */
    public static final String NOTICE_DELETED = "com.iboot.notice.deleted";

    // ==================== 消息中心事件 ====================

    /**
     * 消息发送
     */
    public static final String MESSAGE_SENT = "com.iboot.message.sent";

    /**
     * 消息已读
     */
    public static final String MESSAGE_MARKED_READ = "com.iboot.message.marked.read";

    /**
     * 消息删除
     */
    public static final String MESSAGE_DELETED = "com.iboot.message.deleted";

    /**
     * 消息模板创建
     */
    public static final String MESSAGE_TEMPLATE_CREATED = "com.iboot.message.template.created";

    /**
     * 消息模板更新
     */
    public static final String MESSAGE_TEMPLATE_UPDATED = "com.iboot.message.template.updated";

    /**
     * 消息模板删除
     */
    public static final String MESSAGE_TEMPLATE_DELETED = "com.iboot.message.template.deleted";

    // ==================== 消息推送事件 ====================

    /**
     * 推送连接建立
     */
    public static final String PUSH_CONNECTION_ESTABLISHED = "com.iboot.push.connection.established";

    /**
     * 推送连接断开
     */
    public static final String PUSH_CONNECTION_LOST = "com.iboot.push.connection.lost";

    /**
     * 推送消息发送
     */
    public static final String PUSH_MESSAGE_SENT = "com.iboot.push.message.sent";

    /**
     * 推送新消息通知
     */
    public static final String PUSH_NEW_MESSAGE = "com.iboot.push.new.message";

    /**
     * 推送消息已读通知
     */
    public static final String PUSH_MESSAGE_READ = "com.iboot.push.message.read";

    /**
     * 系统通知推送
     */
    public static final String PUSH_SYSTEM_NOTIFICATION = "com.iboot.push.system.notification";

    // ==================== 定时任务事件 ====================

    /**
     * 任务创建
     */
    public static final String JOB_CREATED = "com.iboot.job.created";

    /**
     * 任务更新
     */
    public static final String JOB_UPDATED = "com.iboot.job.updated";

    /**
     * 任务删除
     */
    public static final String JOB_DELETED = "com.iboot.job.deleted";

    /**
     * 任务启动
     */
    public static final String JOB_STARTED = "com.iboot.job.started";

    /**
     * 任务停止
     */
    public static final String JOB_STOPPED = "com.iboot.job.stopped";

    /**
     * 任务暂停
     */
    public static final String JOB_PAUSED = "com.iboot.job.paused";

    /**
     * 任务恢复
     */
    public static final String JOB_RESUMED = "com.iboot.job.resumed";

    // ==================== 文件管理事件 ====================

    /**
     * 文件上传
     */
    public static final String FILE_UPLOADED = "com.iboot.file.uploaded";

    /**
     * 文件删除
     */
    public static final String FILE_DELETED = "com.iboot.file.deleted";

    // ==================== 日志管理事件 ====================

    /**
     * 操作日志创建
     */
    public static final String LOG_OPERATION_CREATED = "com.iboot.log.operation.created";

    /**
     * 登录日志创建
     */
    public static final String LOG_LOGIN_CREATED = "com.iboot.log.login.created";

    // ==================== 系统监控事件 ====================

    /**
     * 服务器信息获取
     */
    public static final String MONITOR_SERVER_INFO = "com.iboot.monitor.server.info";

    /**
     * Redis 信息获取
     */
    public static final String MONITOR_REDIS_INFO = "com.iboot.monitor.redis.info";

    /**
     * MinIO 信息获取
     */
    public static final String MONITOR_MINIO_INFO = "com.iboot.monitor.minio.info";

    /**
     * 统计信息获取
     */
    public static final String STATISTICS_INFO = "com.iboot.statistics.info";

    /**
     * Dashboard 信息获取
     */
    public static final String DASHBOARD_INFO = "com.iboot.dashboard.info";
}
