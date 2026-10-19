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

package com.iboot.admin.infrastructure.persistence.po;

import java.time.LocalDateTime;

/**
 * 部门持久化对象
 * <p>
 * 对应数据库表：sys_dept
 * </p>
 *
 * <p>仅用于 MyBatis 持久化映射，不包含业务行为；与领域模型之间的转换由仓储实现负责。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class DeptPO {

    /**
     * 部门 ID
     */
    private Long id;

    /**
     * 父部门 ID
     */
    private Long parentId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门编码
     */
    private String deptCode;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 负责人
     */
    private String leader;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态：0-停用，1-正常
     */
    private Integer status;

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

    @SuppressWarnings("all")
    public DeptPO() {
    }

    /**
     * 部门 ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 部门 ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 父部门 ID
     */
    @SuppressWarnings("all")
    public Long getParentId() {
        return this.parentId;
    }

    /**
     * 父部门 ID
     */
    @SuppressWarnings("all")
    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    /**
     * 部门名称
     */
    @SuppressWarnings("all")
    public String getDeptName() {
        return this.deptName;
    }

    /**
     * 部门名称
     */
    @SuppressWarnings("all")
    public void setDeptName(final String deptName) {
        this.deptName = deptName;
    }

    /**
     * 部门编码
     */
    @SuppressWarnings("all")
    public String getDeptCode() {
        return this.deptCode;
    }

    /**
     * 部门编码
     */
    @SuppressWarnings("all")
    public void setDeptCode(final String deptCode) {
        this.deptCode = deptCode;
    }

    /**
     * 显示顺序
     */
    @SuppressWarnings("all")
    public Integer getOrderNum() {
        return this.orderNum;
    }

    /**
     * 显示顺序
     */
    @SuppressWarnings("all")
    public void setOrderNum(final Integer orderNum) {
        this.orderNum = orderNum;
    }

    /**
     * 负责人
     */
    @SuppressWarnings("all")
    public String getLeader() {
        return this.leader;
    }

    /**
     * 负责人
     */
    @SuppressWarnings("all")
    public void setLeader(final String leader) {
        this.leader = leader;
    }

    /**
     * 联系电话
     */
    @SuppressWarnings("all")
    public String getPhone() {
        return this.phone;
    }

    /**
     * 联系电话
     */
    @SuppressWarnings("all")
    public void setPhone(final String phone) {
        this.phone = phone;
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

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DeptPO))
            return false;
        final DeptPO other = (DeptPO) o;
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
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
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
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DeptPO;
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
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
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
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "DeptPO(id=" + this.getId() + ", parentId=" + this.getParentId() + ", deptName=" + this.getDeptName()
                + ", deptCode=" + this.getDeptCode() + ", orderNum=" + this.getOrderNum() + ", leader="
                + this.getLeader() + ", phone=" + this.getPhone() + ", email=" + this.getEmail() + ", status="
                + this.getStatus() + ", createBy=" + this.getCreateBy() + ", createTime=" + this.getCreateTime()
                + ", updateBy=" + this.getUpdateBy() + ", updateTime=" + this.getUpdateTime() + ", deleted="
                + this.getDeleted() + ")";
    }

}
