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

package com.iboot.ops.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 运维控制台的访问凭证配置。
 *
 * <p>控制台不复用门户的 JWT 安全链，而是使用独立的 Basic 认证；账号与密码由部署环境注入，
 * 密码不提供默认值。密码为空时，{@link OpsSecurityConfig} 会在启动阶段拒绝创建控制台。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "iboot.ops.security")
public class OpsSecurityProperties {

    /**
     * 是否启用控制台认证。
     *
     * <p>默认开启。仅在本地开发且显式关闭时，控制台页面与实例接口才允许匿名访问；
     * 生产环境必须保持开启。
     */
    private boolean enabled = true;

    /**
     * 控制台登录用户名。
     */
    private String username = "ops";

    /**
     * 控制台登录密码，必须由环境变量或密钥管理提供。
     */
    private String password;

    /**
     * 判断是否启用控制台认证。
     *
     * @return 启用认证时返回 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用控制台认证。
     *
     * @param enabled 是否启用认证
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取控制台登录用户名。
     *
     * @return 登录用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置控制台登录用户名。
     *
     * @param username 登录用户名
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * 获取控制台登录密码。
     *
     * @return 登录密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置控制台登录密码。
     *
     * @param password 登录密码
     */
    public void setPassword(final String password) {
        this.password = password;
    }
}
