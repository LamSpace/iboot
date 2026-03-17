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

package com.iboot.admin.interfaces.dto.export;

import com.iboot.admin.common.annotation.ExcelColumn;

import java.time.LocalDateTime;

/**
 * 用户导出VO
 *
 * @author iBoot
 */
public class UserExportVO {

    @ExcelColumn(name = "用户ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "用户名", order = 2, width = 15)
    private String username;

    @ExcelColumn(name = "昵称", order = 3, width = 15)
    private String nickname;

    @ExcelColumn(name = "邮箱", order = 4, width = 25)
    private String email;

    @ExcelColumn(name = "手机号", order = 5, width = 15)
    private String phone;

    @ExcelColumn(name = "性别", order = 6, width = 8, dictType = "sys_user_gender")
    private Integer gender;

    @ExcelColumn(name = "状态", order = 7, width = 8, dictType = "sys_normal_disable")
    private Integer status;

    @ExcelColumn(name = "最后登录IP", order = 8, width = 18)
    private String loginIp;

    @ExcelColumn(name = "最后登录时间", order = 9, width = 20)
    private LocalDateTime loginTime;

    @ExcelColumn(name = "创建时间", order = 10, width = 20)
    private LocalDateTime createTime;

    @ExcelColumn(name = "备注", order = 11, width = 30)
    private String remark;

    @SuppressWarnings("all")
    public UserExportVO() {
    }

    @SuppressWarnings("all")
    public UserExportVO(final Long id, final String username, final String nickname, final String email,
                        final String phone, final Integer gender, final Integer status, final String loginIp,
                        final LocalDateTime loginTime, final LocalDateTime createTime, final String remark) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.status = status;
        this.loginIp = loginIp;
        this.loginTime = loginTime;
        this.createTime = createTime;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static UserExportVO.UserExportVOBuilder builder() {
        return new UserExportVO.UserExportVOBuilder();
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
    public String getNickname() {
        return this.nickname;
    }

    @SuppressWarnings("all")
    public void setNickname(final String nickname) {
        this.nickname = nickname;
    }

    @SuppressWarnings("all")
    public String getEmail() {
        return this.email;
    }

    @SuppressWarnings("all")
    public void setEmail(final String email) {
        this.email = email;
    }

    @SuppressWarnings("all")
    public String getPhone() {
        return this.phone;
    }

    @SuppressWarnings("all")
    public void setPhone(final String phone) {
        this.phone = phone;
    }

    @SuppressWarnings("all")
    public Integer getGender() {
        return this.gender;
    }

    @SuppressWarnings("all")
    public void setGender(final Integer gender) {
        this.gender = gender;
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
    public String getLoginIp() {
        return this.loginIp;
    }

    @SuppressWarnings("all")
    public void setLoginIp(final String loginIp) {
        this.loginIp = loginIp;
    }

    @SuppressWarnings("all")
    public LocalDateTime getLoginTime() {
        return this.loginTime;
    }

    @SuppressWarnings("all")
    public void setLoginTime(final LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    @SuppressWarnings("all")
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public String getRemark() {
        return this.remark;
    }

    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof UserExportVO))
            return false;
        final UserExportVO other = (UserExportVO) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$gender = this.getGender();
        final java.lang.Object other$gender = other.getGender();
        if (this$gender == null ? other$gender != null : !this$gender.equals(other$gender))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username))
            return false;
        final java.lang.Object this$nickname = this.getNickname();
        final java.lang.Object other$nickname = other.getNickname();
        if (this$nickname == null ? other$nickname != null : !this$nickname.equals(other$nickname))
            return false;
        final java.lang.Object this$email = this.getEmail();
        final java.lang.Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email))
            return false;
        final java.lang.Object this$phone = this.getPhone();
        final java.lang.Object other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone))
            return false;
        final java.lang.Object this$loginIp = this.getLoginIp();
        final java.lang.Object other$loginIp = other.getLoginIp();
        if (this$loginIp == null ? other$loginIp != null : !this$loginIp.equals(other$loginIp))
            return false;
        final java.lang.Object this$loginTime = this.getLoginTime();
        final java.lang.Object other$loginTime = other.getLoginTime();
        if (this$loginTime == null ? other$loginTime != null : !this$loginTime.equals(other$loginTime))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof UserExportVO;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $gender = this.getGender();
        result = result * PRIME + ($gender == null ? 43 : $gender.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $nickname = this.getNickname();
        result = result * PRIME + ($nickname == null ? 43 : $nickname.hashCode());
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $phone = this.getPhone();
        result = result * PRIME + ($phone == null ? 43 : $phone.hashCode());
        final java.lang.Object $loginIp = this.getLoginIp();
        result = result * PRIME + ($loginIp == null ? 43 : $loginIp.hashCode());
        final java.lang.Object $loginTime = this.getLoginTime();
        result = result * PRIME + ($loginTime == null ? 43 : $loginTime.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "UserExportVO(id=" + this.getId() + ", username=" + this.getUsername() + ", nickname="
                + this.getNickname() + ", email=" + this.getEmail() + ", phone=" + this.getPhone() + ", gender="
                + this.getGender() + ", status=" + this.getStatus() + ", loginIp=" + this.getLoginIp() + ", loginTime="
                + this.getLoginTime() + ", createTime=" + this.getCreateTime() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class UserExportVOBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String username;

        @SuppressWarnings("all")
        private String nickname;

        @SuppressWarnings("all")
        private String email;

        @SuppressWarnings("all")
        private String phone;

        @SuppressWarnings("all")
        private Integer gender;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String loginIp;

        @SuppressWarnings("all")
        private LocalDateTime loginTime;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        UserExportVOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserExportVO.UserExportVOBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserExportVO.UserExportVOBuilder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserExportVO.UserExportVOBuilder nickname(final String nickname) {
            this.nickname = nickname;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserExportVO.UserExportVOBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserExportVO.UserExportVOBuilder phone(final String phone) {
            this.phone = phone;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserExportVO.UserExportVOBuilder gender(final Integer gender) {
            this.gender = gender;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserExportVO.UserExportVOBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserExportVO.UserExportVOBuilder loginIp(final String loginIp) {
            this.loginIp = loginIp;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserExportVO.UserExportVOBuilder loginTime(final LocalDateTime loginTime) {
            this.loginTime = loginTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserExportVO.UserExportVOBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserExportVO.UserExportVOBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public UserExportVO build() {
            return new UserExportVO(this.id, this.username, this.nickname, this.email, this.phone, this.gender,
                    this.status, this.loginIp, this.loginTime, this.createTime, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "UserExportVO.UserExportVOBuilder(id=" + this.id + ", username=" + this.username + ", nickname="
                    + this.nickname + ", email=" + this.email + ", phone=" + this.phone + ", gender=" + this.gender
                    + ", status=" + this.status + ", loginIp=" + this.loginIp + ", loginTime=" + this.loginTime
                    + ", createTime=" + this.createTime + ", remark=" + this.remark + ")";
        }

    }

}
