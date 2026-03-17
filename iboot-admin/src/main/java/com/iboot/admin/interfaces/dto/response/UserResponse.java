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
import java.util.List;

/**
 * 用户响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "用户响应")
public class UserResponse {

    @Schema(description = "用户ID")
    private Long id; // 用户唯一标识ID

    @Schema(description = "用户名")
    private String username; // 用户名，用于登录系统

    @Schema(description = "昵称")
    private String nickname; // 用户昵称

    @Schema(description = "邮箱")
    private String email; // 用户邮箱地址

    @Schema(description = "手机号码")
    private String phone; // 用户手机号码

    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender; // 性别：0-未知，1-男，2-女

    @Schema(description = "头像地址")
    private String avatar; // 头像地址

    @Schema(description = "用户类型：1-系统用户，2-普通用户")
    private Integer userType; // 用户类型：1-系统用户，2-普通用户

    @Schema(description = "部门ID")
    private Long deptId; // 所属部门ID

    @Schema(description = "岗位ID列表")
    private List<Long> postIds; // 岗位ID列表

    @Schema(description = "角色ID列表")
    private List<Long> roleIds; // 角色ID列表

    @Schema(description = "状态：0-停用，1-正常")
    private Integer status; // 状态：0-停用，1-正常

    @Schema(description = "最后登录IP")
    private String loginIp; // 最后登录IP地址

    @Schema(description = "最后登录时间")
    private LocalDateTime loginTime; // 最后登录时间

    @Schema(description = "创建时间")
    private LocalDateTime createTime; // 创建时间

    @Schema(description = "备注")
    private String remark; // 备注信息

    @SuppressWarnings("all")
    public UserResponse() {
    }

    @SuppressWarnings("all")
    public UserResponse(final Long id, final String username, final String nickname, final String email,
                        final String phone, final Integer gender, final String avatar, final Integer userType, final Long deptId,
                        final List<Long> postIds, final List<Long> roleIds, final Integer status, final String loginIp,
                        final LocalDateTime loginTime, final LocalDateTime createTime, final String remark) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.avatar = avatar;
        this.userType = userType;
        this.deptId = deptId;
        this.postIds = postIds;
        this.roleIds = roleIds;
        this.status = status;
        this.loginIp = loginIp;
        this.loginTime = loginTime;
        this.createTime = createTime;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static UserResponse.UserResponseBuilder builder() {
        return new UserResponse.UserResponseBuilder();
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
    public String getAvatar() {
        return this.avatar;
    }

    @SuppressWarnings("all")
    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    @SuppressWarnings("all")
    public Integer getUserType() {
        return this.userType;
    }

    @SuppressWarnings("all")
    public void setUserType(final Integer userType) {
        this.userType = userType;
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
    public List<Long> getPostIds() {
        return this.postIds;
    }

    @SuppressWarnings("all")
    public void setPostIds(final List<Long> postIds) {
        this.postIds = postIds;
    }

    @SuppressWarnings("all")
    public List<Long> getRoleIds() {
        return this.roleIds;
    }

    @SuppressWarnings("all")
    public void setRoleIds(final List<Long> roleIds) {
        this.roleIds = roleIds;
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
        if (!(o instanceof UserResponse))
            return false;
        final UserResponse other = (UserResponse) o;
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
        final java.lang.Object this$userType = this.getUserType();
        final java.lang.Object other$userType = other.getUserType();
        if (this$userType == null ? other$userType != null : !this$userType.equals(other$userType))
            return false;
        final java.lang.Object this$deptId = this.getDeptId();
        final java.lang.Object other$deptId = other.getDeptId();
        if (this$deptId == null ? other$deptId != null : !this$deptId.equals(other$deptId))
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
        final java.lang.Object this$avatar = this.getAvatar();
        final java.lang.Object other$avatar = other.getAvatar();
        if (this$avatar == null ? other$avatar != null : !this$avatar.equals(other$avatar))
            return false;
        final java.lang.Object this$postIds = this.getPostIds();
        final java.lang.Object other$postIds = other.getPostIds();
        if (this$postIds == null ? other$postIds != null : !this$postIds.equals(other$postIds))
            return false;
        final java.lang.Object this$roleIds = this.getRoleIds();
        final java.lang.Object other$roleIds = other.getRoleIds();
        if (this$roleIds == null ? other$roleIds != null : !this$roleIds.equals(other$roleIds))
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
        return other instanceof UserResponse;
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
        final java.lang.Object $userType = this.getUserType();
        result = result * PRIME + ($userType == null ? 43 : $userType.hashCode());
        final java.lang.Object $deptId = this.getDeptId();
        result = result * PRIME + ($deptId == null ? 43 : $deptId.hashCode());
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
        final java.lang.Object $avatar = this.getAvatar();
        result = result * PRIME + ($avatar == null ? 43 : $avatar.hashCode());
        final java.lang.Object $postIds = this.getPostIds();
        result = result * PRIME + ($postIds == null ? 43 : $postIds.hashCode());
        final java.lang.Object $roleIds = this.getRoleIds();
        result = result * PRIME + ($roleIds == null ? 43 : $roleIds.hashCode());
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
        return "UserResponse(id=" + this.getId() + ", username=" + this.getUsername() + ", nickname="
                + this.getNickname() + ", email=" + this.getEmail() + ", phone=" + this.getPhone() + ", gender="
                + this.getGender() + ", avatar=" + this.getAvatar() + ", userType=" + this.getUserType() + ", deptId="
                + this.getDeptId() + ", postIds=" + this.getPostIds() + ", roleIds=" + this.getRoleIds() + ", status="
                + this.getStatus() + ", loginIp=" + this.getLoginIp() + ", loginTime=" + this.getLoginTime()
                + ", createTime=" + this.getCreateTime() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class UserResponseBuilder {

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
        private String avatar;

        @SuppressWarnings("all")
        private Integer userType;

        @SuppressWarnings("all")
        private Long deptId;

        @SuppressWarnings("all")
        private List<Long> postIds;

        @SuppressWarnings("all")
        private List<Long> roleIds;

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
        UserResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder nickname(final String nickname) {
            this.nickname = nickname;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder phone(final String phone) {
            this.phone = phone;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder gender(final Integer gender) {
            this.gender = gender;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder avatar(final String avatar) {
            this.avatar = avatar;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder userType(final Integer userType) {
            this.userType = userType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder deptId(final Long deptId) {
            this.deptId = deptId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder postIds(final List<Long> postIds) {
            this.postIds = postIds;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder roleIds(final List<Long> roleIds) {
            this.roleIds = roleIds;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder loginIp(final String loginIp) {
            this.loginIp = loginIp;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder loginTime(final LocalDateTime loginTime) {
            this.loginTime = loginTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserResponse.UserResponseBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public UserResponse build() {
            return new UserResponse(this.id, this.username, this.nickname, this.email, this.phone, this.gender,
                    this.avatar, this.userType, this.deptId, this.postIds, this.roleIds, this.status, this.loginIp,
                    this.loginTime, this.createTime, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "UserResponse.UserResponseBuilder(id=" + this.id + ", username=" + this.username + ", nickname="
                    + this.nickname + ", email=" + this.email + ", phone=" + this.phone + ", gender=" + this.gender
                    + ", avatar=" + this.avatar + ", userType=" + this.userType + ", deptId=" + this.deptId
                    + ", postIds=" + this.postIds + ", roleIds=" + this.roleIds + ", status=" + this.status
                    + ", loginIp=" + this.loginIp + ", loginTime=" + this.loginTime + ", createTime=" + this.createTime
                    + ", remark=" + this.remark + ")";
        }

    }

}
