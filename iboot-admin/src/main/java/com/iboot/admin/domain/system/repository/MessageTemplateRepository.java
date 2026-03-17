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

package com.iboot.admin.domain.system.repository;

import com.iboot.admin.domain.system.model.MessageTemplate;

import java.util.List;
import java.util.Optional;

/**
 * 消息模板仓储接口
 * <p>
 * 负责消息模板的持久化操作，支持模板的新增、更新、查询和状态管理
 * </p>
 *
 * @author iBoot
 */
public interface MessageTemplateRepository {

    /**
     * 保存消息模板
     *
     * @param template 消息模板实体
     *
     * @return 保存后的消息模板
     */
    MessageTemplate save(MessageTemplate template);

    /**
     * 更新消息模板
     *
     * @param template 消息模板实体
     *
     * @return 是否更新成功
     */
    boolean update(MessageTemplate template);

    /**
     * 根据 ID 删除消息模板（逻辑删除）
     *
     * @param id 消息模板 ID
     *
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据 ID 查询消息模板
     *
     * @param id 消息模板 ID
     *
     * @return 消息模板实体，不存在则返回空
     */
    Optional<MessageTemplate> findById(Long id);

    /**
     * 根据模板编码查询消息模板
     *
     * @param templateCode 模板编码
     *
     * @return 消息模板实体，不存在则返回空
     */
    Optional<MessageTemplate> findByTemplateCode(String templateCode);

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
    List<MessageTemplate> findPageByCondition(String templateName, String messageType, Integer status, int pageNum,
                                              int pageSize);

    /**
     * 按条件统计消息模板数量
     *
     * @param templateName 模板名称（可选）
     * @param messageType  消息类型（可选）
     * @param status       状态（可选）
     *
     * @return 消息模板数量
     */
    long countByCondition(String templateName, String messageType, Integer status);

    /**
     * 检查模板编码是否存在
     *
     * @param templateCode 模板编码
     *
     * @return 是否存在
     */
    boolean existsByTemplateCode(String templateCode);

    /**
     * 物理删除已逻辑删除的消息模板记录
     * <p>
     * 用于清理已被逻辑删除的历史数据
     * </p>
     *
     * @param templateCode 模板编码
     *
     * @return 是否删除成功
     */
    boolean removeDeletedByTemplateCode(String templateCode);

    /**
     * 查询所有启用的消息模板
     *
     * @return 启用的消息模板列表
     */
    List<MessageTemplate> findAllEnabled();

}
