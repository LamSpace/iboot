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
 * 消息响应 DTO（管理端）
 *
 * @author iBoot
 */
@Schema(description = "消息响应")
public class MessageResponse {

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

    @Schema(description = "接收者类型")
    private String receiverType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String remark;

    @SuppressWarnings("all")
    public MessageResponse() {
    }

    @SuppressWarnings("all")
    public MessageResponse(final Long id, final String title, final String content, final String messageType,
                           final String priority, final String senderType, final Long senderId, final String receiverType,
                           final String status, final String createBy, final LocalDateTime createTime, final String remark) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.messageType = messageType;
        this.priority = priority;
        this.senderType = senderType;
        this.senderId = senderId;
        this.receiverType = receiverType;
        this.status = status;
        this.createBy = createBy;
        this.createTime = createTime;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static MessageResponse.MessageResponseBuilder builder() {
        return new MessageResponse.MessageResponseBuilder();
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
    public String getReceiverType() {
        return this.receiverType;
    }

    @SuppressWarnings("all")
    public void setReceiverType(final String receiverType) {
        this.receiverType = receiverType;
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
        if (!(o instanceof MessageResponse))
            return false;
        final MessageResponse other = (MessageResponse) o;
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
        final java.lang.Object this$receiverType = this.getReceiverType();
        final java.lang.Object other$receiverType = other.getReceiverType();
        if (this$receiverType == null ? other$receiverType != null : !this$receiverType.equals(other$receiverType))
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
        return other instanceof MessageResponse;
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
        final java.lang.Object $receiverType = this.getReceiverType();
        result = result * PRIME + ($receiverType == null ? 43 : $receiverType.hashCode());
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
        return "MessageResponse(id=" + this.getId() + ", title=" + this.getTitle() + ", content=" + this.getContent()
                + ", messageType=" + this.getMessageType() + ", priority=" + this.getPriority() + ", senderType="
                + this.getSenderType() + ", senderId=" + this.getSenderId() + ", receiverType=" + this.getReceiverType()
                + ", status=" + this.getStatus() + ", createBy=" + this.getCreateBy() + ", createTime="
                + this.getCreateTime() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class MessageResponseBuilder {

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
        private String receiverType;

        @SuppressWarnings("all")
        private String status;

        @SuppressWarnings("all")
        private String createBy;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        MessageResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder title(final String title) {
            this.title = title;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder content(final String content) {
            this.content = content;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder messageType(final String messageType) {
            this.messageType = messageType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder priority(final String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder senderType(final String senderType) {
            this.senderType = senderType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder senderId(final Long senderId) {
            this.senderId = senderId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder receiverType(final String receiverType) {
            this.receiverType = receiverType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageResponse.MessageResponseBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public MessageResponse build() {
            return new MessageResponse(this.id, this.title, this.content, this.messageType, this.priority,
                    this.senderType, this.senderId, this.receiverType, this.status, this.createBy, this.createTime,
                    this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "MessageResponse.MessageResponseBuilder(id=" + this.id + ", title=" + this.title + ", content="
                    + this.content + ", messageType=" + this.messageType + ", priority=" + this.priority
                    + ", senderType=" + this.senderType + ", senderId=" + this.senderId + ", receiverType="
                    + this.receiverType + ", status=" + this.status + ", createBy=" + this.createBy + ", createTime="
                    + this.createTime + ", remark=" + this.remark + ")";
        }

    }

}
