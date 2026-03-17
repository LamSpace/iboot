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
 * 角色导出VO
 *
 * @author iBoot
 */
public class RoleExportVO {

    @ExcelColumn(name = "角色ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "角色编码", order = 2, width = 20)
    private String roleCode;

    @ExcelColumn(name = "角色名称", order = 3, width = 20)
    private String roleName;

    @ExcelColumn(name = "显示顺序", order = 4, width = 10)
    private Integer orderNum;

    @ExcelColumn(name = "数据权限", order = 5, width = 15, dictType = "sys_data_scope")
    private Integer dataScope;

    @ExcelColumn(name = "状态", order = 6, width = 8, dictType = "sys_normal_disable")
    private Integer status;

    @ExcelColumn(name = "创建时间", order = 7, width = 20)
    private LocalDateTime createTime;

    @ExcelColumn(name = "备注", order = 8, width = 30)
    private String remark;

    @SuppressWarnings("all")
    public RoleExportVO() {
    }

    @SuppressWarnings("all")
    public RoleExportVO(final Long id, final String roleCode, final String roleName, final Integer orderNum,
                        final Integer dataScope, final Integer status, final LocalDateTime createTime, final String remark) {
        this.id = id;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.orderNum = orderNum;
        this.dataScope = dataScope;
        this.status = status;
        this.createTime = createTime;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static RoleExportVO.RoleExportVOBuilder builder() {
        return new RoleExportVO.RoleExportVOBuilder();
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
        if (!(o instanceof RoleExportVO))
            return false;
        final RoleExportVO other = (RoleExportVO) o;
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
        return other instanceof RoleExportVO;
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
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "RoleExportVO(id=" + this.getId() + ", roleCode=" + this.getRoleCode() + ", roleName="
                + this.getRoleName() + ", orderNum=" + this.getOrderNum() + ", dataScope=" + this.getDataScope()
                + ", status=" + this.getStatus() + ", createTime=" + this.getCreateTime() + ", remark="
                + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class RoleExportVOBuilder {

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
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        RoleExportVOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleExportVO.RoleExportVOBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleExportVO.RoleExportVOBuilder roleCode(final String roleCode) {
            this.roleCode = roleCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleExportVO.RoleExportVOBuilder roleName(final String roleName) {
            this.roleName = roleName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleExportVO.RoleExportVOBuilder orderNum(final Integer orderNum) {
            this.orderNum = orderNum;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleExportVO.RoleExportVOBuilder dataScope(final Integer dataScope) {
            this.dataScope = dataScope;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleExportVO.RoleExportVOBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleExportVO.RoleExportVOBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RoleExportVO.RoleExportVOBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public RoleExportVO build() {
            return new RoleExportVO(this.id, this.roleCode, this.roleName, this.orderNum, this.dataScope, this.status,
                    this.createTime, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "RoleExportVO.RoleExportVOBuilder(id=" + this.id + ", roleCode=" + this.roleCode + ", roleName="
                    + this.roleName + ", orderNum=" + this.orderNum + ", dataScope=" + this.dataScope + ", status="
                    + this.status + ", createTime=" + this.createTime + ", remark=" + this.remark + ")";
        }

    }

}
