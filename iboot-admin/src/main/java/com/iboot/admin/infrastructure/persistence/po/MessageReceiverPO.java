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

package com.iboot.admin.infrastructure.persistence.po;

import java.time.LocalDateTime;

/**
 * 消息接收记录持久化对象
 * <p>
 * 对应数据库表：sys_message_receiver
 * </p>
 *
 * @author iBoot
 */
public class MessageReceiverPO {

    /**
     * 记录 ID
     */
    private Long id;

    /**
     * 消息 ID
     */
    private Long messageId;

    /**
     * 用户 ID
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
     * 是否删除：0-未删除，1-已删除
     */
    private Integer isDeleted;

    @SuppressWarnings("all")
    public MessageReceiverPO() {
    }

    /**
     * 记录 ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 记录 ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 消息 ID
     */
    @SuppressWarnings("all")
    public Long getMessageId() {
        return this.messageId;
    }

    /**
     * 消息 ID
     */
    @SuppressWarnings("all")
    public void setMessageId(final Long messageId) {
        this.messageId = messageId;
    }

    /**
     * 用户 ID
     */
    @SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    /**
     * 用户 ID
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
     * 是否删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public Integer getIsDeleted() {
        return this.isDeleted;
    }

    /**
     * 是否删除：0-未删除，1-已删除
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
        if (!(o instanceof MessageReceiverPO))
            return false;
        final MessageReceiverPO other = (MessageReceiverPO) o;
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
        return other instanceof MessageReceiverPO;
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
        return "MessageReceiverPO(id=" + this.getId() + ", messageId=" + this.getMessageId() + ", userId="
                + this.getUserId() + ", isRead=" + this.getIsRead() + ", readTime=" + this.getReadTime()
                + ", isDeleted=" + this.getIsDeleted() + ")";
    }

}
