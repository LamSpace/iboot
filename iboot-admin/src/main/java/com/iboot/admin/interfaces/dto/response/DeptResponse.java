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
 * 部门响应 DTO。
 *
 * <p>用于向客户端返回数据；字段与领域模型或数据库字段对应，不包含业务行为。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "部门响应")
public class DeptResponse {

    /**
     * 部门ID。
     */
    @Schema(description = "部门ID")
    private Long id;

    /**
     * 父部门ID。
     */
    @Schema(description = "父部门ID")
    private Long parentId;

    /**
     * 部门编码。
     */
    @Schema(description = "部门编码")
    private String deptCode;

    /**
     * 部门名称。
     */
    @Schema(description = "部门名称")
    private String deptName;

    /**
     * 显示顺序。
     */
    @Schema(description = "显示顺序")
    private Integer orderNum;

    /**
     * 负责人。
     */
    @Schema(description = "负责人")
    private String leader;

    /**
     * 负责人电话。
     */
    @Schema(description = "负责人电话")
    private String phone;

    /**
     * 负责人邮箱。
     */
    @Schema(description = "负责人邮箱")
    private String email;

    /**
     * 状态：0-停用，1-正常。
     */
    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;

    /**
     * 创建时间。
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 子部门列表。
     */
    @Schema(description = "子部门列表")
    private List<DeptResponse> children;

    @SuppressWarnings("all")
    public DeptResponse() {
    }

    @SuppressWarnings("all")
    public DeptResponse(final Long id, final Long parentId, final String deptCode, final String deptName,
                        final Integer orderNum, final String leader, final String phone, final String email, final Integer status,
                        final LocalDateTime createTime, final List<DeptResponse> children) {
        this.id = id;
        this.parentId = parentId;
        this.deptCode = deptCode;
        this.deptName = deptName;
        this.orderNum = orderNum;
        this.leader = leader;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.createTime = createTime;
        this.children = children;
    }

    @SuppressWarnings("all")
    public static DeptResponse.DeptResponseBuilder builder() {
        return new DeptResponse.DeptResponseBuilder();
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
    public Long getParentId() {
        return this.parentId;
    }

    @SuppressWarnings("all")
    public void setParentId(final Long parentId) {
        this.parentId = parentId;
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
    public String getDeptName() {
        return this.deptName;
    }

    @SuppressWarnings("all")
    public void setDeptName(final String deptName) {
        this.deptName = deptName;
    }

    @SuppressWarnings("all")
    public Integer getOrderNum() {
        return this.orderNum;
    }

    @SuppressWarnings("all")
    public void setOrderNum(final Integer orderNum) {
        this.orderNum = orderNum;
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
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public List<DeptResponse> getChildren() {
        return this.children;
    }

    @SuppressWarnings("all")
    public void setChildren(final List<DeptResponse> children) {
        this.children = children;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DeptResponse))
            return false;
        final DeptResponse other = (DeptResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId))
            return false;
        final java.lang.Object this$orderNum = this.getOrderNum();
        final java.lang.Object other$orderNum = other.getOrderNum();
        if (this$orderNum == null ? other$orderNum != null : !this$orderNum.equals(other$orderNum))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$deptCode = this.getDeptCode();
        final java.lang.Object other$deptCode = other.getDeptCode();
        if (this$deptCode == null ? other$deptCode != null : !this$deptCode.equals(other$deptCode))
            return false;
        final java.lang.Object this$deptName = this.getDeptName();
        final java.lang.Object other$deptName = other.getDeptName();
        if (this$deptName == null ? other$deptName != null : !this$deptName.equals(other$deptName))
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
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        final java.lang.Object this$children = this.getChildren();
        final java.lang.Object other$children = other.getChildren();
        if (this$children == null ? other$children != null : !this$children.equals(other$children))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DeptResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
        final java.lang.Object $orderNum = this.getOrderNum();
        result = result * PRIME + ($orderNum == null ? 43 : $orderNum.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $deptCode = this.getDeptCode();
        result = result * PRIME + ($deptCode == null ? 43 : $deptCode.hashCode());
        final java.lang.Object $deptName = this.getDeptName();
        result = result * PRIME + ($deptName == null ? 43 : $deptName.hashCode());
        final java.lang.Object $leader = this.getLeader();
        result = result * PRIME + ($leader == null ? 43 : $leader.hashCode());
        final java.lang.Object $phone = this.getPhone();
        result = result * PRIME + ($phone == null ? 43 : $phone.hashCode());
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $children = this.getChildren();
        result = result * PRIME + ($children == null ? 43 : $children.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "DeptResponse(id=" + this.getId() + ", parentId=" + this.getParentId() + ", deptCode="
                + this.getDeptCode() + ", deptName=" + this.getDeptName() + ", orderNum=" + this.getOrderNum()
                + ", leader=" + this.getLeader() + ", phone=" + this.getPhone() + ", email=" + this.getEmail()
                + ", status=" + this.getStatus() + ", createTime=" + this.getCreateTime() + ", children="
                + this.getChildren() + ")";
    }

    @SuppressWarnings("all")
    public static class DeptResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private Long parentId;

        @SuppressWarnings("all")
        private String deptCode;

        @SuppressWarnings("all")
        private String deptName;

        @SuppressWarnings("all")
        private Integer orderNum;

        @SuppressWarnings("all")
        private String leader;

        @SuppressWarnings("all")
        private String phone;

        @SuppressWarnings("all")
        private String email;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private List<DeptResponse> children;

        @SuppressWarnings("all")
        DeptResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptResponse.DeptResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptResponse.DeptResponseBuilder parentId(final Long parentId) {
            this.parentId = parentId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptResponse.DeptResponseBuilder deptCode(final String deptCode) {
            this.deptCode = deptCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptResponse.DeptResponseBuilder deptName(final String deptName) {
            this.deptName = deptName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptResponse.DeptResponseBuilder orderNum(final Integer orderNum) {
            this.orderNum = orderNum;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptResponse.DeptResponseBuilder leader(final String leader) {
            this.leader = leader;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptResponse.DeptResponseBuilder phone(final String phone) {
            this.phone = phone;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptResponse.DeptResponseBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptResponse.DeptResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptResponse.DeptResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptResponse.DeptResponseBuilder children(final List<DeptResponse> children) {
            this.children = children;
            return this;
        }

        @SuppressWarnings("all")
        public DeptResponse build() {
            return new DeptResponse(this.id, this.parentId, this.deptCode, this.deptName, this.orderNum, this.leader,
                    this.phone, this.email, this.status, this.createTime, this.children);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "DeptResponse.DeptResponseBuilder(id=" + this.id + ", parentId=" + this.parentId + ", deptCode="
                    + this.deptCode + ", deptName=" + this.deptName + ", orderNum=" + this.orderNum + ", leader="
                    + this.leader + ", phone=" + this.phone + ", email=" + this.email + ", status=" + this.status
                    + ", createTime=" + this.createTime + ", children=" + this.children + ")";
        }

    }

}
