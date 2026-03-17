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
 * 角色响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "角色响应")
public class RoleResponse {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "显示顺序")
    private Integer orderNum;

    @Schema(description = "数据权限范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人")
    private Integer dataScope;

    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;

    @Schema(description = "部门ID列表")
    private List<Long> deptIds;

    @SuppressWarnings("all")
    public RoleResponse() {
    }

    @SuppressWarnings("all")
    public RoleResponse(final Long id, final String roleCode, final String roleName, final Integer orderNum,
                        final Integer dataScope, final Integer status, final String remark, final LocalDateTime createTime,
                        final List<Long> menuIds, final List<Long> deptIds) {
        this.id = id;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.orderNum = orderNum;
        this.dataScope = dataScope;
        this.status = status;
        this.remark = remark;
        this.createTime = createTime;
        this.menuIds = menuIds;
        this.deptIds = deptIds;
    }

    @SuppressWarnings("all")
    public static RoleResponse.RoleResponseBuilder builder() {
        return new RoleResponse.RoleResponseBuilder();
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
    public String getRoleCode() {
        return this.roleCode;
    }

    @SuppressWarnings("all")
    public void setRoleCode(final String roleCode) {
        this.roleCode = roleCode;
    }

    @SuppressWarnings("all")
    public String getRoleName() {
        return this.roleName;
    }

    @SuppressWarnings("all")
    public void setRoleName(final String roleName) {
        this.roleName = roleName;
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
    public Integer getDataScope() {
        return this.dataScope;
    }

    @SuppressWarnings("all")
    public void setDataScope(final Integer dataScope) {
        this.dataScope = dataScope;
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
    public String getRemark() {
        return this.remark;
    }

    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
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
    public List<Long> getMenuIds() {
        return this.menuIds;
    }

    @SuppressWarnings("all")
    public void setMenuIds(final List<Long> menuIds) {
        this.menuIds = menuIds;
    }

    @SuppressWarnings("all")
    public List<Long> getDeptIds() {
        return this.deptIds;
    }

    @SuppressWarnings("all")
    public void setDeptIds(final List<Long> deptIds) {
        this.deptIds = deptIds;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RoleResponse))
            return false;
        final RoleResponse other = (RoleResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$orderNum = this.getOrderNum();
        final java.lang.Object other$orderNum = other.getOrderNum();
        if (this$orderNum == null ? other$orderNum != null : !this$orderNum.equals(other$orderNum))
            return false;
        final java.lang.Object this$dataScope = this.getDataScope();
        final java.lang.Object other$dataScope = other.getDataScope();
        if (this$dataScope == null ? other$dataScope != null : !this$dataScope.equals(other$dataScope))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$roleCode = this.getRoleCode();
        final java.lang.Object other$roleCode = other.getRoleCode();
        if (this$roleCode == null ? other$roleCode != null : !this$roleCode.equals(other$roleCode))
            return false;
        final java.lang.Object this$roleName = this.getRoleName();
        final java.lang.Object other$roleName = other.getRoleName();
        if (this$roleName == null ? other$roleName != null : !this$roleName.equals(other$roleName))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        final java.lang.Object this$menuIds = this.getMenuIds();
        final java.lang.Object other$menuIds = other.getMenuIds();
        if (this$menuIds == null ? other$menuIds != null : !this$menuIds.equals(other$menuIds))
            return false;
        final java.lang.Object this$deptIds = this.getDeptIds();
        final java.lang.Object other$deptIds = other.getDeptIds();
        if (this$deptIds == null ? other$deptIds != null : !this$deptIds.equals(other$deptIds))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RoleResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $orderNum = this.getOrderNum();
        result = result * PRIME + ($orderNum == null ? 43 : $orderNum.hashCode());
        final java.lang.Object $dataScope = this.getDataScope();
        result = result * PRIME + ($dataScope == null ? 43 : $dataScope.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $roleCode = this.getRoleCode();
        result = result * PRIME + ($roleCode == null ? 43 : $roleCode.hashCode());
        final java.lang.Object $roleName = this.getRoleName();
        result = result * PRIME + ($roleName == null ? 43 : $roleName.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $menuIds = this.getMenuIds();
        result = result * PRIME + ($menuIds == null ? 43 : $menuIds.hashCode());
        final java.lang.Object $deptIds = this.getDeptIds();
        result = result * PRIME + ($deptIds == null ? 43 : $deptIds.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "RoleResponse(id=" + this.getId() + ", roleCode=" + this.getRoleCode() + ", roleName="
                + this.getRoleName() + ", orderNum=" + this.getOrderNum() + ", dataScope=" + this.getDataScope()
                + ", status=" + this.getStatus() + ", remark=" + this.getRemark() + ", createTime="
                + this.getCreateTime() + ", menuIds=" + this.getMenuIds() + ", deptIds=" + this.getDeptIds() + ")";
    }

    @SuppressWarnings("all")
    public static class RoleResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String roleCode;

        @SuppressWarnings("all")
        private String roleName;

        @SuppressWarnings("all")
        private Integer orderNum;

        @SuppressWarnings("all")
        private Integer dataScope;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private List<Long> menuIds;

        @SuppressWarnings("all")
        private List<Long> deptIds;

        @SuppressWarnings("all")
        RoleResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleResponse.RoleResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleResponse.RoleResponseBuilder roleCode(final String roleCode) {
            this.roleCode = roleCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleResponse.RoleResponseBuilder roleName(final String roleName) {
            this.roleName = roleName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleResponse.RoleResponseBuilder orderNum(final Integer orderNum) {
            this.orderNum = orderNum;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleResponse.RoleResponseBuilder dataScope(final Integer dataScope) {
            this.dataScope = dataScope;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleResponse.RoleResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleResponse.RoleResponseBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleResponse.RoleResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleResponse.RoleResponseBuilder menuIds(final List<Long> menuIds) {
            this.menuIds = menuIds;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleResponse.RoleResponseBuilder deptIds(final List<Long> deptIds) {
            this.deptIds = deptIds;
            return this;
        }

        @SuppressWarnings("all")
        public RoleResponse build() {
            return new RoleResponse(this.id, this.roleCode, this.roleName, this.orderNum, this.dataScope, this.status,
                    this.remark, this.createTime, this.menuIds, this.deptIds);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "RoleResponse.RoleResponseBuilder(id=" + this.id + ", roleCode=" + this.roleCode + ", roleName="
                    + this.roleName + ", orderNum=" + this.orderNum + ", dataScope=" + this.dataScope + ", status="
                    + this.status + ", remark=" + this.remark + ", createTime=" + this.createTime + ", menuIds="
                    + this.menuIds + ", deptIds=" + this.deptIds + ")";
        }

    }

}
