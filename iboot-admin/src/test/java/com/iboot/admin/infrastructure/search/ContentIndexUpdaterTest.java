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

package com.iboot.admin.infrastructure.search;

import com.iboot.admin.domain.system.model.Message;
import com.iboot.admin.domain.system.model.MessageReceiver;
import com.iboot.admin.domain.system.model.Notice;
import com.iboot.admin.domain.system.repository.MessageReceiverRepository;
import com.iboot.admin.domain.system.repository.MessageRepository;
import com.iboot.admin.domain.system.repository.NoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ContentIndexUpdater} 的单元测试。
 *
 * <p>验证公告、消息与接收记录在不同可见性下是写入还是移除索引文档。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ContentIndexUpdaterTest {

    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2026, 10, 21, 8, 30);

    @Mock
    private ElasticsearchContentIndex contentIndex;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageReceiverRepository messageReceiverRepository;

    private ContentIndexUpdater updater;

    @BeforeEach
    void setUp() {
        updater = new ContentIndexUpdater(contentIndex, noticeRepository, messageRepository, messageReceiverRepository);
    }

    /**
     * 已发布公告应写入单个文档。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldUpsertPublishedNotice() throws IOException {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice("1", 0)));

        updater.refreshNotice(1L);

        ArgumentCaptor<ContentDocument> captor = ArgumentCaptor.forClass(ContentDocument.class);
        verify(contentIndex).upsert(captor.capture());
        assertThat(captor.getValue().documentId()).isEqualTo("notice-1");
    }

    /**
     * 非发布状态或已删除的公告应移除文档。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldDeleteInvisibleNotice() throws IOException {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice("0", 0)));
        when(noticeRepository.findById(2L)).thenReturn(Optional.empty());

        updater.refreshNotice(1L);
        updater.refreshNotice(2L);

        verify(contentIndex).delete(ContentDocType.NOTICE, 1L, null);
        verify(contentIndex).delete(ContentDocType.NOTICE, 2L, null);
        verify(contentIndex, never()).upsert(any());
    }

    /**
     * 已发送消息应按接收人写入文档，并移除接收人已删除的文档。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldUpsertMessageForActiveReceiversOnly() throws IOException {
        when(messageRepository.findById(5L)).thenReturn(Optional.of(message("1", 0)));
        when(messageReceiverRepository.findByMessageId(5L)).thenReturn(List.of(
                receiver(5L, 7L, 0),
                receiver(5L, 8L, 1)));

        updater.refreshMessage(5L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ContentDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(contentIndex).upsertAll(captor.capture());
        assertThat(captor.getValue()).extracting(ContentDocument::documentId).containsExactly("message-5-7");
        verify(contentIndex).delete(ContentDocType.MESSAGE, 5L, 8L);
    }

    /**
     * 消息不存在时应移除整条消息的文档。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldDeleteMessageDocumentsWhenMessageMissing() throws IOException {
        when(messageRepository.findById(5L)).thenReturn(Optional.empty());

        updater.refreshMessage(5L);

        verify(contentIndex).deleteBySource(ContentDocType.MESSAGE, 5L);
        verify(contentIndex, never()).upsertAll(any());
    }

    /**
     * 接收人侧删除应移除对应文档。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldDeleteReceiverDocumentWhenReceiverDeleted() throws IOException {
        updater.refreshMessageReceiver(5L, 7L, true);

        verify(contentIndex).delete(ContentDocType.MESSAGE, 5L, 7L);
        verify(contentIndex, never()).upsert(any());
    }

    /**
     * 接收人未删除且消息已发送时应写入对应文档。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldUpsertReceiverDocument() throws IOException {
        when(messageRepository.findById(5L)).thenReturn(Optional.of(message("1", 0)));

        updater.refreshMessageReceiver(5L, 7L, false);

        ArgumentCaptor<ContentDocument> captor = ArgumentCaptor.forClass(ContentDocument.class);
        verify(contentIndex).upsert(captor.capture());
        assertThat(captor.getValue().documentId()).isEqualTo("message-5-7");
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
                .noticeType("1")
                .noticeContent("年度绩效评估安排")
                .status(status)
                .isTop(0)
                .createBy("admin")
                .createTime(CREATE_TIME)
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
                .status(status)
                .createBy("admin")
                .createTime(CREATE_TIME)
                .deleted(deleted)
                .build();
    }

    /**
     * 创建测试用接收记录。
     *
     * @param messageId 消息 ID
     * @param userId    用户 ID
     * @param isDeleted 是否已删除
     *
     * @return 接收记录实体
     */
    private MessageReceiver receiver(final Long messageId, final Long userId, final int isDeleted) {
        return MessageReceiver.builder()
                .messageId(messageId)
                .userId(userId)
                .isRead(0)
                .isDeleted(isDeleted)
                .build();
    }

}
