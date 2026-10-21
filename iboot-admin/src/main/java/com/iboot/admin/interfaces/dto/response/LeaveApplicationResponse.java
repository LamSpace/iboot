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
 * 请假申请响应 DTO。
 *
 * <p>用于审批中心列表与详情，包含申请人/审批人的显示名。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "请假申请响应")
public class LeaveApplicationResponse {

    /**
     * 申请 ID。
     */
    @Schema(description = "申请 ID")
    private Long id;

    /**
     * 申请人用户 ID。
     */
    @Schema(description = "申请人用户 ID")
    private Long applicantId;

    /**
     * 申请人显示名。
     */
    @Schema(description = "申请人显示名")
    private String applicantName;

    /**
     * 审批人用户 ID。
     */
    @Schema(description = "审批人用户 ID")
    private Long approverId;

    /**
     * 审批人显示名。
     */
    @Schema(description = "审批人显示名")
    private String approverName;

    /**
     * 请假类型。
     */
    @Schema(description = "请假类型")
    private String leaveType;

    /**
     * 开始时间。
     */
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    /**
     * 结束时间。
     */
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    /**
     * 请假事由。
     */
    @Schema(description = "请假事由")
    private String reason;

    /**
     * 状态：PENDING/APPROVED/REJECTED。
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 审批意见或驳回原因。
     */
    @Schema(description = "审批意见或驳回原因")
    private String approveComment;

    /**
     * 提交时间。
     */
    @Schema(description = "提交时间")
    private LocalDateTime createTime;

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

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(final String applicantName) {
        this.applicantName = applicantName;
    }

    public Long getApproverId() {
        return approverId;
    }

    public void setApproverId(final Long approverId) {
        this.approverId = approverId;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(final String approverName) {
        this.approverName = approverName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getApproveComment() {
        return approveComment;
    }

    public void setApproveComment(final String approveComment) {
        this.approveComment = approveComment;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
