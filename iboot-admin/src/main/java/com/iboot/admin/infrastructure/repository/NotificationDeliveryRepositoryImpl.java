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

package com.iboot.admin.infrastructure.repository;

import com.iboot.admin.domain.notification.model.NotificationChannelType;
import com.iboot.admin.domain.notification.model.NotificationDelivery;
import com.iboot.admin.domain.notification.model.NotificationDeliveryStatus;
import com.iboot.admin.domain.notification.model.NotificationEventType;
import com.iboot.admin.domain.notification.repository.NotificationDeliveryRepository;
import com.iboot.admin.infrastructure.persistence.mapper.NotificationDeliveryMapper;
import com.iboot.admin.infrastructure.persistence.po.NotificationDeliveryPO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 通知投递记录仓储实现。
 *
 * <p>基于 MyBatis Mapper 完成持久化，并在 PO 与领域模型之间转换；不包含业务规则，
 * 事务边界由应用服务控制。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Repository
public class NotificationDeliveryRepositoryImpl implements NotificationDeliveryRepository {

    private final NotificationDeliveryMapper mapper;

    @SuppressWarnings("all")
    public NotificationDeliveryRepositoryImpl(final NotificationDeliveryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean insertIfAbsent(final NotificationDelivery delivery) {
        return mapper.insertIgnore(toPO(delivery)) > 0;
    }

    @Override
    public Optional<NotificationDelivery> findById(final Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<NotificationDelivery> findPage(final String status, final String channel, final String eventType,
                                               final String recipient, final LocalDateTime startTime,
                                               final LocalDateTime endTime, final int offset, final int limit) {
        return mapper.selectPage(status, channel, eventType, recipient, startTime, endTime, offset, limit).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count(final String status, final String channel, final String eventType, final String recipient,
                      final LocalDateTime startTime, final LocalDateTime endTime) {
        return mapper.countPage(status, channel, eventType, recipient, startTime, endTime);
    }

    @Override
    public List<Long> findDueIds(final LocalDateTime now, final int limit) {
        return mapper.selectDueIds(now, limit);
    }

    @Override
    public List<Long> findDispatchableIdsByEventId(final String eventId, final LocalDateTime now) {
        return mapper.selectDispatchableIdsByEventId(eventId, now);
    }

    @Override
    public boolean claim(final Long id, final LocalDateTime now) {
        return mapper.claim(id, now) > 0;
    }

    @Override
    public boolean update(final NotificationDelivery delivery) {
        return mapper.update(toPO(delivery)) > 0;
    }

    @Override
    public int resetStaleSending(final LocalDateTime threshold, final LocalDateTime now) {
        return mapper.resetStaleSending(threshold, now);
    }

    @Override
    public boolean resetForRetry(final Long id, final LocalDateTime now) {
        return mapper.resetForRetry(id, now) > 0;
    }

    private NotificationDelivery toDomain(final NotificationDeliveryPO po) {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setId(po.getId());
        delivery.setEventId(po.getEventId());
        delivery.setEventType(NotificationEventType.from(po.getEventType()));
        delivery.setChannel(NotificationChannelType.from(po.getChannel()));
        delivery.setRecipientKey(po.getRecipientKey());
        delivery.setRecipientId(po.getRecipientId());
        delivery.setRecipientAddress(po.getRecipientAddress());
        delivery.setTemplateCode(po.getTemplateCode());
        delivery.setTitle(po.getTitle());
        delivery.setContent(po.getContent());
        delivery.setParamsJson(po.getParamsJson());
        delivery.setStatus(NotificationDeliveryStatus.valueOf(po.getStatus()));
        delivery.setAttempts(po.getAttempts() == null ? 0 : po.getAttempts());
        delivery.setMaxAttempts(po.getMaxAttempts() == null ? 5 : po.getMaxAttempts());
        delivery.setNextRetryTime(po.getNextRetryTime());
        delivery.setLastError(po.getLastError());
        delivery.setMessageId(po.getMessageId());
        delivery.setSentTime(po.getSentTime());
        delivery.setCreateTime(po.getCreateTime());
        delivery.setUpdateTime(po.getUpdateTime());
        return delivery;
    }

    private NotificationDeliveryPO toPO(final NotificationDelivery delivery) {
        NotificationDeliveryPO po = new NotificationDeliveryPO();
        po.setId(delivery.getId());
        po.setEventId(delivery.getEventId());
        po.setEventType(delivery.getEventType() == null ? null : delivery.getEventType().name());
        po.setChannel(delivery.getChannel() == null ? null : delivery.getChannel().name());
        po.setRecipientKey(delivery.getRecipientKey());
        po.setRecipientId(delivery.getRecipientId());
        po.setRecipientAddress(delivery.getRecipientAddress());
        po.setTemplateCode(delivery.getTemplateCode());
        po.setTitle(delivery.getTitle());
        po.setContent(delivery.getContent());
        po.setParamsJson(delivery.getParamsJson());
        po.setStatus(delivery.getStatus() == null ? null : delivery.getStatus().name());
        po.setAttempts(delivery.getAttempts());
        po.setMaxAttempts(delivery.getMaxAttempts());
        po.setNextRetryTime(delivery.getNextRetryTime());
        po.setLastError(delivery.getLastError());
        po.setMessageId(delivery.getMessageId());
        po.setSentTime(delivery.getSentTime());
        po.setCreateTime(delivery.getCreateTime());
        po.setUpdateTime(delivery.getUpdateTime());
        return po;
    }
}
