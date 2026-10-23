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

package com.iboot.admin.application.service;

import com.iboot.admin.domain.system.model.Message;
import com.iboot.admin.domain.system.model.MessageReceiver;
import com.iboot.admin.domain.system.repository.MessageReceiverRepository;
import com.iboot.admin.domain.system.repository.MessageRepository;
import com.iboot.admin.domain.system.repository.UserRepository;
import com.iboot.admin.infrastructure.push.MessageSentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MessageApplicationService#sendSystemMessage} 的单元测试。
 *
 * <p>验证系统消息以 system 身份落库、批量创建接收记录并发布 SSE 事件。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class MessageApplicationServiceSystemMessageTest {

    private MessageRepository messageRepository;

    private MessageReceiverRepository messageReceiverRepository;

    private ApplicationEventPublisher eventPublisher;

    private MessageApplicationService service;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessageRepository.class);
        messageReceiverRepository = mock(MessageReceiverRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new MessageApplicationService(messageRepository, messageReceiverRepository,
                mock(UserRepository.class), mock(ConfigApplicationService.class), eventPublisher);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(10L);
            return message;
        });
        when(messageRepository.update(any(Message.class))).thenReturn(true);
    }

    /**
     * 验证系统消息落库、接收记录与事件发布。
     */
    @Test
    void sendsSystemMessageWithSystemIdentity() {
        Long messageId = service.sendSystemMessage("审批待办通知", "您有一条审批待办", "2", "1", List.of(2L));

        assertThat(messageId).isEqualTo(10L);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message saved = messageCaptor.getValue();
        assertThat(saved.getSenderType()).isEqualTo("0");
        assertThat(saved.getSenderId()).isNull();
        assertThat(saved.getCreateBy()).isEqualTo("system");
        assertThat(saved.getReceiverType()).isEqualTo("1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MessageReceiver>> receiverCaptor = ArgumentCaptor.forClass(List.class);
        verify(messageReceiverRepository).batchSave(receiverCaptor.capture());
        assertThat(receiverCaptor.getValue()).singleElement()
                .extracting(MessageReceiver::getUserId)
                .isEqualTo(2L);

        ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(eventCaptor.capture());
        MessageSentEvent sentEvent = eventCaptor.getAllValues().stream()
                .filter(MessageSentEvent.class::isInstance)
                .map(MessageSentEvent.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(sentEvent.getMessageId()).isEqualTo(10L);
        assertThat(sentEvent.getSenderId()).isNull();
    }
}
