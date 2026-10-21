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

package com.iboot.admin.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 发起请假请求 DTO。
 *
 * <p>申请人取自当前登录用户，不在请求中传递；审批人必须是启用状态的其它用户。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "发起请假请求")
public class LeaveApplyRequest {

    /**
     * 审批人用户 ID。
     */
    @Schema(description = "审批人用户 ID")
    @NotNull(message = "审批人不能为空")
    private Long approverId;

    /**
     * 请假类型。
     */
    @Schema(description = "请假类型")
    @NotBlank(message = "请假类型不能为空")
    @Size(max = 20, message = "请假类型长度不能超过20")
    private String leaveType;

    /**
     * 开始时间。
     */
    @Schema(description = "开始时间")
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间。
     */
    @Schema(description = "结束时间")
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 请假事由。
     */
    @Schema(description = "请假事由")
    @NotBlank(message = "请假事由不能为空")
    @Size(max = 500, message = "请假事由长度不能超过500")
    private String reason;

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(final Long approverId) {
        this.approverId = approverId;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(final String leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }
}
