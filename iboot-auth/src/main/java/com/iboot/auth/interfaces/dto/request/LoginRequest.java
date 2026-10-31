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

package com.iboot.auth.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求 DTO。
 *
 * <p>字段结构与管理端拆分前完全一致，前端与回归脚本无需改动。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "登录请求")
public class LoginRequest {

    /**
     * 用户名。
     */
    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码。
     */
    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 创建空请求，供框架反序列化使用。
     */
    public LoginRequest() {
    }

    /**
     * 返回用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置用户名。
     *
     * @param username 用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 返回密码。
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码。
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
