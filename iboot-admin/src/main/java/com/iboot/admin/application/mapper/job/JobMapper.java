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

package com.iboot.admin.application.mapper.job;

import com.iboot.admin.domain.job.model.Job;
import com.iboot.admin.interfaces.dto.export.JobExportVO;
import com.iboot.admin.interfaces.dto.request.JobRequest;
import com.iboot.admin.interfaces.dto.response.JobResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 定时任务 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface JobMapper {

    // ==================== 查询映射 ====================

    /**
     * 将定时任务实体转换为响应 DTO
     *
     * @param job 定时任务实体
     *
     * @return 定时任务响应 DTO
     */
    JobResponse toResponse(Job job);

    /**
     * 将定时任务实体列表转换为响应 DTO 列表
     *
     * @param jobs 定时任务实体列表
     *
     * @return 定时任务响应 DTO 列表
     */
    List<JobResponse> toResponseList(List<Job> jobs);

    /**
     * 将定时任务实体转换为导出 VO
     *
     * @param job 定时任务实体
     *
     * @return 定时任务导出 VO
     */
    JobExportVO toExportVO(Job job);

    /**
     * 将定时任务实体列表转换为导出 VO 列表
     *
     * @param jobs 定时任务实体列表
     *
     * @return 定时任务导出 VO 列表
     */
    List<JobExportVO> toExportVOList(List<Job> jobs);

    // ==================== 创建映射 ====================

    /**
     * 将定时任务请求转换为实体
     *
     * @param request 定时任务请求
     *
     * @return 定时任务实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    Job toEntity(JobRequest request);

    // ==================== 更新映射 ====================

    /**
     * 将定时任务请求映射到现有实体
     *
     * @param request 定时任务请求
     * @param job     现有定时任务实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void updateEntityFromRequest(JobRequest request, @MappingTarget Job job);

}
