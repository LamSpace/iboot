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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 内容索引事件监听器。
 *
 * <p>在业务事务提交后异步执行，避免索引未提交的数据；索引失败只记录日志，不影响业务结果，
 * 由 {@link ContentIndexReconcileTask} 的定时对账在 5 分钟内补齐。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see ContentIndexEvent
 * @see ContentIndexUpdater
 */
@Component
public class ContentIndexListener {

    private static final Logger log = LoggerFactory.getLogger(ContentIndexListener.class);

    private final ContentIndexUpdater contentIndexUpdater;

    private final ElasticsearchContentIndex contentIndex;

    @SuppressWarnings("all")
    public ContentIndexListener(final ContentIndexUpdater contentIndexUpdater,
                                final ElasticsearchContentIndex contentIndex) {
        this.contentIndexUpdater = contentIndexUpdater;
        this.contentIndex = contentIndex;
    }

    /**
     * 消费内容索引事件。
     *
     * <p>任何异常都在此捕获并记录，不向业务调用方传播；重复消费不会产生重复文档。
     *
     * @param event 内容索引事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(final ContentIndexEvent event) {
        try {
            switch (event.getAction()) {
                case REFRESH_SOURCE -> refresh(event);
                case DELETE_SOURCE -> contentIndex.deleteBySource(event.getDocType(), event.getSourceId());
                case DELETE_RECEIVER -> contentIndex.delete(ContentDocType.MESSAGE, event.getSourceId(),
                        event.getReceiverId());
            }
        } catch (Exception e) {
            log.warn("内容索引更新失败，type: {}, action: {}, sourceId: {}, receiverId: {}", event.getDocType(),
                    event.getAction(), event.getSourceId(), event.getReceiverId(), e);
        }
    }

    /**
     * 按事件类型刷新索引。
     *
     * @param event 内容索引事件
     *
     * @throws java.io.IOException 与 Elasticsearch 通信失败时抛出
     */
    private void refresh(final ContentIndexEvent event) throws java.io.IOException {
        if (event.getDocType() == ContentDocType.NOTICE) {
            contentIndexUpdater.refreshNotice(event.getSourceId());
            return;
        }
        contentIndexUpdater.refreshMessage(event.getSourceId());
    }

}
