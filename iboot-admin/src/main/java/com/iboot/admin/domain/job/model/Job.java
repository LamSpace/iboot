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

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务领域模型
 *
 * @author iBoot
 */
public class Job implements Serializable {

    /**
     * 错过执行策略：立即执行
     */
    public static final int MISFIRE_DO_NOTHING = 1;

    /**
     * 错过执行策略：执行一次
     */
    public static final int MISFIRE_FIRE_AND_PROCEED = 2;

    /**
     * 错过执行策略：放弃执行
     */
    public static final int MISFIRE_IGNORE_MISFIRES = 3;

    /**
     * 任务状态：暂停
     */
    public static final int STATUS_PAUSE = 0;

    /**
     * 任务状态：正常
     */
    public static final int STATUS_NORMAL = 1;

    /**
     * 禁止并发执行
     */
    public static final int CONCURRENT_NO = 0;

    /**
     * 允许并发执行
     */
    public static final int CONCURRENT_YES = 1;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务组名
     */
    private String jobGroup;

    /**
     * 调用目标字符串(beanName.methodName)
     */
    private String invokeTarget;

    /**
     * cron执行表达式
     */
    private String cronExpression;

    /**
     * 计划执行错误策略：1-立即执行，2-执行一次，3-放弃执行
     */
    private Integer misfirePolicy;

    /**
     * 是否并发执行：0-禁止，1-允许
     */
    private Integer concurrent;

    /**
     * 状态：0-暂停，1-正常
     */
    private Integer status;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    @SuppressWarnings("all")
    public Job() {
    }

    /**
     * Creates a new {@code Job} instance.
     *
     * @param id             任务ID
     * @param jobName        任务名称
     * @param jobGroup       任务组名
     * @param invokeTarget   调用目标字符串(beanName.methodName)
     * @param cronExpression cron执行表达式
     * @param misfirePolicy  计划执行错误策略：1-立即执行，2-执行一次，3-放弃执行
     * @param concurrent     是否并发执行：0-禁止，1-允许
     * @param status         状态：0-暂停，1-正常
     * @param remark         备注信息
     * @param createBy       创建者
     * @param createTime     创建时间
     * @param updateBy       更新者
     * @param updateTime     更新时间
     */
    @SuppressWarnings("all")
    public Job(final Long id, final String jobName, final String jobGroup, final String invokeTarget,
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
    public static Job.JobBuilder builder() {
        return new Job.JobBuilder();
    }

    /**
     * 判断任务是否正常运行
     */
    public boolean isNormal() {
        return STATUS_NORMAL == this.status;
    }

    /**
     * 判断任务是否暂停
     */
    public boolean isPaused() {
        return STATUS_PAUSE == this.status;
    }

    /**
     * 判断是否禁止并发
     */
    public boolean isConcurrentDisallowed() {
        return CONCURRENT_NO == this.concurrent;
    }

    /**
     * 任务ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 任务ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
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
     * 调用目标字符串(beanName.methodName)
     */
    @SuppressWarnings("all")
    public String getInvokeTarget() {
        return this.invokeTarget;
    }

    /**
     * 调用目标字符串(beanName.methodName)
     */
    @SuppressWarnings("all")
    public void setInvokeTarget(final String invokeTarget) {
        this.invokeTarget = invokeTarget;
    }

    /**
     * cron执行表达式
     */
    @SuppressWarnings("all")
    public String getCronExpression() {
        return this.cronExpression;
    }

    /**
     * cron执行表达式
     */
    @SuppressWarnings("all")
    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * 计划执行错误策略：1-立即执行，2-执行一次，3-放弃执行
     */
    @SuppressWarnings("all")
    public Integer getMisfirePolicy() {
        return this.misfirePolicy;
    }

    /**
     * 计划执行错误策略：1-立即执行，2-执行一次，3-放弃执行
     */
    @SuppressWarnings("all")
    public void setMisfirePolicy(final Integer misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }

    /**
     * 是否并发执行：0-禁止，1-允许
     */
    @SuppressWarnings("all")
    public Integer getConcurrent() {
        return this.concurrent;
    }

    /**
     * 是否并发执行：0-禁止，1-允许
     */
    @SuppressWarnings("all")
    public void setConcurrent(final Integer concurrent) {
        this.concurrent = concurrent;
    }

    /**
     * 状态：0-暂停，1-正常
     */
    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 状态：0-暂停，1-正常
     */
    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 备注信息
     */
    @SuppressWarnings("all")
    public String getRemark() {
        return this.remark;
    }

    /**
     * 备注信息
     */
    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    /**
     * 创建者
     */
    @SuppressWarnings("all")
    public String getCreateBy() {
        return this.createBy;
    }

    /**
     * 创建者
     */
    @SuppressWarnings("all")
    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
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

    /**
     * 更新者
     */
    @SuppressWarnings("all")
    public String getUpdateBy() {
        return this.updateBy;
    }

    /**
     * 更新者
     */
    @SuppressWarnings("all")
    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 更新时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    /**
     * 更新时间
     */
    @SuppressWarnings("all")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Job))
            return false;
        final Job other = (Job) o;
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
        return other instanceof Job;
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
        return "Job(id=" + this.getId() + ", jobName=" + this.getJobName() + ", jobGroup=" + this.getJobGroup()
                + ", invokeTarget=" + this.getInvokeTarget() + ", cronExpression=" + this.getCronExpression()
                + ", misfirePolicy=" + this.getMisfirePolicy() + ", concurrent=" + this.getConcurrent() + ", status="
                + this.getStatus() + ", remark=" + this.getRemark() + ", createBy=" + this.getCreateBy()
                + ", createTime=" + this.getCreateTime() + ", updateBy=" + this.getUpdateBy() + ", updateTime="
                + this.getUpdateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class JobBuilder {

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
        JobBuilder() {
        }

        /**
         * 任务ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 任务名称
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder jobName(final String jobName) {
            this.jobName = jobName;
            return this;
        }

        /**
         * 任务组名
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder jobGroup(final String jobGroup) {
            this.jobGroup = jobGroup;
            return this;
        }

        /**
         * 调用目标字符串(beanName.methodName)
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder invokeTarget(final String invokeTarget) {
            this.invokeTarget = invokeTarget;
            return this;
        }

        /**
         * cron执行表达式
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder cronExpression(final String cronExpression) {
            this.cronExpression = cronExpression;
            return this;
        }

        /**
         * 计划执行错误策略：1-立即执行，2-执行一次，3-放弃执行
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder misfirePolicy(final Integer misfirePolicy) {
            this.misfirePolicy = misfirePolicy;
            return this;
        }

        /**
         * 是否并发执行：0-禁止，1-允许
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder concurrent(final Integer concurrent) {
            this.concurrent = concurrent;
            return this;
        }

        /**
         * 状态：0-暂停，1-正常
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * 备注信息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * 创建者
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * 创建时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * 更新者
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * 更新时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Job.JobBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        @SuppressWarnings("all")
        public Job build() {
            return new Job(this.id, this.jobName, this.jobGroup, this.invokeTarget, this.cronExpression,
                    this.misfirePolicy, this.concurrent, this.status, this.remark, this.createBy, this.createTime,
                    this.updateBy, this.updateTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "Job.JobBuilder(id=" + this.id + ", jobName=" + this.jobName + ", jobGroup=" + this.jobGroup
                    + ", invokeTarget=" + this.invokeTarget + ", cronExpression=" + this.cronExpression
                    + ", misfirePolicy=" + this.misfirePolicy + ", concurrent=" + this.concurrent + ", status="
                    + this.status + ", remark=" + this.remark + ", createBy=" + this.createBy + ", createTime="
                    + this.createTime + ", updateBy=" + this.updateBy + ", updateTime=" + this.updateTime + ")";
        }

    }

}
