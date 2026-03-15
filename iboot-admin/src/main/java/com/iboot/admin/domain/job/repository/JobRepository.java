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

import com.iboot.admin.domain.job.model.Job;

import java.util.List;
import java.util.Optional;

/**
 * 定时任务仓储接口
 * <p>
 * 负责定时任务的持久化操作，支持任务的调度管理和状态控制
 * </p>
 *
 * @author iBoot
 */
public interface JobRepository {

    /**
     * 保存定时任务
     *
     * @param job 定时任务实体
     * @return 保存后的定时任务
     */
    Job save(Job job);

    /**
     * 更新定时任务
     *
     * @param job 定时任务实体
     * @return 更新后的定时任务
     */
    Job update(Job job);

    /**
     * 根据 ID 查询定时任务
     *
     * @param id 定时任务 ID
     * @return 定时任务实体，不存在则返回空
     */
    Optional<Job> findById(Long id);

    /**
     * 根据任务名称和任务组查询
     *
     * @param jobName 任务名称
     * @param jobGroup 任务组
     * @return 定时任务实体，不存在则返回空
     */
    Optional<Job> findByJobNameAndGroup(String jobName, String jobGroup);

    /**
     * 查询所有定时任务
     *
     * @return 定时任务列表
     */
    List<Job> findAll();

    /**
     * 分页查询定时任务
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 定时任务列表
     */
    List<Job> findPage(int pageNum, int pageSize);

    /**
     * 根据条件分页查询定时任务
     *
     * @param jobName 任务名称（可选）
     * @param jobGroup 任务组（可选）
     * @param status 状态（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 定时任务列表
     */
    List<Job> findPageByCondition(String jobName, String jobGroup, Integer status,
                                   int pageNum, int pageSize);

    /**
     * 统计定时任务总数
     *
     * @return 定时任务总数
     */
    long count();

    /**
     * 根据条件统计定时任务总数
     *
     * @param jobName 任务名称（可选）
     * @param jobGroup 任务组（可选）
     * @param status 状态（可选）
     * @return 定时任务总数
     */
    long countByCondition(String jobName, String jobGroup, Integer status);

    /**
     * 根据条件查询所有定时任务（导出用）
     *
     * @param jobName 任务名称（可选）
     * @param jobGroup 任务组（可选）
     * @param status 状态（可选）
     * @return 定时任务列表
     */
    List<Job> findAllByCondition(String jobName, String jobGroup, Integer status);

    /**
     * 根据 ID 删除定时任务（逻辑删除）
     *
     * @param id 定时任务 ID
     * @return 删除的记录数
     */
    int deleteById(Long id);

    /**
     * 更新任务状态
     *
     * @param id 定时任务 ID
     * @param status 新状态
     * @return 更新的记录数
     */
    int updateStatus(Long id, Integer status);
}
