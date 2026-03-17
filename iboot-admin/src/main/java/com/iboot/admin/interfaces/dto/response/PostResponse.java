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
 * 岗位响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "岗位响应")
public class PostResponse {

    @Schema(description = "岗位ID")
    private Long id;

    @Schema(description = "岗位编码")
    private String postCode;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "显示顺序")
    private Integer orderNum;

    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    public PostResponse() {
    }

    @SuppressWarnings("all")
    public PostResponse(final Long id, final String postCode, final String postName, final Integer orderNum,
                        final Integer status, final String remark, final LocalDateTime createTime) {
        this.id = id;
        this.postCode = postCode;
        this.postName = postName;
        this.orderNum = orderNum;
        this.status = status;
        this.remark = remark;
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public static PostResponse.PostResponseBuilder builder() {
        return new PostResponse.PostResponseBuilder();
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
    public String getPostCode() {
        return this.postCode;
    }

    @SuppressWarnings("all")
    public void setPostCode(final String postCode) {
        this.postCode = postCode;
    }

    @SuppressWarnings("all")
    public String getPostName() {
        return this.postName;
    }

    @SuppressWarnings("all")
    public void setPostName(final String postName) {
        this.postName = postName;
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
        if (!(o instanceof PostResponse))
            return false;
        final PostResponse other = (PostResponse) o;
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
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$postCode = this.getPostCode();
        final java.lang.Object other$postCode = other.getPostCode();
        if (this$postCode == null ? other$postCode != null : !this$postCode.equals(other$postCode))
            return false;
        final java.lang.Object this$postName = this.getPostName();
        final java.lang.Object other$postName = other.getPostName();
        if (this$postName == null ? other$postName != null : !this$postName.equals(other$postName))
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
        return other instanceof PostResponse;
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
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $postCode = this.getPostCode();
        result = result * PRIME + ($postCode == null ? 43 : $postCode.hashCode());
        final java.lang.Object $postName = this.getPostName();
        result = result * PRIME + ($postName == null ? 43 : $postName.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "PostResponse(id=" + this.getId() + ", postCode=" + this.getPostCode() + ", postName="
                + this.getPostName() + ", orderNum=" + this.getOrderNum() + ", status=" + this.getStatus() + ", remark="
                + this.getRemark() + ", createTime=" + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class PostResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String postCode;

        @SuppressWarnings("all")
        private String postName;

        @SuppressWarnings("all")
        private Integer orderNum;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        PostResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PostResponse.PostResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PostResponse.PostResponseBuilder postCode(final String postCode) {
            this.postCode = postCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PostResponse.PostResponseBuilder postName(final String postName) {
            this.postName = postName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PostResponse.PostResponseBuilder orderNum(final Integer orderNum) {
            this.orderNum = orderNum;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PostResponse.PostResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PostResponse.PostResponseBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PostResponse.PostResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public PostResponse build() {
            return new PostResponse(this.id, this.postCode, this.postName, this.orderNum, this.status, this.remark,
                    this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "PostResponse.PostResponseBuilder(id=" + this.id + ", postCode=" + this.postCode + ", postName="
                    + this.postName + ", orderNum=" + this.orderNum + ", status=" + this.status + ", remark="
                    + this.remark + ", createTime=" + this.createTime + ")";
        }

    }

}
