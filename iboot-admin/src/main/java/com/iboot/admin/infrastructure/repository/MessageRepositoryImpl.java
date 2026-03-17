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

import com.iboot.admin.domain.system.model.Message;
import com.iboot.admin.domain.system.repository.MessageRepository;
import com.iboot.admin.infrastructure.persistence.mapper.MessageMapper;
import com.iboot.admin.infrastructure.persistence.po.MessagePO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 消息仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现系统消息数据的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
public class MessageRepositoryImpl implements MessageRepository {

    private final MessageMapper messageMapper;

    @SuppressWarnings("all")
    public MessageRepositoryImpl(final MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 保存消息
     * <p>
     * 插入新消息记录并设置生成的 ID
     * </p>
     *
     * @param message 消息实体
     *
     * @return 保存后的消息
     */
    @Override
    public Message save(Message message) {
        MessagePO po = convertToPO(message);
        messageMapper.insert(po);
        message.setId(po.getId());
        return message;
    }

    /**
     * 更新消息
     *
     * @param message 消息实体
     *
     * @return 是否更新成功
     */
    @Override
    public boolean update(Message message) {
        MessagePO po = convertToPO(message);
        return messageMapper.update(po) > 0;
    }

    /**
     * 根据 ID 删除消息（逻辑删除）
     *
     * @param id 消息 ID
     *
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return messageMapper.deleteById(id) > 0;
    }

    /**
     * 根据 ID 查询消息
     *
     * @param id 消息 ID
     *
     * @return 消息实体，不存在则返回空
     */
    @Override
    public Optional<Message> findById(Long id) {
        MessagePO po = messageMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 按条件分页查询消息
     *
     * @param title       消息标题（可选）
     * @param messageType 消息类型（可选）
     * @param status      状态（可选）
     * @param pageNum     页码
     * @param pageSize    每页数量
     *
     * @return 消息列表
     */
    @Override
    public List<Message> findPageByCondition(String title, String messageType, String status, int pageNum,
                                             int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return messageMapper.selectPageByCondition(title, messageType, status, offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 按条件统计消息总数
     *
     * @param title       消息标题（可选）
     * @param messageType 消息类型（可选）
     * @param status      状态（可选）
     *
     * @return 消息总数
     */
    @Override
    public long countByCondition(String title, String messageType, String status) {
        return messageMapper.countByCondition(title, messageType, status);
    }

    /**
     * 按条件查询所有消息（不分页，用于导出）
     *
     * @param title       消息标题（可选）
     * @param messageType 消息类型（可选）
     * @param status      状态（可选）
     *
     * @return 消息列表
     */
    @Override
    public List<Message> findAllByCondition(String title, String messageType, String status) {
        return messageMapper.selectAllByCondition(title, messageType, status)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 消息持久化对象
     *
     * @return 消息领域对象
     */
    private Message convertToDomain(MessagePO po) {
        return Message.builder()
                .id(po.getId())
                .title(po.getTitle())
                .content(po.getContent())
                .messageType(po.getMessageType())
                .priority(po.getPriority())
                .senderType(po.getSenderType())
                .senderId(po.getSenderId())
                .receiverType(po.getReceiverType())
                .status(po.getStatus())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .updateBy(po.getUpdateBy())
                .updateTime(po.getUpdateTime())
                .deleted(po.getDeleted())
                .remark(po.getRemark())
                .build();
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param domain 消息领域对象
     *
     * @return 消息持久化对象
     */
    private MessagePO convertToPO(Message domain) {
        MessagePO po = new MessagePO();
        po.setId(domain.getId());
        po.setTitle(domain.getTitle());
        po.setContent(domain.getContent());
        po.setMessageType(domain.getMessageType());
        po.setPriority(domain.getPriority());
        po.setSenderType(domain.getSenderType());
        po.setSenderId(domain.getSenderId());
        po.setReceiverType(domain.getReceiverType());
        po.setStatus(domain.getStatus());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        po.setDeleted(domain.getDeleted());
        po.setRemark(domain.getRemark());
        return po;
    }

}
