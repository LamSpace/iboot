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

package com.iboot.admin.interfaces.dto.export;

import com.iboot.admin.common.annotation.ExcelColumn;

import java.time.LocalDateTime;

/**
 * 定时任务导出VO
 *
 * @author iBoot
 */
public class JobExportVO {

    @ExcelColumn(name = "任务ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "任务名称", order = 2, width = 25)
    private String jobName;

    @ExcelColumn(name = "任务组名", order = 3, width = 15, dictType = "sys_job_group")
    private String jobGroup;

    @ExcelColumn(name = "调用目标", order = 4, width = 40)
    private String invokeTarget;

    @ExcelColumn(name = "cron表达式", order = 5, width = 20)
    private String cronExpression;

    @ExcelColumn(name = "错过策略", order = 6, width = 12, dictType = "sys_job_misfire")
    private Integer misfirePolicy;

    @ExcelColumn(name = "是否并发", order = 7, width = 10, dictType = "sys_job_concurrent")
    private Integer concurrent;

    @ExcelColumn(name = "状态", order = 8, width = 8, dictType = "sys_job_status")
    private Integer status;

    @ExcelColumn(name = "备注", order = 9, width = 30)
    private String remark;

    @ExcelColumn(name = "创建时间", order = 10, width = 20)
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    public JobExportVO() {
    }

    @SuppressWarnings("all")
    public JobExportVO(final Long id, final String jobName, final String jobGroup, final String invokeTarget,
                       final String cronExpression, final Integer misfirePolicy, final Integer concurrent, final Integer status,
                       final String remark, final LocalDateTime createTime) {
        this.id = id;
        this.jobName = jobName;
        this.jobGroup = jobGroup;
        this.invokeTarget = invokeTarget;
        this.cronExpression = cronExpression;
        this.misfirePolicy = misfirePolicy;
        this.concurrent = concurrent;
        this.status = status;
        this.remark = remark;
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public static JobExportVO.JobExportVOBuilder builder() {
        return new JobExportVO.JobExportVOBuilder();
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
        if (!(o instanceof JobExportVO))
            return false;
        final JobExportVO other = (JobExportVO) o;
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
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof JobExportVO;
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
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "JobExportVO(id=" + this.getId() + ", jobName=" + this.getJobName() + ", jobGroup=" + this.getJobGroup()
                + ", invokeTarget=" + this.getInvokeTarget() + ", cronExpression=" + this.getCronExpression()
                + ", misfirePolicy=" + this.getMisfirePolicy() + ", concurrent=" + this.getConcurrent() + ", status="
                + this.getStatus() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class JobExportVOBuilder {

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
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        JobExportVOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobExportVO.JobExportVOBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobExportVO.JobExportVOBuilder jobName(final String jobName) {
            this.jobName = jobName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobExportVO.JobExportVOBuilder jobGroup(final String jobGroup) {
            this.jobGroup = jobGroup;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobExportVO.JobExportVOBuilder invokeTarget(final String invokeTarget) {
            this.invokeTarget = invokeTarget;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobExportVO.JobExportVOBuilder cronExpression(final String cronExpression) {
            this.cronExpression = cronExpression;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobExportVO.JobExportVOBuilder misfirePolicy(final Integer misfirePolicy) {
            this.misfirePolicy = misfirePolicy;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobExportVO.JobExportVOBuilder concurrent(final Integer concurrent) {
            this.concurrent = concurrent;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobExportVO.JobExportVOBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobExportVO.JobExportVOBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobExportVO.JobExportVOBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public JobExportVO build() {
            return new JobExportVO(this.id, this.jobName, this.jobGroup, this.invokeTarget, this.cronExpression,
                    this.misfirePolicy, this.concurrent, this.status, this.remark, this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "JobExportVO.JobExportVOBuilder(id=" + this.id + ", jobName=" + this.jobName + ", jobGroup="
                    + this.jobGroup + ", invokeTarget=" + this.invokeTarget + ", cronExpression=" + this.cronExpression
                    + ", misfirePolicy=" + this.misfirePolicy + ", concurrent=" + this.concurrent + ", status="
                    + this.status + ", remark=" + this.remark + ", createTime=" + this.createTime + ")";
        }

    }

}
