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

package com.iboot.admin.application.service;

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.job.model.Job;
import com.iboot.admin.domain.job.model.JobLog;
import com.iboot.admin.domain.job.repository.JobLogRepository;
import com.iboot.admin.domain.job.repository.JobRepository;
import com.iboot.admin.infrastructure.job.ScheduleUtils;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 定时任务应用服务
 * <p>
 * 负责定时任务的创建、更新、删除、执行、暂停、恢复和查询等业务逻辑处理。 基于 Quartz 调度框架实现任务调度，支持 Cron 表达式配置执行计划。 同时提供任务日志的查询和清理功能。
 * </p>
 *
 * @author iBoot
 */
@Service
public class JobApplicationService {

    private static final Logger log = LoggerFactory.getLogger(JobApplicationService.class);

    private final JobRepository jobRepository;

    private final JobLogRepository jobLogRepository;

    private final Scheduler scheduler;

    @SuppressWarnings("all")
    public JobApplicationService(final JobRepository jobRepository, final JobLogRepository jobLogRepository,
                                 final Scheduler scheduler) {
        this.jobRepository = jobRepository;
        this.jobLogRepository = jobLogRepository;
        this.scheduler = scheduler;
    }

    /**
     * 创建定时任务
     * <p>
     * 校验 Cron 表达式合法性，检查任务名唯一性，保存任务并创建 Quartz 调度任务。
     * </p>
     *
     * @param job 定时任务实体
     *
     * @return 创建后的任务实体
     *
     * @throws BusinessException 当 Cron 表达式不合法或任务名已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Job createJob(Job job) {
        // 校验 cron 表达式
        if (!ScheduleUtils.isValidCronExpression(job.getCronExpression())) {
            throw new BusinessException("Cron 表达式不合法");
        }
        // 检查任务名是否重复
        Optional<Job> existingJob = jobRepository.findByJobNameAndGroup(job.getJobName(), job.getJobGroup());
        if (existingJob.isPresent()) {
            throw new BusinessException("任务名称已存在");
        }
        // 保存任务
        Job savedJob = jobRepository.save(job);
        // 创建调度任务
        try {
            ScheduleUtils.createScheduleJob(scheduler, savedJob);
        } catch (SchedulerException e) {
            log.error("创建调度任务失败", e);
            throw new BusinessException("创建调度任务失败：" + e.getMessage());
        }
        return savedJob;
    }

    /**
     * 更新定时任务
     * <p>
     * 校验 Cron 表达式合法性，检查任务是否存在且任务名不重复，更新任务并同步 Quartz 调度任务。
     * </p>
     *
     * @param job 定时任务实体
     *
     * @return 更新后的任务实体
     *
     * @throws BusinessException 当 Cron 表达式不合法、任务不存在或任务名已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Job updateJob(Job job) {
        // 校验 cron 表达式
        if (!ScheduleUtils.isValidCronExpression(job.getCronExpression())) {
            throw new BusinessException("Cron 表达式不合法");
        }
        // 检查任务是否存在
        Job existingJob = jobRepository.findById(job.getId()).orElseThrow(() -> new BusinessException("任务不存在"));
        // 检查任务名是否重复（排除当前任务）
        Optional<Job> duplicateJob = jobRepository.findByJobNameAndGroup(job.getJobName(), job.getJobGroup());
        if (duplicateJob.isPresent() && !duplicateJob.get().getId().equals(job.getId())) {
            throw new BusinessException("任务名称已存在");
        }
        // 更新任务
        Job updatedJob = jobRepository.update(job);
        // 更新调度任务
        try {
            ScheduleUtils.updateScheduleJob(scheduler, updatedJob);
        } catch (SchedulerException e) {
            log.error("更新调度任务失败", e);
            throw new BusinessException("更新调度任务失败：" + e.getMessage());
        }
        return updatedJob;
    }

    /**
     * 删除定时任务
     * <p>
     * 删除 Quartz 调度任务并逻辑删除数据库记录。
     * </p>
     *
     * @param jobId 任务 ID
     *
     * @throws BusinessException 当任务不存在或删除调度任务失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteJob(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new BusinessException("任务不存在"));
        // 删除调度任务
        try {
            ScheduleUtils.deleteScheduleJob(scheduler, job.getId(), job.getJobGroup());
        } catch (SchedulerException e) {
            log.error("删除调度任务失败", e);
            throw new BusinessException("删除调度任务失败：" + e.getMessage());
        }
        // 逻辑删除任务
        jobRepository.deleteById(jobId);
    }

    /**
     * 立即执行一次任务
     * <p>
     * 触发任务立即执行一次，不影响原有的调度计划。
     * </p>
     *
     * @param jobId 任务 ID
     *
     * @throws BusinessException 当任务不存在或立即执行失败时抛出
     */
    public void runJob(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new BusinessException("任务不存在"));
        try {
            ScheduleUtils.runJob(scheduler, job);
        } catch (SchedulerException e) {
            log.error("立即执行任务失败", e);
            throw new BusinessException("立即执行任务失败：" + e.getMessage());
        }
    }

    /**
     * 暂停任务
     * <p>
     * 暂停 Quartz 调度任务，并更新任务状态为暂停（STATUS_PAUSE）。
     * </p>
     *
     * @param jobId 任务 ID
     *
     * @throws BusinessException 当任务不存在或暂停任务失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void pauseJob(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new BusinessException("任务不存在"));
        try {
            ScheduleUtils.pauseJob(scheduler, job.getId(), job.getJobGroup());
        } catch (SchedulerException e) {
            log.error("暂停任务失败", e);
            throw new BusinessException("暂停任务失败：" + e.getMessage());
        }
        jobRepository.updateStatus(jobId, Job.STATUS_PAUSE);
    }

    /**
     * 恢复任务
     * <p>
     * 恢复被暂停的 Quartz 调度任务，并更新任务状态为正常（STATUS_NORMAL）。
     * </p>
     *
     * @param jobId 任务 ID
     *
     * @throws BusinessException 当任务不存在或恢复任务失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void resumeJob(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new BusinessException("任务不存在"));
        try {
            ScheduleUtils.resumeJob(scheduler, job.getId(), job.getJobGroup());
        } catch (SchedulerException e) {
            log.error("恢复任务失败", e);
            throw new BusinessException("恢复任务失败：" + e.getMessage());
        }
        jobRepository.updateStatus(jobId, Job.STATUS_NORMAL);
    }

    /**
     * 修改任务状态
     * <p>
     * 根据状态值调用对应的暂停或恢复方法。
     * </p>
     *
     * @param jobId  任务 ID
     * @param status 状态值：STATUS_NORMAL-正常，STATUS_PAUSE-暂停
     *
     * @throws BusinessException 当状态值不合法时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeJobStatus(Long jobId, Integer status) {
        if (Job.STATUS_NORMAL == status) {
            resumeJob(jobId);
        } else if (Job.STATUS_PAUSE == status) {
            pauseJob(jobId);
        } else {
            throw new BusinessException("不支持的状态值：" + status);
        }
    }

    /**
     * 根据 ID 获取任务
     *
     * @param id 任务 ID
     *
     * @return 任务实体，不存在则返回 Optional.empty()
     */
    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }

    /**
     * 获取所有任务
     *
     * @return 任务列表
     */
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    /**
     * 分页查询任务
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 任务列表
     */
    public List<Job> getJobPage(int pageNum, int pageSize) {
        return jobRepository.findPage(pageNum, pageSize);
    }

    /**
     * 根据条件分页查询任务
     * <p>
     * 支持按任务名称、任务组和状态进行条件查询。
     * </p>
     *
     * @param jobName  任务名称（可选）
     * @param jobGroup 任务组（可选）
     * @param status   状态（可选）
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 任务列表
     */
    public List<Job> getJobPageByCondition(String jobName, String jobGroup, Integer status, int pageNum, int pageSize) {
        return jobRepository.findPageByCondition(jobName, jobGroup, status, pageNum, pageSize);
    }

    /**
     * 统计任务总数
     *
     * @return 任务总数
     */
    public long countJobs() {
        return jobRepository.count();
    }

    /**
     * 根据条件统计任务总数
     * <p>
     * 支持按任务名称、任务组和状态进行条件统计。
     * </p>
     *
     * @param jobName  任务名称（可选）
     * @param jobGroup 任务组（可选）
     * @param status   状态（可选）
     *
     * @return 任务总数
     */
    public long countJobsByCondition(String jobName, String jobGroup, Integer status) {
        return jobRepository.countByCondition(jobName, jobGroup, status);
    }

    // =================== 任务日志相关 ===================

    /**
     * 按条件获取所有任务（导出用）
     * <p>
     * 不分页获取所有符合条件的任务，用于数据导出。
     * </p>
     *
     * @param jobName  任务名称（可选）
     * @param jobGroup 任务组（可选）
     * @param status   状态（可选）
     *
     * @return 任务列表
     */
    public List<Job> getAllJobsByCondition(String jobName, String jobGroup, Integer status) {
        return jobRepository.findAllByCondition(jobName, jobGroup, status);
    }

    /**
     * 根据 ID 获取任务日志
     *
     * @param id 日志 ID
     *
     * @return 任务日志实体，不存在则返回 Optional.empty()
     */
    public Optional<JobLog> getJobLogById(Long id) {
        return jobLogRepository.findById(id);
    }

    /**
     * 分页查询任务日志
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 任务日志列表
     */
    public List<JobLog> getJobLogPage(int pageNum, int pageSize) {
        return jobLogRepository.findPage(pageNum, pageSize);
    }

    /**
     * 根据条件分页查询任务日志
     * <p>
     * 支持按任务 ID、任务名称、任务组、状态和时间范围进行条件查询。
     * </p>
     *
     * @param jobId     任务 ID（可选）
     * @param jobName   任务名称（可选）
     * @param jobGroup  任务组（可选）
     * @param status    状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param pageNum   页码，从 1 开始
     * @param pageSize  每页数量
     *
     * @return 任务日志列表
     */
    public List<JobLog> getJobLogPageByCondition(Long jobId, String jobName, String jobGroup, Integer status,
                                                 LocalDateTime startTime, LocalDateTime endTime, int pageNum, int pageSize) {
        return jobLogRepository.findPageByCondition(jobId, jobName, jobGroup, status, startTime, endTime, pageNum,
                pageSize);
    }

    /**
     * 统计任务日志总数
     *
     * @return 任务日志总数
     */
    public long countJobLogs() {
        return jobLogRepository.count();
    }

    /**
     * 根据条件统计任务日志总数
     * <p>
     * 支持按任务 ID、任务名称、任务组、状态和时间范围进行条件统计。
     * </p>
     *
     * @param jobId     任务 ID（可选）
     * @param jobName   任务名称（可选）
     * @param jobGroup  任务组（可选）
     * @param status    状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     *
     * @return 任务日志总数
     */
    public long countJobLogsByCondition(Long jobId, String jobName, String jobGroup, Integer status,
                                        LocalDateTime startTime, LocalDateTime endTime) {
        return jobLogRepository.countByCondition(jobId, jobName, jobGroup, status, startTime, endTime);
    }

    /**
     * 清理指定日期之前的任务日志
     * <p>
     * 删除执行时间早于指定日期的所有任务日志记录。
     * </p>
     *
     * @param beforeDate 日期边界
     *
     * @return 清理的日志记录数
     */
    public int cleanJobLogs(LocalDateTime beforeDate) {
        return jobLogRepository.cleanLogBeforeDate(beforeDate);
    }

    /**
     * 清理指定任务的所有日志
     * <p>
     * 删除指定任务的所有执行日志记录。
     * </p>
     *
     * @param jobId 任务 ID
     *
     * @return 清理的日志记录数
     */
    public int cleanJobLogsByJobId(Long jobId) {
        return jobLogRepository.cleanLogByJobId(jobId);
    }

}
