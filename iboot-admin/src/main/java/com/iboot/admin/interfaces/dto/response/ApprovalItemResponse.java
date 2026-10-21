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

package com.iboot.admin.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 审批中心列表项响应。
 *
 * <p>在请假申请字段基础上附加当前任务或完成时间：待办项包含任务 ID 与任务创建时间，
 * 已办项包含完成时间。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "审批中心列表项")
public class ApprovalItemResponse extends LeaveApplicationResponse {

    /**
     * 当前任务 ID；已办项为 {@code null}。
     */
    @Schema(description = "当前任务 ID")
    private String taskId;

    /**
     * 当前任务名称；已办项为 {@code null}。
     */
    @Schema(description = "当前任务名称")
    private String taskName;

    /**
     * 任务创建时间；已办项为 {@code null}。
     */
    @Schema(description = "任务创建时间")
    private LocalDateTime taskCreateTime;

    /**
     * 任务完成时间；待办项为 {@code null}。
     */
    @Schema(description = "任务完成时间")
    private LocalDateTime completedTime;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(final String taskName) {
        this.taskName = taskName;
    }

    public LocalDateTime getTaskCreateTime() {
        return taskCreateTime;
    }

    public void setTaskCreateTime(final LocalDateTime taskCreateTime) {
        this.taskCreateTime = taskCreateTime;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(final LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }
}
