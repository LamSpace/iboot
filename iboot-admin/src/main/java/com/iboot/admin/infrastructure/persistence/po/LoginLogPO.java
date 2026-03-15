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

package com.iboot.admin.infrastructure.persistence.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志持久化对象
 * <p>
 * 对应数据库表：sys_login_log
 * </p>
 *
 * @author iBoot
 */
@Data
public class LoginLogPO {

    /**
     * 日志 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 登录 IP 地址
     */
    private String ipAddress;

    /**
     * 登录地点
     */
    private String loginLocation;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 登录状态：0-失败，1-成功
     */
    private Integer status;

    /**
     * 提示消息
     */
    private String message;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;
}
