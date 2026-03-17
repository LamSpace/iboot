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

package com.iboot.admin.infrastructure.persistence.mapper;

import com.iboot.admin.infrastructure.persistence.po.MessageTemplatePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息模板 Mapper 接口
 * <p>
 * 负责消息模板的数据库操作，支持模板的增删改查和状态管理
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface MessageTemplateMapper {

    /**
     * 插入消息模板记录
     *
     * @param templatePO 消息模板持久化对象
     *
     * @return 影响的记录数
     */
    int insert(MessageTemplatePO templatePO);

    /**
     * 更新消息模板记录
     *
     * @param templatePO 消息模板持久化对象
     *
     * @return 影响的记录数
     */
    int update(MessageTemplatePO templatePO);

    /**
     * 根据 ID 删除消息模板
     *
     * @param id 消息模板 ID
     *
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询消息模板
     *
     * @param id 消息模板 ID
     *
     * @return 消息模板持久化对象
     */
    MessageTemplatePO selectById(@Param("id") Long id);

    /**
     * 根据模板编码查询消息模板
     *
     * @param templateCode 模板编码
     *
     * @return 消息模板持久化对象
     */
    MessageTemplatePO selectByTemplateCode(@Param("templateCode") String templateCode);

    /**
     * 按条件分页查询消息模板
     *
     * @param templateName 模板名称（可选）
     * @param messageType  消息类型（可选）
     * @param status       状态（可选）
     * @param offset       偏移量
     * @param limit        限制数量
     *
     * @return 消息模板列表
     */
    List<MessageTemplatePO> selectPageByCondition(@Param("templateName") String templateName,
                                                  @Param("messageType") String messageType,
                                                  @Param("status") Integer status, @Param("offset") int offset,
                                                  @Param("limit") int limit);

    /**
     * 按条件统计消息模板数量
     *
     * @param templateName 模板名称（可选）
     * @param messageType  消息类型（可选）
     * @param status       状态（可选）
     *
     * @return 消息模板数量
     */
    long countByCondition(@Param("templateName") String templateName, @Param("messageType") String messageType,
                          @Param("status") Integer status);

    /**
     * 根据模板编码统计数量
     *
     * @param templateCode 模板编码
     *
     * @return 数量
     */
    int countByTemplateCode(@Param("templateCode") String templateCode);

    /**
     * 物理删除已逻辑删除的消息模板记录
     * <p>
     * 用于清理已被逻辑删除的历史数据
     * </p>
     *
     * @param templateCode 模板编码
     *
     * @return 影响的记录数
     */
    int removeDeletedByTemplateCode(@Param("templateCode") String templateCode);

    /**
     * 查询所有启用的消息模板
     *
     * @return 启用的消息模板列表
     */
    List<MessageTemplatePO> selectAllEnabled();

}
