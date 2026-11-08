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
import com.iboot.workflow.infrastructure.config.RocketMqProperties;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 基于 RocketMQ gRPC 客户端的通知事件发布器。
 *
 * <p>生产者懒创建：首次发布时才连接 broker，创建失败不阻断应用启动；每次发送失败后
 * 关闭并清空生产者，下次发布重新创建。消息主题取自配置，标签为事件类型，消息键为
 * 事件 ID，便于按 {@code eventId} 检索。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class RocketMqNotificationPublisher implements NotificationMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RocketMqNotificationPublisher.class);

    private final RocketMqProperties properties;

    private final ClientServiceProvider provider;

    private final ProducerFactory producerFactory;

    private volatile Producer producer;

    @Autowired
    public RocketMqNotificationPublisher(final RocketMqProperties properties) {
        this.properties = properties;
        this.provider = ClientServiceProvider.loadService();
        this.producerFactory = this::createProducer;
    }

    RocketMqNotificationPublisher(final RocketMqProperties properties, final ProducerFactory producerFactory) {
        this(properties, ClientServiceProvider.loadService(), producerFactory);
    }

    RocketMqNotificationPublisher(final RocketMqProperties properties,
                                  final ClientServiceProvider provider,
                                  final ProducerFactory producerFactory) {
        this.properties = properties;
        this.provider = provider;
        this.producerFactory = producerFactory;
    }

    @Override
    public String publish(final NotificationOutboxEvent event) {
        Producer current = producer();
        Message message = provider.newMessageBuilder()
                .setTopic(properties.getTopic())
                .setTag(event.getEventType())
                .setKeys(event.getEventId())
                .setBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .build();
        try {
            SendReceipt receipt = current.send(message);
            return receipt.getMessageId().toString();
        } catch (Exception e) {
            closeQuietly(current);
            throw new IllegalStateException("通知事件投递失败，eventId: " + event.getEventId(), e);
        }
    }

    private Producer producer() {
        Producer current = producer;
        if (current == null) {
            synchronized (this) {
                current = producer;
                if (current == null) {
                    try {
                        current = producerFactory.create();
                        producer = current;
                    } catch (Exception e) {
                        throw new IllegalStateException("RocketMQ 生产者初始化失败", e);
                    }
                }
            }
        }
        return current;
    }

    private void closeQuietly(final Producer current) {
        synchronized (this) {
            if (producer == current) {
                producer = null;
            }
        }
        try {
            current.close();
        } catch (Exception e) {
            log.warn("关闭 RocketMQ 生产者失败", e);
        }
    }

    private Producer createProducer() throws Exception {
        ClientConfiguration configuration = ClientConfiguration.newBuilder()
                .setEndpoints(properties.getEndpoints())
                .enableSsl(properties.isSslEnabled())
                .enableVirtualThreads(true)
                .build();
        return provider.newProducerBuilder()
                .setClientConfiguration(configuration)
                .build();
    }

    /**
     * 生产者工厂，便于测试替换。
     */
    @FunctionalInterface
    interface ProducerFactory {

        /**
         * 创建一个生产者。
         *
         * @return 生产者
         * @throws Exception 创建失败时抛出
         */
        Producer create() throws Exception;
    }
}
