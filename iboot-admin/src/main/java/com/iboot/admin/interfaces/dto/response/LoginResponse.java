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

package com.iboot.admin.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 登录响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "权限列表")
    private List<String> permissions;

    @Schema(description = "角色列表")
    private List<String> roles;

    @SuppressWarnings("all")
    LoginResponse(final String token, final Long userId, final String username, final String nickname,
                  final String avatar, final List<String> permissions, final List<String> roles) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
        this.permissions = permissions;
        this.roles = roles;
    }

    @SuppressWarnings("all")
    public static LoginResponse.LoginResponseBuilder builder() {
        return new LoginResponse.LoginResponseBuilder();
    }

    @SuppressWarnings("all")
    public String getToken() {
        return this.token;
    }

    @SuppressWarnings("all")
    public void setToken(final String token) {
        this.token = token;
    }

    @SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    @SuppressWarnings("all")
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    @SuppressWarnings("all")
    public String getUsername() {
        return this.username;
    }

    @SuppressWarnings("all")
    public void setUsername(final String username) {
        this.username = username;
    }

    @SuppressWarnings("all")
    public String getNickname() {
        return this.nickname;
    }

    @SuppressWarnings("all")
    public void setNickname(final String nickname) {
        this.nickname = nickname;
    }

    @SuppressWarnings("all")
    public String getAvatar() {
        return this.avatar;
    }

    @SuppressWarnings("all")
    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    @SuppressWarnings("all")
    public List<String> getPermissions() {
        return this.permissions;
    }

    @SuppressWarnings("all")
    public void setPermissions(final List<String> permissions) {
        this.permissions = permissions;
    }

    @SuppressWarnings("all")
    public List<String> getRoles() {
        return this.roles;
    }

    @SuppressWarnings("all")
    public void setRoles(final List<String> roles) {
        this.roles = roles;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LoginResponse))
            return false;
        final LoginResponse other = (LoginResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId))
            return false;
        final java.lang.Object this$token = this.getToken();
        final java.lang.Object other$token = other.getToken();
        if (this$token == null ? other$token != null : !this$token.equals(other$token))
            return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username))
            return false;
        final java.lang.Object this$nickname = this.getNickname();
        final java.lang.Object other$nickname = other.getNickname();
        if (this$nickname == null ? other$nickname != null : !this$nickname.equals(other$nickname))
            return false;
        final java.lang.Object this$avatar = this.getAvatar();
        final java.lang.Object other$avatar = other.getAvatar();
        if (this$avatar == null ? other$avatar != null : !this$avatar.equals(other$avatar))
            return false;
        final java.lang.Object this$permissions = this.getPermissions();
        final java.lang.Object other$permissions = other.getPermissions();
        if (this$permissions == null ? other$permissions != null : !this$permissions.equals(other$permissions))
            return false;
        final java.lang.Object this$roles = this.getRoles();
        final java.lang.Object other$roles = other.getRoles();
        if (this$roles == null ? other$roles != null : !this$roles.equals(other$roles))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LoginResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $token = this.getToken();
        result = result * PRIME + ($token == null ? 43 : $token.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $nickname = this.getNickname();
        result = result * PRIME + ($nickname == null ? 43 : $nickname.hashCode());
        final java.lang.Object $avatar = this.getAvatar();
        result = result * PRIME + ($avatar == null ? 43 : $avatar.hashCode());
        final java.lang.Object $permissions = this.getPermissions();
        result = result * PRIME + ($permissions == null ? 43 : $permissions.hashCode());
        final java.lang.Object $roles = this.getRoles();
        result = result * PRIME + ($roles == null ? 43 : $roles.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "LoginResponse(token=" + this.getToken() + ", userId=" + this.getUserId() + ", username="
                + this.getUsername() + ", nickname=" + this.getNickname() + ", avatar=" + this.getAvatar()
                + ", permissions=" + this.getPermissions() + ", roles=" + this.getRoles() + ")";
    }

    @SuppressWarnings("all")
    public static class LoginResponseBuilder {

        @SuppressWarnings("all")
        private String token;

        @SuppressWarnings("all")
        private Long userId;

        @SuppressWarnings("all")
        private String username;

        @SuppressWarnings("all")
        private String nickname;

        @SuppressWarnings("all")
        private String avatar;

        @SuppressWarnings("all")
        private List<String> permissions;

        @SuppressWarnings("all")
        private List<String> roles;

        @SuppressWarnings("all")
        LoginResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginResponse.LoginResponseBuilder token(final String token) {
            this.token = token;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginResponse.LoginResponseBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginResponse.LoginResponseBuilder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginResponse.LoginResponseBuilder nickname(final String nickname) {
            this.nickname = nickname;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginResponse.LoginResponseBuilder avatar(final String avatar) {
            this.avatar = avatar;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginResponse.LoginResponseBuilder permissions(final List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginResponse.LoginResponseBuilder roles(final List<String> roles) {
            this.roles = roles;
            return this;
        }

        @SuppressWarnings("all")
        public LoginResponse build() {
            return new LoginResponse(this.token, this.userId, this.username, this.nickname, this.avatar,
                    this.permissions, this.roles);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "LoginResponse.LoginResponseBuilder(token=" + this.token + ", userId=" + this.userId + ", username="
                    + this.username + ", nickname=" + this.nickname + ", avatar=" + this.avatar + ", permissions="
                    + this.permissions + ", roles=" + this.roles + ")";
        }

    }

}
