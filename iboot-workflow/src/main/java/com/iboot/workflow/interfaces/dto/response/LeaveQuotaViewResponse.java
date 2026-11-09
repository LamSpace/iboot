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

package com.iboot.workflow.interfaces.dto.response;

import com.iboot.workflow.application.service.quota.LeaveQuotaSnapshot;

/**
 * 本人请假额度响应。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class LeaveQuotaViewResponse {

    /**
     * 年度总额度。
     */
    private int totalCount;

    /**
     * 已用次数。
     */
    private int usedCount;

    /**
     * 剩余次数。
     */
    private int remainingCount;

    /**
     * 从额度快照创建响应。
     *
     * @param snapshot 额度快照
     * @return 响应
     */
    public static LeaveQuotaViewResponse from(final LeaveQuotaSnapshot snapshot) {
        LeaveQuotaViewResponse response = new LeaveQuotaViewResponse();
        response.setTotalCount(snapshot.totalCount());
        response.setUsedCount(snapshot.usedCount());
        response.setRemainingCount(snapshot.remainingCount());
        return response;
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
