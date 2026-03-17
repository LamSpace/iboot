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
 * 在线用户响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "在线用户响应")
public class OnlineUserResponse {

    @Schema(description = "会话令牌ID（脱敏）")
    private String tokenId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "登录IP")
    private String loginIp;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "登录时间")
    private String loginTime;

    @Schema(description = "角色列表")
    private List<String> roles;

    @SuppressWarnings("all")
    OnlineUserResponse(final String tokenId, final Long userId, final String username, final String nickname,
                       final Long deptId, final String deptName, final String loginIp, final String browser, final String os,
                       final String loginTime, final List<String> roles) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.deptId = deptId;
        this.deptName = deptName;
        this.loginIp = loginIp;
        this.browser = browser;
        this.os = os;
        this.loginTime = loginTime;
        this.roles = roles;
    }

    @SuppressWarnings("all")
    public static OnlineUserResponse.OnlineUserResponseBuilder builder() {
        return new OnlineUserResponse.OnlineUserResponseBuilder();
    }

    @SuppressWarnings("all")
    public String getTokenId() {
        return this.tokenId;
    }

    @SuppressWarnings("all")
    public void setTokenId(final String tokenId) {
        this.tokenId = tokenId;
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
    public Long getDeptId() {
        return this.deptId;
    }

    @SuppressWarnings("all")
    public void setDeptId(final Long deptId) {
        this.deptId = deptId;
    }

    @SuppressWarnings("all")
    public String getDeptName() {
        return this.deptName;
    }

    @SuppressWarnings("all")
    public void setDeptName(final String deptName) {
        this.deptName = deptName;
    }

    @SuppressWarnings("all")
    public String getLoginIp() {
        return this.loginIp;
    }

    @SuppressWarnings("all")
    public void setLoginIp(final String loginIp) {
        this.loginIp = loginIp;
    }

    @SuppressWarnings("all")
    public String getBrowser() {
        return this.browser;
    }

    @SuppressWarnings("all")
    public void setBrowser(final String browser) {
        this.browser = browser;
    }

    @SuppressWarnings("all")
    public String getOs() {
        return this.os;
    }

    @SuppressWarnings("all")
    public void setOs(final String os) {
        this.os = os;
    }

    @SuppressWarnings("all")
    public String getLoginTime() {
        return this.loginTime;
    }

    @SuppressWarnings("all")
    public void setLoginTime(final String loginTime) {
        this.loginTime = loginTime;
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
        if (!(o instanceof OnlineUserResponse))
            return false;
        final OnlineUserResponse other = (OnlineUserResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId))
            return false;
        final java.lang.Object this$deptId = this.getDeptId();
        final java.lang.Object other$deptId = other.getDeptId();
        if (this$deptId == null ? other$deptId != null : !this$deptId.equals(other$deptId))
            return false;
        final java.lang.Object this$tokenId = this.getTokenId();
        final java.lang.Object other$tokenId = other.getTokenId();
        if (this$tokenId == null ? other$tokenId != null : !this$tokenId.equals(other$tokenId))
            return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username))
            return false;
        final java.lang.Object this$nickname = this.getNickname();
        final java.lang.Object other$nickname = other.getNickname();
        if (this$nickname == null ? other$nickname != null : !this$nickname.equals(other$nickname))
            return false;
        final java.lang.Object this$deptName = this.getDeptName();
        final java.lang.Object other$deptName = other.getDeptName();
        if (this$deptName == null ? other$deptName != null : !this$deptName.equals(other$deptName))
            return false;
        final java.lang.Object this$loginIp = this.getLoginIp();
        final java.lang.Object other$loginIp = other.getLoginIp();
        if (this$loginIp == null ? other$loginIp != null : !this$loginIp.equals(other$loginIp))
            return false;
        final java.lang.Object this$browser = this.getBrowser();
        final java.lang.Object other$browser = other.getBrowser();
        if (this$browser == null ? other$browser != null : !this$browser.equals(other$browser))
            return false;
        final java.lang.Object this$os = this.getOs();
        final java.lang.Object other$os = other.getOs();
        if (this$os == null ? other$os != null : !this$os.equals(other$os))
            return false;
        final java.lang.Object this$loginTime = this.getLoginTime();
        final java.lang.Object other$loginTime = other.getLoginTime();
        if (this$loginTime == null ? other$loginTime != null : !this$loginTime.equals(other$loginTime))
            return false;
        final java.lang.Object this$roles = this.getRoles();
        final java.lang.Object other$roles = other.getRoles();
        if (this$roles == null ? other$roles != null : !this$roles.equals(other$roles))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OnlineUserResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $deptId = this.getDeptId();
        result = result * PRIME + ($deptId == null ? 43 : $deptId.hashCode());
        final java.lang.Object $tokenId = this.getTokenId();
        result = result * PRIME + ($tokenId == null ? 43 : $tokenId.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $nickname = this.getNickname();
        result = result * PRIME + ($nickname == null ? 43 : $nickname.hashCode());
        final java.lang.Object $deptName = this.getDeptName();
        result = result * PRIME + ($deptName == null ? 43 : $deptName.hashCode());
        final java.lang.Object $loginIp = this.getLoginIp();
        result = result * PRIME + ($loginIp == null ? 43 : $loginIp.hashCode());
        final java.lang.Object $browser = this.getBrowser();
        result = result * PRIME + ($browser == null ? 43 : $browser.hashCode());
        final java.lang.Object $os = this.getOs();
        result = result * PRIME + ($os == null ? 43 : $os.hashCode());
        final java.lang.Object $loginTime = this.getLoginTime();
        result = result * PRIME + ($loginTime == null ? 43 : $loginTime.hashCode());
        final java.lang.Object $roles = this.getRoles();
        result = result * PRIME + ($roles == null ? 43 : $roles.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "OnlineUserResponse(tokenId=" + this.getTokenId() + ", userId=" + this.getUserId() + ", username="
                + this.getUsername() + ", nickname=" + this.getNickname() + ", deptId=" + this.getDeptId()
                + ", deptName=" + this.getDeptName() + ", loginIp=" + this.getLoginIp() + ", browser="
                + this.getBrowser() + ", os=" + this.getOs() + ", loginTime=" + this.getLoginTime() + ", roles="
                + this.getRoles() + ")";
    }

    @SuppressWarnings("all")
    public static class OnlineUserResponseBuilder {

        @SuppressWarnings("all")
        private String tokenId;

        @SuppressWarnings("all")
        private Long userId;

        @SuppressWarnings("all")
        private String username;

        @SuppressWarnings("all")
        private String nickname;

        @SuppressWarnings("all")
        private Long deptId;

        @SuppressWarnings("all")
        private String deptName;

        @SuppressWarnings("all")
        private String loginIp;

        @SuppressWarnings("all")
        private String browser;

        @SuppressWarnings("all")
        private String os;

        @SuppressWarnings("all")
        private String loginTime;

        @SuppressWarnings("all")
        private List<String> roles;

        @SuppressWarnings("all")
        OnlineUserResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OnlineUserResponse.OnlineUserResponseBuilder tokenId(final String tokenId) {
            this.tokenId = tokenId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OnlineUserResponse.OnlineUserResponseBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OnlineUserResponse.OnlineUserResponseBuilder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OnlineUserResponse.OnlineUserResponseBuilder nickname(final String nickname) {
            this.nickname = nickname;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OnlineUserResponse.OnlineUserResponseBuilder deptId(final Long deptId) {
            this.deptId = deptId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OnlineUserResponse.OnlineUserResponseBuilder deptName(final String deptName) {
            this.deptName = deptName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OnlineUserResponse.OnlineUserResponseBuilder loginIp(final String loginIp) {
            this.loginIp = loginIp;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OnlineUserResponse.OnlineUserResponseBuilder browser(final String browser) {
            this.browser = browser;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OnlineUserResponse.OnlineUserResponseBuilder os(final String os) {
            this.os = os;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OnlineUserResponse.OnlineUserResponseBuilder loginTime(final String loginTime) {
            this.loginTime = loginTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OnlineUserResponse.OnlineUserResponseBuilder roles(final List<String> roles) {
            this.roles = roles;
            return this;
        }

        @SuppressWarnings("all")
        public OnlineUserResponse build() {
            return new OnlineUserResponse(this.tokenId, this.userId, this.username, this.nickname, this.deptId,
                    this.deptName, this.loginIp, this.browser, this.os, this.loginTime, this.roles);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "OnlineUserResponse.OnlineUserResponseBuilder(tokenId=" + this.tokenId + ", userId=" + this.userId
                    + ", username=" + this.username + ", nickname=" + this.nickname + ", deptId=" + this.deptId
                    + ", deptName=" + this.deptName + ", loginIp=" + this.loginIp + ", browser=" + this.browser
                    + ", os=" + this.os + ", loginTime=" + this.loginTime + ", roles=" + this.roles + ")";
        }

    }

}
