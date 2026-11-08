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

import com.iboot.notify.infrastructure.config.RocketMqProperties;
import com.iboot.notify.infrastructure.messaging.RocketMqNotificationConsumerLifecycle.PushConsumerFactory;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link RocketMqNotificationConsumerLifecycle} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RocketMqNotificationConsumerLifecycleTest {

    private RocketMqProperties properties;

    @Test
    void retriesSubscriptionUntilBrokerIsAvailable() throws Exception {
        PushConsumer consumer = mock(PushConsumer.class);
        CountDownLatch subscribed = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        RocketMqNotificationConsumerLifecycle lifecycle = lifecycle(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("broker down");
            }
            subscribed.countDown();
            return consumer;
        });

        lifecycle.start();
        assertThat(subscribed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(lifecycle.isRunning()).isTrue();

        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
        verify(consumer).close();
    }

    @Test
    void doesNotStartWhenConsumerDisabled() throws Exception {
        PushConsumer consumer = mock(PushConsumer.class);
        AtomicInteger attempts = new AtomicInteger();
        RocketMqNotificationConsumerLifecycle lifecycle = lifecycle(() -> {
            attempts.incrementAndGet();
            return consumer;
        });
        properties.setConsumerEnabled(false);

        lifecycle.start();

        assertThat(lifecycle.isRunning()).isFalse();
        assertThat(attempts).hasValue(0);
        verify(consumer, never()).close();
    }

    private RocketMqNotificationConsumerLifecycle lifecycle(PushConsumerFactory factory) {
        properties = new RocketMqProperties();
        properties.setRetryInterval(Duration.ofMillis(10));
        return new RocketMqNotificationConsumerLifecycle(properties, listener(), factory);
    }

    private MessageListener listener() {
        return (MessageView message) -> ConsumeResult.SUCCESS;
    }
}
