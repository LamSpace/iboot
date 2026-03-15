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

import com.iboot.admin.infrastructure.persistence.po.MessageReceiverPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 消息接收记录 Mapper 接口
 * <p>
 * 负责消息接收记录的数据库操作，跟踪消息的发送和阅读状态
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface MessageReceiverMapper {

    /**
     * 插入消息接收记录
     *
     * @param receiverPO 消息接收记录持久化对象
     * @return 影响的记录数
     */
    int insert(MessageReceiverPO receiverPO);

    /**
     * 批量插入消息接收记录
     *
     * @param list 消息接收记录列表
     * @return 影响的记录数
     */
    int batchInsert(@Param("list") List<MessageReceiverPO> list);

    /**
     * 将消息标记为已读
     *
     * @param messageId 消息 ID
     * @param userId 用户 ID
     * @return 影响的记录数
     */
    int updateRead(@Param("messageId") Long messageId, @Param("userId") Long userId);

    /**
     * 将用户的所有消息标记为已读
     *
     * @param userId 用户 ID
     * @return 影响的记录数
     */
    int updateAllRead(@Param("userId") Long userId);

    /**
     * 软删除消息（逻辑删除）
     *
     * @param messageId 消息 ID
     * @param userId 用户 ID
     * @return 影响的记录数
     */
    int updateDeleted(@Param("messageId") Long messageId, @Param("userId") Long userId);

    /**
     * 根据消息 ID 和用户 ID 统计数量
     *
     * @param messageId 消息 ID
     * @param userId 用户 ID
     * @return 数量
     */
    int countByMessageIdAndUserId(@Param("messageId") Long messageId, @Param("userId") Long userId);

    /**
     * 统计用户未读消息数量
     *
     * @param userId 用户 ID
     * @return 未读消息数量
     */
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 分页查询用户的消息列表
     *
     * @param userId 用户 ID
     * @param messageType 消息类型（可选）
     * @param isRead 是否已读（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 消息列表
     */
    List<Map<String, Object>> selectPageByUserId(@Param("userId") Long userId,
                                                  @Param("messageType") String messageType,
                                                  @Param("isRead") Integer isRead,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    /**
     * 按条件统计用户消息数量
     *
     * @param userId 用户 ID
     * @param messageType 消息类型
     * @param isRead 是否已读
     * @return 消息数量
     */
    long countByUserId(@Param("userId") Long userId,
                       @Param("messageType") String messageType,
                       @Param("isRead") Integer isRead);

    /**
     * 根据消息 ID 删除接收记录
     *
     * @param messageId 消息 ID
     * @return 影响的记录数
     */
    int deleteByMessageId(@Param("messageId") Long messageId);

    /**
     * 根据消息 ID 查询接收记录列表
     *
     * @param messageId 消息 ID
     * @return 消息接收记录列表
     */
    List<Map<String, Object>> findByMessageId(@Param("messageId") Long messageId);
}
