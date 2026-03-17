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

/**
 * 字典数据响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "字典数据响应")
public class DictDataResponse {

    @Schema(description = "字典数据ID")
    private Long id;

    @Schema(description = "字典类型")
    private String dictType;

    @Schema(description = "字典标签")
    private String dictLabel;

    @Schema(description = "字典值")
    private String dictValue;

    @Schema(description = "显示顺序")
    private Integer orderNum;

    @Schema(description = "样式属性")
    private String cssClass;

    @Schema(description = "表格回显样式")
    private String listClass;

    @Schema(description = "是否默认：0-否，1-是")
    private Integer isDefault;

    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    public DictDataResponse() {
    }

    @SuppressWarnings("all")
    public DictDataResponse(final Long id, final String dictType, final String dictLabel, final String dictValue,
                            final Integer orderNum, final String cssClass, final String listClass, final Integer isDefault,
                            final Integer status, final String remark, final LocalDateTime createTime) {
        this.id = id;
        this.dictType = dictType;
        this.dictLabel = dictLabel;
        this.dictValue = dictValue;
        this.orderNum = orderNum;
        this.cssClass = cssClass;
        this.listClass = listClass;
        this.isDefault = isDefault;
        this.status = status;
        this.remark = remark;
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public static DictDataResponse.DictDataResponseBuilder builder() {
        return new DictDataResponse.DictDataResponseBuilder();
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
    public String getDictType() {
        return this.dictType;
    }

    @SuppressWarnings("all")
    public void setDictType(final String dictType) {
        this.dictType = dictType;
    }

    @SuppressWarnings("all")
    public String getDictLabel() {
        return this.dictLabel;
    }

    @SuppressWarnings("all")
    public void setDictLabel(final String dictLabel) {
        this.dictLabel = dictLabel;
    }

    @SuppressWarnings("all")
    public String getDictValue() {
        return this.dictValue;
    }

    @SuppressWarnings("all")
    public void setDictValue(final String dictValue) {
        this.dictValue = dictValue;
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
    public String getCssClass() {
        return this.cssClass;
    }

    @SuppressWarnings("all")
    public void setCssClass(final String cssClass) {
        this.cssClass = cssClass;
    }

    @SuppressWarnings("all")
    public String getListClass() {
        return this.listClass;
    }

    @SuppressWarnings("all")
    public void setListClass(final String listClass) {
        this.listClass = listClass;
    }

    @SuppressWarnings("all")
    public Integer getIsDefault() {
        return this.isDefault;
    }

    @SuppressWarnings("all")
    public void setIsDefault(final Integer isDefault) {
        this.isDefault = isDefault;
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
        if (!(o instanceof DictDataResponse))
            return false;
        final DictDataResponse other = (DictDataResponse) o;
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
        final java.lang.Object this$isDefault = this.getIsDefault();
        final java.lang.Object other$isDefault = other.getIsDefault();
        if (this$isDefault == null ? other$isDefault != null : !this$isDefault.equals(other$isDefault))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$dictType = this.getDictType();
        final java.lang.Object other$dictType = other.getDictType();
        if (this$dictType == null ? other$dictType != null : !this$dictType.equals(other$dictType))
            return false;
        final java.lang.Object this$dictLabel = this.getDictLabel();
        final java.lang.Object other$dictLabel = other.getDictLabel();
        if (this$dictLabel == null ? other$dictLabel != null : !this$dictLabel.equals(other$dictLabel))
            return false;
        final java.lang.Object this$dictValue = this.getDictValue();
        final java.lang.Object other$dictValue = other.getDictValue();
        if (this$dictValue == null ? other$dictValue != null : !this$dictValue.equals(other$dictValue))
            return false;
        final java.lang.Object this$cssClass = this.getCssClass();
        final java.lang.Object other$cssClass = other.getCssClass();
        if (this$cssClass == null ? other$cssClass != null : !this$cssClass.equals(other$cssClass))
            return false;
        final java.lang.Object this$listClass = this.getListClass();
        final java.lang.Object other$listClass = other.getListClass();
        if (this$listClass == null ? other$listClass != null : !this$listClass.equals(other$listClass))
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
        return other instanceof DictDataResponse;
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
        final java.lang.Object $isDefault = this.getIsDefault();
        result = result * PRIME + ($isDefault == null ? 43 : $isDefault.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $dictType = this.getDictType();
        result = result * PRIME + ($dictType == null ? 43 : $dictType.hashCode());
        final java.lang.Object $dictLabel = this.getDictLabel();
        result = result * PRIME + ($dictLabel == null ? 43 : $dictLabel.hashCode());
        final java.lang.Object $dictValue = this.getDictValue();
        result = result * PRIME + ($dictValue == null ? 43 : $dictValue.hashCode());
        final java.lang.Object $cssClass = this.getCssClass();
        result = result * PRIME + ($cssClass == null ? 43 : $cssClass.hashCode());
        final java.lang.Object $listClass = this.getListClass();
        result = result * PRIME + ($listClass == null ? 43 : $listClass.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "DictDataResponse(id=" + this.getId() + ", dictType=" + this.getDictType() + ", dictLabel="
                + this.getDictLabel() + ", dictValue=" + this.getDictValue() + ", orderNum=" + this.getOrderNum()
                + ", cssClass=" + this.getCssClass() + ", listClass=" + this.getListClass() + ", isDefault="
                + this.getIsDefault() + ", status=" + this.getStatus() + ", remark=" + this.getRemark()
                + ", createTime=" + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class DictDataResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String dictType;

        @SuppressWarnings("all")
        private String dictLabel;

        @SuppressWarnings("all")
        private String dictValue;

        @SuppressWarnings("all")
        private Integer orderNum;

        @SuppressWarnings("all")
        private String cssClass;

        @SuppressWarnings("all")
        private String listClass;

        @SuppressWarnings("all")
        private Integer isDefault;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        DictDataResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictDataResponse.DictDataResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictDataResponse.DictDataResponseBuilder dictType(final String dictType) {
            this.dictType = dictType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictDataResponse.DictDataResponseBuilder dictLabel(final String dictLabel) {
            this.dictLabel = dictLabel;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictDataResponse.DictDataResponseBuilder dictValue(final String dictValue) {
            this.dictValue = dictValue;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictDataResponse.DictDataResponseBuilder orderNum(final Integer orderNum) {
            this.orderNum = orderNum;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictDataResponse.DictDataResponseBuilder cssClass(final String cssClass) {
            this.cssClass = cssClass;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictDataResponse.DictDataResponseBuilder listClass(final String listClass) {
            this.listClass = listClass;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictDataResponse.DictDataResponseBuilder isDefault(final Integer isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictDataResponse.DictDataResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictDataResponse.DictDataResponseBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictDataResponse.DictDataResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public DictDataResponse build() {
            return new DictDataResponse(this.id, this.dictType, this.dictLabel, this.dictValue, this.orderNum,
                    this.cssClass, this.listClass, this.isDefault, this.status, this.remark, this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "DictDataResponse.DictDataResponseBuilder(id=" + this.id + ", dictType=" + this.dictType
                    + ", dictLabel=" + this.dictLabel + ", dictValue=" + this.dictValue + ", orderNum=" + this.orderNum
                    + ", cssClass=" + this.cssClass + ", listClass=" + this.listClass + ", isDefault=" + this.isDefault
                    + ", status=" + this.status + ", remark=" + this.remark + ", createTime=" + this.createTime + ")";
        }

    }

}
