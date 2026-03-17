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
 * 定时任务响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "定时任务响应")
public class JobResponse {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务组名")
    private String jobGroup;

    @Schema(description = "调用目标字符串")
    private String invokeTarget;

    @Schema(description = "cron执行表达式")
    private String cronExpression;

    @Schema(description = "计划执行错误策略(1立即执行 2执行一次 3放弃执行)")
    private Integer misfirePolicy;

    @Schema(description = "是否并发执行(0禁止 1允许)")
    private Integer concurrent;

    @Schema(description = "状态(0暂停 1正常)")
    private Integer status;

    @Schema(description = "备注信息")
    private String remark;

    @Schema(description = "创建者")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updateBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @SuppressWarnings("all")
    public JobResponse() {
    }

    @SuppressWarnings("all")
    public JobResponse(final Long id, final String jobName, final String jobGroup, final String invokeTarget,
                       final String cronExpression, final Integer misfirePolicy, final Integer concurrent, final Integer status,
                       final String remark, final String createBy, final LocalDateTime createTime, final String updateBy,
                       final LocalDateTime updateTime) {
        this.id = id;
        this.jobName = jobName;
        this.jobGroup = jobGroup;
        this.invokeTarget = invokeTarget;
        this.cronExpression = cronExpression;
        this.misfirePolicy = misfirePolicy;
        this.concurrent = concurrent;
        this.status = status;
        this.remark = remark;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
    }

    @SuppressWarnings("all")
    public static JobResponse.JobResponseBuilder builder() {
        return new JobResponse.JobResponseBuilder();
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
    public String getCronExpression() {
        return this.cronExpression;
    }

    @SuppressWarnings("all")
    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    @SuppressWarnings("all")
    public Integer getMisfirePolicy() {
        return this.misfirePolicy;
    }

    @SuppressWarnings("all")
    public void setMisfirePolicy(final Integer misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }

    @SuppressWarnings("all")
    public Integer getConcurrent() {
        return this.concurrent;
    }

    @SuppressWarnings("all")
    public void setConcurrent(final Integer concurrent) {
        this.concurrent = concurrent;
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
    public String getRemark() {
        return this.remark;
    }

    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public String getCreateBy() {
        return this.createBy;
    }

    @SuppressWarnings("all")
    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    @SuppressWarnings("all")
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public String getUpdateBy() {
        return this.updateBy;
    }

    @SuppressWarnings("all")
    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    @SuppressWarnings("all")
    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    @SuppressWarnings("all")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JobResponse))
            return false;
        final JobResponse other = (JobResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$misfirePolicy = this.getMisfirePolicy();
        final java.lang.Object other$misfirePolicy = other.getMisfirePolicy();
        if (this$misfirePolicy == null ? other$misfirePolicy != null : !this$misfirePolicy.equals(other$misfirePolicy))
            return false;
        final java.lang.Object this$concurrent = this.getConcurrent();
        final java.lang.Object other$concurrent = other.getConcurrent();
        if (this$concurrent == null ? other$concurrent != null : !this$concurrent.equals(other$concurrent))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
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
        final java.lang.Object this$cronExpression = this.getCronExpression();
        final java.lang.Object other$cronExpression = other.getCronExpression();
        if (this$cronExpression == null ? other$cronExpression != null
                : !this$cronExpression.equals(other$cronExpression))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy))
            return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof JobResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $misfirePolicy = this.getMisfirePolicy();
        result = result * PRIME + ($misfirePolicy == null ? 43 : $misfirePolicy.hashCode());
        final java.lang.Object $concurrent = this.getConcurrent();
        result = result * PRIME + ($concurrent == null ? 43 : $concurrent.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $jobName = this.getJobName();
        result = result * PRIME + ($jobName == null ? 43 : $jobName.hashCode());
        final java.lang.Object $jobGroup = this.getJobGroup();
        result = result * PRIME + ($jobGroup == null ? 43 : $jobGroup.hashCode());
        final java.lang.Object $invokeTarget = this.getInvokeTarget();
        result = result * PRIME + ($invokeTarget == null ? 43 : $invokeTarget.hashCode());
        final java.lang.Object $cronExpression = this.getCronExpression();
        result = result * PRIME + ($cronExpression == null ? 43 : $cronExpression.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "JobResponse(id=" + this.getId() + ", jobName=" + this.getJobName() + ", jobGroup=" + this.getJobGroup()
                + ", invokeTarget=" + this.getInvokeTarget() + ", cronExpression=" + this.getCronExpression()
                + ", misfirePolicy=" + this.getMisfirePolicy() + ", concurrent=" + this.getConcurrent() + ", status="
                + this.getStatus() + ", remark=" + this.getRemark() + ", createBy=" + this.getCreateBy()
                + ", createTime=" + this.getCreateTime() + ", updateBy=" + this.getUpdateBy() + ", updateTime="
                + this.getUpdateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class JobResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String jobName;

        @SuppressWarnings("all")
        private String jobGroup;

        @SuppressWarnings("all")
        private String invokeTarget;

        @SuppressWarnings("all")
        private String cronExpression;

        @SuppressWarnings("all")
        private Integer misfirePolicy;

        @SuppressWarnings("all")
        private Integer concurrent;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        private String createBy;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private String updateBy;

        @SuppressWarnings("all")
        private LocalDateTime updateTime;

        @SuppressWarnings("all")
        JobResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder jobName(final String jobName) {
            this.jobName = jobName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder jobGroup(final String jobGroup) {
            this.jobGroup = jobGroup;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder invokeTarget(final String invokeTarget) {
            this.invokeTarget = invokeTarget;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder cronExpression(final String cronExpression) {
            this.cronExpression = cronExpression;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder misfirePolicy(final Integer misfirePolicy) {
            this.misfirePolicy = misfirePolicy;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder concurrent(final Integer concurrent) {
            this.concurrent = concurrent;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobResponse.JobResponseBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        @SuppressWarnings("all")
        public JobResponse build() {
            return new JobResponse(this.id, this.jobName, this.jobGroup, this.invokeTarget, this.cronExpression,
                    this.misfirePolicy, this.concurrent, this.status, this.remark, this.createBy, this.createTime,
                    this.updateBy, this.updateTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "JobResponse.JobResponseBuilder(id=" + this.id + ", jobName=" + this.jobName + ", jobGroup="
                    + this.jobGroup + ", invokeTarget=" + this.invokeTarget + ", cronExpression=" + this.cronExpression
                    + ", misfirePolicy=" + this.misfirePolicy + ", concurrent=" + this.concurrent + ", status="
                    + this.status + ", remark=" + this.remark + ", createBy=" + this.createBy + ", createTime="
                    + this.createTime + ", updateBy=" + this.updateBy + ", updateTime=" + this.updateTime + ")";
        }

    }

}
