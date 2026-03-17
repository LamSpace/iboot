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

package com.iboot.admin.domain.job.model;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志领域模型
 *
 * @author iBoot
 */
public class JobLog {

    /**
     * 执行状态：失败
     */
    public static final int STATUS_FAIL = 0;

    /**
     * 执行状态：成功
     */
    public static final int STATUS_SUCCESS = 1;

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 任务ID
     */
    private Long jobId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务组名
     */
    private String jobGroup;

    /**
     * 调用目标字符串
     */
    private String invokeTarget;

    /**
     * 日志信息
     */
    private String jobMessage;

    /**
     * 执行状态：0-失败，1-成功
     */
    private Integer status;

    /**
     * 异常信息
     */
    private String exceptionInfo;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long costTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    public JobLog() {
    }

    /**
     * Creates a new {@code JobLog} instance.
     *
     * @param id            日志ID
     * @param jobId         任务ID
     * @param jobName       任务名称
     * @param jobGroup      任务组名
     * @param invokeTarget  调用目标字符串
     * @param jobMessage    日志信息
     * @param status        执行状态：0-失败，1-成功
     * @param exceptionInfo 异常信息
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param costTime      执行耗时（毫秒）
     * @param createTime    创建时间
     */
    @SuppressWarnings("all")
    public JobLog(final Long id, final Long jobId, final String jobName, final String jobGroup,
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
    public static JobLog.JobLogBuilder builder() {
        return new JobLog.JobLogBuilder();
    }

    /**
     * 判断执行是否成功
     */
    public boolean isSuccess() {
        return STATUS_SUCCESS == this.status;
    }

    /**
     * 日志ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 日志ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 任务ID
     */
    @SuppressWarnings("all")
    public Long getJobId() {
        return this.jobId;
    }

    /**
     * 任务ID
     */
    @SuppressWarnings("all")
    public void setJobId(final Long jobId) {
        this.jobId = jobId;
    }

    /**
     * 任务名称
     */
    @SuppressWarnings("all")
    public String getJobName() {
        return this.jobName;
    }

    /**
     * 任务名称
     */
    @SuppressWarnings("all")
    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    /**
     * 任务组名
     */
    @SuppressWarnings("all")
    public String getJobGroup() {
        return this.jobGroup;
    }

    /**
     * 任务组名
     */
    @SuppressWarnings("all")
    public void setJobGroup(final String jobGroup) {
        this.jobGroup = jobGroup;
    }

    /**
     * 调用目标字符串
     */
    @SuppressWarnings("all")
    public String getInvokeTarget() {
        return this.invokeTarget;
    }

    /**
     * 调用目标字符串
     */
    @SuppressWarnings("all")
    public void setInvokeTarget(final String invokeTarget) {
        this.invokeTarget = invokeTarget;
    }

    /**
     * 日志信息
     */
    @SuppressWarnings("all")
    public String getJobMessage() {
        return this.jobMessage;
    }

    /**
     * 日志信息
     */
    @SuppressWarnings("all")
    public void setJobMessage(final String jobMessage) {
        this.jobMessage = jobMessage;
    }

    /**
     * 执行状态：0-失败，1-成功
     */
    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 执行状态：0-失败，1-成功
     */
    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 异常信息
     */
    @SuppressWarnings("all")
    public String getExceptionInfo() {
        return this.exceptionInfo;
    }

    /**
     * 异常信息
     */
    @SuppressWarnings("all")
    public void setExceptionInfo(final String exceptionInfo) {
        this.exceptionInfo = exceptionInfo;
    }

    /**
     * 开始时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    /**
     * 开始时间
     */
    @SuppressWarnings("all")
    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * 结束时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    /**
     * 结束时间
     */
    @SuppressWarnings("all")
    public void setEndTime(final LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * 执行耗时（毫秒）
     */
    @SuppressWarnings("all")
    public Long getCostTime() {
        return this.costTime;
    }

    /**
     * 执行耗时（毫秒）
     */
    @SuppressWarnings("all")
    public void setCostTime(final Long costTime) {
        this.costTime = costTime;
    }

    /**
     * 创建时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    /**
     * 创建时间
     */
    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JobLog))
            return false;
        final JobLog other = (JobLog) o;
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
        return other instanceof JobLog;
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
        return "JobLog(id=" + this.getId() + ", jobId=" + this.getJobId() + ", jobName=" + this.getJobName()
                + ", jobGroup=" + this.getJobGroup() + ", invokeTarget=" + this.getInvokeTarget() + ", jobMessage="
                + this.getJobMessage() + ", status=" + this.getStatus() + ", exceptionInfo=" + this.getExceptionInfo()
                + ", startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ", costTime="
                + this.getCostTime() + ", createTime=" + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class JobLogBuilder {

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
        JobLogBuilder() {
        }

        /**
         * 日志ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 任务ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder jobId(final Long jobId) {
            this.jobId = jobId;
            return this;
        }

        /**
         * 任务名称
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder jobName(final String jobName) {
            this.jobName = jobName;
            return this;
        }

        /**
         * 任务组名
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder jobGroup(final String jobGroup) {
            this.jobGroup = jobGroup;
            return this;
        }

        /**
         * 调用目标字符串
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder invokeTarget(final String invokeTarget) {
            this.invokeTarget = invokeTarget;
            return this;
        }

        /**
         * 日志信息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder jobMessage(final String jobMessage) {
            this.jobMessage = jobMessage;
            return this;
        }

        /**
         * 执行状态：0-失败，1-成功
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * 异常信息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder exceptionInfo(final String exceptionInfo) {
            this.exceptionInfo = exceptionInfo;
            return this;
        }

        /**
         * 开始时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder startTime(final LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * 结束时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder endTime(final LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        /**
         * 执行耗时（毫秒）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder costTime(final Long costTime) {
            this.costTime = costTime;
            return this;
        }

        /**
         * 创建时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public JobLog.JobLogBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public JobLog build() {
            return new JobLog(this.id, this.jobId, this.jobName, this.jobGroup, this.invokeTarget, this.jobMessage,
                    this.status, this.exceptionInfo, this.startTime, this.endTime, this.costTime, this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "JobLog.JobLogBuilder(id=" + this.id + ", jobId=" + this.jobId + ", jobName=" + this.jobName
                    + ", jobGroup=" + this.jobGroup + ", invokeTarget=" + this.invokeTarget + ", jobMessage="
                    + this.jobMessage + ", status=" + this.status + ", exceptionInfo=" + this.exceptionInfo
                    + ", startTime=" + this.startTime + ", endTime=" + this.endTime + ", costTime=" + this.costTime
                    + ", createTime=" + this.createTime + ")";
        }

    }

}
