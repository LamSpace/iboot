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

package com.iboot.workflow.infrastructure.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.workflow.application.service.notification.NotificationCommand;
import com.iboot.workflow.domain.notification.model.NotificationEventType;
import com.iboot.workflow.domain.notification.model.NotificationOutboxEvent;
import com.iboot.workflow.domain.notification.model.NotificationOutboxStatus;
import com.iboot.workflow.domain.notification.repository.NotificationOutboxRepository;
import com.iboot.workflow.infrastructure.config.OutboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OutboxNotificationPort} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class OutboxNotificationPortTest {

    private NotificationOutboxRepository repository;

    private OutboxNotificationPort port;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationOutboxRepository.class);
        when(repository.save(any(NotificationOutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        port = new OutboxNotificationPort(repository, new ObjectMapper(), new OutboxProperties());
    }

    @Test
    void persistsPendingEventWithStableIdAndPayload() {
        port.enqueue(command("approval-todo-pid-1"));

        ArgumentCaptor<NotificationOutboxEvent> captor = ArgumentCaptor.forClass(NotificationOutboxEvent.class);
        verify(repository).save(captor.capture());
        NotificationOutboxEvent event = captor.getValue();
        assertThat(event.getEventId()).isEqualTo("approval-todo-pid-1");
        assertThat(event.getEventType()).isEqualTo("APPROVAL_TODO");
        assertThat(event.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getMaxAttempts()).isEqualTo(10);
        assertThat(event.getCreateTime()).isNotNull();
        assertThat(event.getPayload())
                .contains("\"eventId\":\"approval-todo-pid-1\"")
                .contains("\"eventType\":\"APPROVAL_TODO\"")
                .contains("\"templateCode\":\"APPROVAL_TODO\"")
                .contains("\"recipientId\":2");
    }

    @Test
    void propagatesPersistenceFailureToRollBackCallerTransaction() {
        when(repository.save(any(NotificationOutboxEvent.class)))
                .thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> port.enqueue(command("approval-todo-pid-2")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");
    }

    private NotificationCommand command(String eventId) {
        return new NotificationCommand(eventId, NotificationEventType.APPROVAL_TODO,
                "APPROVAL_TODO", 2L, Map.of("taskName", "ANNUAL"));
    }
}
