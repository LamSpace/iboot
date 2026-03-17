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
 * 站内消息实体
 *
 * @author iBoot
 */
public class Message {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型（字典 sys_message_type）
     */
    private String messageType;

    /**
     * 优先级（字典 sys_message_priority）：0-普通，1-重要，2-紧急
     */
    private String priority;

    /**
     * 发送者类型（字典 sys_message_sender_type）：0-系统，1-管理员
     */
    private String senderType;

    /**
     * 发送者用户ID（管理员发送时记录）
     */
    private Long senderId;

    /**
     * 接收者类型（字典 sys_message_receiver_type）：0-全部用户，1-指定用户
     */
    private String receiverType;

    /**
     * 状态（字典 sys_message_status）：0-草稿，1-已发送，2-已撤回
     */
    private String status;

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
    public Message() {
    }

    /**
     * Creates a new {@code Message} instance.
     *
     * @param id           消息ID
     * @param title        消息标题
     * @param content      消息内容
     * @param messageType  消息类型（字典 sys_message_type）
     * @param priority     优先级（字典 sys_message_priority）：0-普通，1-重要，2-紧急
     * @param senderType   发送者类型（字典 sys_message_sender_type）：0-系统，1-管理员
     * @param senderId     发送者用户ID（管理员发送时记录）
     * @param receiverType 接收者类型（字典 sys_message_receiver_type）：0-全部用户，1-指定用户
     * @param status       状态（字典 sys_message_status）：0-草稿，1-已发送，2-已撤回
     * @param createBy     创建人
     * @param createTime   创建时间
     * @param updateBy     更新人
     * @param updateTime   更新时间
     * @param deleted      逻辑删除：0-未删除，1-已删除
     * @param remark       备注
     */
    @SuppressWarnings("all")
    public Message(final Long id, final String title, final String content, final String messageType,
                   final String priority, final String senderType, final Long senderId, final String receiverType,
                   final String status, final String createBy, final LocalDateTime createTime, final String updateBy,
                   final LocalDateTime updateTime, final Integer deleted, final String remark) {
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
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.deleted = deleted;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static Message.MessageBuilder builder() {
        return new Message.MessageBuilder();
    }

    /**
     * 发送消息
     */
    public void send() {
        this.status = "1";
    }

    /**
     * 撤回消息
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
     * 是否为已发送状态
     */
    public boolean isSent() {
        return "1".equals(this.status);
    }

    /**
     * 是否为已撤回状态
     */
    public boolean isRevoked() {
        return "2".equals(this.status);
    }

    /**
     * 消息ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 消息ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 消息标题
     */
    @SuppressWarnings("all")
    public String getTitle() {
        return this.title;
    }

    /**
     * 消息标题
     */
    @SuppressWarnings("all")
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * 消息内容
     */
    @SuppressWarnings("all")
    public String getContent() {
        return this.content;
    }

    /**
     * 消息内容
     */
    @SuppressWarnings("all")
    public void setContent(final String content) {
        this.content = content;
    }

    /**
     * 消息类型（字典 sys_message_type）
     */
    @SuppressWarnings("all")
    public String getMessageType() {
        return this.messageType;
    }

    /**
     * 消息类型（字典 sys_message_type）
     */
    @SuppressWarnings("all")
    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    /**
     * 优先级（字典 sys_message_priority）：0-普通，1-重要，2-紧急
     */
    @SuppressWarnings("all")
    public String getPriority() {
        return this.priority;
    }

    /**
     * 优先级（字典 sys_message_priority）：0-普通，1-重要，2-紧急
     */
    @SuppressWarnings("all")
    public void setPriority(final String priority) {
        this.priority = priority;
    }

    /**
     * 发送者类型（字典 sys_message_sender_type）：0-系统，1-管理员
     */
    @SuppressWarnings("all")
    public String getSenderType() {
        return this.senderType;
    }

    /**
     * 发送者类型（字典 sys_message_sender_type）：0-系统，1-管理员
     */
    @SuppressWarnings("all")
    public void setSenderType(final String senderType) {
        this.senderType = senderType;
    }

    /**
     * 发送者用户ID（管理员发送时记录）
     */
    @SuppressWarnings("all")
    public Long getSenderId() {
        return this.senderId;
    }

    /**
     * 发送者用户ID（管理员发送时记录）
     */
    @SuppressWarnings("all")
    public void setSenderId(final Long senderId) {
        this.senderId = senderId;
    }

    /**
     * 接收者类型（字典 sys_message_receiver_type）：0-全部用户，1-指定用户
     */
    @SuppressWarnings("all")
    public String getReceiverType() {
        return this.receiverType;
    }

    /**
     * 接收者类型（字典 sys_message_receiver_type）：0-全部用户，1-指定用户
     */
    @SuppressWarnings("all")
    public void setReceiverType(final String receiverType) {
        this.receiverType = receiverType;
    }

    /**
     * 状态（字典 sys_message_status）：0-草稿，1-已发送，2-已撤回
     */
    @SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    /**
     * 状态（字典 sys_message_status）：0-草稿，1-已发送，2-已撤回
     */
    @SuppressWarnings("all")
    public void setStatus(final String status) {
        this.status = status;
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
        if (!(o instanceof Message))
            return false;
        final Message other = (Message) o;
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
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
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
        return other instanceof Message;
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
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
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
        return "Message(id=" + this.getId() + ", title=" + this.getTitle() + ", content=" + this.getContent()
                + ", messageType=" + this.getMessageType() + ", priority=" + this.getPriority() + ", senderType="
                + this.getSenderType() + ", senderId=" + this.getSenderId() + ", receiverType=" + this.getReceiverType()
                + ", status=" + this.getStatus() + ", createBy=" + this.getCreateBy() + ", createTime="
                + this.getCreateTime() + ", updateBy=" + this.getUpdateBy() + ", updateTime=" + this.getUpdateTime()
                + ", deleted=" + this.getDeleted() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class MessageBuilder {

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
        private String updateBy;

        @SuppressWarnings("all")
        private LocalDateTime updateTime;

        @SuppressWarnings("all")
        private Integer deleted;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        MessageBuilder() {
        }

        /**
         * 消息ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 消息标题
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder title(final String title) {
            this.title = title;
            return this;
        }

        /**
         * 消息内容
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder content(final String content) {
            this.content = content;
            return this;
        }

        /**
         * 消息类型（字典 sys_message_type）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder messageType(final String messageType) {
            this.messageType = messageType;
            return this;
        }

        /**
         * 优先级（字典 sys_message_priority）：0-普通，1-重要，2-紧急
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder priority(final String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 发送者类型（字典 sys_message_sender_type）：0-系统，1-管理员
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder senderType(final String senderType) {
            this.senderType = senderType;
            return this;
        }

        /**
         * 发送者用户ID（管理员发送时记录）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder senderId(final Long senderId) {
            this.senderId = senderId;
            return this;
        }

        /**
         * 接收者类型（字典 sys_message_receiver_type）：0-全部用户，1-指定用户
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder receiverType(final String receiverType) {
            this.receiverType = receiverType;
            return this;
        }

        /**
         * 状态（字典 sys_message_status）：0-草稿，1-已发送，2-已撤回
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * 创建人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * 创建时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * 更新人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * 更新时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        /**
         * 逻辑删除：0-未删除，1-已删除
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder deleted(final Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * 备注
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Message.MessageBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public Message build() {
            return new Message(this.id, this.title, this.content, this.messageType, this.priority, this.senderType,
                    this.senderId, this.receiverType, this.status, this.createBy, this.createTime, this.updateBy,
                    this.updateTime, this.deleted, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "Message.MessageBuilder(id=" + this.id + ", title=" + this.title + ", content=" + this.content
                    + ", messageType=" + this.messageType + ", priority=" + this.priority + ", senderType="
                    + this.senderType + ", senderId=" + this.senderId + ", receiverType=" + this.receiverType
                    + ", status=" + this.status + ", createBy=" + this.createBy + ", createTime=" + this.createTime
                    + ", updateBy=" + this.updateBy + ", updateTime=" + this.updateTime + ", deleted=" + this.deleted
                    + ", remark=" + this.remark + ")";
        }

    }

}
