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

import com.iboot.admin.common.annotation.DataScope;
import com.iboot.admin.infrastructure.persistence.po.JobPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 定时任务 Mapper 接口
 * <p>
 * 负责定时任务的数据库操作，支持任务的管理和状态控制
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface JobMapper {

    /**
     * 插入定时任务记录
     *
     * @param job 定时任务持久化对象
     * @return 影响的记录数
     */
    int insert(JobPO job);

    /**
     * 更新定时任务记录
     *
     * @param job 定时任务持久化对象
     * @return 影响的记录数
     */
    int update(JobPO job);

    /**
     * 根据 ID 查询定时任务
     *
     * @param id 定时任务 ID
     * @return 定时任务持久化对象
     */
    JobPO selectById(@Param("id") Long id);

    /**
     * 根据任务名称和任务组查询
     *
     * @param jobName 任务名称
     * @param jobGroup 任务组
     * @return 定时任务持久化对象
     */
    JobPO selectByJobNameAndGroup(@Param("jobName") String jobName, @Param("jobGroup") String jobGroup);

    /**
     * 查询所有定时任务
     *
     * @return 定时任务列表
     */
    List<JobPO> selectAll();

    /**
     * 分页查询定时任务（受数据权限控制）
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 定时任务列表
     */
    @DataScope(deptAlias = "j", userAlias = "j")
    List<JobPO> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 根据条件分页查询定时任务（受数据权限控制）
     *
     * @param jobName 任务名称（可选）
     * @param jobGroup 任务组（可选）
     * @param status 状态（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 定时任务列表
     */
    @DataScope(deptAlias = "j", userAlias = "j")
    List<JobPO> selectPageByCondition(@Param("jobName") String jobName,
                                       @Param("jobGroup") String jobGroup,
                                       @Param("status") Integer status,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    /**
     * 统计定时任务总数
     *
     * @return 总数
     */
    long count();

    /**
     * 根据条件统计定时任务总数（受数据权限控制）
     *
     * @param jobName 任务名称（可选）
     * @param jobGroup 任务组（可选）
     * @param status 状态（可选）
     * @return 总数
     */
    @DataScope(deptAlias = "j", userAlias = "j")
    long countByCondition(@Param("jobName") String jobName,
                          @Param("jobGroup") String jobGroup,
                          @Param("status") Integer status);

    /**
     * 根据条件查询所有定时任务（导出用）
     *
     * @param jobName 任务名称（可选）
     * @param jobGroup 任务组（可选）
     * @param status 状态（可选）
     * @return 定时任务列表
     */
    @DataScope(deptAlias = "j", userAlias = "j")
    List<JobPO> selectAllByCondition(@Param("jobName") String jobName,
                                     @Param("jobGroup") String jobGroup,
                                     @Param("status") Integer status);

    /**
     * 逻辑删除定时任务
     *
     * @param id 定时任务 ID
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 更新任务状态
     *
     * @param id 定时任务 ID
     * @param status 新状态
     * @return 影响的记录数
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
