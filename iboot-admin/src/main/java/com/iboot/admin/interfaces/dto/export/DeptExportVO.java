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
 * 部门导出VO
 *
 * @author iBoot
 */
public class DeptExportVO {

    @ExcelColumn(name = "部门ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "层级", order = 2, width = 8)
    private Integer level;

    @ExcelColumn(name = "部门编码", order = 3, width = 15)
    private String deptCode;

    @ExcelColumn(name = "部门名称", order = 4, width = 20)
    private String deptName;

    @ExcelColumn(name = "负责人", order = 5, width = 12)
    private String leader;

    @ExcelColumn(name = "联系电话", order = 6, width = 15)
    private String phone;

    @ExcelColumn(name = "邮箱", order = 7, width = 25)
    private String email;

    @ExcelColumn(name = "排序", order = 8, width = 8)
    private Integer orderNum;

    @ExcelColumn(name = "状态", order = 9, width = 8, dictType = "sys_normal_disable")
    private Integer status;

    @ExcelColumn(name = "创建时间", order = 10, width = 20)
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    public DeptExportVO() {
    }

    @SuppressWarnings("all")
    public DeptExportVO(final Long id, final Integer level, final String deptCode, final String deptName,
                        final String leader, final String phone, final String email, final Integer orderNum, final Integer status,
                        final LocalDateTime createTime) {
        this.id = id;
        this.level = level;
        this.deptCode = deptCode;
        this.deptName = deptName;
        this.leader = leader;
        this.phone = phone;
        this.email = email;
        this.orderNum = orderNum;
        this.status = status;
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public static DeptExportVO.DeptExportVOBuilder builder() {
        return new DeptExportVO.DeptExportVOBuilder();
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
    public Integer getLevel() {
        return this.level;
    }

    @SuppressWarnings("all")
    public void setLevel(final Integer level) {
        this.level = level;
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
    public Integer getOrderNum() {
        return this.orderNum;
    }

    @SuppressWarnings("all")
    public void setOrderNum(final Integer orderNum) {
        this.orderNum = orderNum;
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

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DeptExportVO))
            return false;
        final DeptExportVO other = (DeptExportVO) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$level = this.getLevel();
        final java.lang.Object other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level))
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
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DeptExportVO;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $level = this.getLevel();
        result = result * PRIME + ($level == null ? 43 : $level.hashCode());
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
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "DeptExportVO(id=" + this.getId() + ", level=" + this.getLevel() + ", deptCode=" + this.getDeptCode()
                + ", deptName=" + this.getDeptName() + ", leader=" + this.getLeader() + ", phone=" + this.getPhone()
                + ", email=" + this.getEmail() + ", orderNum=" + this.getOrderNum() + ", status=" + this.getStatus()
                + ", createTime=" + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class DeptExportVOBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private Integer level;

        @SuppressWarnings("all")
        private String deptCode;

        @SuppressWarnings("all")
        private String deptName;

        @SuppressWarnings("all")
        private String leader;

        @SuppressWarnings("all")
        private String phone;

        @SuppressWarnings("all")
        private String email;

        @SuppressWarnings("all")
        private Integer orderNum;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        DeptExportVOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptExportVO.DeptExportVOBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptExportVO.DeptExportVOBuilder level(final Integer level) {
            this.level = level;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptExportVO.DeptExportVOBuilder deptCode(final String deptCode) {
            this.deptCode = deptCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptExportVO.DeptExportVOBuilder deptName(final String deptName) {
            this.deptName = deptName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptExportVO.DeptExportVOBuilder leader(final String leader) {
            this.leader = leader;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptExportVO.DeptExportVOBuilder phone(final String phone) {
            this.phone = phone;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptExportVO.DeptExportVOBuilder email(final String email) {
            this.email = email;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptExportVO.DeptExportVOBuilder orderNum(final Integer orderNum) {
            this.orderNum = orderNum;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptExportVO.DeptExportVOBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DeptExportVO.DeptExportVOBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public DeptExportVO build() {
            return new DeptExportVO(this.id, this.level, this.deptCode, this.deptName, this.leader, this.phone,
                    this.email, this.orderNum, this.status, this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "DeptExportVO.DeptExportVOBuilder(id=" + this.id + ", level=" + this.level + ", deptCode="
                    + this.deptCode + ", deptName=" + this.deptName + ", leader=" + this.leader + ", phone="
                    + this.phone + ", email=" + this.email + ", orderNum=" + this.orderNum + ", status=" + this.status
                    + ", createTime=" + this.createTime + ")";
        }

    }

}
