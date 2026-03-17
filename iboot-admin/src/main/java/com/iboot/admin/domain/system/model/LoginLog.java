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

package com.iboot.admin.domain.system.model;

import java.time.LocalDateTime;

/**
 * 登录日志实体（值对象）
 *
 * @author iBoot
 */
public class LoginLog {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录IP
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

    @SuppressWarnings("all")
    public LoginLog() {
    }

    /**
     * Creates a new {@code LoginLog} instance.
     *
     * @param id            日志ID
     * @param username      用户名
     * @param ipAddress     登录IP
     * @param loginLocation 登录地点
     * @param browser       浏览器类型
     * @param os            操作系统
     * @param status        登录状态：0-失败，1-成功
     * @param message       提示消息
     * @param loginTime     登录时间
     */
    @SuppressWarnings("all")
    public LoginLog(final Long id, final String username, final String ipAddress, final String loginLocation,
                    final String browser, final String os, final Integer status, final String message,
                    final LocalDateTime loginTime) {
        this.id = id;
        this.username = username;
        this.ipAddress = ipAddress;
        this.loginLocation = loginLocation;
        this.browser = browser;
        this.os = os;
        this.status = status;
        this.message = message;
        this.loginTime = loginTime;
    }

    @SuppressWarnings("all")
    public static LoginLog.LoginLogBuilder builder() {
        return new LoginLog.LoginLogBuilder();
    }

    /**
     * 检查是否登录成功
     */
    public boolean isSuccess() {
        return this.status != null && this.status == 1;
    }

    /**
     * 日志ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 日志ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 用户名
     */
    @SuppressWarnings("all")
    public String getUsername() {
        return this.username;
    }

    /**
     * 用户名
     */
    @SuppressWarnings("all")
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * 登录IP
     */
    @SuppressWarnings("all")
    public String getIpAddress() {
        return this.ipAddress;
    }

    /**
     * 登录IP
     */
    @SuppressWarnings("all")
    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * 登录地点
     */
    @SuppressWarnings("all")
    public String getLoginLocation() {
        return this.loginLocation;
    }

    /**
     * 登录地点
     */
    @SuppressWarnings("all")
    public void setLoginLocation(final String loginLocation) {
        this.loginLocation = loginLocation;
    }

    /**
     * 浏览器类型
     */
    @SuppressWarnings("all")
    public String getBrowser() {
        return this.browser;
    }

    /**
     * 浏览器类型
     */
    @SuppressWarnings("all")
    public void setBrowser(final String browser) {
        this.browser = browser;
    }

    /**
     * 操作系统
     */
    @SuppressWarnings("all")
    public String getOs() {
        return this.os;
    }

    /**
     * 操作系统
     */
    @SuppressWarnings("all")
    public void setOs(final String os) {
        this.os = os;
    }

    /**
     * 登录状态：0-失败，1-成功
     */
    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 登录状态：0-失败，1-成功
     */
    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 提示消息
     */
    @SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    /**
     * 提示消息
     */
    @SuppressWarnings("all")
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * 登录时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getLoginTime() {
        return this.loginTime;
    }

    /**
     * 登录时间
     */
    @SuppressWarnings("all")
    public void setLoginTime(final LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LoginLog))
            return false;
        final LoginLog other = (LoginLog) o;
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
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message))
            return false;
        final java.lang.Object this$loginTime = this.getLoginTime();
        final java.lang.Object other$loginTime = other.getLoginTime();
        if (this$loginTime == null ? other$loginTime != null : !this$loginTime.equals(other$loginTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LoginLog;
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
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $loginTime = this.getLoginTime();
        result = result * PRIME + ($loginTime == null ? 43 : $loginTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "LoginLog(id=" + this.getId() + ", username=" + this.getUsername() + ", ipAddress=" + this.getIpAddress()
                + ", loginLocation=" + this.getLoginLocation() + ", browser=" + this.getBrowser() + ", os="
                + this.getOs() + ", status=" + this.getStatus() + ", message=" + this.getMessage() + ", loginTime="
                + this.getLoginTime() + ")";
    }

    @SuppressWarnings("all")
    public static class LoginLogBuilder {

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
        private String message;

        @SuppressWarnings("all")
        private LocalDateTime loginTime;

        @SuppressWarnings("all")
        LoginLogBuilder() {
        }

        /**
         * 日志ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLog.LoginLogBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 用户名
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLog.LoginLogBuilder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * 登录IP
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLog.LoginLogBuilder ipAddress(final String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        /**
         * 登录地点
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLog.LoginLogBuilder loginLocation(final String loginLocation) {
            this.loginLocation = loginLocation;
            return this;
        }

        /**
         * 浏览器类型
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLog.LoginLogBuilder browser(final String browser) {
            this.browser = browser;
            return this;
        }

        /**
         * 操作系统
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLog.LoginLogBuilder os(final String os) {
            this.os = os;
            return this;
        }

        /**
         * 登录状态：0-失败，1-成功
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLog.LoginLogBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * 提示消息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLog.LoginLogBuilder message(final String message) {
            this.message = message;
            return this;
        }

        /**
         * 登录时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public LoginLog.LoginLogBuilder loginTime(final LocalDateTime loginTime) {
            this.loginTime = loginTime;
            return this;
        }

        @SuppressWarnings("all")
        public LoginLog build() {
            return new LoginLog(this.id, this.username, this.ipAddress, this.loginLocation, this.browser, this.os,
                    this.status, this.message, this.loginTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "LoginLog.LoginLogBuilder(id=" + this.id + ", username=" + this.username + ", ipAddress="
                    + this.ipAddress + ", loginLocation=" + this.loginLocation + ", browser=" + this.browser + ", os="
                    + this.os + ", status=" + this.status + ", message=" + this.message + ", loginTime="
                    + this.loginTime + ")";
        }

    }

}
