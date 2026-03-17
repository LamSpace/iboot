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
 * 定时任务日志响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "定时任务日志响应")
public class JobLogResponse {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long jobId;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务组名")
    private String jobGroup;

    @Schema(description = "调用目标字符串")
    private String invokeTarget;

    @Schema(description = "日志信息")
    private String jobMessage;

    @Schema(description = "执行状态(0失败 1成功)")
    private Integer status;

    @Schema(description = "异常信息")
    private String exceptionInfo;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "执行耗时(毫秒)")
    private Long costTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    public JobLogResponse() {
    }

    @SuppressWarnings("all")
    public JobLogResponse(final Long id, final Long jobId, final String jobName, final String jobGroup,
                          final String invokeTarget, final String jobMessage, final Integer status, final String exceptionInfo,
                          final LocalDateTime startTime, final LocalDateTime endTime, final Long costTime,
                          final LocalDateTime createTime) {
        this.id = id;
        this.jobId = jobId;
        this.jobName = jobName;
        this.jobGroup = jobGroup;
        this.invokeTarget = invokeTarget;
        this.jobMessage = jobMessage;
        this.status = status;
        this.exceptionInfo = exceptionInfo;
        this.startTime = startTime;
        this.endTime = endTime;
        this.costTime = costTime;
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public static JobLogResponse.JobLogResponseBuilder builder() {
        return new JobLogResponse.JobLogResponseBuilder();
    }

    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @SuppressWarnings("all")
    public Long getJobId() {
        return this.jobId;
    }

    @SuppressWarnings("all")
    public void setJobId(final Long jobId) {
        this.jobId = jobId;
    }

    @SuppressWarnings("all")
    public String getJobName() {
        return this.jobName;
    }

    @SuppressWarnings("all")
    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    @SuppressWarnings("all")
    public String getJobGroup() {
        return this.jobGroup;
    }

    @SuppressWarnings("all")
    public void setJobGroup(final String jobGroup) {
        this.jobGroup = jobGroup;
    }

    @SuppressWarnings("all")
    public String getInvokeTarget() {
        return this.invokeTarget;
    }

    @SuppressWarnings("all")
    public void setInvokeTarget(final String invokeTarget) {
        this.invokeTarget = invokeTarget;
    }

    @SuppressWarnings("all")
    public String getJobMessage() {
        return this.jobMessage;
    }

    @SuppressWarnings("all")
    public void setJobMessage(final String jobMessage) {
        this.jobMessage = jobMessage;
    }

    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    @SuppressWarnings("all")
    public String getExceptionInfo() {
        return this.exceptionInfo;
    }

    @SuppressWarnings("all")
    public void setExceptionInfo(final String exceptionInfo) {
        this.exceptionInfo = exceptionInfo;
    }

    @SuppressWarnings("all")
    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    @SuppressWarnings("all")
    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    @SuppressWarnings("all")
    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    @SuppressWarnings("all")
    public void setEndTime(final LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @SuppressWarnings("all")
    public Long getCostTime() {
        return this.costTime;
    }

    @SuppressWarnings("all")
    public void setCostTime(final Long costTime) {
        this.costTime = costTime;
    }

    @SuppressWarnings("all")
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JobLogResponse))
            return false;
        final JobLogResponse other = (JobLogResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$jobId = this.getJobId();
        final java.lang.Object other$jobId = other.getJobId();
        if (this$jobId == null ? other$jobId != null : !this$jobId.equals(other$jobId))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$costTime = this.getCostTime();
        final java.lang.Object other$costTime = other.getCostTime();
        if (this$costTime == null ? other$costTime != null : !this$costTime.equals(other$costTime))
            return false;
        final java.lang.Object this$jobName = this.getJobName();
        final java.lang.Object other$jobName = other.getJobName();
        if (this$jobName == null ? other$jobName != null : !this$jobName.equals(other$jobName))
            return false;
        final java.lang.Object this$jobGroup = this.getJobGroup();
        final java.lang.Object other$jobGroup = other.getJobGroup();
        if (this$jobGroup == null ? other$jobGroup != null : !this$jobGroup.equals(other$jobGroup))
            return false;
        final java.lang.Object this$invokeTarget = this.getInvokeTarget();
        final java.lang.Object other$invokeTarget = other.getInvokeTarget();
        if (this$invokeTarget == null ? other$invokeTarget != null : !this$invokeTarget.equals(other$invokeTarget))
            return false;
        final java.lang.Object this$jobMessage = this.getJobMessage();
        final java.lang.Object other$jobMessage = other.getJobMessage();
        if (this$jobMessage == null ? other$jobMessage != null : !this$jobMessage.equals(other$jobMessage))
            return false;
        final java.lang.Object this$exceptionInfo = this.getExceptionInfo();
        final java.lang.Object other$exceptionInfo = other.getExceptionInfo();
        if (this$exceptionInfo == null ? other$exceptionInfo != null : !this$exceptionInfo.equals(other$exceptionInfo))
            return false;
        final java.lang.Object this$startTime = this.getStartTime();
        final java.lang.Object other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !this$startTime.equals(other$startTime))
            return false;
        final java.lang.Object this$endTime = this.getEndTime();
        final java.lang.Object other$endTime = other.getEndTime();
        if (this$endTime == null ? other$endTime != null : !this$endTime.equals(other$endTime))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof JobLogResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $jobId = this.getJobId();
        result = result * PRIME + ($jobId == null ? 43 : $jobId.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $costTime = this.getCostTime();
        result = result * PRIME + ($costTime == null ? 43 : $costTime.hashCode());
        final java.lang.Object $jobName = this.getJobName();
        result = result * PRIME + ($jobName == null ? 43 : $jobName.hashCode());
        final java.lang.Object $jobGroup = this.getJobGroup();
        result = result * PRIME + ($jobGroup == null ? 43 : $jobGroup.hashCode());
        final java.lang.Object $invokeTarget = this.getInvokeTarget();
        result = result * PRIME + ($invokeTarget == null ? 43 : $invokeTarget.hashCode());
        final java.lang.Object $jobMessage = this.getJobMessage();
        result = result * PRIME + ($jobMessage == null ? 43 : $jobMessage.hashCode());
        final java.lang.Object $exceptionInfo = this.getExceptionInfo();
        result = result * PRIME + ($exceptionInfo == null ? 43 : $exceptionInfo.hashCode());
        final java.lang.Object $startTime = this.getStartTime();
        result = result * PRIME + ($startTime == null ? 43 : $startTime.hashCode());
        final java.lang.Object $endTime = this.getEndTime();
        result = result * PRIME + ($endTime == null ? 43 : $endTime.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "JobLogResponse(id=" + this.getId() + ", jobId=" + this.getJobId() + ", jobName=" + this.getJobName()
                + ", jobGroup=" + this.getJobGroup() + ", invokeTarget=" + this.getInvokeTarget() + ", jobMessage="
                + this.getJobMessage() + ", status=" + this.getStatus() + ", exceptionInfo=" + this.getExceptionInfo()
                + ", startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ", costTime="
                + this.getCostTime() + ", createTime=" + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class JobLogResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private Long jobId;

        @SuppressWarnings("all")
        private String jobName;

        @SuppressWarnings("all")
        private String jobGroup;

        @SuppressWarnings("all")
        private String invokeTarget;

        @SuppressWarnings("all")
        private String jobMessage;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String exceptionInfo;

        @SuppressWarnings("all")
        private LocalDateTime startTime;

        @SuppressWarnings("all")
        private LocalDateTime endTime;

        @SuppressWarnings("all")
        private Long costTime;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        JobLogResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder jobId(final Long jobId) {
            this.jobId = jobId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder jobName(final String jobName) {
            this.jobName = jobName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder jobGroup(final String jobGroup) {
            this.jobGroup = jobGroup;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder invokeTarget(final String invokeTarget) {
            this.invokeTarget = invokeTarget;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder jobMessage(final String jobMessage) {
            this.jobMessage = jobMessage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder exceptionInfo(final String exceptionInfo) {
            this.exceptionInfo = exceptionInfo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder startTime(final LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder endTime(final LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder costTime(final Long costTime) {
            this.costTime = costTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLogResponse.JobLogResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public JobLogResponse build() {
            return new JobLogResponse(this.id, this.jobId, this.jobName, this.jobGroup, this.invokeTarget,
                    this.jobMessage, this.status, this.exceptionInfo, this.startTime, this.endTime, this.costTime,
                    this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "JobLogResponse.JobLogResponseBuilder(id=" + this.id + ", jobId=" + this.jobId + ", jobName="
                    + this.jobName + ", jobGroup=" + this.jobGroup + ", invokeTarget=" + this.invokeTarget
                    + ", jobMessage=" + this.jobMessage + ", status=" + this.status + ", exceptionInfo="
                    + this.exceptionInfo + ", startTime=" + this.startTime + ", endTime=" + this.endTime + ", costTime="
                    + this.costTime + ", createTime=" + this.createTime + ")";
        }

    }

}
