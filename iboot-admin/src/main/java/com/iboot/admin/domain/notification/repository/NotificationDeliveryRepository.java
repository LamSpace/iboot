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

package com.iboot.admin.domain.notification.repository;

import com.iboot.admin.domain.notification.model.NotificationDelivery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 通知投递记录仓储。
 *
 * <p>提供幂等登记、分页查询、到期扫描、并发领取与状态更新。列表方法没有数据时返回空列表，
 * 按 ID 查找返回 {@link Optional}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface NotificationDeliveryRepository {

    /**
     * 幂等插入投递记录。
     *
     * @param delivery 投递记录
     * @return 实际插入返回 {@code true}，唯一键冲突返回 {@code false}
     */
    boolean insertIfAbsent(NotificationDelivery delivery);

    /**
     * 按 ID 查询投递记录。
     *
     * @param id 记录 ID
     * @return 投递记录，不存在返回空
     */
    Optional<NotificationDelivery> findById(Long id);

    /**
     * 分页查询投递记录。
     *
     * @param status    状态，可为 {@code null}
     * @param channel   渠道，可为 {@code null}
     * @param eventType 事件类型，可为 {@code null}
     * @param recipient 接收人关键字，可为 {@code null}
     * @param startTime 创建时间下界，可为 {@code null}
     * @param endTime   创建时间上界，可为 {@code null}
     * @param offset    起始偏移
     * @param limit     每页数量
     * @return 投递记录列表
     */
    List<NotificationDelivery> findPage(String status, String channel, String eventType, String recipient,
                                        LocalDateTime startTime, LocalDateTime endTime, int offset, int limit);

    /**
     * 统计投递记录数。
     *
     * @param status    状态，可为 {@code null}
     * @param channel   渠道，可为 {@code null}
     * @param eventType 事件类型，可为 {@code null}
     * @param recipient 接收人关键字，可为 {@code null}
     * @param startTime 创建时间下界，可为 {@code null}
     * @param endTime   创建时间上界，可为 {@code null}
     * @return 记录数
     */
    long count(String status, String channel, String eventType, String recipient,
               LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询到期可重试的记录 ID。
     *
     * @param now   当前时间
     * @param limit 最大数量
     * @return 记录 ID 列表
     */
    List<Long> findDueIds(LocalDateTime now, int limit);

    /**
     * 查询某个事件下当前可投递的记录 ID。
     *
     * @param eventId 事件 ID
     * @param now     当前时间
     * @return 记录 ID 列表
     */
    List<Long> findDispatchableIdsByEventId(String eventId, LocalDateTime now);

    /**
     * 条件领取记录。
     *
     * @param id  记录 ID
     * @param now 当前时间
     * @return 领取成功返回 {@code true}
     */
    boolean claim(Long id, LocalDateTime now);

    /**
     * 更新投递状态字段。
     *
     * @param delivery 投递记录
     * @return 更新成功返回 {@code true}
     */
    boolean update(NotificationDelivery delivery);

    /**
     * 重置超时未完成的发送中记录。
     *
     * @param threshold 超时阈值
     * @param now       当前时间
     * @return 受影响行数
     */
    int resetStaleSending(LocalDateTime threshold, LocalDateTime now);

    /**
     * 管理员人工重试。
     *
     * @param id  记录 ID
     * @param now 当前时间
     * @return 重置成功返回 {@code true}
     */
    boolean resetForRetry(Long id, LocalDateTime now);
}
