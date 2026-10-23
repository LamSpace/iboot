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
import com.iboot.admin.domain.notification.model.NotificationEventType;
import com.iboot.admin.domain.notification.model.NotificationSendResult;
import com.iboot.admin.domain.notification.service.NotificationDeliveryException;
import com.iboot.admin.infrastructure.notification.NotificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InAppNotificationChannel} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class InAppNotificationChannelTest {

    private MessageApplicationService messageApplicationService;

    private NotificationProperties properties;

    private InAppNotificationChannel channel;

    @BeforeEach
    void setUp() {
        messageApplicationService = mock(MessageApplicationService.class);
        properties = new NotificationProperties();
        channel = new InAppNotificationChannel(messageApplicationService, properties);
    }

    /**
     * 验证首次发送创建系统消息并返回消息 ID。
     */
    @Test
    void sendCreatesSystemMessage() {
        when(messageApplicationService.sendSystemMessage("审批待办通知", "您有一条审批待办", "2", "1",
                "APPROVAL_TODO", List.of(2L))).thenReturn(55L);

        NotificationSendResult result = channel.send(delivery());

        assertThat(result.messageId()).isEqualTo(55L);
        verify(messageApplicationService).sendSystemMessage("审批待办通知", "您有一条审批待办", "2", "1",
                "APPROVAL_TODO", List.of(2L));
    }

    /**
     * 验证已有消息 ID 时复用，不重复创建。
     */
    @Test
    void sendReusesExistingMessageId() {
        NotificationDelivery delivery = delivery();
        delivery.setMessageId(77L);

        NotificationSendResult result = channel.send(delivery);

        assertThat(result.messageId()).isEqualTo(77L);
        verify(messageApplicationService, never()).sendSystemMessage(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /**
     * 验证接收人为空时不可重试失败。
     */
    @Test
    void sendRejectsMissingRecipient() {
        NotificationDelivery delivery = delivery();
        delivery.setRecipientId(null);

        assertThatThrownBy(() -> channel.send(delivery))
                .isInstanceOf(NotificationDeliveryException.class)
                .satisfies(error -> assertThat(((NotificationDeliveryException) error).isRetryable()).isFalse());
    }

    /**
     * 验证渠道开关绑定配置。
     */
    @Test
    void enabledFollowsProperties() {
        assertThat(channel.channelType()).isEqualTo(NotificationChannelType.IN_APP);
        assertThat(channel.isEnabled()).isTrue();

        properties.setInAppEnabled(false);

        assertThat(channel.isEnabled()).isFalse();
    }

    private NotificationDelivery delivery() {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setEventType(NotificationEventType.APPROVAL_TODO);
        delivery.setChannel(NotificationChannelType.IN_APP);
        delivery.setRecipientId(2L);
        delivery.setTitle("审批待办通知");
        delivery.setContent("您有一条审批待办");
        return delivery;
    }
}
