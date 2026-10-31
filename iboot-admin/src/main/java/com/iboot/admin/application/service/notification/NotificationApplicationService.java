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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.admin.application.service.MessageTemplateApplicationService;
import com.iboot.kernel.web.BusinessException;
import com.iboot.admin.domain.notification.model.NotificationChannelType;
import com.iboot.admin.domain.notification.model.NotificationDelivery;
import com.iboot.admin.domain.notification.model.NotificationDeliveryStatus;
import com.iboot.admin.domain.notification.model.NotificationSendResult;
import com.iboot.admin.domain.notification.repository.NotificationDeliveryRepository;
import com.iboot.admin.domain.notification.service.NotificationChannel;
import com.iboot.admin.domain.notification.service.NotificationDeliveryException;
import com.iboot.admin.domain.system.model.MessageTemplate;
import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.domain.system.repository.UserRepository;
import com.iboot.admin.infrastructure.notification.NotificationProperties;
import com.iboot.admin.infrastructure.notification.NotificationSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通知编排应用服务。
 *
 * <p>负责把业务事件拆成多渠道投递记录、提交后即时发送、失败重试与人工重试。
 * 登记与业务事务同提交；发送在事务提交后执行，避免外部 I/O 拖长业务事务。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class NotificationApplicationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationApplicationService.class);

    private static final String GROUP_RECIPIENT_KEY = "GROUP:default";

    private final NotificationDeliveryRepository deliveryRepository;

    private final List<NotificationChannel> channels;

    private final UserRepository userRepository;

    private final MessageTemplateApplicationService templateService;

    private final ApplicationEventPublisher eventPublisher;

    private final NotificationProperties properties;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    @SuppressWarnings("all")
    public NotificationApplicationService(final NotificationDeliveryRepository deliveryRepository,
                                          final List<NotificationChannel> channels,
                                          final UserRepository userRepository,
                                          final MessageTemplateApplicationService templateService,
                                          final ApplicationEventPublisher eventPublisher,
                                          final NotificationProperties properties,
                                          final ObjectMapper objectMapper,
                                          final Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.channels = channels;
        this.userRepository = userRepository;
        this.templateService = templateService;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * 登记一次通知事件。
     *
     * <p>在同一事务内为每个启用渠道插入投递记录；接收地址缺失的渠道直接标记为
     * {@code SKIPPED}。事务提交后由监听器触发即时投递。
     *
     * @param command 通知登记命令
     * @return 业务事件 ID
     * @throws BusinessException 模板不存在/停用、接收人不存在或参数序列化失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public String enqueue(final NotificationCommand command) {
        validate(command);
        MessageTemplate template = templateService.getTemplateByCode(command.getTemplateCode());
        if (!template.isEnabled()) {
            throw new BusinessException("消息模板已停用");
        }
        User recipient = userRepository.findById(command.getRecipientId())
                .orElseThrow(() -> new BusinessException(400, "通知接收人不存在"));

        LocalDateTime now = LocalDateTime.now(clock);
        String content = template.fillContent(command.getParams());
        String paramsJson = toJson(command.getParams());
        int maxAttempts = properties.getRetry().normalizedMaxAttempts();

        List<NotificationDelivery> deliveries = new ArrayList<>();
        for (NotificationChannel channel : channels) {
            if (!channel.isEnabled()) {
                continue;
            }
            NotificationDelivery delivery = buildDelivery(command, template, content, paramsJson, recipient, now,
                    maxAttempts, channel.channelType());
            if (requiresAddress(channel.channelType()) && isBlank(delivery.getRecipientAddress())) {
                delivery.markSkipped(channel.channelType() == NotificationChannelType.EMAIL
                        ? "接收地址为空（邮箱）" : "接收地址为空（手机号）");
            }
            deliveries.add(delivery);
        }

        for (NotificationDelivery delivery : deliveries) {
            deliveryRepository.insertIfAbsent(delivery);
        }
        if (!deliveries.isEmpty()) {
            eventPublisher.publishEvent(new NotificationQueuedEvent(this, command.getEventId()));
        }
        log.info("通知事件已登记，eventId: {}, 渠道数: {}", command.getEventId(), deliveries.size());
        return command.getEventId();
    }

    /**
     * 投递单条记录。
     *
     * <p>先条件领取为 {@code SENDING}，再调用渠道；成功后记录消息 ID 与发送时间，
     * 失败按可重试标志进入退避或终止。
     *
     * @param deliveryId 投递记录 ID
     */
    public void dispatch(final Long deliveryId) {
        if (deliveryId == null) {
            return;
        }
        Optional<NotificationDelivery> found = deliveryRepository.findById(deliveryId);
        if (found.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!deliveryRepository.claim(deliveryId, now)) {
            return;
        }
        NotificationDelivery delivery = found.get();
        delivery.setStatus(NotificationDeliveryStatus.SENDING);
        delivery.setUpdateTime(now);

        NotificationChannel channel = channelFor(delivery.getChannel());
        if (channel == null || !channel.isEnabled()) {
            delivery.markSkipped("渠道未启用");
            deliveryRepository.update(delivery);
            return;
        }
        if (requiresAddress(delivery.getChannel()) && isBlank(delivery.getRecipientAddress())) {
            delivery.markSkipped("接收地址为空");
            deliveryRepository.update(delivery);
            return;
        }

        try {
            NotificationSendResult result = channel.send(delivery);
            delivery.markSuccess(result == null ? null : result.messageId(), now);
            deliveryRepository.update(delivery);
            log.info("通知投递成功，deliveryId: {}, channel: {}", deliveryId, delivery.getChannel());
        } catch (NotificationDeliveryException e) {
            handleFailure(delivery, e.isRetryable(), e.getMessage(), now);
        } catch (Exception e) {
            handleFailure(delivery, true, e.getMessage(), now);
        }
    }

    /**
     * 投递某个事件下所有到期的记录。
     *
     * @param eventId 事件 ID
     */
    public void dispatchEvent(final String eventId) {
        if (isBlank(eventId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long id : deliveryRepository.findDispatchableIdsByEventId(eventId, now)) {
            dispatch(id);
        }
    }

    /**
     * 管理员人工重试。
     *
     * <p>重置是一条独立的条件更新，不包在长事务中；重置完成后立即发布异步投递事件。
     *
     * @param deliveryId 投递记录 ID
     * @throws BusinessException 记录不存在、状态不允许或状态已变化时抛出
     */
    public void retryManually(final Long deliveryId) {
        NotificationDelivery delivery = getById(deliveryId);
        if (!delivery.getStatus().canRetryManually()) {
            throw new BusinessException(409, "当前投递状态不允许重试");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!deliveryRepository.resetForRetry(deliveryId, now)) {
            throw new BusinessException(409, "投递状态已变化，请刷新后重试");
        }
        eventPublisher.publishEvent(new NotificationRetryRequestedEvent(this, deliveryId));
    }

    /**
     * 按 ID 查询投递记录。
     *
     * @param deliveryId 记录 ID
     * @return 投递记录
     * @throws BusinessException 记录不存在时抛出
     */
    public NotificationDelivery getById(final Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(404, "投递记录不存在"));
    }

    /**
     * 分页查询投递记录。
     *
     * @param status    状态筛选，可为 {@code null}
     * @param channel   渠道筛选，可为 {@code null}
     * @param eventType 事件类型筛选，可为 {@code null}
     * @param recipient 接收人关键字，可为 {@code null}
     * @param startTime 创建时间下界，可为 {@code null}
     * @param endTime   创建时间上界，可为 {@code null}
     * @param pageNum   页码，从 1 开始
     * @param pageSize  每页数量
     * @return 当前页记录
     */
    public List<NotificationDelivery> findPage(final String status, final String channel, final String eventType,
                                               final String recipient, final LocalDateTime startTime,
                                               final LocalDateTime endTime, final int pageNum, final int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePageNum - 1) * safePageSize;
        return deliveryRepository.findPage(normalize(status), normalize(channel), normalize(eventType), recipient,
                startTime, endTime, offset, safePageSize);
    }

    /**
     * 统计投递记录数。
     *
     * @param status    状态筛选，可为 {@code null}
     * @param channel   渠道筛选，可为 {@code null}
     * @param eventType 事件类型筛选，可为 {@code null}
     * @param recipient 接收人关键字，可为 {@code null}
     * @param startTime 创建时间下界，可为 {@code null}
     * @param endTime   创建时间上界，可为 {@code null}
     * @return 记录数
     */
    public long count(final String status, final String channel, final String eventType, final String recipient,
                      final LocalDateTime startTime, final LocalDateTime endTime) {
        return deliveryRepository.count(normalize(status), normalize(channel), normalize(eventType), recipient,
                startTime, endTime);
    }

    private void validate(final NotificationCommand command) {
        if (command == null) {
            throw new BusinessException(400, "通知命令不能为空");
        }
        if (isBlank(command.getEventId())) {
            throw new BusinessException(400, "通知事件 ID 不能为空");
        }
        if (command.getEventType() == null) {
            throw new BusinessException(400, "通知事件类型不能为空");
        }
        if (isBlank(command.getTemplateCode())) {
            throw new BusinessException(400, "通知模板编码不能为空");
        }
        if (command.getRecipientId() == null) {
            throw new BusinessException(400, "通知接收人不能为空");
        }
    }

    private NotificationDelivery buildDelivery(final NotificationCommand command, final MessageTemplate template,
                                               final String content, final String paramsJson, final User recipient,
                                               final LocalDateTime now, final int maxAttempts,
                                               final NotificationChannelType channelType) {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setEventId(command.getEventId());
        delivery.setEventType(command.getEventType());
        delivery.setChannel(channelType);
        delivery.setTemplateCode(command.getTemplateCode());
        delivery.setTitle(template.getTemplateName());
        delivery.setContent(content);
        delivery.setParamsJson(paramsJson);
        delivery.setStatus(NotificationDeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setMaxAttempts(maxAttempts);
        delivery.setCreateTime(now);
        delivery.setUpdateTime(now);
        if (channelType == NotificationChannelType.DINGTALK) {
            delivery.setRecipientKey(GROUP_RECIPIENT_KEY);
        } else {
            delivery.setRecipientKey("USER:" + recipient.getId());
            delivery.setRecipientId(recipient.getId());
            if (channelType == NotificationChannelType.EMAIL) {
                delivery.setRecipientAddress(recipient.getEmail());
            } else if (channelType == NotificationChannelType.SMS) {
                delivery.setRecipientAddress(recipient.getPhone());
            }
        }
        return delivery;
    }

    private void handleFailure(final NotificationDelivery delivery, final boolean retryable, final String error,
                               final LocalDateTime now) {
        String sanitized = NotificationSanitizer.sanitizeError(error);
        int maxAttempts = Math.max(delivery.getMaxAttempts(), 1);
        if (retryable) {
            delivery.markFailed(sanitized, maxAttempts, properties.getRetry().normalizedBackoffSeconds(), now);
        } else {
            delivery.markDead(sanitized, now);
        }
        deliveryRepository.update(delivery);
        log.warn("通知投递失败，deliveryId: {}, channel: {}, status: {}, error: {}", delivery.getId(),
                delivery.getChannel(), delivery.getStatus(), sanitized);
    }

    private NotificationChannel channelFor(final NotificationChannelType type) {
        if (type == null) {
            return null;
        }
        return channels.stream().filter(channel -> channel.channelType() == type).findFirst().orElse(null);
    }

    private boolean requiresAddress(final NotificationChannelType type) {
        return type == NotificationChannelType.EMAIL || type == NotificationChannelType.SMS;
    }

    private String toJson(final Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "通知参数序列化失败", e);
        }
    }

    private String normalize(final String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
