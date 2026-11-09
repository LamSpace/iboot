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

package com.iboot.system.interfaces.dto.response;

import com.iboot.system.domain.system.model.LeaveQuota;

/**
 * 请假额度响应。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class LeaveQuotaResponse {

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 年度。
     */
    private int year;

    /**
     * 年度总额度。
     */
    private int totalCount;

    /**
     * 已用次数。
     */
    private int usedCount;

    /**
     * 冻结次数。
     */
    private int frozenCount;

    /**
     * 剩余次数。
     */
    private int remainingCount;

    /**
     * 从领域模型创建响应。
     *
     * @param quota 额度
     * @return 响应
     */
    public static LeaveQuotaResponse from(final LeaveQuota quota) {
        LeaveQuotaResponse response = new LeaveQuotaResponse();
        response.setUserId(quota.getUserId());
        response.setYear(quota.getYear());
        response.setTotalCount(quota.getTotalCount());
        response.setUsedCount(quota.getUsedCount());
        response.setFrozenCount(quota.getFrozenCount());
        response.setRemainingCount(quota.remainingCount());
        return response;
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
     * 设置用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(final Long userId) {
        this.userId = userId;
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
     * 设置年度。
     *
     * @param year 年度
     */
    public void setYear(final int year) {
        this.year = year;
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
     * 设置总额度。
     *
     * @param totalCount 总额度
     */
    public void setTotalCount(final int totalCount) {
        this.totalCount = totalCount;
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
     * 设置已用次数。
     *
     * @param usedCount 已用次数
     */
    public void setUsedCount(final int usedCount) {
        this.usedCount = usedCount;
    }

    /**
     * 返回冻结次数。
     *
     * @return 冻结次数
     */
    public int getFrozenCount() {
        return frozenCount;
    }

    /**
     * 设置冻结次数。
     *
     * @param frozenCount 冻结次数
     */
    public void setFrozenCount(final int frozenCount) {
        this.frozenCount = frozenCount;
    }

    /**
     * 返回剩余次数。
     *
     * @return 剩余次数
     */
    public int getRemainingCount() {
        return remainingCount;
    }

    /**
     * 设置剩余次数。
     *
     * @param remainingCount 剩余次数
     */
    public void setRemainingCount(final int remainingCount) {
        this.remainingCount = remainingCount;
    }
}
