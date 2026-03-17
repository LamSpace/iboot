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
 * 用户消息响应 DTO（收件箱）
 *
 * @author iBoot
 */
@Schema(description = "用户消息响应")
public class UserMessageResponse {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "发送者类型")
    private String senderType;

    @Schema(description = "发送者ID")
    private Long senderId;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "是否已读")
    private Integer isRead;

    @Schema(description = "阅读时间")
    private LocalDateTime readTime;

    @SuppressWarnings("all")
    public UserMessageResponse() {
    }

    @SuppressWarnings("all")
    public UserMessageResponse(final Long id, final String title, final String content, final String messageType,
                               final String priority, final String senderType, final Long senderId, final String createBy,
                               final LocalDateTime createTime, final Integer isRead, final LocalDateTime readTime) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.messageType = messageType;
        this.priority = priority;
        this.senderType = senderType;
        this.senderId = senderId;
        this.createBy = createBy;
        this.createTime = createTime;
        this.isRead = isRead;
        this.readTime = readTime;
    }

    @SuppressWarnings("all")
    public static UserMessageResponse.UserMessageResponseBuilder builder() {
        return new UserMessageResponse.UserMessageResponseBuilder();
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
    public String getTitle() {
        return this.title;
    }

    @SuppressWarnings("all")
    public void setTitle(final String title) {
        this.title = title;
    }

    @SuppressWarnings("all")
    public String getContent() {
        return this.content;
    }

    @SuppressWarnings("all")
    public void setContent(final String content) {
        this.content = content;
    }

    @SuppressWarnings("all")
    public String getMessageType() {
        return this.messageType;
    }

    @SuppressWarnings("all")
    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    @SuppressWarnings("all")
    public String getPriority() {
        return this.priority;
    }

    @SuppressWarnings("all")
    public void setPriority(final String priority) {
        this.priority = priority;
    }

    @SuppressWarnings("all")
    public String getSenderType() {
        return this.senderType;
    }

    @SuppressWarnings("all")
    public void setSenderType(final String senderType) {
        this.senderType = senderType;
    }

    @SuppressWarnings("all")
    public Long getSenderId() {
        return this.senderId;
    }

    @SuppressWarnings("all")
    public void setSenderId(final Long senderId) {
        this.senderId = senderId;
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
    public Integer getIsRead() {
        return this.isRead;
    }

    @SuppressWarnings("all")
    public void setIsRead(final Integer isRead) {
        this.isRead = isRead;
    }

    @SuppressWarnings("all")
    public LocalDateTime getReadTime() {
        return this.readTime;
    }

    @SuppressWarnings("all")
    public void setReadTime(final LocalDateTime readTime) {
        this.readTime = readTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof UserMessageResponse))
            return false;
        final UserMessageResponse other = (UserMessageResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$senderId = this.getSenderId();
        final java.lang.Object other$senderId = other.getSenderId();
        if (this$senderId == null ? other$senderId != null : !this$senderId.equals(other$senderId))
            return false;
        final java.lang.Object this$isRead = this.getIsRead();
        final java.lang.Object other$isRead = other.getIsRead();
        if (this$isRead == null ? other$isRead != null : !this$isRead.equals(other$isRead))
            return false;
        final java.lang.Object this$title = this.getTitle();
        final java.lang.Object other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title))
            return false;
        final java.lang.Object this$content = this.getContent();
        final java.lang.Object other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content))
            return false;
        final java.lang.Object this$messageType = this.getMessageType();
        final java.lang.Object other$messageType = other.getMessageType();
        if (this$messageType == null ? other$messageType != null : !this$messageType.equals(other$messageType))
            return false;
        final java.lang.Object this$priority = this.getPriority();
        final java.lang.Object other$priority = other.getPriority();
        if (this$priority == null ? other$priority != null : !this$priority.equals(other$priority))
            return false;
        final java.lang.Object this$senderType = this.getSenderType();
        final java.lang.Object other$senderType = other.getSenderType();
        if (this$senderType == null ? other$senderType != null : !this$senderType.equals(other$senderType))
            return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        final java.lang.Object this$readTime = this.getReadTime();
        final java.lang.Object other$readTime = other.getReadTime();
        if (this$readTime == null ? other$readTime != null : !this$readTime.equals(other$readTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof UserMessageResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $senderId = this.getSenderId();
        result = result * PRIME + ($senderId == null ? 43 : $senderId.hashCode());
        final java.lang.Object $isRead = this.getIsRead();
        result = result * PRIME + ($isRead == null ? 43 : $isRead.hashCode());
        final java.lang.Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final java.lang.Object $content = this.getContent();
        result = result * PRIME + ($content == null ? 43 : $content.hashCode());
        final java.lang.Object $messageType = this.getMessageType();
        result = result * PRIME + ($messageType == null ? 43 : $messageType.hashCode());
        final java.lang.Object $priority = this.getPriority();
        result = result * PRIME + ($priority == null ? 43 : $priority.hashCode());
        final java.lang.Object $senderType = this.getSenderType();
        result = result * PRIME + ($senderType == null ? 43 : $senderType.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $readTime = this.getReadTime();
        result = result * PRIME + ($readTime == null ? 43 : $readTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "UserMessageResponse(id=" + this.getId() + ", title=" + this.getTitle() + ", content="
                + this.getContent() + ", messageType=" + this.getMessageType() + ", priority=" + this.getPriority()
                + ", senderType=" + this.getSenderType() + ", senderId=" + this.getSenderId() + ", createBy="
                + this.getCreateBy() + ", createTime=" + this.getCreateTime() + ", isRead=" + this.getIsRead()
                + ", readTime=" + this.getReadTime() + ")";
    }

    @SuppressWarnings("all")
    public static class UserMessageResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String title;

        @SuppressWarnings("all")
        private String content;

        @SuppressWarnings("all")
        private String messageType;

        @SuppressWarnings("all")
        private String priority;

        @SuppressWarnings("all")
        private String senderType;

        @SuppressWarnings("all")
        private Long senderId;

        @SuppressWarnings("all")
        private String createBy;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private Integer isRead;

        @SuppressWarnings("all")
        private LocalDateTime readTime;

        @SuppressWarnings("all")
        UserMessageResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserMessageResponse.UserMessageResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserMessageResponse.UserMessageResponseBuilder title(final String title) {
            this.title = title;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserMessageResponse.UserMessageResponseBuilder content(final String content) {
            this.content = content;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserMessageResponse.UserMessageResponseBuilder messageType(final String messageType) {
            this.messageType = messageType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserMessageResponse.UserMessageResponseBuilder priority(final String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserMessageResponse.UserMessageResponseBuilder senderType(final String senderType) {
            this.senderType = senderType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserMessageResponse.UserMessageResponseBuilder senderId(final Long senderId) {
            this.senderId = senderId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserMessageResponse.UserMessageResponseBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserMessageResponse.UserMessageResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserMessageResponse.UserMessageResponseBuilder isRead(final Integer isRead) {
            this.isRead = isRead;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public UserMessageResponse.UserMessageResponseBuilder readTime(final LocalDateTime readTime) {
            this.readTime = readTime;
            return this;
        }

        @SuppressWarnings("all")
        public UserMessageResponse build() {
            return new UserMessageResponse(this.id, this.title, this.content, this.messageType, this.priority,
                    this.senderType, this.senderId, this.createBy, this.createTime, this.isRead, this.readTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "UserMessageResponse.UserMessageResponseBuilder(id=" + this.id + ", title=" + this.title
                    + ", content=" + this.content + ", messageType=" + this.messageType + ", priority=" + this.priority
                    + ", senderType=" + this.senderType + ", senderId=" + this.senderId + ", createBy=" + this.createBy
                    + ", createTime=" + this.createTime + ", isRead=" + this.isRead + ", readTime=" + this.readTime
                    + ")";
        }

    }

}
