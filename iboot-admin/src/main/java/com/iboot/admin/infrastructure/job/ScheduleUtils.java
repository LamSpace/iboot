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

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.job.model.Job;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定时任务调度工具类
 *
 * @author iBoot
 */
public final class ScheduleUtils {

    private static final Logger log = LoggerFactory.getLogger(ScheduleUtils.class);

    private ScheduleUtils() {
    }

    /**
     * 获取 Quartz Job类
     *
     * @param job 定时任务
     * @return Job类
     */
    private static Class<? extends org.quartz.Job> getQuartzJobClass(Job job) {
        boolean isConcurrent = job.getConcurrent() == Job.CONCURRENT_YES;
        return isConcurrent ? QuartzJobExecution.class : QuartzDisallowConcurrentExecution.class;
    }

    /**
     * 获取触发器 key
     */
    public static TriggerKey getTriggerKey(Long jobId, String jobGroup) {
        return TriggerKey.triggerKey(ScheduleConstants.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 获取Job key
     */
    public static JobKey getJobKey(Long jobId, String jobGroup) {
        return JobKey.jobKey(ScheduleConstants.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 创建定时任务
     */
    public static void createScheduleJob(Scheduler scheduler, Job job) throws SchedulerException {
        Class<? extends org.quartz.Job> jobClass = getQuartzJobClass(job);

        // 构建job信息
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(getJobKey(jobId, jobGroup))
                .build();

        // 表达式调度构建器
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        cronScheduleBuilder = handleCronScheduleMisfirePolicy(job, cronScheduleBuilder);

        // 按新的cronExpression表达式构建一个新的trigger
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(getTriggerKey(jobId, jobGroup))
                .withSchedule(cronScheduleBuilder)
                .build();

        // 放入参数，运行时的方法可以获取
        jobDetail.getJobDataMap().put(ScheduleConstants.TASK_PROPERTIES, job);

        // 判断是否存在
        if (scheduler.checkExists(getJobKey(jobId, jobGroup))) {
            // 防止创建时存在数据问题 先移除，然后再执行创建操作
            scheduler.deleteJob(getJobKey(jobId, jobGroup));
        }

        scheduler.scheduleJob(jobDetail, trigger);

        // 暂停任务
        if (job.isPaused()) {
            scheduler.pauseJob(getJobKey(jobId, jobGroup));
        }
    }

    /**
     * 更新定时任务
     */
    public static void updateScheduleJob(Scheduler scheduler, Job job) throws SchedulerException {
        JobKey jobKey = getJobKey(job.getId(), job.getJobGroup());
        if (scheduler.checkExists(jobKey)) {
            // 先删除，再创建
            scheduler.deleteJob(jobKey);
        }
        createScheduleJob(scheduler, job);
    }

    /**
     * 立即执行任务
     */
    public static void runJob(Scheduler scheduler, Job job) throws SchedulerException {
        // 参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, job);
        JobKey jobKey = getJobKey(job.getId(), job.getJobGroup());
        if (scheduler.checkExists(jobKey)) {
            scheduler.triggerJob(jobKey, dataMap);
        } else {
            // 如果任务不存在，先创建再执行
            createScheduleJob(scheduler, job);
            scheduler.triggerJob(jobKey, dataMap);
        }
    }

    /**
     * 暂停任务
     */
    public static void pauseJob(Scheduler scheduler, Long jobId, String jobGroup) throws SchedulerException {
        scheduler.pauseJob(getJobKey(jobId, jobGroup));
    }

    /**
     * 恢复任务
     */
    public static void resumeJob(Scheduler scheduler, Long jobId, String jobGroup) throws SchedulerException {
        scheduler.resumeJob(getJobKey(jobId, jobGroup));
    }

    /**
     * 删除定时任务
     */
    public static void deleteScheduleJob(Scheduler scheduler, Long jobId, String jobGroup) throws SchedulerException {
        scheduler.deleteJob(getJobKey(jobId, jobGroup));
    }

    /**
     * 设置定时任务策略
     */
    public static CronScheduleBuilder handleCronScheduleMisfirePolicy(Job job, CronScheduleBuilder cb) {
        return switch (job.getMisfirePolicy()) {
            case Job.MISFIRE_DO_NOTHING -> cb.withMisfireHandlingInstructionDoNothing();
            case Job.MISFIRE_FIRE_AND_PROCEED -> cb.withMisfireHandlingInstructionFireAndProceed();
            case Job.MISFIRE_IGNORE_MISFIRES -> cb.withMisfireHandlingInstructionIgnoreMisfires();
            default -> throw new BusinessException("不支持的错过执行策略: " + job.getMisfirePolicy());
        };
    }

    /**
     * 检查cron表达式是否有效
     *
     * @param cronExpression cron表达式
     * @return 是否有效
     */
    public static boolean isValidCronExpression(String cronExpression) {
        return CronExpression.isValidExpression(cronExpression);
    }
}
