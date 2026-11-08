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

package com.iboot.workflow.infrastructure.repository;

import com.iboot.workflow.domain.notification.model.NotificationOutboxEvent;
import com.iboot.workflow.domain.notification.model.NotificationOutboxStatus;
import com.iboot.workflow.domain.notification.repository.NotificationOutboxRepository;
import com.iboot.workflow.infrastructure.persistence.mapper.NotificationOutboxMapper;
import com.iboot.workflow.infrastructure.persistence.po.NotificationOutboxPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知 outbox 仓储实现。
 *
 * <p>基于 MyBatis Mapper 完成持久化与 PO/领域模型转换。{@link #claimBatch(int, LocalDateTime)}
 * 是唯一的写事务边界：在一个短事务内锁定并标记待发事件，提交后由中继在事务外发送。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Repository
public class NotificationOutboxRepositoryImpl implements NotificationOutboxRepository {

    /**
     * 错误摘要最大长度，与数据库列保持一致。
     */
    private static final int MAX_ERROR_LENGTH = 1000;

    private final NotificationOutboxMapper mapper;

    @SuppressWarnings("all")
    public NotificationOutboxRepositoryImpl(final NotificationOutboxMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public NotificationOutboxEvent save(final NotificationOutboxEvent event) {
        NotificationOutboxPO po = toPO(event);
        mapper.insert(po);
        event.setId(po.getId());
        return event;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<NotificationOutboxEvent> claimBatch(final int batchSize, final LocalDateTime now) {
        List<NotificationOutboxPO> candidates = mapper.selectClaimable(batchSize, now);
        List<NotificationOutboxEvent> claimed = new ArrayList<>(candidates.size());
        for (NotificationOutboxPO candidate : candidates) {
            if (mapper.markSending(candidate.getId(), now) > 0) {
                candidate.setStatus(NotificationOutboxStatus.SENDING.name());
                candidate.setUpdateTime(now);
                claimed.add(toDomain(candidate));
            }
        }
        return claimed;
    }

    @Override
    public boolean markSent(final Long id, final String messageId, final LocalDateTime now) {
        return mapper.markSent(id, messageId, now) > 0;
    }

    @Override
    public boolean markRetry(final Long id, final int attempts, final LocalDateTime nextRetryTime,
                             final String lastError, final LocalDateTime now) {
        return mapper.markRetry(id, attempts, nextRetryTime, truncate(lastError), now) > 0;
    }

    @Override
    public boolean markDead(final Long id, final int attempts, final String lastError,
                            final LocalDateTime now) {
        return mapper.markDead(id, attempts, truncate(lastError), now) > 0;
    }

    @Override
    public int resetStaleSending(final LocalDateTime threshold, final LocalDateTime now) {
        return mapper.resetStaleSending(threshold, now);
    }

    @Override
    public long countByStatus(final NotificationOutboxStatus status) {
        return mapper.countByStatus(status.name());
    }

    @Override
    public int deleteSentBefore(final LocalDateTime threshold) {
        return mapper.deleteSentBefore(threshold);
    }

    private String truncate(final String error) {
        if (error == null || error.length() <= MAX_ERROR_LENGTH) {
            return error;
        }
        return error.substring(0, MAX_ERROR_LENGTH);
    }

    private NotificationOutboxPO toPO(final NotificationOutboxEvent event) {
        NotificationOutboxPO po = new NotificationOutboxPO();
        po.setId(event.getId());
        po.setEventId(event.getEventId());
        po.setEventType(event.getEventType());
        po.setPayload(event.getPayload());
        po.setStatus(event.getStatus() == null ? null : event.getStatus().name());
        po.setAttempts(event.getAttempts());
        po.setMaxAttempts(event.getMaxAttempts());
        po.setNextRetryTime(event.getNextRetryTime());
        po.setLastError(event.getLastError());
        po.setMessageId(event.getMessageId());
        po.setCreateTime(event.getCreateTime());
        po.setUpdateTime(event.getUpdateTime());
        return po;
    }

    private NotificationOutboxEvent toDomain(final NotificationOutboxPO po) {
        NotificationOutboxEvent event = new NotificationOutboxEvent();
        event.setId(po.getId());
        event.setEventId(po.getEventId());
        event.setEventType(po.getEventType());
        event.setPayload(po.getPayload());
        event.setStatus(po.getStatus() == null ? null : NotificationOutboxStatus.valueOf(po.getStatus()));
        event.setAttempts(po.getAttempts());
        event.setMaxAttempts(po.getMaxAttempts());
        event.setNextRetryTime(po.getNextRetryTime());
        event.setLastError(po.getLastError());
        event.setMessageId(po.getMessageId());
        event.setCreateTime(po.getCreateTime());
        event.setUpdateTime(po.getUpdateTime());
        return event;
    }
}
