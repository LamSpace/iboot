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

package com.iboot.workflow.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 额度划转请求。
 *
 * <p>由持有 {@code approval:quota:transfer} 权限的管理员发起，把指定年度的额度
 * 从划出方转给划入方。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "额度划转请求")
public class LeaveQuotaTransferRequest {

    /**
     * 划出方用户 ID。
     */
    @Schema(description = "划出方用户 ID")
    @NotNull(message = "划出方不能为空")
    private Long fromUserId;

    /**
     * 划入方用户 ID。
     */
    @Schema(description = "划入方用户 ID")
    @NotNull(message = "划入方不能为空")
    private Long toUserId;

    /**
     * 年度。
     */
    @Schema(description = "年度")
    @NotNull(message = "年度不能为空")
    @Min(value = 2000, message = "年度不合法")
    @Max(value = 2100, message = "年度不合法")
    private Integer year;

    /**
     * 划转次数。
     */
    @Schema(description = "划转次数")
    @NotNull(message = "划转次数不能为空")
    @Min(value = 1, message = "划转次数必须大于 0")
    private Integer count;

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(final Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(final Long toUserId) {
        this.toUserId = toUserId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(final Integer year) {
        this.year = year;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }
}
