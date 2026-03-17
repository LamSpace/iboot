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

package com.iboot.admin.domain.job.repository;

import com.iboot.admin.domain.job.model.JobLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 定时任务日志仓储接口
 * <p>
 * 负责定时任务执行日志的持久化操作，记录任务执行结果和运行状态
 * </p>
 *
 * @author iBoot
 */
public interface JobLogRepository {

    /**
     * 保存任务执行日志
     *
     * @param jobLog 任务日志实体
     *
     * @return 保存后的任务日志
     */
    JobLog save(JobLog jobLog);

    /**
     * 根据 ID 查询任务日志
     *
     * @param id 任务日志 ID
     *
     * @return 任务日志实体，不存在则返回空
     */
    Optional<JobLog> findById(Long id);

    /**
     * 根据任务 ID 查询执行日志
     *
     * @param jobId 定时任务 ID
     *
     * @return 任务日志列表
     */
    List<JobLog> findByJobId(Long jobId);

    /**
     * 分页查询任务执行日志
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     *
     * @return 任务日志列表
     */
    List<JobLog> findPage(int pageNum, int pageSize);

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
    List<JobLog> findPageByCondition(Long jobId, String jobName, String jobGroup, Integer status,
                                     LocalDateTime startTime, LocalDateTime endTime, int pageNum, int pageSize);

    /**
     * 统计任务执行日志总数
     *
     * @return 任务日志总数
     */
    long count();

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
    long countByCondition(Long jobId, String jobName, String jobGroup, Integer status, LocalDateTime startTime,
                          LocalDateTime endTime);

    /**
     * 清理指定日期之前的日志
     * <p>
     * 用于定期清理历史日志数据，释放存储空间
     * </p>
     *
     * @param beforeDate 清理该日期之前的日志
     *
     * @return 清理的日志数量
     */
    int cleanLogBeforeDate(LocalDateTime beforeDate);

    /**
     * 根据任务 ID 清理日志
     * <p>
     * 用于删除定时任务时级联清理其执行日志
     * </p>
     *
     * @param jobId 定时任务 ID
     *
     * @return 清理的日志数量
     */
    int cleanLogByJobId(Long jobId);

}
