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
 * 公告已读记录持久化对象
 * <p>
 * 对应数据库表：sys_notice_read
 * </p>
 *
 * @author iBoot
 */
public class NoticeReadPO {

    /**
     * 记录 ID
     */
    private Long id;

    /**
     * 公告 ID
     */
    private Long noticeId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    @SuppressWarnings("all")
    public NoticeReadPO() {
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
     * 公告 ID
     */
    @SuppressWarnings("all")
    public Long getNoticeId() {
        return this.noticeId;
    }

    /**
     * 公告 ID
     */
    @SuppressWarnings("all")
    public void setNoticeId(final Long noticeId) {
        this.noticeId = noticeId;
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

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NoticeReadPO))
            return false;
        final NoticeReadPO other = (NoticeReadPO) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$noticeId = this.getNoticeId();
        final java.lang.Object other$noticeId = other.getNoticeId();
        if (this$noticeId == null ? other$noticeId != null : !this$noticeId.equals(other$noticeId))
            return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId))
            return false;
        final java.lang.Object this$readTime = this.getReadTime();
        final java.lang.Object other$readTime = other.getReadTime();
        if (this$readTime == null ? other$readTime != null : !this$readTime.equals(other$readTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof NoticeReadPO;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $noticeId = this.getNoticeId();
        result = result * PRIME + ($noticeId == null ? 43 : $noticeId.hashCode());
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $readTime = this.getReadTime();
        result = result * PRIME + ($readTime == null ? 43 : $readTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "NoticeReadPO(id=" + this.getId() + ", noticeId=" + this.getNoticeId() + ", userId=" + this.getUserId()
                + ", readTime=" + this.getReadTime() + ")";
    }

}
