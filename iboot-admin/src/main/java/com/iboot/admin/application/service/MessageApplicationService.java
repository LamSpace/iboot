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

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.system.model.Message;
import com.iboot.admin.domain.system.model.MessageReceiver;
import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.domain.system.repository.MessageReceiverRepository;
import com.iboot.admin.domain.system.repository.MessageRepository;
import com.iboot.admin.domain.system.repository.UserRepository;
import com.iboot.admin.infrastructure.push.MessageReadEvent;
import com.iboot.admin.infrastructure.push.MessageSentEvent;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息应用服务
 * <p>
 * 负责站内消息的创建、编辑、发送、撤回、删除和查询等业务逻辑处理。
 * 支持消息状态管理（草稿/已发送）、优先级控制、收件箱管理等功能。
 * 消息最大显示数量通过 sys.message.maxDisplay 配置动态读取。
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageApplicationService {

    private final MessageRepository messageRepository;
    private final MessageReceiverRepository messageReceiverRepository;
    private final UserRepository userRepository;
    private final ConfigApplicationService configApplicationService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String CONFIG_KEY_MAX_DISPLAY = "sys.message.maxDisplay";
    private static final int DEFAULT_MAX_DISPLAY = 50;

    // ==================== 后台管理方法 ====================

    /**
     * 创建消息（默认草稿状态）
     * <p>
     * 创建消息时默认状态为草稿（status="0"），发送者类型为系统（senderType="1"）。
     * 优先级未指定时默认为普通（priority="0"）。
     * </p>
     *
     * @param message 消息实体
     * @return 创建后的消息实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Message createMessage(Message message) {
        message.setStatus("0");
        message.setSenderType("1");
        message.setSenderId(SecurityUtils.getCurrentUserId());
        message.setCreateBy(SecurityUtils.getCurrentUsername());
        message.setCreateTime(LocalDateTime.now());
        if (message.getPriority() == null) {
            message.setPriority("0");
        }
        return messageRepository.save(message);
    }

    /**
     * 更新消息（仅草稿可修改）
     * <p>
     * 只有草稿状态的消息可以修改，已发送的消息需先撤回才能修改。
     * </p>
     *
     * @param message 消息实体
     * @return 是否更新成功
     * @throws BusinessException 当消息不存在或不是草稿状态时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMessage(Message message) {
        Message existing = messageRepository.findById(message.getId())
                .orElseThrow(() -> new BusinessException("消息不存在"));

        if (!existing.isDraft()) {
            throw new BusinessException("只有草稿状态的消息才能修改");
        }

        message.setUpdateBy(SecurityUtils.getCurrentUsername());
        message.setUpdateTime(LocalDateTime.now());
        return messageRepository.update(message);
    }

    /**
     * 删除消息（仅草稿可删除）
     * <p>
     * 只有草稿状态的消息可以删除，已发送的消息需先撤回。
     * 删除时会同时删除所有接收记录。
     * </p>
     *
     * @param id 消息 ID
     * @return 是否删除成功
     * @throws BusinessException 当消息不存在或不是草稿状态时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMessage(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new BusinessException("消息不存在"));

        if (!message.isDraft()) {
            throw new BusinessException("只有草稿状态的消息才能删除");
        }

        messageReceiverRepository.deleteByMessageId(id);
        return messageRepository.deleteById(id);
    }

    /**
     * 发送消息
     * <p>
     * 将草稿状态的消息发送给指定用户。
     * 接收类型为"全部用户"时自动获取所有用户列表，为"指定用户"时需要提供 userIds 参数。
     * 发送成功后会批量创建接收记录，并更新消息状态为已发送。
     * </p>
     *
     * @param messageId 消息 ID
     * @param userIds   指定用户 ID 列表（接收类型为"指定用户"时需要）
     * @return 是否发送成功
     * @throws BusinessException 当消息不存在、不是草稿状态或没有可发送目标用户时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean sendMessage(Long messageId, List<Long> userIds) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("消息不存在"));

        if (!message.isDraft()) {
            throw new BusinessException("只有草稿状态的消息才能发送");
        }

        // 确定接收用户列表
        List<Long> targetUserIds;
        if ("0".equals(message.getReceiverType())) {
            // 全部用户
            targetUserIds = userRepository.findAll().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
        } else {
            // 指定用户
            if (userIds == null || userIds.isEmpty()) {
                throw new BusinessException("请选择接收用户");
            }
            targetUserIds = userIds;
        }

        if (targetUserIds.isEmpty()) {
            throw new BusinessException("没有可发送的目标用户");
        }

        // 批量创建接收记录
        List<MessageReceiver> receivers = targetUserIds.stream()
                .map(userId -> MessageReceiver.builder()
                        .messageId(messageId)
                        .userId(userId)
                        .isRead(0)
                        .isDeleted(0)
                        .build())
                .collect(Collectors.toList());

        messageReceiverRepository.batchSave(receivers);

        // 更新消息状态为已发送
        message.send();
        message.setUpdateBy(SecurityUtils.getCurrentUsername());
        message.setUpdateTime(LocalDateTime.now());
        boolean success = messageRepository.update(message);

        // 发布消息发送事件（触发推送通知）
        if (success) {
            MessageSentEvent sentEvent = new MessageSentEvent(
                    this,
                    messageId,
                    message.getTitle(),
                    message.getContent(),
                    message.getMessageType(),
                    message.getPriority(),
                    message.getSenderId()
            );
            eventPublisher.publishEvent(sentEvent);
            log.info("消息发送事件已发布，messageId: {}", messageId);
        }

        return success;
    }

    /**
     * 撤回消息
     * <p>
     * 将已发送的消息撤回，撤回后会删除所有接收记录。
     * 只有已发送状态的消息可以撤回。
     * </p>
     *
     * @param id 消息 ID
     * @return 是否撤回成功
     * @throws BusinessException 当消息不存在或不是已发送状态时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeMessage(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new BusinessException("消息不存在"));

        if (!message.isSent()) {
            throw new BusinessException("只有已发送的消息才能撤回");
        }

        // 删除所有接收记录
        messageReceiverRepository.deleteByMessageId(id);

        message.revoke();
        message.setUpdateBy(SecurityUtils.getCurrentUsername());
        message.setUpdateTime(LocalDateTime.now());
        return messageRepository.update(message);
    }

    /**
     * 根据 ID 获取消息
     *
     * @param id 消息 ID
     * @return 消息实体
     * @throws BusinessException 当消息不存在时抛出
     */
    public Message getMessageById(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new BusinessException("消息不存在"));
    }

    /**
     * 分页查询消息列表（管理端）
     * <p>
     * 支持按消息标题、类型和状态进行条件查询。
     * </p>
     *
     * @param title 消息标题（可选）
     * @param messageType 消息类型（可选）
     * @param status 状态（可选）
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 消息列表
     */
    public List<Message> getMessagePage(String title, String messageType, String status, int pageNum, int pageSize) {
        return messageRepository.findPageByCondition(title, messageType, status, pageNum, pageSize);
    }

    /**
     * 统计消息数量（管理端）
     * <p>
     * 支持按消息标题、类型和状态进行条件统计。
     * </p>
     *
     * @param title 消息标题（可选）
     * @param messageType 消息类型（可选）
     * @param status 状态（可选）
     * @return 消息总数
     */
    public long countMessages(String title, String messageType, String status) {
        return messageRepository.countByCondition(title, messageType, status);
    }

    /**
     * 按条件获取所有消息（导出用）
     * <p>
     * 不分页获取所有符合条件的消息，用于数据导出。
     * </p>
     *
     * @param title 消息标题（可选）
     * @param messageType 消息类型（可选）
     * @param status 状态（可选）
     * @return 消息列表
     */
    public List<Message> getAllMessagesByCondition(String title, String messageType, String status) {
        return messageRepository.findAllByCondition(title, messageType, status);
    }

    // ==================== 用户端方法 ====================

    /**
     * 获取用户收件箱（分页）
     * <p>
     * 获取指定用户的消息收件箱，支持按消息类型和已读状态筛选。
     * </p>
     *
     * @param userId 用户 ID
     * @param messageType 消息类型（可选）
     * @param isRead 已读状态（可选，0-未读，1-已读）
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 消息接收记录列表
     */
    public List<MessageReceiver> getUserMessages(Long userId, String messageType, Integer isRead, int pageNum, int pageSize) {
        return messageReceiverRepository.findPageByUserId(userId, messageType, isRead, pageNum, pageSize);
    }

    /**
     * 统计用户消息数量
     * <p>
     * 统计指定用户的消息数量，支持按消息类型和已读状态筛选。
     * </p>
     *
     * @param userId 用户 ID
     * @param messageType 消息类型（可选）
     * @param isRead 已读状态（可选，0-未读，1-已读）
     * @return 消息总数
     */
    public long countUserMessages(Long userId, String messageType, Integer isRead) {
        return messageReceiverRepository.countByUserId(userId, messageType, isRead);
    }

    /**
     * 获取用户未读消息数量
     *
     * @param userId 用户 ID
     * @return 未读消息数量
     */
    public long getUnreadCount(Long userId) {
        return messageReceiverRepository.countUnreadByUserId(userId);
    }

    /**
     * 标记消息已读
     * <p>
     * 将指定消息标记为已读状态。
     * </p>
     *
     * @param messageId 消息 ID
     * @param userId 用户 ID
     */
    public void markAsRead(Long messageId, Long userId) {
        messageReceiverRepository.markAsRead(messageId, userId);

        // 获取消息发送者 ID（用于推送已读通知）
        Message message = messageRepository.findById(messageId).orElse(null);
        Long senderId = message != null ? message.getSenderId() : null;

        // 发布消息已读事件
        MessageReadEvent readEvent = new MessageReadEvent(this, messageId, userId, senderId);
        eventPublisher.publishEvent(readEvent);
        log.debug("消息已读事件已发布，messageId: {}, userId: {}, senderId: {}", messageId, userId, senderId);
    }

    /**
     * 全部标记已读
     * <p>
     * 将用户的所有消息标记为已读状态。
     * </p>
     *
     * @param userId 用户 ID
     */
    public void markAllAsRead(Long userId) {
        messageReceiverRepository.markAllAsRead(userId);
    }

    /**
     * 用户删除消息（软删除）
     * <p>
     * 用户从收件箱删除消息，采用软删除方式（isDeleted=1），不影响其他用户的接收记录。
     * </p>
     *
     * @param messageId 消息 ID
     * @param userId 用户 ID
     */
    public void deleteUserMessage(Long messageId, Long userId) {
        messageReceiverRepository.softDelete(messageId, userId);
    }

    // ==================== 私有方法 ====================

    /**
     * 从系统配置获取整数值，若配置不存在或格式错误则使用默认值
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private int getConfigIntValue(String configKey, int defaultValue) {
        String value = configApplicationService.getConfigValue(configKey);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("配置项 {} 的值 {} 不是有效的整数，使用默认值 {}", configKey, value, defaultValue);
            }
        }
        return defaultValue;
    }
}
