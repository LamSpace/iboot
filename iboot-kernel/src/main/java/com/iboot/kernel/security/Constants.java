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

package com.iboot.kernel.security;

/**
 * 通用常量。
 *
 * <p>集中定义跨模块复用的字符集、响应码、Redis Key 前缀、安全上下文键和内置账号标识。
 * Redis Key 前缀以冒号结尾，拼接业务标识时直接追加。
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
     * 令牌版本已停用的业务状态码。
     *
     * <p>双栈窗口关闭后，客户端携带旧格式令牌时返回该码，前端据此静默重新登录，
     * 而不是提示凭据错误。
     */
    public static final Integer TOKEN_VERSION_RETIRED = 40101;

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
     * 安全上下文中的令牌键。
     */
    public static final String TOKEN = "token";

    /**
     * 安全上下文中的用户 ID 键。
     */
    public static final String USER_ID = "userId";

    /**
     * 安全上下文中的用户名键。
     */
    public static final String USER_NAME = "userName";

    /**
     * 安全上下文中的头像键。
     */
    public static final String AVATAR = "avatar";

    /**
     * 安全上下文中的权限集合键。
     */
    public static final String PERMISSIONS = "permissions";

    /**
     * 安全上下文中的角色集合键。
     */
    public static final String ROLES = "roles";

    /**
     * 系统内置用户的类型编码。
     */
    public static final Integer USER_TYPE_SYSTEM = 1;

    /**
     * 超级管理员角色编码
     */
    public static final String ROLE_ADMIN_CODE = "ROLE_ADMIN";

    /**
     * 内置超级管理员用户名。
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
     * 安全上下文中的部门 ID 键。
     */
    public static final String DEPT_ID = "deptId";

}
