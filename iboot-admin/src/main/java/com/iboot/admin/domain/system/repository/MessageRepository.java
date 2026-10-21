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

import com.iboot.admin.domain.system.model.Message;

import java.util.List;
import java.util.Optional;

/**
 * 消息仓储接口
 * <p>
 * 负责系统消息的持久化操作，支持消息的发送、管理和查询
 * </p>
 *
 * <p>查询约定：列表方法没有数据时返回空列表而不是 {@code null}；分页方法的页码从 1 开始；
 * 按 ID 查找的方法返回 {@link java.util.Optional}，不存在时为空。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface MessageRepository {

    /**
     * 保存消息
     *
     * @param message 消息实体
     *
     * @return 保存后的消息
     */
    Message save(Message message);

    /**
     * 更新消息
     *
     * @param message 消息实体
     *
     * @return 是否更新成功
     */
    boolean update(Message message);

    /**
     * 根据 ID 删除消息（逻辑删除）
     *
     * @param id 消息 ID
     *
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据 ID 查询消息
     *
     * @param id 消息 ID
     *
     * @return 消息实体，不存在则返回空
     */
    Optional<Message> findById(Long id);

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
    List<Message> findPageByCondition(String title, String messageType, String status, int pageNum, int pageSize);

    /**
     * 按条件统计消息数量
     *
     * @param title       消息标题（可选）
     * @param messageType 消息类型（可选）
     * @param status      状态（可选）
     *
     * @return 消息数量
     */
    long countByCondition(String title, String messageType, String status);

    /**
     * 按条件查询所有消息（导出用）
     *
     * @param title       消息标题（可选）
     * @param messageType 消息类型（可选）
     * @param status      状态（可选）
     *
     * @return 消息列表
     */
    List<Message> findAllByCondition(String title, String messageType, String status);

    /**
     * 查询对账窗口内发生变更的消息
     * <p>
     * 用于全文检索对账；结果包含已逻辑删除的消息，调用方需依据 {@code deleted} 与 {@code status}
     * 判断是写入还是移除索引。
     * </p>
     *
     * @return 窗口内变更的消息列表
     */
    List<Message> findChangedSince();

}
