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
 * 消息接收记录实体
 *
 * @author iBoot
 */
public class MessageReceiver {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 接收者用户ID
     */
    private Long userId;

    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 用户侧删除：0-未删除，1-已删除
     */
    private Integer isDeleted;

    @SuppressWarnings("all")
    public MessageReceiver() {
    }

    /**
     * Creates a new {@code MessageReceiver} instance.
     *
     * @param id        记录ID
     * @param messageId 消息ID
     * @param userId    接收者用户ID
     * @param isRead    是否已读：0-未读，1-已读
     * @param readTime  阅读时间
     * @param isDeleted 用户侧删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public MessageReceiver(final Long id, final Long messageId, final Long userId, final Integer isRead,
                           final LocalDateTime readTime, final Integer isDeleted) {
        this.id = id;
        this.messageId = messageId;
        this.userId = userId;
        this.isRead = isRead;
        this.readTime = readTime;
        this.isDeleted = isDeleted;
    }

    @SuppressWarnings("all")
    public static MessageReceiver.MessageReceiverBuilder builder() {
        return new MessageReceiver.MessageReceiverBuilder();
    }

    /**
     * 标记已读
     */
    public void markAsRead() {
        this.isRead = 1;
        this.readTime = LocalDateTime.now();
    }

    /**
     * 用户侧软删除
     */
    public void softDelete() {
        this.isDeleted = 1;
    }

    /**
     * 记录ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 记录ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 消息ID
     */
    @SuppressWarnings("all")
    public Long getMessageId() {
        return this.messageId;
    }

    /**
     * 消息ID
     */
    @SuppressWarnings("all")
    public void setMessageId(final Long messageId) {
        this.messageId = messageId;
    }

    /**
     * 接收者用户ID
     */
    @SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    /**
     * 接收者用户ID
     */
    @SuppressWarnings("all")
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    /**
     * 是否已读：0-未读，1-已读
     */
    @SuppressWarnings("all")
    public Integer getIsRead() {
        return this.isRead;
    }

    /**
     * 是否已读：0-未读，1-已读
     */
    @SuppressWarnings("all")
    public void setIsRead(final Integer isRead) {
        this.isRead = isRead;
    }

    /**
     * 阅读时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getReadTime() {
        return this.readTime;
    }

    /**
     * 阅读时间
     */
    @SuppressWarnings("all")
    public void setReadTime(final LocalDateTime readTime) {
        this.readTime = readTime;
    }

    /**
     * 用户侧删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public Integer getIsDeleted() {
        return this.isDeleted;
    }

    /**
     * 用户侧删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public void setIsDeleted(final Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MessageReceiver))
            return false;
        final MessageReceiver other = (MessageReceiver) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$messageId = this.getMessageId();
        final java.lang.Object other$messageId = other.getMessageId();
        if (this$messageId == null ? other$messageId != null : !this$messageId.equals(other$messageId))
            return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId))
            return false;
        final java.lang.Object this$isRead = this.getIsRead();
        final java.lang.Object other$isRead = other.getIsRead();
        if (this$isRead == null ? other$isRead != null : !this$isRead.equals(other$isRead))
            return false;
        final java.lang.Object this$isDeleted = this.getIsDeleted();
        final java.lang.Object other$isDeleted = other.getIsDeleted();
        if (this$isDeleted == null ? other$isDeleted != null : !this$isDeleted.equals(other$isDeleted))
            return false;
        final java.lang.Object this$readTime = this.getReadTime();
        final java.lang.Object other$readTime = other.getReadTime();
        if (this$readTime == null ? other$readTime != null : !this$readTime.equals(other$readTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MessageReceiver;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $messageId = this.getMessageId();
        result = result * PRIME + ($messageId == null ? 43 : $messageId.hashCode());
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $isRead = this.getIsRead();
        result = result * PRIME + ($isRead == null ? 43 : $isRead.hashCode());
        final java.lang.Object $isDeleted = this.getIsDeleted();
        result = result * PRIME + ($isDeleted == null ? 43 : $isDeleted.hashCode());
        final java.lang.Object $readTime = this.getReadTime();
        result = result * PRIME + ($readTime == null ? 43 : $readTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "MessageReceiver(id=" + this.getId() + ", messageId=" + this.getMessageId() + ", userId="
                + this.getUserId() + ", isRead=" + this.getIsRead() + ", readTime=" + this.getReadTime()
                + ", isDeleted=" + this.getIsDeleted() + ")";
    }

    @SuppressWarnings("all")
    public static class MessageReceiverBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private Long messageId;

        @SuppressWarnings("all")
        private Long userId;

        @SuppressWarnings("all")
        private Integer isRead;

        @SuppressWarnings("all")
        private LocalDateTime readTime;

        @SuppressWarnings("all")
        private Integer isDeleted;

        @SuppressWarnings("all")
        MessageReceiverBuilder() {
        }

        /**
         * 记录ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageReceiver.MessageReceiverBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 消息ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageReceiver.MessageReceiverBuilder messageId(final Long messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * 接收者用户ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageReceiver.MessageReceiverBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 是否已读：0-未读，1-已读
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageReceiver.MessageReceiverBuilder isRead(final Integer isRead) {
            this.isRead = isRead;
            return this;
        }

        /**
         * 阅读时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageReceiver.MessageReceiverBuilder readTime(final LocalDateTime readTime) {
            this.readTime = readTime;
            return this;
        }

        /**
         * 用户侧删除：0-未删除，1-已删除
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageReceiver.MessageReceiverBuilder isDeleted(final Integer isDeleted) {
            this.isDeleted = isDeleted;
            return this;
        }

        @SuppressWarnings("all")
        public MessageReceiver build() {
            return new MessageReceiver(this.id, this.messageId, this.userId, this.isRead, this.readTime,
                    this.isDeleted);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "MessageReceiver.MessageReceiverBuilder(id=" + this.id + ", messageId=" + this.messageId
                    + ", userId=" + this.userId + ", isRead=" + this.isRead + ", readTime=" + this.readTime
                    + ", isDeleted=" + this.isDeleted + ")";
        }

    }

}
