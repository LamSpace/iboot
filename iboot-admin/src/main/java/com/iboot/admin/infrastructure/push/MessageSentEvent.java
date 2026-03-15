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

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 消息发送事件
 * <p>
 * 当消息发送成功时发布此事件
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Getter
public class MessageSentEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 消息 ID
     */
    private final Long messageId;

    /**
     * 消息标题
     */
    private final String title;

    /**
     * 消息内容
     */
    private final String content;

    /**
     * 消息类型
     */
    private final String messageType;

    /**
     * 优先级
     */
    private final String priority;

    /**
     * 发送者 ID
     */
    private final Long senderId;

    /**
     * 事件 ID
     */
    private final String eventId;

    /**
     * 创建消息发送事件
     *
     * @param source     事件源（通常是消息对象或服务）
     * @param messageId  消息 ID
     * @param title      消息标题
     * @param content    消息内容
     * @param messageType 消息类型
     * @param priority   优先级
     * @param senderId   发送者 ID
     */
    public MessageSentEvent(Object source, Long messageId, String title, String content,
                            String messageType, String priority, Long senderId) {
        super(source);
        this.messageId = messageId;
        this.title = title;
        this.content = content;
        this.messageType = messageType;
        this.priority = priority;
        this.senderId = senderId;
        this.eventId = "evt-msg-sent-" + java.util.UUID.randomUUID();
    }
}
