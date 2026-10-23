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

package com.iboot.admin.application.service.notification;

import com.iboot.admin.domain.notification.model.NotificationEventType;

import java.util.Map;

/**
 * 通知登记命令。
 *
 * <p>由业务应用服务构造，描述一次业务事件、模板、接收人与模板参数。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class NotificationCommand {

    /**
     * 业务事件 ID。
     */
    private final String eventId;

    /**
     * 业务事件类型。
     */
    private final NotificationEventType eventType;

    /**
     * 模板编码。
     */
    private final String templateCode;

    /**
     * 接收用户 ID。
     */
    private final Long recipientId;

    /**
     * 模板参数。
     */
    private final Map<String, String> params;

    /**
     * 创建通知登记命令。
     *
     * @param eventId      业务事件 ID
     * @param eventType    业务事件类型
     * @param templateCode 模板编码
     * @param recipientId  接收用户 ID
     * @param params       模板参数
     */
    public NotificationCommand(final String eventId, final NotificationEventType eventType,
                               final String templateCode, final Long recipientId,
                               final Map<String, String> params) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.templateCode = templateCode;
        this.recipientId = recipientId;
        this.params = params == null ? Map.of() : Map.copyOf(params);
    }

    /**
     * 业务事件 ID。
     *
     * @return 事件 ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * 业务事件类型。
     *
     * @return 事件类型
     */
    public NotificationEventType getEventType() {
        return eventType;
    }

    /**
     * 模板编码。
     *
     * @return 模板编码
     */
    public String getTemplateCode() {
        return templateCode;
    }

    /**
     * 接收用户 ID。
     *
     * @return 用户 ID
     */
    public Long getRecipientId() {
        return recipientId;
    }

    /**
     * 模板参数。
     *
     * @return 参数 Map
     */
    public Map<String, String> getParams() {
        return params;
    }
}
