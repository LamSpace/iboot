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

package com.iboot.admin.infrastructure.repository;

import com.iboot.admin.domain.job.model.JobLog;
import com.iboot.admin.domain.job.repository.JobLogRepository;
import com.iboot.admin.infrastructure.persistence.mapper.JobLogMapper;
import com.iboot.admin.infrastructure.persistence.po.JobLogPO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 定时任务日志仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现定时任务执行日志的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
public class JobLogRepositoryImpl implements JobLogRepository {

    private final JobLogMapper jobLogMapper;

    @SuppressWarnings("all")
    public JobLogRepositoryImpl(final JobLogMapper jobLogMapper) {
        this.jobLogMapper = jobLogMapper;
    }

    /**
     * 保存任务执行日志
     *
     * @param jobLog 任务日志实体
     *
     * @return 保存后的任务日志
     */
    @Override
    public JobLog save(JobLog jobLog) {
        JobLogPO po = convertToPO(jobLog);
        jobLogMapper.insert(po);
        jobLog.setId(po.getId());
        return jobLog;
    }

    /**
     * 根据 ID 查询任务日志
     *
     * @param id 任务日志 ID
     *
     * @return 任务日志实体，不存在则返回空
     */
    @Override
    public Optional<JobLog> findById(Long id) {
        JobLogPO po = jobLogMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 根据任务 ID 查询执行日志
     *
     * @param jobId 定时任务 ID
     *
     * @return 任务日志列表
     */
    @Override
    public List<JobLog> findByJobId(Long jobId) {
        return jobLogMapper.selectByJobId(jobId).stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 分页查询任务执行日志
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 任务日志列表
     */
    @Override
    public List<JobLog> findPage(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return jobLogMapper.selectPage(offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 根据条件分页查询任务执行日志
     *
     * @param jobId     任务 ID（可选）
     * @param jobName   任务名称（可选）
     * @param jobGroup  任务组（可选）
     * @param status    执行状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param pageNum   页码
     * @param pageSize  每页数量
     *
     * @return 任务日志列表
     */
    @Override
    public List<JobLog> findPageByCondition(Long jobId, String jobName, String jobGroup, Integer status,
                                            LocalDateTime startTime, LocalDateTime endTime, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return jobLogMapper
                .selectPageByCondition(jobId, jobName, jobGroup, status, startTime, endTime, offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 统计任务执行日志总数
     *
     * @return 任务日志总数
     */
    @Override
    public long count() {
        return jobLogMapper.count();
    }

    /**
     * 根据条件统计任务执行日志总数
     *
     * @param jobId     任务 ID（可选）
     * @param jobName   任务名称（可选）
     * @param jobGroup  任务组（可选）
     * @param status    执行状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     *
     * @return 任务日志总数
     */
    @Override
    public long countByCondition(Long jobId, String jobName, String jobGroup, Integer status, LocalDateTime startTime,
                                 LocalDateTime endTime) {
        return jobLogMapper.countByCondition(jobId, jobName, jobGroup, status, startTime, endTime);
    }

    /**
     * 清理指定日期之前的日志
     *
     * @param beforeDate 清理该日期之前的日志
     *
     * @return 清理的日志数量
     */
    @Override
    public int cleanLogBeforeDate(LocalDateTime beforeDate) {
        return jobLogMapper.deleteBeforeDate(beforeDate);
    }

    /**
     * 根据任务 ID 清理日志
     *
     * @param jobId 定时任务 ID
     *
     * @return 清理的日志数量
     */
    @Override
    public int cleanLogByJobId(Long jobId) {
        return jobLogMapper.deleteByJobId(jobId);
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param jobLog 任务日志领域对象
     *
     * @return 任务日志持久化对象
     */
    private JobLogPO convertToPO(JobLog jobLog) {
        JobLogPO po = new JobLogPO();
        po.setId(jobLog.getId());
        po.setJobId(jobLog.getJobId());
        po.setJobName(jobLog.getJobName());
        po.setJobGroup(jobLog.getJobGroup());
        po.setInvokeTarget(jobLog.getInvokeTarget());
        po.setJobMessage(jobLog.getJobMessage());
        po.setStatus(jobLog.getStatus());
        po.setExceptionInfo(jobLog.getExceptionInfo());
        po.setStartTime(jobLog.getStartTime());
        po.setEndTime(jobLog.getEndTime());
        po.setCostTime(jobLog.getCostTime());
        return po;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 任务日志持久化对象
     *
     * @return 任务日志领域对象
     */
    private JobLog convertToDomain(JobLogPO po) {
        return JobLog.builder()
                .id(po.getId())
                .jobId(po.getJobId())
                .jobName(po.getJobName())
                .jobGroup(po.getJobGroup())
                .invokeTarget(po.getInvokeTarget())
                .jobMessage(po.getJobMessage())
                .status(po.getStatus())
                .exceptionInfo(po.getExceptionInfo())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .costTime(po.getCostTime())
                .createTime(po.getCreateTime())
                .build();
    }

}
