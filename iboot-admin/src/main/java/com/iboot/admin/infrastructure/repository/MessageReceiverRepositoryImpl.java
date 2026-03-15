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

import com.iboot.admin.domain.system.model.MessageReceiver;
import com.iboot.admin.domain.system.repository.MessageReceiverRepository;
import com.iboot.admin.infrastructure.persistence.mapper.MessageReceiverMapper;
import com.iboot.admin.infrastructure.persistence.po.MessageReceiverPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息接收记录仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现消息接收记录的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
@RequiredArgsConstructor
public class MessageReceiverRepositoryImpl implements MessageReceiverRepository {

    private final MessageReceiverMapper messageReceiverMapper;

    /**
     * 保存消息接收记录
     *
     * @param receiver 消息接收记录实体
     * @return 是否保存成功
     */
    @Override
    public boolean save(MessageReceiver receiver) {
        MessageReceiverPO po = convertToPO(receiver);
        return messageReceiverMapper.insert(po) > 0;
    }

    /**
     * 批量保存消息接收记录
     *
     * @param receivers 消息接收记录列表
     * @return 是否保存成功
     */
    @Override
    public boolean batchSave(List<MessageReceiver> receivers) {
        if (receivers == null || receivers.isEmpty()) {
            return true;
        }
        List<MessageReceiverPO> poList = receivers.stream()
                .map(this::convertToPO)
                .collect(Collectors.toList());
        return messageReceiverMapper.batchInsert(poList) > 0;
    }

    /**
     * 将消息标记为已读
     *
     * @param messageId 消息 ID
     * @param userId 用户 ID
     * @return 是否标记成功
     */
    @Override
    public boolean markAsRead(Long messageId, Long userId) {
        return messageReceiverMapper.updateRead(messageId, userId) > 0;
    }

    /**
     * 将用户的所有消息标记为已读
     *
     * @param userId 用户 ID
     * @return 是否标记成功
     */
    @Override
    public boolean markAllAsRead(Long userId) {
        return messageReceiverMapper.updateAllRead(userId) >= 0;
    }

    /**
     * 软删除消息（逻辑删除）
     *
     * @param messageId 消息 ID
     * @param userId 用户 ID
     * @return 是否删除成功
     */
    @Override
    public boolean softDelete(Long messageId, Long userId) {
        return messageReceiverMapper.updateDeleted(messageId, userId) > 0;
    }

    /**
     * 检查用户是否收到指定消息
     *
     * @param messageId 消息 ID
     * @param userId 用户 ID
     * @return 是否存在
     */
    @Override
    public boolean existsByMessageIdAndUserId(Long messageId, Long userId) {
        return messageReceiverMapper.countByMessageIdAndUserId(messageId, userId) > 0;
    }

    /**
     * 统计用户未读消息数量
     *
     * @param userId 用户 ID
     * @return 未读消息数量
     */
    @Override
    public long countUnreadByUserId(Long userId) {
        return messageReceiverMapper.countUnreadByUserId(userId);
    }

    /**
     * 分页查询用户的消息列表
     *
     * @param userId 用户 ID
     * @param messageType 消息类型（可选）
     * @param isRead 是否已读（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 消息接收记录列表
     */
    @Override
    public List<MessageReceiver> findPageByUserId(Long userId, String messageType, Integer isRead, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = messageReceiverMapper.selectPageByUserId(userId, messageType, isRead, offset, pageSize);
        return rows.stream().map(this::convertMapToDomain).collect(Collectors.toList());
    }

    /**
     * 按条件统计用户消息数量
     *
     * @param userId 用户 ID
     * @param messageType 消息类型
     * @param isRead 是否已读
     * @return 消息数量
     */
    @Override
    public long countByUserId(Long userId, String messageType, Integer isRead) {
        return messageReceiverMapper.countByUserId(userId, messageType, isRead);
    }

    /**
     * 根据消息 ID 删除接收记录
     *
     * @param messageId 消息 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteByMessageId(Long messageId) {
        return messageReceiverMapper.deleteByMessageId(messageId) >= 0;
    }

    /**
     * 根据消息 ID 查询接收记录列表
     *
     * @param messageId 消息 ID
     * @return 消息接收记录列表
     */
    @Override
    public List<MessageReceiver> findByMessageId(Long messageId) {
        List<Map<String, Object>> rows = messageReceiverMapper.findByMessageId(messageId);
        return rows.stream().map(this::convertMapToDomainWithUserId).collect(Collectors.toList());
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param domain 消息接收记录领域对象
     * @return 消息接收记录持久化对象
     */
    private MessageReceiverPO convertToPO(MessageReceiver domain) {
        MessageReceiverPO po = new MessageReceiverPO();
        po.setId(domain.getId());
        po.setMessageId(domain.getMessageId());
        po.setUserId(domain.getUserId());
        po.setIsRead(domain.getIsRead());
        po.setReadTime(domain.getReadTime());
        po.setIsDeleted(domain.getIsDeleted());
        return po;
    }

    /**
     * 将 Map 数据转换为领域对象
     *
     * @param row 数据库行数据
     * @return 消息接收记录领域对象
     */
    private MessageReceiver convertMapToDomain(Map<String, Object> row) {
        MessageReceiver receiver = new MessageReceiver();
        receiver.setMessageId(toLong(row.get("id")));
        receiver.setUserId(null);
        receiver.setIsRead(toInt(row.get("isRead")));
        receiver.setReadTime(row.get("readTime") != null ? (LocalDateTime) row.get("readTime") : null);
        return receiver;
    }

    /**
     * 将 Map 数据转换为领域对象（带用户 ID）
     *
     * @param row 数据库行数据
     * @return 消息接收记录领域对象
     */
    private MessageReceiver convertMapToDomainWithUserId(Map<String, Object> row) {
        MessageReceiver receiver = new MessageReceiver();
        receiver.setId(toLong(row.get("id")));
        receiver.setMessageId(toLong(row.get("message_id")));
        receiver.setUserId(toLong(row.get("user_id")));
        receiver.setIsRead(toInt(row.get("is_read")));
        receiver.setReadTime(row.get("read_time") != null ? (LocalDateTime) row.get("read_time") : null);
        receiver.setIsDeleted(toInt(row.get("is_deleted")));
        return receiver;
    }

    /**
     * 将对象转换为 Long 类型
     *
     * @param val 待转换的对象
     * @return Long 值
     */
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        return Long.valueOf(val.toString());
    }

    /**
     * 将对象转换为 Integer 类型
     *
     * @param val 待转换的对象
     * @return Integer 值
     */
    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        return Integer.valueOf(val.toString());
    }
}
