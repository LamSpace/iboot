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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.workflow.application.service.notification.NotificationCommand;
import com.iboot.workflow.application.service.notification.NotificationPort;
import com.iboot.workflow.domain.notification.model.NotificationOutboxEvent;
import com.iboot.workflow.domain.notification.model.NotificationOutboxStatus;
import com.iboot.workflow.domain.notification.repository.NotificationOutboxRepository;
import com.iboot.workflow.infrastructure.config.OutboxProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 通知登记端口的 outbox 适配器。
 *
 * <p>在调用方事务内把通知命令序列化并写入 {@code biz_notification_outbox}；写入失败
 * 直接抛出，由调用方事务回滚。事务提交后由 {@code OutboxRelay} 异步投递到 RocketMQ，
 * 因此本适配器不感知 broker 可用性，也不在请求线程内做任何网络调用。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class OutboxNotificationPort implements NotificationPort {

    private final NotificationOutboxRepository repository;

    private final ObjectMapper objectMapper;

    private final OutboxProperties properties;

    @SuppressWarnings("all")
    public OutboxNotificationPort(final NotificationOutboxRepository repository,
                                  final ObjectMapper objectMapper,
                                  final OutboxProperties properties) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 在调用方事务内持久化一次通知事件。
     *
     * @param command 通知登记命令
     * @throws IllegalStateException 序列化失败或持久化失败时抛出，调用方事务应回滚
     */
    @Override
    public void enqueue(final NotificationCommand command) {
        NotificationOutboxEvent event = new NotificationOutboxEvent();
        event.setEventId(command.getEventId());
        event.setEventType(command.getEventType().name());
        event.setPayload(toPayload(command));
        event.setStatus(NotificationOutboxStatus.PENDING);
        event.setAttempts(0);
        event.setMaxAttempts(properties.getMaxAttempts());
        event.setCreateTime(LocalDateTime.now());
        repository.save(event);
    }

    private String toPayload(final NotificationCommand command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("通知事件序列化失败", e);
        }
    }
}
