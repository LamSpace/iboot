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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ContentIndexReconcileTask} 的单元测试。
 *
 * <p>验证公告、消息与接收记录三类窗口内变更的重放，以及全量回填失败后的重试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ContentIndexReconcileTaskTest {

    private static final LocalDateTime CREATE_TIME = LocalDateTime.of(2026, 10, 21, 8, 30);

    @Mock
    private ElasticsearchContentIndex contentIndex;

    @Mock
    private ContentIndexUpdater contentIndexUpdater;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageReceiverRepository messageReceiverRepository;

    private ContentIndexReconcileTask reconcileTask;

    @BeforeEach
    void setUp() {
        reconcileTask = new ContentIndexReconcileTask(contentIndex, contentIndexUpdater, noticeRepository,
                messageRepository, messageReceiverRepository);
    }

    /**
     * 公告窗口内变更应写入或移除索引。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldReplayNoticeChanges() throws IOException {
        when(noticeRepository.findChangedSince()).thenReturn(List.of(
                notice(1L, "1", 0),
                notice(2L, "2", 0),
                notice(3L, "1", 1)));

        reconcileTask.reconcile();

        verify(contentIndexUpdater).refreshNotice(1L);
        verify(contentIndex).delete(ContentDocType.NOTICE, 2L, null);
        verify(contentIndex).delete(ContentDocType.NOTICE, 3L, null);
    }

    /**
     * 消息与接收记录窗口内变更应写入或移除索引。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldReplayMessageAndReceiverChanges() throws IOException {
        when(messageRepository.findChangedSince()).thenReturn(List.of(
                message(5L, "1", 0),
                message(6L, "2", 0),
                message(7L, "1", 1)));
        when(messageReceiverRepository.findChangedSince()).thenReturn(List.of(
                receiver(5L, 7L, 0),
                receiver(5L, 8L, 1)));

        reconcileTask.reconcile();

        verify(contentIndexUpdater).refreshMessage(5L);
        verify(contentIndex).deleteBySource(ContentDocType.MESSAGE, 6L);
        verify(contentIndex).deleteBySource(ContentDocType.MESSAGE, 7L);
        verify(contentIndexUpdater).refreshMessageReceiver(5L, 7L, false);
        verify(contentIndexUpdater).refreshMessageReceiver(5L, 8L, true);
    }

    /**
     * 全量回填失败后应在下一轮重试，成功后不再重复。
     *
     * @throws IOException 模拟故障时抛出
     */
    @Test
    void shouldRetryBackfillUntilSuccess() throws IOException {
        doThrow(new IOException("es down")).doNothing().when(contentIndex).upsertAll(any());
        when(messageRepository.findAllByCondition(null, null, "1")).thenReturn(List.of());

        reconcileTask.reconcile();
        verify(contentIndex, times(1)).upsertAll(any());

        reconcileTask.reconcile();
        verify(contentIndex, times(3)).upsertAll(any());

        reconcileTask.reconcile();
        verify(contentIndex, times(3)).upsertAll(any());
    }

    /**
     * 创建测试用公告。
     *
     * @param id      公告 ID
     * @param status  状态
     * @param deleted 逻辑删除标记
     *
     * @return 公告实体
     */
    private Notice notice(final Long id, final String status, final int deleted) {
        return Notice.builder()
                .id(id)
                .noticeTitle("年度公告")
                .noticeContent("年度绩效评估安排")
                .status(status)
                .createBy("admin")
                .createTime(CREATE_TIME)
                .deleted(deleted)
                .build();
    }

    /**
     * 创建测试用消息。
     *
     * @param id      消息 ID
     * @param status  状态
     * @param deleted 逻辑删除标记
     *
     * @return 消息实体
     */
    private Message message(final Long id, final String status, final int deleted) {
        return Message.builder()
                .id(id)
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
                .isDeleted(isDeleted)
                .build();
    }

}
