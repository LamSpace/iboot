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
 * 请假申请实体。
 *
 * <p>保存请假业务字段与关联的流程实例 ID。状态流转由 {@link #markApproved(String)} 与
 * {@link #markRejected(String)} 控制，只允许从待审批进入终态。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see LeaveStatus
 */
public class LeaveApplication {

    /**
     * 申请 ID。
     */
    private Long id;

    /**
     * 申请人用户 ID。
     */
    private Long applicantId;

    /**
     * 审批人用户 ID。
     */
    private Long approverId;

    /**
     * 请假类型。
     */
    private String leaveType;

    /**
     * 请假开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 请假结束时间。
     */
    private LocalDateTime endTime;

    /**
     * 请假事由。
     */
    private String reason;

    /**
     * 当前状态。
     */
    private LeaveStatus status;

    /**
     * 关联的流程实例 ID。
     */
    private String processInstanceId;

    /**
     * 审批意见或驳回原因。
     */
    private String approveComment;

    /**
     * 创建人。
     */
    private String createBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新人。
     */
    private String updateBy;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除。
     */
    private Integer deleted;

    /**
     * 标记为审批通过。
     *
     * <p>仅当当前状态为待审批时允许，否则抛出 {@link IllegalStateException}。
     *
     * @param comment 审批意见，可为 {@code null}
     * @throws IllegalStateException 当前状态不是待审批时抛出
     */
    public void markApproved(final String comment) {
        transitionTo(LeaveStatus.APPROVED, comment);
    }

    /**
     * 标记为审批驳回。
     *
     * <p>仅当当前状态为待审批时允许，否则抛出 {@link IllegalStateException}。
     *
     * @param comment 驳回原因
     * @throws IllegalStateException 当前状态不是待审批时抛出
     */
    public void markRejected(final String comment) {
        transitionTo(LeaveStatus.REJECTED, comment);
    }

    private void transitionTo(final LeaveStatus target, final String comment) {
        if (status != LeaveStatus.PENDING) {
            throw new IllegalStateException("申请已处理，当前状态：" + status);
        }
        this.status = target;
        this.approveComment = comment;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(final Long applicantId) {
        this.applicantId = applicantId;
    }

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

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(final LeaveStatus status) {
        this.status = status;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(final String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getApproveComment() {
        return approveComment;
    }

    public void setApproveComment(final String approveComment) {
        this.approveComment = approveComment;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }
}
