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

package com.iboot.auth.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 登录响应 DTO。
 *
 * <p>返回 JWT 令牌、用户基本信息和权限/角色集合，供前端建立会话。字段集合与管理端拆分前
 * 完全一致，少一个字段都会导致前端或回归脚本解析失败。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "登录响应")
public class LoginResponse {

    /**
     * 访问令牌。
     */
    @Schema(description = "访问令牌")
    private String token;

    /**
     * 用户ID。
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 用户名。
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 昵称。
     */
    @Schema(description = "昵称")
    private String nickname;

    /**
     * 头像。
     */
    @Schema(description = "头像")
    private String avatar;

    /**
     * 权限列表。
     */
    @Schema(description = "权限列表")
    private List<String> permissions;

    /**
     * 角色列表。
     */
    @Schema(description = "角色列表")
    private List<String> roles;

    /**
     * 创建空响应，供框架反序列化使用。
     */
    public LoginResponse() {
    }

    /**
     * 创建登录响应。
     *
     * @param token 访问令牌
     * @param userId 用户 ID
     * @param username 用户名
     * @param nickname 昵称
     * @param avatar 头像地址
     * @param permissions 权限集合
     * @param roles 角色编码集合
     */
    public LoginResponse(String token, Long userId, String username, String nickname, String avatar,
                         List<String> permissions, List<String> roles) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
        this.permissions = permissions;
        this.roles = roles;
    }

    /**
     * 返回访问令牌。
     *
     * @return 访问令牌
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置访问令牌。
     *
     * @param token 访问令牌
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 返回用户 ID。
     *
     * @return 用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
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
     * 返回昵称。
     *
     * @return 昵称
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * 设置昵称。
     *
     * @param nickname 昵称
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 返回头像地址。
     *
     * @return 头像地址
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * 设置头像地址。
     *
     * @param avatar 头像地址
     */
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    /**
     * 返回权限集合。
     *
     * @return 权限集合
     */
    public List<String> getPermissions() {
        return permissions;
    }

    /**
     * 设置权限集合。
     *
     * @param permissions 权限集合
     */
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * 返回角色编码集合。
     *
     * @return 角色编码集合
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * 设置角色编码集合。
     *
     * @param roles 角色编码集合
     */
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
