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

package com.iboot.admin.domain.notification.model;

import java.util.Locale;

/**
 * 业务通知事件类型。
 *
 * <p>本期只包含审批待办；后续新增事件时在此扩展，并保持与模板编码的对应关系。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum NotificationEventType {

    /**
     * 审批待办；站内消息类型为“待办提醒”，优先级为“重要”。
     */
    APPROVAL_TODO("2", "1");

    /**
     * 对应的站内消息类型（字典 {@code sys_message_type}）。
     */
    private final String messageType;

    /**
     * 对应的站内消息优先级（字典 {@code sys_message_priority}）。
     */
    private final String priority;

    NotificationEventType(final String messageType, final String priority) {
        this.messageType = messageType;
        this.priority = priority;
    }

    /**
     * 站内消息类型。
     *
     * @return 字典值
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * 站内消息优先级。
     *
     * @return 字典值
     */
    public String getPriority() {
        return priority;
    }

    /**
     * 从字符串解析事件类型，忽略大小写。
     *
     * @param value 事件标识
     * @return 事件类型
     * @throws IllegalArgumentException 事件标识为空或未知时抛出
     */
    public static NotificationEventType from(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("通知事件类型不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("未知的通知事件类型: " + value, e);
        }
    }
}
