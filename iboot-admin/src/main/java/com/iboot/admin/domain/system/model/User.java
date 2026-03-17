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
import java.util.List;

/**
 * 用户实体（聚合根）
 *
 * @author iBoot
 */
public class User {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 用户类型：1-系统用户，2-普通用户
     */
    private Integer userType;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 岗位 ID 列表
     */
    private List<Long> postIds;

    /**
     * 角色 ID 列表
     */
    private List<Long> roleIds;

    /**
     * 状态：0-停用，1-正常
     */
    private Integer status;

    /**
     * 最后登录 IP
     */
    private String loginIp;

    /**
     * 最后登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;

    /**
     * 备注
     */
    private String remark;

    @SuppressWarnings("all")
    public User() {
    }

    /**
     * Creates a new {@code User} instance.
     *
     * @param id         用户 ID
     * @param username   用户名
     * @param password   密码
     * @param nickname   昵称
     * @param email      邮箱
     * @param phone      手机号码
     * @param gender     性别：0-未知，1-男，2-女
     * @param avatar     头像地址
     * @param userType   用户类型：1-系统用户，2-普通用户
     * @param deptId     部门 ID
     * @param postIds    岗位 ID 列表
     * @param roleIds    角色 ID 列表
     * @param status     状态：0-停用，1-正常
     * @param loginIp    最后登录 IP
     * @param loginTime  最后登录时间
     * @param createBy   创建人
     * @param createTime 创建时间
     * @param updateBy   更新人
     * @param updateTime 更新时间
     * @param deleted    逻辑删除：0-未删除，1-已删除
     * @param remark     备注
     */
    @SuppressWarnings("all")
    public User(final Long id, final String username, final String password, final String nickname, final String email,
                final String phone, final Integer gender, final String avatar, final Integer userType, final Long deptId,
                final List<Long> postIds, final List<Long> roleIds, final Integer status, final String loginIp,
                final LocalDateTime loginTime, final String createBy, final LocalDateTime createTime, final String updateBy,
                final LocalDateTime updateTime, final Integer deleted, final String remark) {
        this.id = id;
        this.username = username;
        this.password = password;
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
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.deleted = deleted;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static User.UserBuilder builder() {
        return new User.UserBuilder();
    }

    /**
     * 修改用户密码
     *
     * @param newPassword 新密码
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    /**
     * 启用用户
     *
     * @return 启用后的用户对象
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 停用用户
     *
     * @return 停用后的用户对象
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 检查用户是否启用
     *
     * @return 如果用户启用则返回true，否则返回false
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }

    /**
     * 更新登录信息
     *
     * @param loginIp   登录IP地址
     * @param loginTime 登录时间
     */
    public void updateLoginInfo(String loginIp, LocalDateTime loginTime) {
        this.loginIp = loginIp;
        this.loginTime = loginTime;
    }

    /**
     * 用户 ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 用户 ID
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
     * 密码
     */
    @SuppressWarnings("all")
    public String getPassword() {
        return this.password;
    }

    /**
     * 密码
     */
    @SuppressWarnings("all")
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * 昵称
     */
    @SuppressWarnings("all")
    public String getNickname() {
        return this.nickname;
    }

    /**
     * 昵称
     */
    @SuppressWarnings("all")
    public void setNickname(final String nickname) {
        this.nickname = nickname;
    }

    /**
     * 邮箱
     */
    @SuppressWarnings("all")
    public String getEmail() {
        return this.email;
    }

    /**
     * 邮箱
     */
    @SuppressWarnings("all")
    public void setEmail(final String email) {
        this.email = email;
    }

    /**
     * 手机号码
     */
    @SuppressWarnings("all")
    public String getPhone() {
        return this.phone;
    }

    /**
     * 手机号码
     */
    @SuppressWarnings("all")
    public void setPhone(final String phone) {
        this.phone = phone;
    }

    /**
     * 性别：0-未知，1-男，2-女
     */
    @SuppressWarnings("all")
    public Integer getGender() {
        return this.gender;
    }

    /**
     * 性别：0-未知，1-男，2-女
     */
    @SuppressWarnings("all")
    public void setGender(final Integer gender) {
        this.gender = gender;
    }

    /**
     * 头像地址
     */
    @SuppressWarnings("all")
    public String getAvatar() {
        return this.avatar;
    }

    /**
     * 头像地址
     */
    @SuppressWarnings("all")
    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    /**
     * 用户类型：1-系统用户，2-普通用户
     */
    @SuppressWarnings("all")
    public Integer getUserType() {
        return this.userType;
    }

    /**
     * 用户类型：1-系统用户，2-普通用户
     */
    @SuppressWarnings("all")
    public void setUserType(final Integer userType) {
        this.userType = userType;
    }

    /**
     * 部门 ID
     */
    @SuppressWarnings("all")
    public Long getDeptId() {
        return this.deptId;
    }

    /**
     * 部门 ID
     */
    @SuppressWarnings("all")
    public void setDeptId(final Long deptId) {
        this.deptId = deptId;
    }

    /**
     * 岗位 ID 列表
     */
    @SuppressWarnings("all")
    public List<Long> getPostIds() {
        return this.postIds;
    }

    /**
     * 岗位 ID 列表
     */
    @SuppressWarnings("all")
    public void setPostIds(final List<Long> postIds) {
        this.postIds = postIds;
    }

    /**
     * 角色 ID 列表
     */
    @SuppressWarnings("all")
    public List<Long> getRoleIds() {
        return this.roleIds;
    }

    /**
     * 角色 ID 列表
     */
    @SuppressWarnings("all")
    public void setRoleIds(final List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    /**
     * 状态：0-停用，1-正常
     */
    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 状态：0-停用，1-正常
     */
    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 最后登录 IP
     */
    @SuppressWarnings("all")
    public String getLoginIp() {
        return this.loginIp;
    }

    /**
     * 最后登录 IP
     */
    @SuppressWarnings("all")
    public void setLoginIp(final String loginIp) {
        this.loginIp = loginIp;
    }

    /**
     * 最后登录时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getLoginTime() {
        return this.loginTime;
    }

    /**
     * 最后登录时间
     */
    @SuppressWarnings("all")
    public void setLoginTime(final LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    /**
     * 创建人
     */
    @SuppressWarnings("all")
    public String getCreateBy() {
        return this.createBy;
    }

    /**
     * 创建人
     */
    @SuppressWarnings("all")
    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    /**
     * 创建时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    /**
     * 创建时间
     */
    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新人
     */
    @SuppressWarnings("all")
    public String getUpdateBy() {
        return this.updateBy;
    }

    /**
     * 更新人
     */
    @SuppressWarnings("all")
    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 更新时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    /**
     * 更新时间
     */
    @SuppressWarnings("all")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    /**
     * 备注
     */
    @SuppressWarnings("all")
    public String getRemark() {
        return this.remark;
    }

    /**
     * 备注
     */
    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof User))
            return false;
        final User other = (User) o;
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
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
            return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username))
            return false;
        final java.lang.Object this$password = this.getPassword();
        final java.lang.Object other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password))
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
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy))
            return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof User;
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
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
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
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "User(id=" + this.getId() + ", username=" + this.getUsername() + ", password=" + this.getPassword()
                + ", nickname=" + this.getNickname() + ", email=" + this.getEmail() + ", phone=" + this.getPhone()
                + ", gender=" + this.getGender() + ", avatar=" + this.getAvatar() + ", userType=" + this.getUserType()
                + ", deptId=" + this.getDeptId() + ", postIds=" + this.getPostIds() + ", roleIds=" + this.getRoleIds()
                + ", status=" + this.getStatus() + ", loginIp=" + this.getLoginIp() + ", loginTime="
                + this.getLoginTime() + ", createBy=" + this.getCreateBy() + ", createTime=" + this.getCreateTime()
                + ", updateBy=" + this.getUpdateBy() + ", updateTime=" + this.getUpdateTime() + ", deleted="
                + this.getDeleted() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class UserBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String username;

        @SuppressWarnings("all")
        private String password;

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
        private String createBy;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private String updateBy;

        @SuppressWarnings("all")
        private LocalDateTime updateTime;

        @SuppressWarnings("all")
        private Integer deleted;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        UserBuilder() {
        }

        /**
         * 用户 ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 用户名
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * 密码
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder password(final String password) {
            this.password = password;
            return this;
        }

        /**
         * 昵称
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder nickname(final String nickname) {
            this.nickname = nickname;
            return this;
        }

        /**
         * 邮箱
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * 手机号码
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder phone(final String phone) {
            this.phone = phone;
            return this;
        }

        /**
         * 性别：0-未知，1-男，2-女
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder gender(final Integer gender) {
            this.gender = gender;
            return this;
        }

        /**
         * 头像地址
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder avatar(final String avatar) {
            this.avatar = avatar;
            return this;
        }

        /**
         * 用户类型：1-系统用户，2-普通用户
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder userType(final Integer userType) {
            this.userType = userType;
            return this;
        }

        /**
         * 部门 ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder deptId(final Long deptId) {
            this.deptId = deptId;
            return this;
        }

        /**
         * 岗位 ID 列表
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder postIds(final List<Long> postIds) {
            this.postIds = postIds;
            return this;
        }

        /**
         * 角色 ID 列表
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder roleIds(final List<Long> roleIds) {
            this.roleIds = roleIds;
            return this;
        }

        /**
         * 状态：0-停用，1-正常
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * 最后登录 IP
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder loginIp(final String loginIp) {
            this.loginIp = loginIp;
            return this;
        }

        /**
         * 最后登录时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder loginTime(final LocalDateTime loginTime) {
            this.loginTime = loginTime;
            return this;
        }

        /**
         * 创建人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * 创建时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * 更新人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * 更新时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        /**
         * 逻辑删除：0-未删除，1-已删除
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder deleted(final Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * 备注
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public User.UserBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public User build() {
            return new User(this.id, this.username, this.password, this.nickname, this.email, this.phone, this.gender,
                    this.avatar, this.userType, this.deptId, this.postIds, this.roleIds, this.status, this.loginIp,
                    this.loginTime, this.createBy, this.createTime, this.updateBy, this.updateTime, this.deleted,
                    this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "User.UserBuilder(id=" + this.id + ", username=" + this.username + ", password=" + this.password
                    + ", nickname=" + this.nickname + ", email=" + this.email + ", phone=" + this.phone + ", gender="
                    + this.gender + ", avatar=" + this.avatar + ", userType=" + this.userType + ", deptId="
                    + this.deptId + ", postIds=" + this.postIds + ", roleIds=" + this.roleIds + ", status="
                    + this.status + ", loginIp=" + this.loginIp + ", loginTime=" + this.loginTime + ", createBy="
                    + this.createBy + ", createTime=" + this.createTime + ", updateBy=" + this.updateBy
                    + ", updateTime=" + this.updateTime + ", deleted=" + this.deleted + ", remark=" + this.remark + ")";
        }

    }

}
