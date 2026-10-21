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

package com.iboot.admin.domain.workflow.model;

import java.time.LocalDateTime;

/**
 * 工作流待办任务。
 *
 * <p>领域层对「当前待处理任务」的抽象，不包含任何引擎类型。由
 * {@link com.iboot.admin.domain.workflow.service.WorkflowService} 返回。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class WorkflowTask {

    /**
     * 任务 ID。
     */
    private String taskId;

    /**
     * 流程实例 ID。
     */
    private String processInstanceId;

    /**
     * 任务处理人用户 ID。
     */
    private Long assigneeId;

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 任务创建时间。
     */
    private LocalDateTime createTime;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(final String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(final Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(final String taskName) {
        this.taskName = taskName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
