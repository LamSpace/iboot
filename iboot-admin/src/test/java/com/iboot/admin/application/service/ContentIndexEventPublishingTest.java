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
import com.iboot.admin.domain.system.model.Notice;
import com.iboot.admin.domain.system.repository.MessageReceiverRepository;
import com.iboot.admin.domain.system.repository.MessageRepository;
import com.iboot.admin.domain.system.repository.NoticeReadRepository;
import com.iboot.admin.domain.system.repository.NoticeRepository;
import com.iboot.admin.domain.system.repository.UserRepository;
import com.iboot.admin.infrastructure.search.ContentDocType;
import com.iboot.admin.infrastructure.search.ContentIndexAction;
import com.iboot.admin.infrastructure.search.ContentIndexEvent;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 内容索引事件发布路径的单元测试。
 *
 * <p>验证公告与消息的每条可见性变更路径都发布了正确动作的 {@link ContentIndexEvent}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ContentIndexEventPublishingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 8, 30);

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeReadRepository noticeReadRepository;

    @Mock
    private ConfigApplicationService configApplicationService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageReceiverRepository messageReceiverRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private NoticeApplicationService noticeApplicationService;

    private MessageApplicationService messageApplicationService;

    @BeforeEach
    void setUp() {
        noticeApplicationService = new NoticeApplicationService(noticeRepository, noticeReadRepository,
                configApplicationService, eventPublisher);
        messageApplicationService = new MessageApplicationService(messageRepository, messageReceiverRepository,
                userRepository, configApplicationService, eventPublisher);
    }

    /**
     * 发布公告应发布刷新事件。
     */
    @Test
    void shouldPublishRefreshWhenNoticePublished() {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice("0", 0)));
        when(noticeRepository.update(any())).thenReturn(true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            noticeApplicationService.publishNotice(1L);
        }

        ContentIndexEvent event = captureSingleContentEvent();
        assertThat(event.getDocType()).isEqualTo(ContentDocType.NOTICE);
        assertThat(event.getAction()).isEqualTo(ContentIndexAction.REFRESH_SOURCE);
        assertThat(event.getSourceId()).isEqualTo(1L);
    }

    /**
     * 撤回公告应发布整条移除事件。
     */
    @Test
    void shouldPublishDeleteWhenNoticeRevoked() {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice("1", 0)));
        when(noticeRepository.update(any())).thenReturn(true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            noticeApplicationService.revokeNotice(1L);
        }

        ContentIndexEvent event = captureSingleContentEvent();
        assertThat(event.getAction()).isEqualTo(ContentIndexAction.DELETE_SOURCE);
        assertThat(event.getSourceId()).isEqualTo(1L);
    }

    /**
     * 删除公告应发布整条移除事件。
     */
    @Test
    void shouldPublishDeleteWhenNoticeDeleted() {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice("0", 0)));
        when(noticeRepository.deleteById(1L)).thenReturn(true);

        noticeApplicationService.deleteNotice(1L);

        ContentIndexEvent event = captureSingleContentEvent();
        assertThat(event.getAction()).isEqualTo(ContentIndexAction.DELETE_SOURCE);
    }

    /**
     * 发送消息应发布刷新事件，同时保留原有的推送事件。
     */
    @Test
    void shouldPublishRefreshWhenMessageSent() {
        when(messageRepository.findById(5L)).thenReturn(Optional.of(message("0", 0)));
        when(messageRepository.update(any())).thenReturn(true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            messageApplicationService.sendMessage(5L, List.of(7L));
        }

        ContentIndexEvent event = captureContentEvents(2);
        assertThat(event.getAction()).isEqualTo(ContentIndexAction.REFRESH_SOURCE);
        assertThat(event.getSourceId()).isEqualTo(5L);
    }

    /**
     * 撤回消息应发布整条移除事件。
     */
    @Test
    void shouldPublishDeleteWhenMessageRevoked() {
        when(messageRepository.findById(5L)).thenReturn(Optional.of(message("1", 0)));
        when(messageRepository.update(any())).thenReturn(true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            messageApplicationService.revokeMessage(5L);
        }

        ContentIndexEvent event = captureSingleContentEvent();
        assertThat(event.getAction()).isEqualTo(ContentIndexAction.DELETE_SOURCE);
    }

    /**
     * 删除消息应发布整条移除事件。
     */
    @Test
    void shouldPublishDeleteWhenMessageDeleted() {
        when(messageRepository.findById(5L)).thenReturn(Optional.of(message("0", 0)));
        when(messageRepository.deleteById(5L)).thenReturn(true);

        messageApplicationService.deleteMessage(5L);

        ContentIndexEvent event = captureSingleContentEvent();
        assertThat(event.getAction()).isEqualTo(ContentIndexAction.DELETE_SOURCE);
    }

    /**
     * 用户删除收件箱消息应发布单个接收人移除事件。
     */
    @Test
    void shouldPublishDeleteReceiverWhenInboxMessageDeleted() {
        when(messageReceiverRepository.softDelete(5L, 7L)).thenReturn(true);

        messageApplicationService.deleteUserMessage(5L, 7L);

        ContentIndexEvent event = captureSingleContentEvent();
        assertThat(event.getAction()).isEqualTo(ContentIndexAction.DELETE_RECEIVER);
        assertThat(event.getSourceId()).isEqualTo(5L);
        assertThat(event.getReceiverId()).isEqualTo(7L);
    }

    /**
     * 捕获唯一的内容索引事件。
     *
     * @return 内容索引事件
     */
    private ContentIndexEvent captureSingleContentEvent() {
        return captureContentEvents(1);
    }

    /**
     * 捕获发布事件中的内容索引事件。
     *
     * @param expectedTotal 期望的事件总数（发送消息时还包含推送事件）
     *
     * @return 内容索引事件
     */
    private ContentIndexEvent captureContentEvents(final int expectedTotal) {
        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, times(expectedTotal)).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(ContentIndexEvent.class::isInstance)
                .map(ContentIndexEvent.class::cast)
                .findFirst()
                .orElseThrow();
    }

    /**
     * 创建测试用公告。
     *
     * @param status  状态
     * @param deleted 逻辑删除标记
     *
     * @return 公告实体
     */
    private Notice notice(final String status, final int deleted) {
        return Notice.builder()
                .id(1L)
                .noticeTitle("年度公告")
                .noticeContent("年度绩效评估安排")
                .status(status)
                .isTop(0)
                .createBy("admin")
                .createTime(NOW)
                .deleted(deleted)
                .build();
    }

    /**
     * 创建测试用消息。
     *
     * @param status  状态
     * @param deleted 逻辑删除标记
     *
     * @return 消息实体
     */
    private Message message(final String status, final int deleted) {
        return Message.builder()
                .id(5L)
                .title("奖金通知")
                .content("绩效奖金发放")
                .messageType("1")
                .senderType("1")
                .senderId(1L)
                .receiverType("1")
                .priority("0")
                .status(status)
                .createBy("admin")
                .createTime(NOW)
                .deleted(deleted)
                .build();
    }

}
