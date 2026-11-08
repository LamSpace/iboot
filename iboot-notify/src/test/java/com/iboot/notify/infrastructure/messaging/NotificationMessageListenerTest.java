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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.kernel.web.BusinessException;
import com.iboot.notify.application.service.notification.NotificationApplicationService;
import com.iboot.notify.application.service.notification.NotificationCommand;
import com.iboot.notify.domain.notification.model.NotificationEventType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageId;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotificationMessageListener} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class NotificationMessageListenerTest {

    private NotificationApplicationService service;

    private SimpleMeterRegistry registry;

    private NotificationMessageListener listener;

    @BeforeEach
    void setUp() {
        service = mock(NotificationApplicationService.class);
        registry = new SimpleMeterRegistry();
        listener = new NotificationMessageListener(service, new ObjectMapper(), registry);
    }

    @Test
    void consumesMessageAndRegistersCommand() {
        ConsumeResult result = listener.consume(message("""
                {"eventId":"approval-todo-pid-1","eventType":"APPROVAL_TODO",
                 "templateCode":"APPROVAL_TODO","recipientId":2,
                 "params":{"taskName":"ANNUAL","initiator":"alice"}}
                """));

        assertThat(result).isEqualTo(ConsumeResult.SUCCESS);
        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(service).enqueue(captor.capture());
        NotificationCommand command = captor.getValue();
        assertThat(command.getEventId()).isEqualTo("approval-todo-pid-1");
        assertThat(command.getEventType()).isEqualTo(NotificationEventType.APPROVAL_TODO);
        assertThat(command.getTemplateCode()).isEqualTo("APPROVAL_TODO");
        assertThat(command.getRecipientId()).isEqualTo(2L);
        assertThat(command.getParams()).containsEntry("taskName", "ANNUAL");
        assertThat(counter("notification_consume_success_total")).isEqualTo(1.0);
    }

    @Test
    void returnsFailureWhenRegistrationFails() {
        when(service.enqueue(any())).thenThrow(new BusinessException("消息模板已停用"));

        ConsumeResult result = listener.consume(message("""
                {"eventId":"approval-todo-pid-2","eventType":"APPROVAL_TODO",
                 "templateCode":"APPROVAL_TODO","recipientId":2,"params":{}}
                """));

        assertThat(result).isEqualTo(ConsumeResult.FAILURE);
        assertThat(counter("notification_consume_failure_total")).isEqualTo(1.0);
    }

    @Test
    void returnsFailureForInvalidPayloadWithoutRegistering() {
        ConsumeResult result = listener.consume(message("not-json"));

        assertThat(result).isEqualTo(ConsumeResult.FAILURE);
        verify(service, never()).enqueue(any());
        assertThat(counter("notification_consume_failure_total")).isEqualTo(1.0);
    }

    @Test
    void duplicateDeliveryKeepsStableEventId() {
        String payload = """
                {"eventId":"approval-todo-pid-3","eventType":"APPROVAL_TODO",
                 "templateCode":"APPROVAL_TODO","recipientId":2,"params":{}}
                """;

        assertThat(listener.consume(message(payload))).isEqualTo(ConsumeResult.SUCCESS);
        assertThat(listener.consume(message(payload))).isEqualTo(ConsumeResult.SUCCESS);

        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(service, times(2)).enqueue(captor.capture());
        assertThat(captor.getAllValues()).extracting(NotificationCommand::getEventId)
                .containsOnly("approval-todo-pid-3");
    }

    private double counter(String name) {
        return registry.get(name).tag("application", "iboot-notify").counter().count();
    }

    private MessageView message(String body) {
        MessageView view = mock(MessageView.class);
        MessageId messageId = mock(MessageId.class);
        when(messageId.toString()).thenReturn("msg-1");
        when(view.getMessageId()).thenReturn(messageId);
        when(view.getBody()).thenReturn(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8)));
        return view;
    }
}
