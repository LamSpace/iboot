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

import com.iboot.admin.domain.system.model.MessageTemplate;
import com.iboot.admin.domain.system.repository.MessageTemplateRepository;
import com.iboot.admin.infrastructure.persistence.mapper.MessageTemplateMapper;
import com.iboot.admin.infrastructure.persistence.po.MessageTemplatePO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 消息模板仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现消息模板的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
public class MessageTemplateRepositoryImpl implements MessageTemplateRepository {

    private final MessageTemplateMapper messageTemplateMapper;

    @SuppressWarnings("all")
    public MessageTemplateRepositoryImpl(final MessageTemplateMapper messageTemplateMapper) {
        this.messageTemplateMapper = messageTemplateMapper;
    }

    /**
     * 保存消息模板
     *
     * @param template 消息模板实体
     *
     * @return 保存后的消息模板
     */
    @Override
    public MessageTemplate save(MessageTemplate template) {
        MessageTemplatePO po = convertToPO(template);
        messageTemplateMapper.insert(po);
        template.setId(po.getId());
        return template;
    }

    /**
     * 更新消息模板
     *
     * @param template 消息模板实体
     *
     * @return 是否更新成功
     */
    @Override
    public boolean update(MessageTemplate template) {
        MessageTemplatePO po = convertToPO(template);
        return messageTemplateMapper.update(po) > 0;
    }

    /**
     * 根据 ID 删除消息模板（逻辑删除）
     *
     * @param id 消息模板 ID
     *
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return messageTemplateMapper.deleteById(id) > 0;
    }

    /**
     * 根据 ID 查询消息模板
     *
     * @param id 消息模板 ID
     *
     * @return 消息模板实体，不存在则返回空
     */
    @Override
    public Optional<MessageTemplate> findById(Long id) {
        MessageTemplatePO po = messageTemplateMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 根据模板编码查询消息模板
     *
     * @param templateCode 模板编码
     *
     * @return 消息模板实体，不存在则返回空
     */
    @Override
    public Optional<MessageTemplate> findByTemplateCode(String templateCode) {
        MessageTemplatePO po = messageTemplateMapper.selectByTemplateCode(templateCode);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 按条件分页查询消息模板
     *
     * @param templateName 模板名称（可选）
     * @param messageType  消息类型（可选）
     * @param status       状态（可选）
     * @param pageNum      页码
     * @param pageSize     每页数量
     *
     * @return 消息模板列表
     */
    @Override
    public List<MessageTemplate> findPageByCondition(String templateName, String messageType, Integer status,
                                                     int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return messageTemplateMapper.selectPageByCondition(templateName, messageType, status, offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 按条件统计消息模板数量
     *
     * @param templateName 模板名称（可选）
     * @param messageType  消息类型（可选）
     * @param status       状态（可选）
     *
     * @return 消息模板数量
     */
    @Override
    public long countByCondition(String templateName, String messageType, Integer status) {
        return messageTemplateMapper.countByCondition(templateName, messageType, status);
    }

    /**
     * 检查模板编码是否存在
     *
     * @param templateCode 模板编码
     *
     * @return 是否存在
     */
    @Override
    public boolean existsByTemplateCode(String templateCode) {
        return messageTemplateMapper.countByTemplateCode(templateCode) > 0;
    }

    /**
     * 物理删除已逻辑删除的消息模板记录
     *
     * @param templateCode 模板编码
     *
     * @return 是否删除成功
     */
    @Override
    public boolean removeDeletedByTemplateCode(String templateCode) {
        return messageTemplateMapper.removeDeletedByTemplateCode(templateCode) >= 0;
    }

    /**
     * 查询所有启用的消息模板
     *
     * @return 启用的消息模板列表
     */
    @Override
    public List<MessageTemplate> findAllEnabled() {
        return messageTemplateMapper.selectAllEnabled()
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 消息模板持久化对象
     *
     * @return 消息模板领域对象
     */
    private MessageTemplate convertToDomain(MessageTemplatePO po) {
        return MessageTemplate.builder()
                .id(po.getId())
                .templateCode(po.getTemplateCode())
                .templateName(po.getTemplateName())
                .templateContent(po.getTemplateContent())
                .messageType(po.getMessageType())
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
     * @param domain 消息模板领域对象
     *
     * @return 消息模板持久化对象
     */
    private MessageTemplatePO convertToPO(MessageTemplate domain) {
        MessageTemplatePO po = new MessageTemplatePO();
        po.setId(domain.getId());
        po.setTemplateCode(domain.getTemplateCode());
        po.setTemplateName(domain.getTemplateName());
        po.setTemplateContent(domain.getTemplateContent());
        po.setMessageType(domain.getMessageType());
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
