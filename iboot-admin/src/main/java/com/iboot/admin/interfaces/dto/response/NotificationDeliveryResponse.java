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

package com.iboot.admin.interfaces.dto.response;

import com.iboot.admin.domain.notification.model.NotificationChannelType;
import com.iboot.admin.domain.notification.model.NotificationDelivery;
import com.iboot.admin.infrastructure.notification.NotificationSanitizer;

import java.time.LocalDateTime;

/**
 * 通知投递记录响应。
 *
 * <p>管理端只展示排障所需字段；邮箱、手机号与错误摘要均已脱敏，不包含 Webhook、
 * Secret 或 AccessKey。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class NotificationDeliveryResponse {

    /**
     * 投递记录 ID。
     */
    private Long id;

    /**
     * 业务事件 ID。
     */
    private String eventId;

    /**
     * 业务事件类型。
     */
    private String eventType;

    /**
     * 渠道。
     */
    private String channel;

    /**
     * 脱敏后的接收人标识。
     */
    private String recipient;

    /**
     * 模板编码。
     */
    private String templateCode;

    /**
     * 消息标题。
     */
    private String title;

    /**
     * 投递状态。
     */
    private String status;

    /**
     * 已尝试次数。
     */
    private Integer attempts;

    /**
     * 最大尝试次数。
     */
    private Integer maxAttempts;

    /**
     * 下次重试时间。
     */
    private LocalDateTime nextRetryTime;

    /**
     * 脱敏后的最后错误。
     */
    private String lastError;

    /**
     * 发送成功时间。
     */
    private LocalDateTime sentTime;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 从领域模型创建响应。
     *
     * @param delivery 投递记录
     * @return 响应对象
     */
    public static NotificationDeliveryResponse from(final NotificationDelivery delivery) {
        NotificationDeliveryResponse response = new NotificationDeliveryResponse();
        response.id = delivery.getId();
        response.eventId = delivery.getEventId();
        response.eventType = delivery.getEventType() == null ? null : delivery.getEventType().name();
        response.channel = delivery.getChannel() == null ? null : delivery.getChannel().name();
        response.recipient = maskRecipient(delivery);
        response.templateCode = delivery.getTemplateCode();
        response.title = delivery.getTitle();
        response.status = delivery.getStatus() == null ? null : delivery.getStatus().name();
        response.attempts = delivery.getAttempts();
        response.maxAttempts = delivery.getMaxAttempts();
        response.nextRetryTime = delivery.getNextRetryTime();
        response.lastError = NotificationSanitizer.sanitizeError(delivery.getLastError());
        response.sentTime = delivery.getSentTime();
        response.createTime = delivery.getCreateTime();
        return response;
    }

    private static String maskRecipient(final NotificationDelivery delivery) {
        if (delivery.getChannel() == NotificationChannelType.EMAIL) {
            return NotificationSanitizer.maskEmail(delivery.getRecipientAddress());
        }
        if (delivery.getChannel() == NotificationChannelType.SMS) {
            return NotificationSanitizer.maskPhone(delivery.getRecipientAddress());
        }
        return delivery.getRecipientKey();
    }

    /**
     * 投递记录 ID。
     *
     * @return 记录 ID
     */
    public Long getId() {
        return id;
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
    public String getEventType() {
        return eventType;
    }

    /**
     * 渠道。
     *
     * @return 渠道
     */
    public String getChannel() {
        return channel;
    }

    /**
     * 脱敏后的接收人标识。
     *
     * @return 接收人
     */
    public String getRecipient() {
        return recipient;
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
     * 消息标题。
     *
     * @return 标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 投递状态。
     *
     * @return 状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 已尝试次数。
     *
     * @return 尝试次数
     */
    public Integer getAttempts() {
        return attempts;
    }

    /**
     * 最大尝试次数。
     *
     * @return 最大尝试次数
     */
    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * 下次重试时间。
     *
     * @return 重试时间
     */
    public LocalDateTime getNextRetryTime() {
        return nextRetryTime;
    }

    /**
     * 脱敏后的最后错误。
     *
     * @return 错误摘要
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * 发送成功时间。
     *
     * @return 发送时间
     */
    public LocalDateTime getSentTime() {
        return sentTime;
    }

    /**
     * 创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }
}
