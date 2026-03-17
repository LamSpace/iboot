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

package com.iboot.admin.infrastructure.push;

import com.iboot.admin.common.cloudevent.CloudEventTypes;
import com.iboot.admin.domain.system.model.MessageReceiver;
import com.iboot.admin.domain.system.repository.MessageReceiverRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息推送监听器
 * <p>
 * 监听消息事件并触发推送通知
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class MessagePushListener {

    private static final Logger log = LoggerFactory.getLogger(MessagePushListener.class);

    private final PushEventService pushEventService;

    private final MessageReceiverRepository messageReceiverRepository;

    @SuppressWarnings("all")
    public MessagePushListener(final PushEventService pushEventService,
                               final MessageReceiverRepository messageReceiverRepository) {
        this.pushEventService = pushEventService;
        this.messageReceiverRepository = messageReceiverRepository;
    }

    /**
     * 监听消息发送事件，在事务提交后异步推送
     *
     * @param event 消息发送事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MessageSentEvent event) {
        log.info("接收到消息发送事件，messageId: {}, eventId: {}", event.getMessageId(), event.getEventId());
        // 构建推送数据
        Map<String, Object> pushData = Map.of("messageId", event.getMessageId(), "title", event.getTitle(), "content",
                event.getContent(), "messageType", event.getMessageType(), "priority", event.getPriority(), "senderId",
                event.getSenderId(), "sentAt", LocalDateTime.now().toString());
        // 构建推送事件
        PushEvent pushEvent = PushEvent.builder()
                .id("push-msg-" + java.util.UUID.randomUUID())
                .type(CloudEventTypes.PUSH_NEW_MESSAGE)
                .source("/api/messages")
                .time(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .dataContentType("application/json")
                .data(pushData)
                .build();
        // 获取所有接收者并推送
        List<MessageReceiver> receivers = messageReceiverRepository.findByMessageId(event.getMessageId());
        if (receivers == null || receivers.isEmpty()) {
            log.warn("消息没有接收者，messageId: {}", event.getMessageId());
            return;
        }
        List<Long> userIds = receivers.stream().map(MessageReceiver::getUserId).collect(Collectors.toList());
        // 组播推送给所有接收者
        pushEventService.sendToUsers(userIds, pushEvent);
        log.info("消息推送完成，messageId: {}, 接收者数量：{}", event.getMessageId(), userIds.size());
    }

    /**
     * 监听消息已读事件
     *
     * @param event 消息已读事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MessageReadEvent event) {
        log.info("接收到消息已读事件，messageId: {}, userId: {}", event.getMessageId(), event.getUserId());
        // 构建推送数据
        Map<String, Object> pushData = Map.of("messageId", event.getMessageId(), "userId", event.getUserId(), "readAt",
                LocalDateTime.now().toString());
        // 构建推送事件
        PushEvent pushEvent = PushEvent.builder()
                .id("push-read-" + java.util.UUID.randomUUID())
                .type(CloudEventTypes.PUSH_MESSAGE_READ)
                .source("/api/messages/read")
                .time(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .dataContentType("application/json")
                .data(pushData)
                .build();
        // 推送给消息发送者（如果在线）
        if (event.getSenderId() != null) {
            pushEventService.sendToUser(event.getSenderId(), pushEvent);
            log.info("已读通知已推送，messageId: {}, senderId: {}", event.getMessageId(), event.getSenderId());
        }
    }

}
