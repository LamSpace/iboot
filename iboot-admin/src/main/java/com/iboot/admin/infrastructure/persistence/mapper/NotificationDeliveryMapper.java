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

import com.iboot.admin.infrastructure.persistence.po.NotificationDeliveryPO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知投递记录 MyBatis Mapper。
 *
 * <p>只负责单表读写，不包含业务规则；条件更新用于并发领取与重试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface NotificationDeliveryMapper {

    /**
     * 幂等插入投递记录，唯一键冲突时忽略。
     *
     * @param po 持久化对象
     * @return 受影响行数
     */
    int insertIgnore(NotificationDeliveryPO po);

    /**
     * 按 ID 查询投递记录。
     *
     * @param id 记录 ID
     * @return 持久化对象，不存在时为 {@code null}
     */
    NotificationDeliveryPO selectById(@Param("id") Long id);

    /**
     * 分页查询投递记录。
     *
     * @param status     状态，可为 {@code null}
     * @param channel    渠道，可为 {@code null}
     * @param eventType  事件类型，可为 {@code null}
     * @param recipient  接收人关键字，可为 {@code null}
     * @param startTime  创建时间下界，可为 {@code null}
     * @param endTime    创建时间上界，可为 {@code null}
     * @param offset     起始偏移
     * @param limit      每页数量
     * @return 持久化对象列表
     */
    List<NotificationDeliveryPO> selectPage(@Param("status") String status,
                                             @Param("channel") String channel,
                                             @Param("eventType") String eventType,
                                             @Param("recipient") String recipient,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    /**
     * 统计符合条件的投递记录数。
     *
     * @param status     状态，可为 {@code null}
     * @param channel    渠道，可为 {@code null}
     * @param eventType  事件类型，可为 {@code null}
     * @param recipient  接收人关键字，可为 {@code null}
     * @param startTime  创建时间下界，可为 {@code null}
     * @param endTime    创建时间上界，可为 {@code null}
     * @return 记录数
     */
    long countPage(@Param("status") String status,
                   @Param("channel") String channel,
                   @Param("eventType") String eventType,
                   @Param("recipient") String recipient,
                   @Param("startTime") LocalDateTime startTime,
                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查询到期可重试的记录 ID。
     *
     * @param now   当前时间
     * @param limit 最大数量
     * @return 记录 ID 列表
     */
    List<Long> selectDueIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 查询某个事件下当前可投递的记录 ID。
     *
     * @param eventId 事件 ID
     * @param now     当前时间
     * @return 记录 ID 列表
     */
    List<Long> selectDispatchableIdsByEventId(@Param("eventId") String eventId,
                                              @Param("now") LocalDateTime now);

    /**
     * 条件领取记录，防止并发重复发送。
     *
     * @param id  记录 ID
     * @param now 当前时间
     * @return 受影响行数，1 表示领取成功
     */
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 更新投递状态字段。
     *
     * @param po 持久化对象
     * @return 受影响行数
     */
    int update(NotificationDeliveryPO po);

    /**
     * 将长时间处于发送中的记录重置为失败，等待重试。
     *
     * @param threshold 超时阈值
     * @param now       当前时间
     * @return 受影响行数
     */
    int resetStaleSending(@Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);

    /**
     * 管理员人工重试：重置为待发送并清零尝试次数。
     *
     * @param id  记录 ID
     * @param now 当前时间
     * @return 受影响行数
     */
    int resetForRetry(@Param("id") Long id, @Param("now") LocalDateTime now);
}
