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

package com.iboot.notify.interfaces.dto.request;

import java.util.Map;

/**
 * 内部通知登记请求。
 *
 * <p>由其他服务（当前为管理端的审批流程）在业务事务提交后调用，字段与
 * {@code NotificationCommand} 一一对应；事件 ID 每次提交保持稳定，供投递记录去重。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class InternalNotificationRequest {

    /**
     * 业务事件 ID，同一次业务提交内保持稳定。
     */
    private String eventId;

    /**
     * 业务事件类型，取值为 {@code NotificationEventType} 的枚举名。
     */
    private String eventType;

    /**
     * 模板编码，对应 {@code sys_message_template.template_code}。
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

    /**
     * 业务事件 ID。
     *
     * @return 事件 ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * 设置业务事件 ID。
     *
     * @param eventId 事件 ID
     */
    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    /**
     * 业务事件类型。
     *
     * @return 事件类型枚举名
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 设置业务事件类型。
     *
     * @param eventType 事件类型枚举名
     */
    public void setEventType(final String eventType) {
        this.eventType = eventType;
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
     * 设置模板编码。
     *
     * @param templateCode 模板编码
     */
    public void setTemplateCode(final String templateCode) {
        this.templateCode = templateCode;
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
     * 设置接收用户 ID。
     *
     * @param recipientId 用户 ID
     */
    public void setRecipientId(final Long recipientId) {
        this.recipientId = recipientId;
    }

    /**
     * 模板参数。
     *
     * @return 参数 Map
     */
    public Map<String, String> getParams() {
        return params;
    }

    /**
     * 设置模板参数。
     *
     * @param params 参数 Map
     */
    public void setParams(final Map<String, String> params) {
        this.params = params;
    }
}
