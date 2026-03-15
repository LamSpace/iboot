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

package com.iboot.admin.infrastructure.job;

import com.iboot.admin.common.util.SpringUtils;
import com.iboot.admin.domain.job.model.Job;
import com.iboot.admin.domain.job.model.JobLog;
import com.iboot.admin.domain.job.repository.JobLogRepository;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

/**
 * 抽象Quartz任务调用
 *
 * @author iBoot
 */
public abstract class AbstractQuartzJob implements org.quartz.Job {

    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

    /**
     * 线程本地变量
     */
    private static final ThreadLocal<LocalDateTime> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public void execute(JobExecutionContext context) {
        Job job = (Job) context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES);
        try {
            before(context, job);
            doExecute(context, job);
            after(context, job, null);
        } catch (Exception e) {
            log.error("任务执行异常 - 任务名称: {}", job.getJobName(), e);
            after(context, job, e);
        }
    }

    /**
     * 执行前
     */
    protected void before(JobExecutionContext context, Job job) {
        THREAD_LOCAL.set(LocalDateTime.now());
    }

    /**
     * 执行后
     */
    protected void after(JobExecutionContext context, Job job, Exception e) {
        LocalDateTime startTime = THREAD_LOCAL.get();
        THREAD_LOCAL.remove();

        final JobLog jobLog = new JobLog();
        jobLog.setJobId(job.getId());
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setInvokeTarget(job.getInvokeTarget());
        jobLog.setStartTime(startTime);
        jobLog.setEndTime(LocalDateTime.now());

        long runTime = java.time.Duration.between(startTime, jobLog.getEndTime()).toMillis();
        jobLog.setCostTime(runTime);

        if (e != null) {
            jobLog.setStatus(JobLog.STATUS_FAIL);
            String errorMsg = getExceptionMessage(e);
            jobLog.setJobMessage("执行失败");
            jobLog.setExceptionInfo(errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);
        } else {
            jobLog.setStatus(JobLog.STATUS_SUCCESS);
            jobLog.setJobMessage("执行成功");
        }

        // 异步保存日志
        saveJobLog(jobLog);
    }

    /**
     * 保存任务执行日志
     */
    private void saveJobLog(JobLog jobLog) {
        try {
            JobLogRepository jobLogRepository = SpringUtils.getBean(JobLogRepository.class);
            jobLogRepository.save(jobLog);
        } catch (Exception e) {
            log.error("保存任务执行日志失败", e);
        }
    }

    /**
     * 获取异常信息
     */
    private String getExceptionMessage(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }

    /**
     * 执行方法，由子类重载
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     * @throws Exception 执行过程中的异常
     */
    protected abstract void doExecute(JobExecutionContext context, Job job) throws Exception;
}
