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
 * 公告响应 DTO（管理端）
 *
 * @author iBoot
 */
@Schema(description = "公告响应")
public class NoticeResponse {

    @Schema(description = "公告ID")
    private Long id;

    @Schema(description = "公告标题")
    private String noticeTitle;

    @Schema(description = "公告类型")
    private String noticeType;

    @Schema(description = "公告内容")
    private String noticeContent;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "是否置顶")
    private Integer isTop;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String remark;

    @SuppressWarnings("all")
    public NoticeResponse() {
    }

    @SuppressWarnings("all")
    public NoticeResponse(final Long id, final String noticeTitle, final String noticeType, final String noticeContent,
                          final String status, final Integer isTop, final String createBy, final LocalDateTime createTime,
                          final String remark) {
        this.id = id;
        this.noticeTitle = noticeTitle;
        this.noticeType = noticeType;
        this.noticeContent = noticeContent;
        this.status = status;
        this.isTop = isTop;
        this.createBy = createBy;
        this.createTime = createTime;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static NoticeResponse.NoticeResponseBuilder builder() {
        return new NoticeResponse.NoticeResponseBuilder();
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
    public String getNoticeTitle() {
        return this.noticeTitle;
    }

    @SuppressWarnings("all")
    public void setNoticeTitle(final String noticeTitle) {
        this.noticeTitle = noticeTitle;
    }

    @SuppressWarnings("all")
    public String getNoticeType() {
        return this.noticeType;
    }

    @SuppressWarnings("all")
    public void setNoticeType(final String noticeType) {
        this.noticeType = noticeType;
    }

    @SuppressWarnings("all")
    public String getNoticeContent() {
        return this.noticeContent;
    }

    @SuppressWarnings("all")
    public void setNoticeContent(final String noticeContent) {
        this.noticeContent = noticeContent;
    }

    @SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    @SuppressWarnings("all")
    public void setStatus(final String status) {
        this.status = status;
    }

    @SuppressWarnings("all")
    public Integer getIsTop() {
        return this.isTop;
    }

    @SuppressWarnings("all")
    public void setIsTop(final Integer isTop) {
        this.isTop = isTop;
    }

    @SuppressWarnings("all")
    public String getCreateBy() {
        return this.createBy;
    }

    @SuppressWarnings("all")
    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
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
        if (!(o instanceof NoticeResponse))
            return false;
        final NoticeResponse other = (NoticeResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$isTop = this.getIsTop();
        final java.lang.Object other$isTop = other.getIsTop();
        if (this$isTop == null ? other$isTop != null : !this$isTop.equals(other$isTop))
            return false;
        final java.lang.Object this$noticeTitle = this.getNoticeTitle();
        final java.lang.Object other$noticeTitle = other.getNoticeTitle();
        if (this$noticeTitle == null ? other$noticeTitle != null : !this$noticeTitle.equals(other$noticeTitle))
            return false;
        final java.lang.Object this$noticeType = this.getNoticeType();
        final java.lang.Object other$noticeType = other.getNoticeType();
        if (this$noticeType == null ? other$noticeType != null : !this$noticeType.equals(other$noticeType))
            return false;
        final java.lang.Object this$noticeContent = this.getNoticeContent();
        final java.lang.Object other$noticeContent = other.getNoticeContent();
        if (this$noticeContent == null ? other$noticeContent != null : !this$noticeContent.equals(other$noticeContent))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy))
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
        return other instanceof NoticeResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $isTop = this.getIsTop();
        result = result * PRIME + ($isTop == null ? 43 : $isTop.hashCode());
        final java.lang.Object $noticeTitle = this.getNoticeTitle();
        result = result * PRIME + ($noticeTitle == null ? 43 : $noticeTitle.hashCode());
        final java.lang.Object $noticeType = this.getNoticeType();
        result = result * PRIME + ($noticeType == null ? 43 : $noticeType.hashCode());
        final java.lang.Object $noticeContent = this.getNoticeContent();
        result = result * PRIME + ($noticeContent == null ? 43 : $noticeContent.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "NoticeResponse(id=" + this.getId() + ", noticeTitle=" + this.getNoticeTitle() + ", noticeType="
                + this.getNoticeType() + ", noticeContent=" + this.getNoticeContent() + ", status=" + this.getStatus()
                + ", isTop=" + this.getIsTop() + ", createBy=" + this.getCreateBy() + ", createTime="
                + this.getCreateTime() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class NoticeResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String noticeTitle;

        @SuppressWarnings("all")
        private String noticeType;

        @SuppressWarnings("all")
        private String noticeContent;

        @SuppressWarnings("all")
        private String status;

        @SuppressWarnings("all")
        private Integer isTop;

        @SuppressWarnings("all")
        private String createBy;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        NoticeResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public NoticeResponse.NoticeResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public NoticeResponse.NoticeResponseBuilder noticeTitle(final String noticeTitle) {
            this.noticeTitle = noticeTitle;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public NoticeResponse.NoticeResponseBuilder noticeType(final String noticeType) {
            this.noticeType = noticeType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public NoticeResponse.NoticeResponseBuilder noticeContent(final String noticeContent) {
            this.noticeContent = noticeContent;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public NoticeResponse.NoticeResponseBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public NoticeResponse.NoticeResponseBuilder isTop(final Integer isTop) {
            this.isTop = isTop;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public NoticeResponse.NoticeResponseBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public NoticeResponse.NoticeResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public NoticeResponse.NoticeResponseBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public NoticeResponse build() {
            return new NoticeResponse(this.id, this.noticeTitle, this.noticeType, this.noticeContent, this.status,
                    this.isTop, this.createBy, this.createTime, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "NoticeResponse.NoticeResponseBuilder(id=" + this.id + ", noticeTitle=" + this.noticeTitle
                    + ", noticeType=" + this.noticeType + ", noticeContent=" + this.noticeContent + ", status="
                    + this.status + ", isTop=" + this.isTop + ", createBy=" + this.createBy + ", createTime="
                    + this.createTime + ", remark=" + this.remark + ")";
        }

    }

}
