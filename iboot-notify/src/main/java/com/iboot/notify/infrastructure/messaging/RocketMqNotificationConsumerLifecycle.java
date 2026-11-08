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
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * 通知服务 RocketMQ 推送消费者的生命周期。
 *
 * <p>在虚拟线程中后台订阅：broker 未就绪或 topic 不存在时按 {@code retry-interval}
 * 重试，不阻塞服务启动；订阅成功后由客户端线程拉取消息。停止时关闭消费者。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class RocketMqNotificationConsumerLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(RocketMqNotificationConsumerLifecycle.class);

    private final RocketMqProperties properties;

    private final MessageListener listener;

    private final ClientServiceProvider provider;

    private final PushConsumerFactory factory;

    private volatile boolean running;

    private volatile PushConsumer consumer;

    private Thread worker;

    @Autowired
    @SuppressWarnings("all")
    public RocketMqNotificationConsumerLifecycle(final RocketMqProperties properties,
                                                 final MessageListener listener) {
        this.properties = properties;
        this.listener = listener;
        this.provider = ClientServiceProvider.loadService();
        this.factory = this::createConsumer;
    }

    RocketMqNotificationConsumerLifecycle(final RocketMqProperties properties,
                                          final MessageListener listener,
                                          final PushConsumerFactory factory) {
        this.properties = properties;
        this.listener = listener;
        this.provider = ClientServiceProvider.loadService();
        this.factory = factory;
    }

    @Override
    public void start() {
        if (!properties.isConsumerEnabled() || running) {
            return;
        }
        running = true;
        worker = Thread.ofVirtual().name("rocketmq-notify-consumer").start(this::subscribe);
        log.info("通知消费订阅已启动，group: {}, topic: {}",
                properties.getConsumerGroup(), properties.getTopic());
    }

    @Override
    public void stop() {
        running = false;
        Thread current = worker;
        if (current != null) {
            current.interrupt();
        }
        PushConsumer currentConsumer = consumer;
        consumer = null;
        if (currentConsumer != null) {
            try {
                currentConsumer.close();
            } catch (Exception e) {
                log.warn("关闭 RocketMQ 消费者失败", e);
            }
        }
        log.info("通知消费订阅已停止");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void subscribe() {
        while (running) {
            try {
                PushConsumer created = factory.create();
                if (!running) {
                    closeQuietly(created);
                    return;
                }
                consumer = created;
                log.info("通知消费者订阅成功，group: {}", properties.getConsumerGroup());
                return;
            } catch (Exception e) {
                log.warn("通知消费者订阅失败，{}ms 后重试: {}",
                        properties.getRetryInterval().toMillis(), e.getMessage());
                sleep(properties.getRetryInterval());
            }
        }
    }

    private PushConsumer createConsumer() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.newBuilder()
                .setEndpoints(properties.getEndpoints())
                .enableSsl(properties.isSslEnabled())
                .enableVirtualThreads(true)
                .build();
        return provider.newPushConsumerBuilder()
                .setClientConfiguration(configuration)
                .setConsumerGroup(properties.getConsumerGroup())
                .setSubscriptionExpressions(Map.of(properties.getTopic(),
                        new FilterExpression("*", FilterExpressionType.TAG)))
                .setMessageListener(listener)
                .build();
    }

    private void sleep(final Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void closeQuietly(final PushConsumer current) {
        try {
            current.close();
        } catch (Exception e) {
            log.warn("关闭 RocketMQ 消费者失败", e);
        }
    }

    /**
     * 推送消费者工厂，便于测试替换。
     */
    @FunctionalInterface
    interface PushConsumerFactory {

        /**
         * 创建一个推送消费者。
         *
         * @return 推送消费者
         * @throws Exception 创建失败时抛出
         */
        PushConsumer create() throws Exception;
    }
}
