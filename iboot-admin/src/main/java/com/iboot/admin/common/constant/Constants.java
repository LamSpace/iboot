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

package com.iboot.admin.common.constant;

/**
 * 常量类
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class Constants {

    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * 成功标记
     */
    public static final Integer SUCCESS = 200;

    /**
     * 失败标记
     */
    public static final Integer FAIL = 500;

    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 注销
     */
    public static final String LOGOUT = "Logout";

    /**
     * 登录失败
     */
    public static final String LOGIN_FAIL = "Error";

    /**
     * 验证码 Redis Key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 登录用户 Token Key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 令牌前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 令牌
     */
    public static final String TOKEN = "token";

    /**
     * 用户ID
     */
    public static final String USER_ID = "userId";

    /**
     * 用户名
     */
    public static final String USER_NAME = "userName";

    /**
     * 用户头像
     */
    public static final String AVATAR = "avatar";

    /**
     * 权限
     */
    public static final String PERMISSIONS = "permissions";

    /**
     * 角色
     */
    public static final String ROLES = "roles";

    /**
     * 系统用户类型
     */
    public static final Integer USER_TYPE_SYSTEM = 1;

    /**
     * 超级管理员角色编码
     */
    public static final String ROLE_ADMIN_CODE = "ROLE_ADMIN";

    /**
     * 超级管理员用户名
     */
    public static final String ADMIN_USERNAME = "admin";

    /**
     * 登录重试次数 Redis Key 前缀
     */
    public static final String LOGIN_RETRY_KEY = "login_retry:";

    /**
     * 在线用户 Token 映射 Redis Key 前缀（userId -> token）
     */
    public static final String ONLINE_TOKEN_KEY = "online_token:";

    /**
     * 部门 ID
     */
    public static final String DEPT_ID = "deptId";

}
