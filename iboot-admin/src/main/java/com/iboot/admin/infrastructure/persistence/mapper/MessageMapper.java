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

import com.iboot.admin.infrastructure.persistence.po.MessagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息 Mapper 接口
 * <p>
 * 负责系统消息的数据库操作，支持消息的发送和管理
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface MessageMapper {

    /**
     * 插入消息记录
     *
     * @param messagePO 消息持久化对象
     * @return 影响的记录数
     */
    int insert(MessagePO messagePO);

    /**
     * 更新消息记录
     *
     * @param messagePO 消息持久化对象
     * @return 影响的记录数
     */
    int update(MessagePO messagePO);

    /**
     * 根据 ID 删除消息
     *
     * @param id 消息 ID
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询消息
     *
     * @param id 消息 ID
     * @return 消息持久化对象
     */
    MessagePO selectById(@Param("id") Long id);

    /**
     * 按条件分页查询消息
     *
     * @param title 消息标题（可选）
     * @param messageType 消息类型（可选）
     * @param status 状态（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 消息列表
     */
    List<MessagePO> selectPageByCondition(@Param("title") String title,
                                           @Param("messageType") String messageType,
                                           @Param("status") String status,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    /**
     * 按条件统计消息数量
     *
     * @param title 消息标题（可选）
     * @param messageType 消息类型（可选）
     * @param status 状态（可选）
     * @return 消息数量
     */
    long countByCondition(@Param("title") String title,
                          @Param("messageType") String messageType,
                          @Param("status") String status);

    /**
     * 按条件查询所有消息（导出用）
     *
     * @param title 消息标题（可选）
     * @param messageType 消息类型（可选）
     * @param status 状态（可选）
     * @return 消息列表
     */
    List<MessagePO> selectAllByCondition(@Param("title") String title,
                                         @Param("messageType") String messageType,
                                         @Param("status") String status);
}
