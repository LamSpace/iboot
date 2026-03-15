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
import com.iboot.admin.domain.system.model.MessageTemplate;
import com.iboot.admin.domain.system.repository.MessageTemplateRepository;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息模板应用服务
 * <p>
 * 提供消息模板的创建、更新、删除、查询以及模板内容填充等功能
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageTemplateApplicationService {

    private final MessageTemplateRepository messageTemplateRepository;

    /**
     * 创建消息模板
     * <p>
     * 检查模板编码是否已存在，清理同编码已删除的记录，设置默认状态为启用
     * </p>
     *
     * @param template 消息模板实体
     * @return 创建后的消息模板
     * @throws BusinessException 当模板编码已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageTemplate createTemplate(MessageTemplate template) {
        if (messageTemplateRepository.existsByTemplateCode(template.getTemplateCode())) {
            throw new BusinessException("模板编码已存在");
        }

        messageTemplateRepository.removeDeletedByTemplateCode(template.getTemplateCode());

        template.setCreateBy(SecurityUtils.getCurrentUsername());
        template.setCreateTime(LocalDateTime.now());
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        return messageTemplateRepository.save(template);
    }

    /**
     * 更新消息模板
     * <p>
     * 检查模板是否存在，更新模板信息
     * </p>
     *
     * @param template 消息模板实体
     * @return 是否更新成功
     * @throws BusinessException 当模板不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplate(MessageTemplate template) {
        messageTemplateRepository.findById(template.getId())
                .orElseThrow(() -> new BusinessException("消息模板不存在"));

        template.setUpdateBy(SecurityUtils.getCurrentUsername());
        template.setUpdateTime(LocalDateTime.now());
        return messageTemplateRepository.update(template);
    }

    /**
     * 删除消息模板
     * <p>
     * 检查模板是否存在，执行逻辑删除
     * </p>
     *
     * @param id 消息模板 ID
     * @return 是否删除成功
     * @throws BusinessException 当模板不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long id) {
        messageTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("消息模板不存在"));

        return messageTemplateRepository.deleteById(id);
    }

    /**
     * 根据 ID 获取消息模板
     *
     * @param id 消息模板 ID
     * @return 消息模板实体
     * @throws BusinessException 当模板不存在时抛出
     */
    public MessageTemplate getTemplateById(Long id) {
        return messageTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("消息模板不存在"));
    }

    /**
     * 根据模板编码获取消息模板
     *
     * @param templateCode 模板编码
     * @return 消息模板实体
     * @throws BusinessException 当模板不存在时抛出
     */
    public MessageTemplate getTemplateByCode(String templateCode) {
        return messageTemplateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new BusinessException("消息模板不存在"));
    }

    /**
     * 分页查询消息模板列表
     *
     * @param templateName 模板名称（可选）
     * @param messageType 消息类型（可选）
     * @param status 状态（可选）
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 消息模板列表
     */
    public List<MessageTemplate> getTemplatePage(String templateName, String messageType, Integer status, int pageNum, int pageSize) {
        return messageTemplateRepository.findPageByCondition(templateName, messageType, status, pageNum, pageSize);
    }

    /**
     * 统计消息模板数量
     *
     * @param templateName 模板名称（可选）
     * @param messageType 消息类型（可选）
     * @param status 状态（可选）
     * @return 消息模板总数
     */
    public long countTemplates(String templateName, String messageType, Integer status) {
        return messageTemplateRepository.countByCondition(templateName, messageType, status);
    }

    /**
     * 获取所有启用的消息模板
     *
     * @return 启用的消息模板列表
     */
    public List<MessageTemplate> getAllEnabledTemplates() {
        return messageTemplateRepository.findAllEnabled();
    }

    /**
     * 填充模板内容
     * <p>
     * 根据模板编码获取模板，检查是否启用，使用提供的参数填充模板变量
     * </p>
     *
     * @param templateCode 模板编码
     * @param params 变量参数 Map，键为变量名，值为变量值
     * @return 填充后的模板内容
     * @throws BusinessException 当模板不存在或已停用时抛出
     */
    public String fillTemplate(String templateCode, Map<String, String> params) {
        MessageTemplate template = getTemplateByCode(templateCode);
        if (!template.isEnabled()) {
            throw new BusinessException("消息模板已停用");
        }
        return template.fillContent(params);
    }
}
