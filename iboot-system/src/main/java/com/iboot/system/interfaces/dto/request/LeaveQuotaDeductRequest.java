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
 * 内部额度扣减请求。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class LeaveQuotaDeductRequest {

    /**
     * 申请 ID，幂等键。
     */
    @NotBlank(message = "申请 ID 不能为空")
    @Size(max = 64, message = "申请 ID 长度不能超过 64")
    private String requestId;

    /**
     * 用户 ID。
     */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /**
     * 年度。
     */
    @NotNull(message = "年度不能为空")
    @Min(value = 2000, message = "年度不合法")
    private Integer year;

    /**
     * 返回申请 ID。
     *
     * @return 申请 ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 设置申请 ID。
     *
     * @param requestId 申请 ID
     */
    public void setRequestId(final String requestId) {
        this.requestId = requestId;
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
}
