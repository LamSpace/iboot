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
 * 字典类型响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "字典类型响应")
public class DictTypeResponse {

    @Schema(description = "字典类型ID")
    private Long id;

    @Schema(description = "字典类型")
    private String dictType;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    public DictTypeResponse() {
    }

    @SuppressWarnings("all")
    public DictTypeResponse(final Long id, final String dictType, final String dictName, final Integer status,
                            final String remark, final LocalDateTime createTime) {
        this.id = id;
        this.dictType = dictType;
        this.dictName = dictName;
        this.status = status;
        this.remark = remark;
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public static DictTypeResponse.DictTypeResponseBuilder builder() {
        return new DictTypeResponse.DictTypeResponseBuilder();
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
    public String getDictName() {
        return this.dictName;
    }

    @SuppressWarnings("all")
    public void setDictName(final String dictName) {
        this.dictName = dictName;
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
        if (!(o instanceof DictTypeResponse))
            return false;
        final DictTypeResponse other = (DictTypeResponse) o;
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
        final java.lang.Object this$dictType = this.getDictType();
        final java.lang.Object other$dictType = other.getDictType();
        if (this$dictType == null ? other$dictType != null : !this$dictType.equals(other$dictType))
            return false;
        final java.lang.Object this$dictName = this.getDictName();
        final java.lang.Object other$dictName = other.getDictName();
        if (this$dictName == null ? other$dictName != null : !this$dictName.equals(other$dictName))
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
        return other instanceof DictTypeResponse;
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
        final java.lang.Object $dictType = this.getDictType();
        result = result * PRIME + ($dictType == null ? 43 : $dictType.hashCode());
        final java.lang.Object $dictName = this.getDictName();
        result = result * PRIME + ($dictName == null ? 43 : $dictName.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "DictTypeResponse(id=" + this.getId() + ", dictType=" + this.getDictType() + ", dictName="
                + this.getDictName() + ", status=" + this.getStatus() + ", remark=" + this.getRemark() + ", createTime="
                + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class DictTypeResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String dictType;

        @SuppressWarnings("all")
        private String dictName;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        DictTypeResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeResponse.DictTypeResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeResponse.DictTypeResponseBuilder dictType(final String dictType) {
            this.dictType = dictType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeResponse.DictTypeResponseBuilder dictName(final String dictName) {
            this.dictName = dictName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeResponse.DictTypeResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeResponse.DictTypeResponseBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DictTypeResponse.DictTypeResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public DictTypeResponse build() {
            return new DictTypeResponse(this.id, this.dictType, this.dictName, this.status, this.remark,
                    this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "DictTypeResponse.DictTypeResponseBuilder(id=" + this.id + ", dictType=" + this.dictType
                    + ", dictName=" + this.dictName + ", status=" + this.status + ", remark=" + this.remark
                    + ", createTime=" + this.createTime + ")";
        }

    }

}
