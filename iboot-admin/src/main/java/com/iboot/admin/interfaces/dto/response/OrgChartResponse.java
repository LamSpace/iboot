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
 * 组织架构图响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "组织架构图响应")
public class OrgChartResponse {

    @Schema(description = "部门ID")
    private Long id;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门编码")
    private String deptCode;

    @Schema(description = "负责人")
    private String leader;

    @Schema(description = "负责人电话")
    private String phone;

    @Schema(description = "负责人邮箱")
    private String email;

    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;

    @Schema(description = "成员数量")
    private Integer memberCount;

    @Schema(description = "子部门列表")
    private List<OrgChartResponse> children;

    @SuppressWarnings("all")
    public OrgChartResponse() {
    }

    @SuppressWarnings("all")
    public OrgChartResponse(final Long id, final String deptName, final String deptCode, final String leader,
                            final String phone, final String email, final Integer status, final Integer memberCount,
                            final List<OrgChartResponse> children) {
        this.id = id;
        this.deptName = deptName;
        this.deptCode = deptCode;
        this.leader = leader;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.memberCount = memberCount;
        this.children = children;
    }

    @SuppressWarnings("all")
    public static OrgChartResponse.OrgChartResponseBuilder builder() {
        return new OrgChartResponse.OrgChartResponseBuilder();
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
    public String getDeptName() {
        return this.deptName;
    }

    @SuppressWarnings("all")
    public void setDeptName(final String deptName) {
        this.deptName = deptName;
    }

    @SuppressWarnings("all")
    public String getDeptCode() {
        return this.deptCode;
    }

    @SuppressWarnings("all")
    public void setDeptCode(final String deptCode) {
        this.deptCode = deptCode;
    }

    @SuppressWarnings("all")
    public String getLeader() {
        return this.leader;
    }

    @SuppressWarnings("all")
    public void setLeader(final String leader) {
        this.leader = leader;
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
    public String getEmail() {
        return this.email;
    }

    @SuppressWarnings("all")
    public void setEmail(final String email) {
        this.email = email;
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
    public Integer getMemberCount() {
        return this.memberCount;
    }

    @SuppressWarnings("all")
    public void setMemberCount(final Integer memberCount) {
        this.memberCount = memberCount;
    }

    @SuppressWarnings("all")
    public List<OrgChartResponse> getChildren() {
        return this.children;
    }

    @SuppressWarnings("all")
    public void setChildren(final List<OrgChartResponse> children) {
        this.children = children;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof OrgChartResponse))
            return false;
        final OrgChartResponse other = (OrgChartResponse) o;
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
        final java.lang.Object this$memberCount = this.getMemberCount();
        final java.lang.Object other$memberCount = other.getMemberCount();
        if (this$memberCount == null ? other$memberCount != null : !this$memberCount.equals(other$memberCount))
            return false;
        final java.lang.Object this$deptName = this.getDeptName();
        final java.lang.Object other$deptName = other.getDeptName();
        if (this$deptName == null ? other$deptName != null : !this$deptName.equals(other$deptName))
            return false;
        final java.lang.Object this$deptCode = this.getDeptCode();
        final java.lang.Object other$deptCode = other.getDeptCode();
        if (this$deptCode == null ? other$deptCode != null : !this$deptCode.equals(other$deptCode))
            return false;
        final java.lang.Object this$leader = this.getLeader();
        final java.lang.Object other$leader = other.getLeader();
        if (this$leader == null ? other$leader != null : !this$leader.equals(other$leader))
            return false;
        final java.lang.Object this$phone = this.getPhone();
        final java.lang.Object other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone))
            return false;
        final java.lang.Object this$email = this.getEmail();
        final java.lang.Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email))
            return false;
        final java.lang.Object this$children = this.getChildren();
        final java.lang.Object other$children = other.getChildren();
        if (this$children == null ? other$children != null : !this$children.equals(other$children))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrgChartResponse;
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
        final java.lang.Object $memberCount = this.getMemberCount();
        result = result * PRIME + ($memberCount == null ? 43 : $memberCount.hashCode());
        final java.lang.Object $deptName = this.getDeptName();
        result = result * PRIME + ($deptName == null ? 43 : $deptName.hashCode());
        final java.lang.Object $deptCode = this.getDeptCode();
        result = result * PRIME + ($deptCode == null ? 43 : $deptCode.hashCode());
        final java.lang.Object $leader = this.getLeader();
        result = result * PRIME + ($leader == null ? 43 : $leader.hashCode());
        final java.lang.Object $phone = this.getPhone();
        result = result * PRIME + ($phone == null ? 43 : $phone.hashCode());
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $children = this.getChildren();
        result = result * PRIME + ($children == null ? 43 : $children.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "OrgChartResponse(id=" + this.getId() + ", deptName=" + this.getDeptName() + ", deptCode="
                + this.getDeptCode() + ", leader=" + this.getLeader() + ", phone=" + this.getPhone() + ", email="
                + this.getEmail() + ", status=" + this.getStatus() + ", memberCount=" + this.getMemberCount()
                + ", children=" + this.getChildren() + ")";
    }

    @SuppressWarnings("all")
    public static class OrgChartResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String deptName;

        @SuppressWarnings("all")
        private String deptCode;

        @SuppressWarnings("all")
        private String leader;

        @SuppressWarnings("all")
        private String phone;

        @SuppressWarnings("all")
        private String email;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private Integer memberCount;

        @SuppressWarnings("all")
        private List<OrgChartResponse> children;

        @SuppressWarnings("all")
        OrgChartResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OrgChartResponse.OrgChartResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OrgChartResponse.OrgChartResponseBuilder deptName(final String deptName) {
            this.deptName = deptName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OrgChartResponse.OrgChartResponseBuilder deptCode(final String deptCode) {
            this.deptCode = deptCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OrgChartResponse.OrgChartResponseBuilder leader(final String leader) {
            this.leader = leader;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OrgChartResponse.OrgChartResponseBuilder phone(final String phone) {
            this.phone = phone;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OrgChartResponse.OrgChartResponseBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OrgChartResponse.OrgChartResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OrgChartResponse.OrgChartResponseBuilder memberCount(final Integer memberCount) {
            this.memberCount = memberCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OrgChartResponse.OrgChartResponseBuilder children(final List<OrgChartResponse> children) {
            this.children = children;
            return this;
        }

        @SuppressWarnings("all")
        public OrgChartResponse build() {
            return new OrgChartResponse(this.id, this.deptName, this.deptCode, this.leader, this.phone, this.email,
                    this.status, this.memberCount, this.children);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "OrgChartResponse.OrgChartResponseBuilder(id=" + this.id + ", deptName=" + this.deptName
                    + ", deptCode=" + this.deptCode + ", leader=" + this.leader + ", phone=" + this.phone + ", email="
                    + this.email + ", status=" + this.status + ", memberCount=" + this.memberCount + ", children="
                    + this.children + ")";
        }

    }

}
