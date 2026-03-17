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
 * 个人信息响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "个人信息响应")
public class ProfileResponse {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "岗位名称列表")
    private List<String> postNames;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    ProfileResponse(final Long id, final String username, final String nickname, final String email, final String phone,
                    final Integer gender, final String avatar, final String deptName, final List<String> postNames,
                    final List<String> roleNames, final LocalDateTime createTime) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.avatar = avatar;
        this.deptName = deptName;
        this.postNames = postNames;
        this.roleNames = roleNames;
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public static ProfileResponse.ProfileResponseBuilder builder() {
        return new ProfileResponse.ProfileResponseBuilder();
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
    public String getDeptName() {
        return this.deptName;
    }

    @SuppressWarnings("all")
    public void setDeptName(final String deptName) {
        this.deptName = deptName;
    }

    @SuppressWarnings("all")
    public List<String> getPostNames() {
        return this.postNames;
    }

    @SuppressWarnings("all")
    public void setPostNames(final List<String> postNames) {
        this.postNames = postNames;
    }

    @SuppressWarnings("all")
    public List<String> getRoleNames() {
        return this.roleNames;
    }

    @SuppressWarnings("all")
    public void setRoleNames(final List<String> roleNames) {
        this.roleNames = roleNames;
    }

    @SuppressWarnings("all")
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ProfileResponse))
            return false;
        final ProfileResponse other = (ProfileResponse) o;
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
        final java.lang.Object this$deptName = this.getDeptName();
        final java.lang.Object other$deptName = other.getDeptName();
        if (this$deptName == null ? other$deptName != null : !this$deptName.equals(other$deptName))
            return false;
        final java.lang.Object this$postNames = this.getPostNames();
        final java.lang.Object other$postNames = other.getPostNames();
        if (this$postNames == null ? other$postNames != null : !this$postNames.equals(other$postNames))
            return false;
        final java.lang.Object this$roleNames = this.getRoleNames();
        final java.lang.Object other$roleNames = other.getRoleNames();
        if (this$roleNames == null ? other$roleNames != null : !this$roleNames.equals(other$roleNames))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ProfileResponse;
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
        final java.lang.Object $deptName = this.getDeptName();
        result = result * PRIME + ($deptName == null ? 43 : $deptName.hashCode());
        final java.lang.Object $postNames = this.getPostNames();
        result = result * PRIME + ($postNames == null ? 43 : $postNames.hashCode());
        final java.lang.Object $roleNames = this.getRoleNames();
        result = result * PRIME + ($roleNames == null ? 43 : $roleNames.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "ProfileResponse(id=" + this.getId() + ", username=" + this.getUsername() + ", nickname="
                + this.getNickname() + ", email=" + this.getEmail() + ", phone=" + this.getPhone() + ", gender="
                + this.getGender() + ", avatar=" + this.getAvatar() + ", deptName=" + this.getDeptName()
                + ", postNames=" + this.getPostNames() + ", roleNames=" + this.getRoleNames() + ", createTime="
                + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class ProfileResponseBuilder {

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
        private String deptName;

        @SuppressWarnings("all")
        private List<String> postNames;

        @SuppressWarnings("all")
        private List<String> roleNames;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        ProfileResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ProfileResponse.ProfileResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ProfileResponse.ProfileResponseBuilder username(final String username) {
            this.username = username;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ProfileResponse.ProfileResponseBuilder nickname(final String nickname) {
            this.nickname = nickname;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ProfileResponse.ProfileResponseBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ProfileResponse.ProfileResponseBuilder phone(final String phone) {
            this.phone = phone;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ProfileResponse.ProfileResponseBuilder gender(final Integer gender) {
            this.gender = gender;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ProfileResponse.ProfileResponseBuilder avatar(final String avatar) {
            this.avatar = avatar;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ProfileResponse.ProfileResponseBuilder deptName(final String deptName) {
            this.deptName = deptName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ProfileResponse.ProfileResponseBuilder postNames(final List<String> postNames) {
            this.postNames = postNames;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ProfileResponse.ProfileResponseBuilder roleNames(final List<String> roleNames) {
            this.roleNames = roleNames;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ProfileResponse.ProfileResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public ProfileResponse build() {
            return new ProfileResponse(this.id, this.username, this.nickname, this.email, this.phone, this.gender,
                    this.avatar, this.deptName, this.postNames, this.roleNames, this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "ProfileResponse.ProfileResponseBuilder(id=" + this.id + ", username=" + this.username
                    + ", nickname=" + this.nickname + ", email=" + this.email + ", phone=" + this.phone + ", gender="
                    + this.gender + ", avatar=" + this.avatar + ", deptName=" + this.deptName + ", postNames="
                    + this.postNames + ", roleNames=" + this.roleNames + ", createTime=" + this.createTime + ")";
        }

    }

}
