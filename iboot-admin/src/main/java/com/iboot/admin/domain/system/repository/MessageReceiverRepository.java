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

import com.iboot.admin.domain.system.model.MessageReceiver;

import java.util.List;

/**
 * 消息接收记录仓储接口
 * <p>
 * 负责消息接收记录的持久化操作，跟踪消息的发送和阅读状态
 * </p>
 *
 * @author iBoot
 */
public interface MessageReceiverRepository {

    /**
     * 保存消息接收记录
     *
     * @param receiver 消息接收记录实体
     * @return 是否保存成功
     */
    boolean save(MessageReceiver receiver);

    /**
     * 批量保存消息接收记录
     *
     * @param receivers 消息接收记录列表
     * @return 是否保存成功
     */
    boolean batchSave(List<MessageReceiver> receivers);

    /**
     * 将消息标记为已读
     *
     * @param messageId 消息 ID
     * @param userId 用户 ID
     * @return 是否标记成功
     */
    boolean markAsRead(Long messageId, Long userId);

    /**
     * 将用户的所有消息标记为已读
     *
     * @param userId 用户 ID
     * @return 是否标记成功
     */
    boolean markAllAsRead(Long userId);

    /**
     * 软删除消息（逻辑删除）
     *
     * @param messageId 消息 ID
     * @param userId 用户 ID
     * @return 是否删除成功
     */
    boolean softDelete(Long messageId, Long userId);

    /**
     * 检查用户是否收到指定消息
     *
     * @param messageId 消息 ID
     * @param userId 用户 ID
     * @return 是否存在
     */
    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    /**
     * 统计用户未读消息数量
     *
     * @param userId 用户 ID
     * @return 未读消息数量
     */
    long countUnreadByUserId(Long userId);

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
    List<MessageReceiver> findPageByUserId(Long userId, String messageType, Integer isRead, int pageNum, int pageSize);

    /**
     * 按条件统计用户消息数量
     *
     * @param userId 用户 ID
     * @param messageType 消息类型
     * @param isRead 是否已读
     * @return 消息数量
     */
    long countByUserId(Long userId, String messageType, Integer isRead);

    /**
     * 根据消息 ID 删除接收记录
     * <p>
     * 用于消息删除时级联清理接收记录
     * </p>
     *
     * @param messageId 消息 ID
     * @return 是否删除成功
     */
    boolean deleteByMessageId(Long messageId);

    /**
     * 根据消息 ID 查询接收记录列表
     * <p>
     * 用于推送通知时获取所有接收者
     * </p>
     *
     * @param messageId 消息 ID
     * @return 消息接收记录列表
     */
    List<MessageReceiver> findByMessageId(Long messageId);
}
