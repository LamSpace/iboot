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
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 消息发送请求 DTO
 *
 * @author iBoot
 */
@Schema(description = "消息发送请求")
public class MessageSendRequest {

    @Schema(description = "消息ID")
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    @Schema(description = "接收用户ID列表（接收类型为指定用户时必填）")
    private List<Long> userIds;

    @SuppressWarnings("all")
    public MessageSendRequest() {
    }

    @SuppressWarnings("all")
    public Long getMessageId() {
        return this.messageId;
    }

    @SuppressWarnings("all")
    public void setMessageId(final Long messageId) {
        this.messageId = messageId;
    }

    @SuppressWarnings("all")
    public List<Long> getUserIds() {
        return this.userIds;
    }

    @SuppressWarnings("all")
    public void setUserIds(final List<Long> userIds) {
        this.userIds = userIds;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MessageSendRequest))
            return false;
        final MessageSendRequest other = (MessageSendRequest) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$messageId = this.getMessageId();
        final java.lang.Object other$messageId = other.getMessageId();
        if (this$messageId == null ? other$messageId != null : !this$messageId.equals(other$messageId))
            return false;
        final java.lang.Object this$userIds = this.getUserIds();
        final java.lang.Object other$userIds = other.getUserIds();
        if (this$userIds == null ? other$userIds != null : !this$userIds.equals(other$userIds))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MessageSendRequest;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $messageId = this.getMessageId();
        result = result * PRIME + ($messageId == null ? 43 : $messageId.hashCode());
        final java.lang.Object $userIds = this.getUserIds();
        result = result * PRIME + ($userIds == null ? 43 : $userIds.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "MessageSendRequest(messageId=" + this.getMessageId() + ", userIds=" + this.getUserIds() + ")";
    }

}
