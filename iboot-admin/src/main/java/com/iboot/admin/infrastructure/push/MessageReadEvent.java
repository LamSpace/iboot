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

package com.iboot.admin.infrastructure.push;

import org.springframework.context.ApplicationEvent;

/**
 * 消息已读事件
 * <p>
 * 当消息被标记为已读时发布此事件
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class MessageReadEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 消息 ID
     */
    private final Long messageId;

    /**
     * 用户 ID
     */
    private final Long userId;

    /**
     * 发送者 ID（可选）
     */
    private final Long senderId;

    /**
     * 创建消息已读事件
     *
     * @param source    事件源
     * @param messageId 消息 ID
     * @param userId    用户 ID
     * @param senderId  发送者 ID
     */
    public MessageReadEvent(Object source, Long messageId, Long userId, Long senderId) {
        super(source);
        this.messageId = messageId;
        this.userId = userId;
        this.senderId = senderId;
    }

    /**
     * 消息 ID
     */
    @SuppressWarnings("all")
    public Long getMessageId() {
        return this.messageId;
    }

    /**
     * 用户 ID
     */
    @SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    /**
     * 发送者 ID（可选）
     */
    @SuppressWarnings("all")
    public Long getSenderId() {
        return this.senderId;
    }

}
