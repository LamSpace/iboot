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

import com.iboot.workflow.domain.notification.model.NotificationOutboxEvent;
import com.iboot.workflow.infrastructure.config.RocketMqProperties;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageId;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RocketMqNotificationPublisher} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RocketMqNotificationPublisherTest {

    @Test
    void publishesEventWithTopicTagKeyAndBody() throws Exception {
        Producer producer = producerReturning("msg-1");
        RocketMqNotificationPublisher publisher = new RocketMqNotificationPublisher(
                new RocketMqProperties(), () -> producer);

        String messageId = publisher.publish(event("evt-1", "{\"eventId\":\"evt-1\"}"));

        assertThat(messageId).isEqualTo("msg-1");
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(producer).send(captor.capture());
        Message message = captor.getValue();
        assertThat(message.getTopic()).isEqualTo("iboot-notification");
        assertThat(message.getTag()).contains("APPROVAL_TODO");
        assertThat(message.getKeys()).containsExactly("evt-1");
        assertThat(StandardCharsets.UTF_8.decode(message.getBody()).toString())
                .isEqualTo("{\"eventId\":\"evt-1\"}");
    }

    @Test
    void recreatesProducerAfterSendFailure() throws Exception {
        Producer failing = mock(Producer.class);
        try {
            when(failing.send(any())).thenThrow(new RuntimeException("broker down"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Producer recovered = producerReturning("msg-2");
        AtomicInteger created = new AtomicInteger();
        RocketMqNotificationPublisher publisher = new RocketMqNotificationPublisher(
                new RocketMqProperties(), () -> created.getAndIncrement() == 0 ? failing : recovered);

        assertThatThrownBy(() -> publisher.publish(event("evt-1", "{}")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(created).hasValue(1);

        assertThat(publisher.publish(event("evt-1", "{}"))).isEqualTo("msg-2");
        assertThat(created).hasValue(2);
        verify(failing).close();
    }

    private Producer producerReturning(String messageId) {
        Producer producer = mock(Producer.class);
        SendReceipt receipt = mock(SendReceipt.class);
        MessageId id = mock(MessageId.class);
        when(id.toString()).thenReturn(messageId);
        when(receipt.getMessageId()).thenReturn(id);
        try {
            when(producer.send(any())).thenReturn(receipt);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return producer;
    }

    private NotificationOutboxEvent event(String eventId, String payload) {
        NotificationOutboxEvent event = new NotificationOutboxEvent();
        event.setEventId(eventId);
        event.setEventType("APPROVAL_TODO");
        event.setPayload(payload);
        return event;
    }
}
