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

package com.iboot.system.interfaces.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 内部额度划转 Try 请求。
 *
 * <p>由审批服务在全局事务内调用；Confirm/Cancel 由 Seata 协调器回调，不使用该请求。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class LeaveQuotaTransferTryRequest {

    /**
     * 划转业务标识，幂等键。
     */
    @NotBlank(message = "划转标识不能为空")
    @Size(max = 64, message = "划转标识长度不能超过 64")
    private String transferId;

    /**
     * 划出方用户 ID。
     */
    @NotNull(message = "划出方不能为空")
    private Long fromUserId;

    /**
     * 划入方用户 ID。
     */
    @NotNull(message = "划入方不能为空")
    private Long toUserId;

    /**
     * 年度。
     */
    @NotNull(message = "年度不能为空")
    @Min(value = 2000, message = "年度不合法")
    private Integer year;

    /**
     * 划转次数。
     */
    @NotNull(message = "划转次数不能为空")
    @Min(value = 1, message = "划转次数必须大于 0")
    private Integer count;

    /**
     * 返回划转业务标识。
     *
     * @return 划转业务标识
     */
    public String getTransferId() {
        return transferId;
    }

    /**
     * 设置划转业务标识。
     *
     * @param transferId 划转业务标识
     */
    public void setTransferId(final String transferId) {
        this.transferId = transferId;
    }

    /**
     * 返回划出方用户 ID。
     *
     * @return 划出方用户 ID
     */
    public Long getFromUserId() {
        return fromUserId;
    }

    /**
     * 设置划出方用户 ID。
     *
     * @param fromUserId 划出方用户 ID
     */
    public void setFromUserId(final Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    /**
     * 返回划入方用户 ID。
     *
     * @return 划入方用户 ID
     */
    public Long getToUserId() {
        return toUserId;
    }

    /**
     * 设置划入方用户 ID。
     *
     * @param toUserId 划入方用户 ID
     */
    public void setToUserId(final Long toUserId) {
        this.toUserId = toUserId;
    }

    /**
     * 返回年度。
     *
     * @return 年度
     */
    public Integer getYear() {
        return year;
    }

    /**
     * 设置年度。
     *
     * @param year 年度
     */
    public void setYear(final Integer year) {
        this.year = year;
    }

    /**
     * 返回划转次数。
     *
     * @return 划转次数
     */
    public Integer getCount() {
        return count;
    }

    /**
     * 设置划转次数。
     *
     * @param count 划转次数
     */
    public void setCount(final Integer count) {
        this.count = count;
    }
}
