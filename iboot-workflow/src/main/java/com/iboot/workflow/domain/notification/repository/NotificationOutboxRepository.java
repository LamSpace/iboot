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

package com.iboot.workflow.domain.notification.repository;

import com.iboot.workflow.domain.notification.model.NotificationOutboxEvent;
import com.iboot.workflow.domain.notification.model.NotificationOutboxStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知 outbox 仓储。
 *
 * <p>写入必须发生在审批事务内；{@link #claimBatch(int, LocalDateTime)} 自带上锁与
 * 短事务，供中继抢占事件，其余方法由中继在发送完成后调用。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface NotificationOutboxRepository {

    /**
     * 写入一条待发事件。
     *
     * @param event 事件
     * @return 保存后的事件，包含生成的主键
     */
    NotificationOutboxEvent save(NotificationOutboxEvent event);

    /**
     * 抢占一批到期的待发事件并标记为发送中。
     *
     * @param batchSize 批量大小
     * @param now       当前时间
     * @return 被本实例抢占的事件
     */
    List<NotificationOutboxEvent> claimBatch(int batchSize, LocalDateTime now);

    /**
     * 标记事件已成功投递。
     *
     * @param id        事件主键
     * @param messageId broker 消息 ID
     * @param now       当前时间
     * @return 是否更新成功
     */
    boolean markSent(Long id, String messageId, LocalDateTime now);

    /**
     * 记录一次失败并按退避时间重新排队。
     *
     * @param id            事件主键
     * @param attempts      已尝试次数
     * @param nextRetryTime 下次可投递时间
     * @param lastError     错误摘要
     * @param now           当前时间
     * @return 是否更新成功
     */
    boolean markRetry(Long id, int attempts, LocalDateTime nextRetryTime, String lastError, LocalDateTime now);

    /**
     * 标记事件为死信。
     *
     * @param id        事件主键
     * @param attempts  已尝试次数
     * @param lastError 错误摘要
     * @param now       当前时间
     * @return 是否更新成功
     */
    boolean markDead(Long id, int attempts, String lastError, LocalDateTime now);

    /**
     * 把超过租约仍处于发送中的事件重置为待发。
     *
     * @param threshold 早于该时间的 SENDING 记录会被重置
     * @param now       当前时间
     * @return 重置的记录数
     */
    int resetStaleSending(LocalDateTime threshold, LocalDateTime now);

    /**
     * 统计指定状态的事件数。
     *
     * @param status 状态
     * @return 记录数
     */
    long countByStatus(NotificationOutboxStatus status);

    /**
     * 删除保留期之前的已发送事件。
     *
     * @param threshold 早于该时间的 SENT 记录会被删除
     * @return 删除的记录数
     */
    int deleteSentBefore(LocalDateTime threshold);
}
