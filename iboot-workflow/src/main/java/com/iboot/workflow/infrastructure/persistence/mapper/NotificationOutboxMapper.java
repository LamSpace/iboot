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

package com.iboot.workflow.infrastructure.persistence.mapper;

import com.iboot.workflow.infrastructure.persistence.po.NotificationOutboxPO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知 outbox MyBatis Mapper。
 *
 * <p>只负责单表读写；{@code selectClaimable} 使用 {@code FOR UPDATE SKIP LOCKED}
 * 抢占，调用方必须处于事务中。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface NotificationOutboxMapper {

    /**
     * 新增事件。
     *
     * @param po 持久化对象
     * @return 受影响行数
     */
    int insert(NotificationOutboxPO po);

    /**
     * 查询并锁定一批到期待发事件。
     *
     * @param batchSize 批量大小
     * @param now       当前时间
     * @return 持久化对象列表
     */
    List<NotificationOutboxPO> selectClaimable(@Param("batchSize") int batchSize,
                                               @Param("now") LocalDateTime now);

    /**
     * 标记为发送中。
     *
     * @param id  主键
     * @param now 当前时间
     * @return 受影响行数
     */
    int markSending(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 标记为已发送。
     *
     * @param id        主键
     * @param messageId broker 消息 ID
     * @param now       当前时间
     * @return 受影响行数
     */
    int markSent(@Param("id") Long id, @Param("messageId") String messageId,
                 @Param("now") LocalDateTime now);

    /**
     * 记录失败并重新排队。
     *
     * @param id            主键
     * @param attempts      已尝试次数
     * @param nextRetryTime 下次可投递时间
     * @param lastError     错误摘要
     * @param now           当前时间
     * @return 受影响行数
     */
    int markRetry(@Param("id") Long id, @Param("attempts") int attempts,
                  @Param("nextRetryTime") LocalDateTime nextRetryTime,
                  @Param("lastError") String lastError, @Param("now") LocalDateTime now);

    /**
     * 标记为死信。
     *
     * @param id        主键
     * @param attempts  已尝试次数
     * @param lastError 错误摘要
     * @param now       当前时间
     * @return 受影响行数
     */
    int markDead(@Param("id") Long id, @Param("attempts") int attempts,
                 @Param("lastError") String lastError, @Param("now") LocalDateTime now);

    /**
     * 重置超租约的发送中记录。
     *
     * @param threshold 早于该时间视为超租约
     * @param now       当前时间
     * @return 受影响行数
     */
    int resetStaleSending(@Param("threshold") LocalDateTime threshold,
                          @Param("now") LocalDateTime now);

    /**
     * 按状态统计。
     *
     * @param status 状态
     * @return 记录数
     */
    long countByStatus(@Param("status") String status);

    /**
     * 删除保留期之前的已发送记录。
     *
     * @param threshold 早于该时间删除
     * @return 受影响行数
     */
    int deleteSentBefore(@Param("threshold") LocalDateTime threshold);
}
