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
 * 工作流历史节点。
 *
 * <p>用于待办完成记录与审批详情中的时间线。{@code approved} 为 {@code null} 时表示
 * 提交节点，{@code true} 表示通过，{@code false} 表示驳回。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class WorkflowHistoryItem {

    /**
     * 流程实例 ID。
     */
    private String processInstanceId;

    /**
     * 任务 ID；提交节点为 {@code null}。
     */
    private String taskId;

    /**
     * 节点名称，如「提交申请」「审批通过」「审批驳回」。
     */
    private String activityName;

    /**
     * 操作人用户 ID。
     */
    private Long actorId;

    /**
     * 审批结论：{@code null}-提交，{@code true}-通过，{@code false}-驳回。
     */
    private Boolean approved;

    /**
     * 审批意见或驳回原因。
     */
    private String comment;

    /**
     * 节点发生时间。
     */
    private LocalDateTime time;

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(final String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(final String activityName) {
        this.activityName = activityName;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(final Long actorId) {
        this.actorId = actorId;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(final Boolean approved) {
        this.approved = approved;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(final LocalDateTime time) {
        this.time = time;
    }
}
