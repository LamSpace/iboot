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
 * 审批历史节点响应。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "审批历史节点")
public class ApprovalHistoryResponse {

    /**
     * 节点名称：提交申请 / 审批中 / 审批通过 / 审批驳回。
     */
    @Schema(description = "节点名称")
    private String activityName;

    /**
     * 操作人用户 ID。
     */
    @Schema(description = "操作人用户 ID")
    private Long actorId;

    /**
     * 操作人显示名。
     */
    @Schema(description = "操作人显示名")
    private String actorName;

    /**
     * 审批结论：{@code null}-提交或待审批，{@code true}-通过，{@code false}-驳回。
     */
    @Schema(description = "审批结论")
    private Boolean approved;

    /**
     * 审批意见或驳回原因。
     */
    @Schema(description = "审批意见或驳回原因")
    private String comment;

    /**
     * 节点发生时间。
     */
    @Schema(description = "节点发生时间")
    private LocalDateTime time;

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

    public String getActorName() {
        return actorName;
    }

    public void setActorName(final String actorName) {
        this.actorName = actorName;
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
