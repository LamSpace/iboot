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

package com.iboot.admin.infrastructure.persistence.mapper;

import com.iboot.admin.infrastructure.persistence.po.JobLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务日志 Mapper 接口
 * <p>
 * 负责定时任务执行日志的数据库操作，记录任务执行结果
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface JobLogMapper {

    /**
     * 插入任务日志记录
     *
     * @param jobLog 任务日志持久化对象
     * @return 影响的记录数
     */
    int insert(JobLogPO jobLog);

    /**
     * 根据 ID 查询任务日志
     *
     * @param id 任务日志 ID
     * @return 任务日志持久化对象
     */
    JobLogPO selectById(@Param("id") Long id);

    /**
     * 根据任务 ID 查询执行日志
     *
     * @param jobId 定时任务 ID
     * @return 任务日志列表
     */
    List<JobLogPO> selectByJobId(@Param("jobId") Long jobId);

    /**
     * 分页查询任务日志
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 任务日志列表
     */
    List<JobLogPO> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 根据条件分页查询任务日志
     *
     * @param jobId 任务 ID（可选）
     * @param jobName 任务名称（可选）
     * @param jobGroup 任务组（可选）
     * @param status 执行状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 任务日志列表
     */
    List<JobLogPO> selectPageByCondition(@Param("jobId") Long jobId,
                                          @Param("jobName") String jobName,
                                          @Param("jobGroup") String jobGroup,
                                          @Param("status") Integer status,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    /**
     * 统计任务日志总数
     *
     * @return 总数
     */
    long count();

    /**
     * 根据条件统计任务日志总数
     *
     * @param jobId 任务 ID（可选）
     * @param jobName 任务名称（可选）
     * @param jobGroup 任务组（可选）
     * @param status 执行状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 总数
     */
    long countByCondition(@Param("jobId") Long jobId,
                          @Param("jobName") String jobName,
                          @Param("jobGroup") String jobGroup,
                          @Param("status") Integer status,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    /**
     * 清理指定日期之前的日志
     *
     * @param beforeDate 清理该日期之前的日志
     * @return 影响的记录数
     */
    int deleteBeforeDate(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * 根据任务 ID 清理日志
     *
     * @param jobId 定时任务 ID
     * @return 影响的记录数
     */
    int deleteByJobId(@Param("jobId") Long jobId);
}
