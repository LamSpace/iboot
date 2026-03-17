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

import com.iboot.admin.domain.job.model.Job;
import com.iboot.admin.domain.job.repository.JobRepository;
import com.iboot.admin.infrastructure.persistence.mapper.JobMapper;
import com.iboot.admin.infrastructure.persistence.po.JobPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 定时任务仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现定时任务数据的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
public class JobRepositoryImpl implements JobRepository {

    private final JobMapper jobMapper;

    @SuppressWarnings("all")
    public JobRepositoryImpl(final JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    /**
     * 保存定时任务
     * <p>
     * 插入新任务记录并设置生成的 ID
     * </p>
     *
     * @param job 定时任务实体
     *
     * @return 保存后的定时任务
     */
    @Override
    public Job save(Job job) {
        JobPO po = convertToPO(job);
        jobMapper.insert(po);
        job.setId(po.getId());
        return job;
    }

    /**
     * 更新定时任务
     *
     * @param job 定时任务实体
     *
     * @return 更新后的定时任务
     */
    @Override
    public Job update(Job job) {
        JobPO po = convertToPO(job);
        jobMapper.update(po);
        return job;
    }

    /**
     * 根据 ID 查询定时任务
     *
     * @param id 定时任务 ID
     *
     * @return 定时任务实体，不存在则返回空
     */
    @Override
    public Optional<Job> findById(Long id) {
        JobPO po = jobMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 根据任务名和分组查询定时任务
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     *
     * @return 定时任务实体，不存在则返回空
     */
    @Override
    public Optional<Job> findByJobNameAndGroup(String jobName, String jobGroup) {
        JobPO po = jobMapper.selectByJobNameAndGroup(jobName, jobGroup);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 查询所有定时任务
     *
     * @return 定时任务列表
     */
    @Override
    public List<Job> findAll() {
        return jobMapper.selectAll().stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 分页查询定时任务
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 定时任务列表
     */
    @Override
    public List<Job> findPage(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return jobMapper.selectPage(offset, pageSize).stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 按条件分页查询定时任务
     *
     * @param jobName  任务名称（可选）
     * @param jobGroup 任务分组（可选）
     * @param status   状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页数量
     *
     * @return 定时任务列表
     */
    @Override
    public List<Job> findPageByCondition(String jobName, String jobGroup, Integer status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return jobMapper.selectPageByCondition(jobName, jobGroup, status, offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 统计定时任务总数
     *
     * @return 定时任务总数
     */
    @Override
    public long count() {
        return jobMapper.count();
    }

    /**
     * 按条件统计定时任务总数
     *
     * @param jobName  任务名称（可选）
     * @param jobGroup 任务分组（可选）
     * @param status   状态（可选）
     *
     * @return 定时任务总数
     */
    @Override
    public long countByCondition(String jobName, String jobGroup, Integer status) {
        return jobMapper.countByCondition(jobName, jobGroup, status);
    }

    /**
     * 按条件查询所有定时任务（不分页，用于导出）
     *
     * @param jobName  任务名称（可选）
     * @param jobGroup 任务分组（可选）
     * @param status   状态（可选）
     *
     * @return 定时任务列表
     */
    @Override
    public List<Job> findAllByCondition(String jobName, String jobGroup, Integer status) {
        return jobMapper.selectAllByCondition(jobName, jobGroup, status)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 删除定时任务（逻辑删除）
     *
     * @param id 定时任务 ID
     *
     * @return 删除的记录数
     */
    @Override
    public int deleteById(Long id) {
        return jobMapper.deleteById(id);
    }

    /**
     * 更新定时任务状态
     *
     * @param id     定时任务 ID
     * @param status 新状态
     *
     * @return 更新的记录数
     */
    @Override
    public int updateStatus(Long id, Integer status) {
        return jobMapper.updateStatus(id, status);
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param job 定时任务领域对象
     *
     * @return 定时任务持久化对象
     */
    private JobPO convertToPO(Job job) {
        JobPO po = new JobPO();
        po.setId(job.getId());
        po.setJobName(job.getJobName());
        po.setJobGroup(job.getJobGroup());
        po.setInvokeTarget(job.getInvokeTarget());
        po.setCronExpression(job.getCronExpression());
        po.setMisfirePolicy(job.getMisfirePolicy());
        po.setConcurrent(job.getConcurrent());
        po.setStatus(job.getStatus());
        po.setRemark(job.getRemark());
        po.setCreateBy(job.getCreateBy());
        po.setUpdateBy(job.getUpdateBy());
        return po;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 定时任务持久化对象
     *
     * @return 定时任务领域对象
     */
    private Job convertToDomain(JobPO po) {
        return Job.builder()
                .id(po.getId())
                .jobName(po.getJobName())
                .jobGroup(po.getJobGroup())
                .invokeTarget(po.getInvokeTarget())
                .cronExpression(po.getCronExpression())
                .misfirePolicy(po.getMisfirePolicy())
                .concurrent(po.getConcurrent())
                .status(po.getStatus())
                .remark(po.getRemark())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .updateBy(po.getUpdateBy())
                .updateTime(po.getUpdateTime())
                .build();
    }

}
