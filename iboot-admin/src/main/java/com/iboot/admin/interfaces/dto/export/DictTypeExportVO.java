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
 * 字典类型导出VO
 *
 * @author iBoot
 */
public class DictTypeExportVO {

    @ExcelColumn(name = "字典ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "字典名称", order = 2, width = 20)
    private String dictName;

    @ExcelColumn(name = "字典类型", order = 3, width = 25)
    private String dictType;

    @ExcelColumn(name = "状态", order = 4, width = 8, dictType = "sys_normal_disable")
    private Integer status;

    @ExcelColumn(name = "备注", order = 5, width = 30)
    private String remark;

    @ExcelColumn(name = "创建时间", order = 6, width = 20)
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    public DictTypeExportVO() {
    }

    @SuppressWarnings("all")
    public DictTypeExportVO(final Long id, final String dictName, final String dictType, final Integer status,
                            final String remark, final LocalDateTime createTime) {
        this.id = id;
        this.dictName = dictName;
        this.dictType = dictType;
        this.status = status;
        this.remark = remark;
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public static DictTypeExportVO.DictTypeExportVOBuilder builder() {
        return new DictTypeExportVO.DictTypeExportVOBuilder();
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
    public String getDictName() {
        return this.dictName;
    }

    @SuppressWarnings("all")
    public void setDictName(final String dictName) {
        this.dictName = dictName;
    }

    @SuppressWarnings("all")
    public String getDictType() {
        return this.dictType;
    }

    @SuppressWarnings("all")
    public void setDictType(final String dictType) {
        this.dictType = dictType;
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

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DictTypeExportVO))
            return false;
        final DictTypeExportVO other = (DictTypeExportVO) o;
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
        final java.lang.Object this$dictName = this.getDictName();
        final java.lang.Object other$dictName = other.getDictName();
        if (this$dictName == null ? other$dictName != null : !this$dictName.equals(other$dictName))
            return false;
        final java.lang.Object this$dictType = this.getDictType();
        final java.lang.Object other$dictType = other.getDictType();
        if (this$dictType == null ? other$dictType != null : !this$dictType.equals(other$dictType))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DictTypeExportVO;
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
        final java.lang.Object $dictName = this.getDictName();
        result = result * PRIME + ($dictName == null ? 43 : $dictName.hashCode());
        final java.lang.Object $dictType = this.getDictType();
        result = result * PRIME + ($dictType == null ? 43 : $dictType.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "DictTypeExportVO(id=" + this.getId() + ", dictName=" + this.getDictName() + ", dictType="
                + this.getDictType() + ", status=" + this.getStatus() + ", remark=" + this.getRemark() + ", createTime="
                + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class DictTypeExportVOBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String dictName;

        @SuppressWarnings("all")
        private String dictType;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        DictTypeExportVOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeExportVO.DictTypeExportVOBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeExportVO.DictTypeExportVOBuilder dictName(final String dictName) {
            this.dictName = dictName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeExportVO.DictTypeExportVOBuilder dictType(final String dictType) {
            this.dictType = dictType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeExportVO.DictTypeExportVOBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeExportVO.DictTypeExportVOBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeExportVO.DictTypeExportVOBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public DictTypeExportVO build() {
            return new DictTypeExportVO(this.id, this.dictName, this.dictType, this.status, this.remark,
                    this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "DictTypeExportVO.DictTypeExportVOBuilder(id=" + this.id + ", dictName=" + this.dictName
                    + ", dictType=" + this.dictType + ", status=" + this.status + ", remark=" + this.remark
                    + ", createTime=" + this.createTime + ")";
        }

    }

}
