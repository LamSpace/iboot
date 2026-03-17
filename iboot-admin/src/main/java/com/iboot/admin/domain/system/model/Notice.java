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

package com.iboot.admin.domain.system.model;

import java.time.LocalDateTime;

/**
 * 系统公告实体
 *
 * @author iBoot
 */
public class Notice {

    /**
     * 公告ID
     */
    private Long id;

    /**
     * 公告标题
     */
    private String noticeTitle;

    /**
     * 公告类型（字典 sys_notice_type）
     */
    private String noticeType;

    /**
     * 公告内容
     */
    private String noticeContent;

    /**
     * 状态（字典 sys_notice_status）：0-草稿，1-已发布，2-已撤回
     */
    private String status;

    /**
     * 是否置顶：0-否，1-是
     */
    private Integer isTop;

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

    /**
     * 备注
     */
    private String remark;

    @SuppressWarnings("all")
    public Notice() {
    }

    /**
     * Creates a new {@code Notice} instance.
     *
     * @param id            公告ID
     * @param noticeTitle   公告标题
     * @param noticeType    公告类型（字典 sys_notice_type）
     * @param noticeContent 公告内容
     * @param status        状态（字典 sys_notice_status）：0-草稿，1-已发布，2-已撤回
     * @param isTop         是否置顶：0-否，1-是
     * @param createBy      创建人
     * @param createTime    创建时间
     * @param updateBy      更新人
     * @param updateTime    更新时间
     * @param deleted       逻辑删除：0-未删除，1-已删除
     * @param remark        备注
     */
    @SuppressWarnings("all")
    public Notice(final Long id, final String noticeTitle, final String noticeType, final String noticeContent,
                  final String status, final Integer isTop, final String createBy, final LocalDateTime createTime,
                  final String updateBy, final LocalDateTime updateTime, final Integer deleted, final String remark) {
        this.id = id;
        this.noticeTitle = noticeTitle;
        this.noticeType = noticeType;
        this.noticeContent = noticeContent;
        this.status = status;
        this.isTop = isTop;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.deleted = deleted;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static Notice.NoticeBuilder builder() {
        return new Notice.NoticeBuilder();
    }

    /**
     * 发布公告
     */
    public void publish() {
        this.status = "1";
    }

    /**
     * 撤回公告
     */
    public void revoke() {
        this.status = "2";
    }

    /**
     * 是否为草稿状态
     */
    public boolean isDraft() {
        return "0".equals(this.status);
    }

    /**
     * 是否为已发布状态
     */
    public boolean isPublished() {
        return "1".equals(this.status);
    }

    /**
     * 是否为已撤回状态
     */
    public boolean isRevoked() {
        return "2".equals(this.status);
    }

    /**
     * 公告ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 公告ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 公告标题
     */
    @SuppressWarnings("all")
    public String getNoticeTitle() {
        return this.noticeTitle;
    }

    /**
     * 公告标题
     */
    @SuppressWarnings("all")
    public void setNoticeTitle(final String noticeTitle) {
        this.noticeTitle = noticeTitle;
    }

    /**
     * 公告类型（字典 sys_notice_type）
     */
    @SuppressWarnings("all")
    public String getNoticeType() {
        return this.noticeType;
    }

    /**
     * 公告类型（字典 sys_notice_type）
     */
    @SuppressWarnings("all")
    public void setNoticeType(final String noticeType) {
        this.noticeType = noticeType;
    }

    /**
     * 公告内容
     */
    @SuppressWarnings("all")
    public String getNoticeContent() {
        return this.noticeContent;
    }

    /**
     * 公告内容
     */
    @SuppressWarnings("all")
    public void setNoticeContent(final String noticeContent) {
        this.noticeContent = noticeContent;
    }

    /**
     * 状态（字典 sys_notice_status）：0-草稿，1-已发布，2-已撤回
     */
    @SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    /**
     * 状态（字典 sys_notice_status）：0-草稿，1-已发布，2-已撤回
     */
    @SuppressWarnings("all")
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * 是否置顶：0-否，1-是
     */
    @SuppressWarnings("all")
    public Integer getIsTop() {
        return this.isTop;
    }

    /**
     * 是否置顶：0-否，1-是
     */
    @SuppressWarnings("all")
    public void setIsTop(final Integer isTop) {
        this.isTop = isTop;
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

    /**
     * 备注
     */
    @SuppressWarnings("all")
    public String getRemark() {
        return this.remark;
    }

    /**
     * 备注
     */
    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Notice))
            return false;
        final Notice other = (Notice) o;
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
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
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
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy))
            return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Notice;
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
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
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
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "Notice(id=" + this.getId() + ", noticeTitle=" + this.getNoticeTitle() + ", noticeType="
                + this.getNoticeType() + ", noticeContent=" + this.getNoticeContent() + ", status=" + this.getStatus()
                + ", isTop=" + this.getIsTop() + ", createBy=" + this.getCreateBy() + ", createTime="
                + this.getCreateTime() + ", updateBy=" + this.getUpdateBy() + ", updateTime=" + this.getUpdateTime()
                + ", deleted=" + this.getDeleted() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class NoticeBuilder {

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
        private String updateBy;

        @SuppressWarnings("all")
        private LocalDateTime updateTime;

        @SuppressWarnings("all")
        private Integer deleted;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        NoticeBuilder() {
        }

        /**
         * 公告ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 公告标题
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder noticeTitle(final String noticeTitle) {
            this.noticeTitle = noticeTitle;
            return this;
        }

        /**
         * 公告类型（字典 sys_notice_type）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder noticeType(final String noticeType) {
            this.noticeType = noticeType;
            return this;
        }

        /**
         * 公告内容
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder noticeContent(final String noticeContent) {
            this.noticeContent = noticeContent;
            return this;
        }

        /**
         * 状态（字典 sys_notice_status）：0-草稿，1-已发布，2-已撤回
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * 是否置顶：0-否，1-是
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder isTop(final Integer isTop) {
            this.isTop = isTop;
            return this;
        }

        /**
         * 创建人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * 创建时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * 更新人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * 更新时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        /**
         * 逻辑删除：0-未删除，1-已删除
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder deleted(final Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * 备注
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Notice.NoticeBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public Notice build() {
            return new Notice(this.id, this.noticeTitle, this.noticeType, this.noticeContent, this.status, this.isTop,
                    this.createBy, this.createTime, this.updateBy, this.updateTime, this.deleted, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "Notice.NoticeBuilder(id=" + this.id + ", noticeTitle=" + this.noticeTitle + ", noticeType="
                    + this.noticeType + ", noticeContent=" + this.noticeContent + ", status=" + this.status + ", isTop="
                    + this.isTop + ", createBy=" + this.createBy + ", createTime=" + this.createTime + ", updateBy="
                    + this.updateBy + ", updateTime=" + this.updateTime + ", deleted=" + this.deleted + ", remark="
                    + this.remark + ")";
        }

    }

}
