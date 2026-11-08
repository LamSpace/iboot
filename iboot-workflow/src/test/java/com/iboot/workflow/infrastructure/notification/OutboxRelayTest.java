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

import com.iboot.workflow.application.service.notification.NotificationMessagePublisher;
import com.iboot.workflow.domain.notification.model.NotificationOutboxEvent;
import com.iboot.workflow.domain.notification.model.NotificationOutboxStatus;
import com.iboot.workflow.domain.notification.repository.NotificationOutboxRepository;
import com.iboot.workflow.infrastructure.config.OutboxProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OutboxRelay} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class OutboxRelayTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T10:00:00Z"), ZoneOffset.UTC);

    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private NotificationOutboxRepository repository;

    private NotificationMessagePublisher publisher;

    private OutboxProperties properties;

    private SimpleMeterRegistry registry;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationOutboxRepository.class);
        publisher = mock(NotificationMessagePublisher.class);
        properties = new OutboxProperties();
        registry = new SimpleMeterRegistry();
        when(repository.claimBatch(anyInt(), any())).thenReturn(List.of());
        relay = new OutboxRelay(repository, publisher, properties, registry, CLOCK);
    }

    @Test
    void publishesClaimedEventAndMarksSent() {
        NotificationOutboxEvent event = event(1L, 0);
        when(repository.claimBatch(properties.getBatchSize(), NOW)).thenReturn(List.of(event));
        when(publisher.publish(event)).thenReturn("msg-1");

        relay.relayOnce();

        verify(repository).markSent(1L, "msg-1", NOW);
        assertThat(counter("notification_outbox_sent_total")).isEqualTo(1.0);
    }

    @Test
    void schedulesRetryWithExponentialBackoff() {
        NotificationOutboxEvent event = event(2L, 0);
        when(repository.claimBatch(properties.getBatchSize(), NOW)).thenReturn(List.of(event));
        when(publisher.publish(event)).thenThrow(new RuntimeException("broker down"));

        relay.relayOnce();

        verify(repository).markRetry(2L, 1, NOW.plusSeconds(5), "broker down", NOW);
        assertThat(counter("notification_outbox_retry_total")).isEqualTo(1.0);
    }

    @Test
    void marksDeadAfterMaxAttempts() {
        NotificationOutboxEvent event = event(3L, 9);
        when(repository.claimBatch(properties.getBatchSize(), NOW)).thenReturn(List.of(event));
        when(publisher.publish(event)).thenThrow(new RuntimeException("broker down"));

        relay.relayOnce();

        verify(repository).markDead(3L, 10, "broker down", NOW);
        assertThat(counter("notification_outbox_dead_total")).isEqualTo(1.0);
    }

    @Test
    void resetsStaleSendingAndRefreshesGauges() {
        when(repository.countByStatus(NotificationOutboxStatus.PENDING)).thenReturn(3L);
        when(repository.countByStatus(NotificationOutboxStatus.DEAD)).thenReturn(1L);

        relay.relayOnce();

        verify(repository).resetStaleSending(NOW.minusMinutes(5), NOW);
        assertThat(gauge("notification_outbox_pending")).isEqualTo(3.0);
        assertThat(gauge("notification_outbox_dead")).isEqualTo(1.0);
    }

    @Test
    void cleansUpExpiredSentRecords() {
        relay.cleanup(NOW);

        verify(repository).deleteSentBefore(NOW.minusDays(7));
    }

    @Test
    void doesNotStartWhenDisabled() {
        properties.setEnabled(false);

        relay.start();

        assertThat(relay.isRunning()).isFalse();
    }

    @Test
    void startsAndStopsVirtualThreadLoop() {
        relay.start();
        assertThat(relay.isRunning()).isTrue();

        relay.stop();
        assertThat(relay.isRunning()).isFalse();
    }

    private double counter(String name) {
        return registry.get(name).tag("application", "iboot-workflow").counter().count();
    }

    private double gauge(String name) {
        return registry.get(name).tag("application", "iboot-workflow").gauge().value();
    }

    private NotificationOutboxEvent event(Long id, int attempts) {
        NotificationOutboxEvent event = new NotificationOutboxEvent();
        event.setId(id);
        event.setEventId("evt-" + id);
        event.setEventType("APPROVAL_TODO");
        event.setPayload("{}");
        event.setStatus(NotificationOutboxStatus.SENDING);
        event.setAttempts(attempts);
        event.setMaxAttempts(10);
        return event;
    }
}
