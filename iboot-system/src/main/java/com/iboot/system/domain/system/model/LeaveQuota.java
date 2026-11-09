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

package com.iboot.system.domain.system.model;

/**
 * 用户年度请假额度。
 *
 * <p>按用户与年度记录总额度、已用次数与冻结次数。冻结次数为后续 TCC 预占预留，
 * 当前业务只使用总额度与已用次数；剩余次数由三者计算，不单独持久化。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class LeaveQuota {

    private final Long userId;

    private final int year;

    private final int totalCount;

    private final int usedCount;

    private final int frozenCount;

    /**
     * 创建额度快照。
     *
     * @param userId      用户 ID
     * @param year        年度
     * @param totalCount  总额度
     * @param usedCount   已用次数
     * @param frozenCount 冻结次数
     */
    public LeaveQuota(final Long userId, final int year, final int totalCount,
                      final int usedCount, final int frozenCount) {
        this.userId = userId;
        this.year = year;
        this.totalCount = totalCount;
        this.usedCount = usedCount;
        this.frozenCount = frozenCount;
    }

    /**
     * 计算剩余可用次数。
     *
     * @return 总额度减去已用与冻结后的剩余次数，最小为 0
     */
    public int remainingCount() {
        return Math.max(0, totalCount - usedCount - frozenCount);
    }

    /**
     * 返回用户 ID。
     *
     * @return 用户 ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 返回年度。
     *
     * @return 年度
     */
    public int getYear() {
        return year;
    }

    /**
     * 返回总额度。
     *
     * @return 总额度
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * 返回已用次数。
     *
     * @return 已用次数
     */
    public int getUsedCount() {
        return usedCount;
    }

    /**
     * 返回冻结次数。
     *
     * @return 冻结次数
     */
    public int getFrozenCount() {
        return frozenCount;
    }
}
