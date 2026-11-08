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

package com.iboot.notify.infrastructure.messaging;

import java.util.Map;

/**
 * RocketMQ 中的通知事件消息体。
 *
 * <p>字段名与 workflow 的 outbox payload 及既有内部 HTTP 合同保持一致：
 * {@code eventId}、{@code eventType}、{@code templateCode}、{@code recipientId}、{@code params}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class NotificationEventMessage {

    /**
     * 业务事件 ID，也是消息键。
     */
    private String eventId;

    /**
     * 事件类型名称，与 {@code NotificationEventType} 枚举一致。
     */
    private String eventType;

    /**
     * 模板编码。
     */
    private String templateCode;

    /**
     * 接收用户 ID。
     */
    private Long recipientId;

    /**
     * 模板参数。
     */
    private Map<String, String> params;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(final String templateCode) {
        this.templateCode = templateCode;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(final Long recipientId) {
        this.recipientId = recipientId;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(final Map<String, String> params) {
        this.params = params;
    }
}
