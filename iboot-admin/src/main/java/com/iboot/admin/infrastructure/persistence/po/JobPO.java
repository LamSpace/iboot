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

package com.iboot.admin.infrastructure.persistence.po;

import java.time.LocalDateTime;

/**
 * 定时任务持久化对象
 * <p>
 * 对应数据库表：sys_job
 * </p>
 *
 * @author iBoot
 */
public class JobPO {

    /**
     * 任务 ID
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
     * 所属部门 ID
     */
    private Long deptId;

    /**
     * 调用目标字符串
     */
    private String invokeTarget;

    /**
     * Cron 执行表达式
     */
    private String cronExpression;

    /**
     * 计划执行错误策略：1-立即执行，2-放弃执行
     */
    private Integer misfirePolicy;

    /**
     * 是否并发执行：0-禁止，1-允许
     */
    private Integer concurrent;

    /**
     * 状态：0-停用，1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;

    @SuppressWarnings("all")
    public JobPO() {
    }

    /**
     * 任务 ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 任务 ID
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
     * 所属部门 ID
     */
    @SuppressWarnings("all")
    public Long getDeptId() {
        return this.deptId;
    }

    /**
     * 所属部门 ID
     */
    @SuppressWarnings("all")
    public void setDeptId(final Long deptId) {
        this.deptId = deptId;
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
     * Cron 执行表达式
     */
    @SuppressWarnings("all")
    public String getCronExpression() {
        return this.cronExpression;
    }

    /**
     * Cron 执行表达式
     */
    @SuppressWarnings("all")
    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * 计划执行错误策略：1-立即执行，2-放弃执行
     */
    @SuppressWarnings("all")
    public Integer getMisfirePolicy() {
        return this.misfirePolicy;
    }

    /**
     * 计划执行错误策略：1-立即执行，2-放弃执行
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
     * 状态：0-停用，1-启用
     */
    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 状态：0-停用，1-启用
     */
    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 备注
     */
    @SuppressWarnings("all")
    public String getRemark() {
        return this.remark;
    }

    /**
     * 备注
     */
    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    /**
     * 创建人
     */
    @SuppressWarnings("all")
    public String getCreateBy() {
        return this.createBy;
    }

    /**
     * 创建人
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
     * 更新人
     */
    @SuppressWarnings("all")
    public String getUpdateBy() {
        return this.updateBy;
    }

    /**
     * 更新人
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

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JobPO))
            return false;
        final JobPO other = (JobPO) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$deptId = this.getDeptId();
        final java.lang.Object other$deptId = other.getDeptId();
        if (this$deptId == null ? other$deptId != null : !this$deptId.equals(other$deptId))
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
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
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
        return other instanceof JobPO;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $deptId = this.getDeptId();
        result = result * PRIME + ($deptId == null ? 43 : $deptId.hashCode());
        final java.lang.Object $misfirePolicy = this.getMisfirePolicy();
        result = result * PRIME + ($misfirePolicy == null ? 43 : $misfirePolicy.hashCode());
        final java.lang.Object $concurrent = this.getConcurrent();
        result = result * PRIME + ($concurrent == null ? 43 : $concurrent.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
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
        return "JobPO(id=" + this.getId() + ", jobName=" + this.getJobName() + ", jobGroup=" + this.getJobGroup()
                + ", deptId=" + this.getDeptId() + ", invokeTarget=" + this.getInvokeTarget() + ", cronExpression="
                + this.getCronExpression() + ", misfirePolicy=" + this.getMisfirePolicy() + ", concurrent="
                + this.getConcurrent() + ", status=" + this.getStatus() + ", remark=" + this.getRemark() + ", createBy="
                + this.getCreateBy() + ", createTime=" + this.getCreateTime() + ", updateBy=" + this.getUpdateBy()
                + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }

}
