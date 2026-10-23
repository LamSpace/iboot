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

package com.iboot.admin.application.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.admin.application.service.MessageTemplateApplicationService;
import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.notification.model.NotificationChannelType;
import com.iboot.admin.domain.notification.model.NotificationDelivery;
import com.iboot.admin.domain.notification.model.NotificationDeliveryStatus;
import com.iboot.admin.domain.notification.model.NotificationEventType;
import com.iboot.admin.domain.notification.model.NotificationSendResult;
import com.iboot.admin.domain.notification.repository.NotificationDeliveryRepository;
import com.iboot.admin.domain.notification.service.NotificationChannel;
import com.iboot.admin.domain.notification.service.NotificationDeliveryException;
import com.iboot.admin.domain.system.model.MessageTemplate;
import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.domain.system.repository.UserRepository;
import com.iboot.admin.infrastructure.notification.NotificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotificationApplicationService} 的单元测试。
 *
 * <p>覆盖渠道 fan-out、缺地址跳过、发送成功、失败退避、终止、领取冲突与人工重试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class NotificationApplicationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 12, 0);

    private NotificationDeliveryRepository repository;

    private UserRepository userRepository;

    private MessageTemplateApplicationService templateService;

    private ApplicationEventPublisher eventPublisher;

    private NotificationProperties properties;

    private FakeChannel inApp;

    private FakeChannel email;

    private FakeChannel dingtalk;

    private FakeChannel sms;

    private NotificationApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationDeliveryRepository.class);
        userRepository = mock(UserRepository.class);
        templateService = mock(MessageTemplateApplicationService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        properties = new NotificationProperties();
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setBackoffSeconds(60);

        inApp = new FakeChannel(NotificationChannelType.IN_APP, true);
        email = new FakeChannel(NotificationChannelType.EMAIL, true);
        dingtalk = new FakeChannel(NotificationChannelType.DINGTALK, true);
        sms = new FakeChannel(NotificationChannelType.SMS, false);

        Clock clock = Clock.fixed(Instant.parse("2026-10-23T04:00:00Z"), ZoneId.of("Asia/Shanghai"));
        service = new NotificationApplicationService(repository, List.of(inApp, email, dingtalk, sms),
                userRepository, templateService, eventPublisher, properties, new ObjectMapper(), clock);

        when(repository.insertIfAbsent(any())).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user()));
        when(templateService.getTemplateByCode("APPROVAL_TODO")).thenReturn(template());
    }

    /**
     * 验证启用渠道各生成一条记录，关闭渠道不生成。
     */
    @Test
    void enqueueCreatesOneRowPerEnabledChannel() {
        String eventId = service.enqueue(command());

        ArgumentCaptor<NotificationDelivery> captor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(repository, org.mockito.Mockito.times(3)).insertIfAbsent(captor.capture());
        List<NotificationDelivery> rows = captor.getAllValues();

        assertThat(eventId).isEqualTo("approval-todo-1");
        assertThat(rows).extracting(NotificationDelivery::getChannel)
                .containsExactlyInAnyOrder(NotificationChannelType.IN_APP, NotificationChannelType.EMAIL,
                        NotificationChannelType.DINGTALK);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getEventId()).isEqualTo("approval-todo-1");
            assertThat(row.getTemplateCode()).isEqualTo("APPROVAL_TODO");
            assertThat(row.getTitle()).isEqualTo("审批待办通知");
            assertThat(row.getContent()).contains("年假").contains("Alice");
            assertThat(row.getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
        });
        assertThat(rows).filteredOn(row -> row.getChannel() == NotificationChannelType.EMAIL)
                .singleElement()
                .extracting(NotificationDelivery::getRecipientAddress)
                .isEqualTo("user@example.com");
        assertThat(rows).filteredOn(row -> row.getChannel() == NotificationChannelType.DINGTALK)
                .singleElement()
                .extracting(NotificationDelivery::getRecipientKey)
                .isEqualTo("GROUP:default");
        verify(eventPublisher).publishEvent(any(NotificationQueuedEvent.class));
    }

    /**
     * 验证邮箱为空时邮件记录标记为跳过，其他渠道不受影响。
     */
    @Test
    void enqueueMarksEmailSkippedWhenAddressMissing() {
        User user = user();
        user.setEmail(null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        service.enqueue(command());

        ArgumentCaptor<NotificationDelivery> captor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(repository, org.mockito.Mockito.times(3)).insertIfAbsent(captor.capture());
        assertThat(captor.getAllValues())
                .filteredOn(row -> row.getChannel() == NotificationChannelType.EMAIL)
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.getStatus()).isEqualTo(NotificationDeliveryStatus.SKIPPED);
                    assertThat(row.getLastError()).contains("邮箱");
                });
    }

    /**
     * 验证没有启用渠道时不产生记录也不发布事件。
     */
    @Test
    void enqueueDoesNothingWhenNoChannelEnabled() {
        inApp.enabled = false;
        email.enabled = false;
        dingtalk.enabled = false;
        sms.enabled = false;

        service.enqueue(command());

        verify(repository, never()).insertIfAbsent(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    /**
     * 验证发送成功更新状态、消息 ID 与发送时间。
     */
    @Test
    void dispatchSendsAndMarksSuccess() {
        NotificationDelivery delivery = delivery(NotificationChannelType.IN_APP);
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));
        when(repository.claim(1L, NOW)).thenReturn(true);
        inApp.result = new NotificationSendResult(77L);

        service.dispatch(1L);

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.SUCCESS);
        assertThat(delivery.getMessageId()).isEqualTo(77L);
        assertThat(delivery.getSentTime()).isEqualTo(NOW);
        verify(repository).update(delivery);
    }

    /**
     * 验证可重试失败按退避安排下一次重试。
     */
    @Test
    void dispatchFailureSchedulesRetry() {
        NotificationDelivery delivery = delivery(NotificationChannelType.EMAIL);
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));
        when(repository.claim(1L, NOW)).thenReturn(true);
        email.failure = new NotificationDeliveryException("smtp down", true);

        service.dispatch(1L);

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(delivery.getNextRetryTime()).isEqualTo(NOW.plusSeconds(60));
        verify(repository).update(delivery);
    }

    /**
     * 验证不可重试失败直接进入终止状态。
     */
    @Test
    void dispatchPermanentFailureGoesDead() {
        NotificationDelivery delivery = delivery(NotificationChannelType.EMAIL);
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));
        when(repository.claim(1L, NOW)).thenReturn(true);
        email.failure = new NotificationDeliveryException("invalid address", false);

        service.dispatch(1L);

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.DEAD);
        assertThat(delivery.getNextRetryTime()).isNull();
    }

    /**
     * 验证领取冲突时跳过发送。
     */
    @Test
    void dispatchSkipsWhenClaimFails() {
        NotificationDelivery delivery = delivery(NotificationChannelType.EMAIL);
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));
        when(repository.claim(1L, NOW)).thenReturn(false);

        service.dispatch(1L);

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(email.sendCount).isZero();
        verify(repository, never()).update(any());
    }

    /**
     * 验证渠道在发送时被关闭则标记跳过。
     */
    @Test
    void dispatchSkipsDisabledChannel() {
        NotificationDelivery delivery = delivery(NotificationChannelType.EMAIL);
        email.enabled = false;
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));
        when(repository.claim(1L, NOW)).thenReturn(true);

        service.dispatch(1L);

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.SKIPPED);
        assertThat(email.sendCount).isZero();
    }

    /**
     * 验证非失败/终止状态不允许人工重试。
     */
    @Test
    void manualRetryRejectsSuccessfulDelivery() {
        NotificationDelivery delivery = delivery(NotificationChannelType.EMAIL);
        delivery.markSuccess(null, NOW);
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.retryManually(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许");
    }

    /**
     * 验证人工重试重置状态并发布即时投递事件。
     */
    @Test
    void manualRetryResetsAndPublishes() {
        NotificationDelivery delivery = delivery(NotificationChannelType.EMAIL);
        delivery.markFailed("dead", 1, 60, NOW);
        when(repository.findById(1L)).thenReturn(Optional.of(delivery));
        when(repository.resetForRetry(1L, NOW)).thenReturn(true);

        service.retryManually(1L);

        verify(repository).resetForRetry(1L, NOW);
        verify(eventPublisher).publishEvent(any(NotificationRetryRequestedEvent.class));
    }

    private NotificationCommand command() {
        return new NotificationCommand("approval-todo-1", NotificationEventType.APPROVAL_TODO, "APPROVAL_TODO", 2L,
                Map.of("taskName", "年假", "initiator", "Alice", "applyTime", "2026-11-02"));
    }

    private User user() {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");
        user.setNickname("Bob");
        user.setEmail("user@example.com");
        user.setPhone("13800138000");
        user.setStatus(1);
        return user;
    }

    private MessageTemplate template() {
        MessageTemplate template = new MessageTemplate();
        template.setTemplateCode("APPROVAL_TODO");
        template.setTemplateName("审批待办通知");
        template.setTemplateContent("您有一条新的审批待办：${taskName}，发起人：${initiator}，申请时间：${applyTime}。");
        template.setStatus(1);
        return template;
    }

    private NotificationDelivery delivery(NotificationChannelType channel) {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setId(1L);
        delivery.setEventId("approval-todo-1");
        delivery.setEventType(NotificationEventType.APPROVAL_TODO);
        delivery.setChannel(channel);
        delivery.setRecipientKey("USER:2");
        delivery.setRecipientId(2L);
        delivery.setRecipientAddress("user@example.com");
        delivery.setTemplateCode("APPROVAL_TODO");
        delivery.setTitle("审批待办通知");
        delivery.setContent("您有一条新的审批待办");
        delivery.setMaxAttempts(3);
        delivery.setCreateTime(NOW);
        delivery.setUpdateTime(NOW);
        return delivery;
    }

    /**
     * 测试用渠道实现，可配置启用状态、返回值与异常。
     */
    private static final class FakeChannel implements NotificationChannel {

        private final NotificationChannelType type;

        private boolean enabled;

        private NotificationSendResult result = NotificationSendResult.none();

        private NotificationDeliveryException failure;

        private int sendCount;

        private FakeChannel(final NotificationChannelType type, final boolean enabled) {
            this.type = type;
            this.enabled = enabled;
        }

        @Override
        public NotificationChannelType channelType() {
            return type;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public NotificationSendResult send(final NotificationDelivery delivery) {
            sendCount++;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }
}
