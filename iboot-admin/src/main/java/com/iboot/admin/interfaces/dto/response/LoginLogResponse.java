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

import java.time.LocalDateTime;

/**
 * 登录日志响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "登录日志响应")
public class LoginLogResponse {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "登录IP")
    private String ipAddress;

    @Schema(description = "登录地点")
    private String loginLocation;

    @Schema(description = "浏览器")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "登录状态：0-失败，1-成功")
    private Integer status;

    @Schema(description = "提示消息")
    private String msg;

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;

    @SuppressWarnings("all")
    public LoginLogResponse() {
    }

    @SuppressWarnings("all")
    public LoginLogResponse(final Long id, final String username, final String ipAddress, final String loginLocation,
                            final String browser, final String os, final Integer status, final String msg,
                            final LocalDateTime loginTime) {
        this.id = id;
        this.username = username;
        this.ipAddress = ipAddress;
        this.loginLocation = loginLocation;
        this.browser = browser;
        this.os = os;
        this.status = status;
        this.msg = msg;
        this.loginTime = loginTime;
    }

    @SuppressWarnings("all")
    public static LoginLogResponse.LoginLogResponseBuilder builder() {
        return new LoginLogResponse.LoginLogResponseBuilder();
    }

    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
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
    public String getIpAddress() {
        return this.ipAddress;
    }

    @SuppressWarnings("all")
    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @SuppressWarnings("all")
    public String getLoginLocation() {
        return this.loginLocation;
    }

    @SuppressWarnings("all")
    public void setLoginLocation(final String loginLocation) {
        this.loginLocation = loginLocation;
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
    public Integer getStatus() {
        return this.status;
    }

    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    @SuppressWarnings("all")
    public String getMsg() {
        return this.msg;
    }

    @SuppressWarnings("all")
    public void setMsg(final String msg) {
        this.msg = msg;
    }

    @SuppressWarnings("all")
    public LocalDateTime getLoginTime() {
        return this.loginTime;
    }

    @SuppressWarnings("all")
    public void setLoginTime(final LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LoginLogResponse))
            return false;
        final LoginLogResponse other = (LoginLogResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username))
            return false;
        final java.lang.Object this$ipAddress = this.getIpAddress();
        final java.lang.Object other$ipAddress = other.getIpAddress();
        if (this$ipAddress == null ? other$ipAddress != null : !this$ipAddress.equals(other$ipAddress))
            return false;
        final java.lang.Object this$loginLocation = this.getLoginLocation();
        final java.lang.Object other$loginLocation = other.getLoginLocation();
        if (this$loginLocation == null ? other$loginLocation != null : !this$loginLocation.equals(other$loginLocation))
            return false;
        final java.lang.Object this$browser = this.getBrowser();
        final java.lang.Object other$browser = other.getBrowser();
        if (this$browser == null ? other$browser != null : !this$browser.equals(other$browser))
            return false;
        final java.lang.Object this$os = this.getOs();
        final java.lang.Object other$os = other.getOs();
        if (this$os == null ? other$os != null : !this$os.equals(other$os))
            return false;
        final java.lang.Object this$msg = this.getMsg();
        final java.lang.Object other$msg = other.getMsg();
        if (this$msg == null ? other$msg != null : !this$msg.equals(other$msg))
            return false;
        final java.lang.Object this$loginTime = this.getLoginTime();
        final java.lang.Object other$loginTime = other.getLoginTime();
        if (this$loginTime == null ? other$loginTime != null : !this$loginTime.equals(other$loginTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LoginLogResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $ipAddress = this.getIpAddress();
        result = result * PRIME + ($ipAddress == null ? 43 : $ipAddress.hashCode());
        final java.lang.Object $loginLocation = this.getLoginLocation();
        result = result * PRIME + ($loginLocation == null ? 43 : $loginLocation.hashCode());
        final java.lang.Object $browser = this.getBrowser();
        result = result * PRIME + ($browser == null ? 43 : $browser.hashCode());
        final java.lang.Object $os = this.getOs();
        result = result * PRIME + ($os == null ? 43 : $os.hashCode());
        final java.lang.Object $msg = this.getMsg();
        result = result * PRIME + ($msg == null ? 43 : $msg.hashCode());
        final java.lang.Object $loginTime = this.getLoginTime();
        result = result * PRIME + ($loginTime == null ? 43 : $loginTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "LoginLogResponse(id=" + this.getId() + ", username=" + this.getUsername() + ", ipAddress="
                + this.getIpAddress() + ", loginLocation=" + this.getLoginLocation() + ", browser=" + this.getBrowser()
                + ", os=" + this.getOs() + ", status=" + this.getStatus() + ", msg=" + this.getMsg() + ", loginTime="
                + this.getLoginTime() + ")";
    }

    @SuppressWarnings("all")
    public static class LoginLogResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String username;

        @SuppressWarnings("all")
        private String ipAddress;

        @SuppressWarnings("all")
        private String loginLocation;

        @SuppressWarnings("all")
        private String browser;

        @SuppressWarnings("all")
        private String os;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String msg;

        @SuppressWarnings("all")
        private LocalDateTime loginTime;

        @SuppressWarnings("all")
        LoginLogResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLogResponse.LoginLogResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLogResponse.LoginLogResponseBuilder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLogResponse.LoginLogResponseBuilder ipAddress(final String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLogResponse.LoginLogResponseBuilder loginLocation(final String loginLocation) {
            this.loginLocation = loginLocation;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLogResponse.LoginLogResponseBuilder browser(final String browser) {
            this.browser = browser;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLogResponse.LoginLogResponseBuilder os(final String os) {
            this.os = os;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLogResponse.LoginLogResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLogResponse.LoginLogResponseBuilder msg(final String msg) {
            this.msg = msg;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLogResponse.LoginLogResponseBuilder loginTime(final LocalDateTime loginTime) {
            this.loginTime = loginTime;
            return this;
        }

        @SuppressWarnings("all")
        public LoginLogResponse build() {
            return new LoginLogResponse(this.id, this.username, this.ipAddress, this.loginLocation, this.browser,
                    this.os, this.status, this.msg, this.loginTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "LoginLogResponse.LoginLogResponseBuilder(id=" + this.id + ", username=" + this.username
                    + ", ipAddress=" + this.ipAddress + ", loginLocation=" + this.loginLocation + ", browser="
                    + this.browser + ", os=" + this.os + ", status=" + this.status + ", msg=" + this.msg
                    + ", loginTime=" + this.loginTime + ")";
        }

    }

}
