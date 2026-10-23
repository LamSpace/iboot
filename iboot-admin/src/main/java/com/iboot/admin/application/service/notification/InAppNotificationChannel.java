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

import com.iboot.admin.application.service.MessageApplicationService;
import com.iboot.admin.domain.notification.model.NotificationChannelType;
import com.iboot.admin.domain.notification.model.NotificationDelivery;
import com.iboot.admin.domain.notification.model.NotificationSendResult;
import com.iboot.admin.domain.notification.service.NotificationChannel;
import com.iboot.admin.domain.notification.service.NotificationDeliveryException;
import com.iboot.admin.infrastructure.notification.NotificationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 站内通知渠道。
 *
 * <p>把通知转换为系统消息落库，复用现有 {@code MessageSentEvent} → SSE 链路推送。
 * 重试时若记录已有消息 ID，直接复用，避免消息中心出现重复消息。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see MessageApplicationService
 */
@Component
public class InAppNotificationChannel implements NotificationChannel {

    private final MessageApplicationService messageApplicationService;

    private final NotificationProperties properties;

    @SuppressWarnings("all")
    public InAppNotificationChannel(final MessageApplicationService messageApplicationService,
                                    final NotificationProperties properties) {
        this.messageApplicationService = messageApplicationService;
        this.properties = properties;
    }

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.IN_APP;
    }

    @Override
    public boolean isEnabled() {
        return properties.isInAppEnabled();
    }

    @Override
    public NotificationSendResult send(final NotificationDelivery delivery) {
        if (delivery.getMessageId() != null) {
            return new NotificationSendResult(delivery.getMessageId());
        }
        if (delivery.getRecipientId() == null) {
            throw new NotificationDeliveryException("站内通知接收人为空", false);
        }
        Long messageId = messageApplicationService.sendSystemMessage(delivery.getTitle(), delivery.getContent(),
                delivery.getEventType().getMessageType(), delivery.getEventType().getPriority(),
                delivery.getEventType().name(), List.of(delivery.getRecipientId()));
        return new NotificationSendResult(messageId);
    }
}
