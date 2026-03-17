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

package com.iboot.admin.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 消息请求 DTO
 *
 * @author iBoot
 */
@Schema(description = "消息请求")
public class MessageRequest {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "消息标题")
    @NotBlank(message = "消息标题不能为空")
    @Size(max = 200, message = "消息标题长度不能超过200")
    private String title;

    @Schema(description = "消息内容")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Schema(description = "消息类型（字典 sys_message_type）")
    @NotBlank(message = "消息类型不能为空")
    private String messageType;

    @Schema(description = "优先级（字典 sys_message_priority）")
    private String priority;

    @Schema(description = "接收者类型（字典 sys_message_receiver_type）")
    @NotBlank(message = "接收者类型不能为空")
    private String receiverType;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    @SuppressWarnings("all")
    public MessageRequest() {
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
    public String getReceiverType() {
        return this.receiverType;
    }

    @SuppressWarnings("all")
    public void setReceiverType(final String receiverType) {
        this.receiverType = receiverType;
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
        if (!(o instanceof MessageRequest))
            return false;
        final MessageRequest other = (MessageRequest) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
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
        final java.lang.Object this$receiverType = this.getReceiverType();
        final java.lang.Object other$receiverType = other.getReceiverType();
        if (this$receiverType == null ? other$receiverType != null : !this$receiverType.equals(other$receiverType))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MessageRequest;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final java.lang.Object $content = this.getContent();
        result = result * PRIME + ($content == null ? 43 : $content.hashCode());
        final java.lang.Object $messageType = this.getMessageType();
        result = result * PRIME + ($messageType == null ? 43 : $messageType.hashCode());
        final java.lang.Object $priority = this.getPriority();
        result = result * PRIME + ($priority == null ? 43 : $priority.hashCode());
        final java.lang.Object $receiverType = this.getReceiverType();
        result = result * PRIME + ($receiverType == null ? 43 : $receiverType.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "MessageRequest(id=" + this.getId() + ", title=" + this.getTitle() + ", content=" + this.getContent()
                + ", messageType=" + this.getMessageType() + ", priority=" + this.getPriority() + ", receiverType="
                + this.getReceiverType() + ", remark=" + this.getRemark() + ")";
    }

}
