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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 内容索引定时对账任务。
 *
 * <p>每 5 分钟重放最近 10 分钟的源数据变更（窗口在数据库侧用 {@code NOW()} 计算），补偿事件丢失、
 * 进程崩溃或 Elasticsearch 短暂不可用造成的索引偏差；每轮处理的都是最新状态，因此天然幂等。
 *
 * <p>应用启动后会异步执行一次全量回填；回填失败时标记保持待回填状态，由后续每轮对账重试。
 * 该任务通过 {@code sys_job} 中的 {@code contentIndexReconcileTask.reconcile} 调度。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class ContentIndexReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(ContentIndexReconcileTask.class);

    private final ElasticsearchContentIndex contentIndex;

    private final ContentIndexUpdater contentIndexUpdater;

    private final NoticeRepository noticeRepository;

    private final MessageRepository messageRepository;

    private final MessageReceiverRepository messageReceiverRepository;

    /**
     * 是否需要执行全量回填，首次成功前保持待回填状态。
     */
    private volatile boolean backfillPending = true;

    @SuppressWarnings("all")
    public ContentIndexReconcileTask(final ElasticsearchContentIndex contentIndex,
                                     final ContentIndexUpdater contentIndexUpdater,
                                     final NoticeRepository noticeRepository,
                                     final MessageRepository messageRepository,
                                     final MessageReceiverRepository messageReceiverRepository) {
        this.contentIndex = contentIndex;
        this.contentIndexUpdater = contentIndexUpdater;
        this.noticeRepository = noticeRepository;
        this.messageRepository = messageRepository;
        this.messageReceiverRepository = messageReceiverRepository;
    }

    /**
     * 执行一轮对账。
     *
     * <p>依次确保索引存在、按需全量回填、重放公告、消息与接收记录的窗口内变更。任何异常都在
     * 此记录，不向调度器抛出，避免任务被 Quartz 标记为失败。
     */
    public void reconcile() {
        try {
            contentIndex.ensureIndex();
            if (backfillPending) {
                backfill();
            }
            reconcileNotices();
            reconcileMessages();
            reconcileReceivers();
        } catch (Exception e) {
            log.warn("内容索引对账失败，将在下一轮重试", e);
        }
    }

    /**
     * 异步触发一次对账，用于应用启动后的全量回填。
     *
     * <p>通过独立 Bean 的代理调用触发 {@link Async}，不阻塞应用启动。
     */
    @Async
    public void reconcileAsync() {
        reconcile();
    }

    /**
     * 全量回填全部已发布公告与已发送消息的接收人文档。
     *
     * @throws java.io.IOException 与 Elasticsearch 通信失败时抛出
     */
    private void backfill() throws java.io.IOException {
        List<ContentDocument> noticeDocuments = new ArrayList<>();
        for (Notice notice : noticeRepository.findAllByCondition(null, null, null)) {
            ContentDocument document = ContentDocumentFactory.fromNotice(notice);
            if (document != null) {
                noticeDocuments.add(document);
            }
        }
        contentIndex.upsertAll(noticeDocuments);

        List<ContentDocument> messageDocuments = new ArrayList<>();
        for (Message message : messageRepository.findAllByCondition(null, null, "1")) {
            for (MessageReceiver receiver : messageReceiverRepository.findByMessageId(message.getId())) {
                ContentDocument document = ContentDocumentFactory.fromMessage(message, receiver.getUserId());
                if (document != null && !isDeleted(receiver.getIsDeleted())) {
                    messageDocuments.add(document);
                }
            }
        }
        contentIndex.upsertAll(messageDocuments);

        backfillPending = false;
        log.info("内容索引全量回填完成，公告 {} 条，消息接收人 {} 条", noticeDocuments.size(), messageDocuments.size());
    }

    /**
     * 重放窗口内的公告变更。
     *
     * @throws java.io.IOException 与 Elasticsearch 通信失败时抛出
     */
    private void reconcileNotices() throws java.io.IOException {
        for (Notice notice : noticeRepository.findChangedSince()) {
            if (isDeleted(notice.getDeleted()) || !notice.isPublished()) {
                contentIndex.delete(ContentDocType.NOTICE, notice.getId(), null);
                continue;
            }
            contentIndexUpdater.refreshNotice(notice.getId());
        }
    }

    /**
     * 重放窗口内的消息变更。
     *
     * @throws java.io.IOException 与 Elasticsearch 通信失败时抛出
     */
    private void reconcileMessages() throws java.io.IOException {
        for (Message message : messageRepository.findChangedSince()) {
            if (isDeleted(message.getDeleted()) || !message.isSent()) {
                contentIndex.deleteBySource(ContentDocType.MESSAGE, message.getId());
                continue;
            }
            contentIndexUpdater.refreshMessage(message.getId());
        }
    }

    /**
     * 重放窗口内的接收记录变更，覆盖用户侧删除。
     *
     * @throws java.io.IOException 与 Elasticsearch 通信失败时抛出
     */
    private void reconcileReceivers() throws java.io.IOException {
        for (MessageReceiver receiver : messageReceiverRepository.findChangedSince()) {
            contentIndexUpdater.refreshMessageReceiver(receiver.getMessageId(), receiver.getUserId(),
                    isDeleted(receiver.getIsDeleted()));
        }
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
