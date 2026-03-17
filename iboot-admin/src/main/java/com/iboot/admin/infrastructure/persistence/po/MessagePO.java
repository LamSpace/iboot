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
 * 消息持久化对象
 * <p>
 * 对应数据库表：sys_message
 * </p>
 *
 * @author iBoot
 */
public class MessagePO {

    /**
     * 消息 ID
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
     * 消息类型：1-系统消息，2-用户消息
     */
    private String messageType;

    /**
     * 优先级：LOW-低，MEDIUM-中，HIGH-高
     */
    private String priority;

    /**
     * 发送者类型：1-用户，2-系统
     */
    private String senderType;

    /**
     * 发送者 ID
     */
    private Long senderId;

    /**
     * 接收者类型：1-指定用户，2-指定角色，3-全体成员
     */
    private String receiverType;

    /**
     * 消息状态：0-草稿，1-已发送，2-已撤回
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
    public MessagePO() {
    }

    /**
     * 消息 ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 消息 ID
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
     * 消息类型：1-系统消息，2-用户消息
     */
    @SuppressWarnings("all")
    public String getMessageType() {
        return this.messageType;
    }

    /**
     * 消息类型：1-系统消息，2-用户消息
     */
    @SuppressWarnings("all")
    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    /**
     * 优先级：LOW-低，MEDIUM-中，HIGH-高
     */
    @SuppressWarnings("all")
    public String getPriority() {
        return this.priority;
    }

    /**
     * 优先级：LOW-低，MEDIUM-中，HIGH-高
     */
    @SuppressWarnings("all")
    public void setPriority(final String priority) {
        this.priority = priority;
    }

    /**
     * 发送者类型：1-用户，2-系统
     */
    @SuppressWarnings("all")
    public String getSenderType() {
        return this.senderType;
    }

    /**
     * 发送者类型：1-用户，2-系统
     */
    @SuppressWarnings("all")
    public void setSenderType(final String senderType) {
        this.senderType = senderType;
    }

    /**
     * 发送者 ID
     */
    @SuppressWarnings("all")
    public Long getSenderId() {
        return this.senderId;
    }

    /**
     * 发送者 ID
     */
    @SuppressWarnings("all")
    public void setSenderId(final Long senderId) {
        this.senderId = senderId;
    }

    /**
     * 接收者类型：1-指定用户，2-指定角色，3-全体成员
     */
    @SuppressWarnings("all")
    public String getReceiverType() {
        return this.receiverType;
    }

    /**
     * 接收者类型：1-指定用户，2-指定角色，3-全体成员
     */
    @SuppressWarnings("all")
    public void setReceiverType(final String receiverType) {
        this.receiverType = receiverType;
    }

    /**
     * 消息状态：0-草稿，1-已发送，2-已撤回
     */
    @SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    /**
     * 消息状态：0-草稿，1-已发送，2-已撤回
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
        if (!(o instanceof MessagePO))
            return false;
        final MessagePO other = (MessagePO) o;
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
        return other instanceof MessagePO;
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
        return "MessagePO(id=" + this.getId() + ", title=" + this.getTitle() + ", content=" + this.getContent()
                + ", messageType=" + this.getMessageType() + ", priority=" + this.getPriority() + ", senderType="
                + this.getSenderType() + ", senderId=" + this.getSenderId() + ", receiverType=" + this.getReceiverType()
                + ", status=" + this.getStatus() + ", createBy=" + this.getCreateBy() + ", createTime="
                + this.getCreateTime() + ", updateBy=" + this.getUpdateBy() + ", updateTime=" + this.getUpdateTime()
                + ", deleted=" + this.getDeleted() + ", remark=" + this.getRemark() + ")";
    }

}
