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
import com.iboot.notify.application.service.notification.NotificationApplicationService;
import com.iboot.notify.application.service.notification.NotificationCommand;
import com.iboot.notify.domain.notification.model.NotificationEventType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 审批待办通知事件的 RocketMQ 监听器。
 *
 * <p>把消息体还原为 {@link NotificationCommand} 并复用既有登记服务；登记失败返回
 * {@link ConsumeResult#FAILURE}，由 RocketMQ 重试，超过上限进入
 * {@code %DLQ%<consumerGroup>}。登记侧的唯一键与 {@code INSERT IGNORE} 负责吸收重复消息。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class NotificationMessageListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationMessageListener.class);

    private final NotificationApplicationService notificationApplicationService;

    private final ObjectMapper objectMapper;

    private final Counter successCounter;

    private final Counter failureCounter;

    @SuppressWarnings("all")
    public NotificationMessageListener(final NotificationApplicationService notificationApplicationService,
                                       final ObjectMapper objectMapper,
                                       final MeterRegistry meterRegistry) {
        this.notificationApplicationService = notificationApplicationService;
        this.objectMapper = objectMapper;
        this.successCounter = Counter.builder("notification_consume_success_total")
                .description("通知事件消费成功次数")
                .tag("application", "iboot-notify")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("notification_consume_failure_total")
                .description("通知事件消费失败次数")
                .tag("application", "iboot-notify")
                .register(meterRegistry);
    }

    @Override
    public ConsumeResult consume(final MessageView messageView) {
        try {
            NotificationEventMessage message = objectMapper.readValue(
                    StandardCharsets.UTF_8.decode(messageView.getBody()).toString(),
                    NotificationEventMessage.class);
            notificationApplicationService.enqueue(new NotificationCommand(
                    message.getEventId(),
                    NotificationEventType.valueOf(message.getEventType()),
                    message.getTemplateCode(),
                    message.getRecipientId(),
                    message.getParams()));
            successCounter.increment();
            log.info("通知事件消费成功，eventId: {}, messageId: {}",
                    message.getEventId(), messageView.getMessageId());
            return ConsumeResult.SUCCESS;
        } catch (Exception e) {
            failureCounter.increment();
            log.error("通知事件消费失败，messageId: {}", messageView.getMessageId(), e);
            return ConsumeResult.FAILURE;
        }
    }
}
