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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 按源数据最新状态刷新内容索引。
 *
 * <p>是事件监听与定时对账共用的写入逻辑：每次都重新读取数据库，按当前可见性决定写入还是
 * 移除文档，因此重复调用是幂等的。不捕获异常，由调用方记录并降级。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class ContentIndexUpdater {

    private final ElasticsearchContentIndex contentIndex;

    private final NoticeRepository noticeRepository;

    private final MessageRepository messageRepository;

    private final MessageReceiverRepository messageReceiverRepository;

    @SuppressWarnings("all")
    public ContentIndexUpdater(final ElasticsearchContentIndex contentIndex, final NoticeRepository noticeRepository,
                               final MessageRepository messageRepository,
                               final MessageReceiverRepository messageReceiverRepository) {
        this.contentIndex = contentIndex;
        this.noticeRepository = noticeRepository;
        this.messageRepository = messageRepository;
        this.messageReceiverRepository = messageReceiverRepository;
    }

    /**
     * 按公告当前状态刷新索引。
     *
     * <p>公告不存在、已删除、非已发布状态时移除索引文档。
     *
     * @param noticeId 公告 ID
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    public void refreshNotice(final Long noticeId) throws IOException {
        Optional<Notice> notice = noticeRepository.findById(noticeId);
        ContentDocument document = notice.map(ContentDocumentFactory::fromNotice).orElse(null);
        if (document == null) {
            contentIndex.delete(ContentDocType.NOTICE, noticeId, null);
            return;
        }
        contentIndex.upsert(document);
    }

    /**
     * 按消息当前状态刷新全部接收人的索引文档。
     *
     * <p>消息不存在、已删除或非已发送状态时移除该消息的全部文档；否则按接收人批量覆盖，
     * 并移除接收人已删除的文档。
     *
     * @param messageId 消息 ID
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    public void refreshMessage(final Long messageId) throws IOException {
        Optional<Message> current = messageRepository.findById(messageId);
        if (current.isEmpty()) {
            contentIndex.deleteBySource(ContentDocType.MESSAGE, messageId);
            return;
        }
        Message message = current.get();
        List<MessageReceiver> receivers = messageReceiverRepository.findByMessageId(messageId);
        List<ContentDocument> documents = new ArrayList<>();
        List<Long> deletedReceivers = new ArrayList<>();
        for (MessageReceiver receiver : receivers) {
            if (isDeleted(receiver.getIsDeleted())) {
                deletedReceivers.add(receiver.getUserId());
                continue;
            }
            ContentDocument document = ContentDocumentFactory.fromMessage(message, receiver.getUserId());
            if (document != null) {
                documents.add(document);
            }
        }
        contentIndex.upsertAll(documents);
        for (Long receiverId : deletedReceivers) {
            contentIndex.delete(ContentDocType.MESSAGE, messageId, receiverId);
        }
    }

    /**
     * 按消息与接收人的当前状态刷新单个索引文档。
     *
     * @param messageId  消息 ID
     * @param receiverId 接收人用户 ID
     * @param deleted    接收记录是否已被用户删除
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    public void refreshMessageReceiver(final Long messageId, final Long receiverId, final boolean deleted)
            throws IOException {
        if (deleted) {
            contentIndex.delete(ContentDocType.MESSAGE, messageId, receiverId);
            return;
        }
        Optional<Message> current = messageRepository.findById(messageId);
        ContentDocument document = current.map(message -> ContentDocumentFactory.fromMessage(message, receiverId))
                .orElse(null);
        if (document == null) {
            contentIndex.delete(ContentDocType.MESSAGE, messageId, receiverId);
            return;
        }
        contentIndex.upsert(document);
    }

    /**
     * 判断逻辑删除标记是否为已删除。
     *
     * @param deleted 逻辑删除标记
     *
     * @return 已删除返回 {@code true}
     */
    private boolean isDeleted(final Integer deleted) {
        return deleted != null && deleted == 1;
    }

}
