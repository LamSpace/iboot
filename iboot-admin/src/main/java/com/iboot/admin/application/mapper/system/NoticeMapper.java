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

package com.iboot.admin.application.mapper.system;

import com.iboot.admin.domain.system.model.Notice;
import com.iboot.admin.interfaces.dto.export.NoticeExportVO;
import com.iboot.admin.interfaces.dto.request.NoticeRequest;
import com.iboot.admin.interfaces.dto.response.NoticeResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 公告 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface NoticeMapper {

    // ==================== 查询映射 ====================

    /**
     * 将公告实体转换为响应 DTO
     *
     * @param notice 公告实体
     *
     * @return 公告响应 DTO
     */
    NoticeResponse toResponse(Notice notice);

    /**
     * 将公告实体列表转换为响应 DTO 列表
     *
     * @param notices 公告实体列表
     *
     * @return 公告响应 DTO 列表
     */
    List<NoticeResponse> toResponseList(List<Notice> notices);

    /**
     * 将公告实体转换为导出 VO
     *
     * @param notice 公告实体
     *
     * @return 公告导出 VO
     */
    NoticeExportVO toExportVO(Notice notice);

    /**
     * 将公告实体列表转换为导出 VO 列表
     *
     * @param notices 公告实体列表
     *
     * @return 公告导出 VO 列表
     */
    List<NoticeExportVO> toExportVOList(List<Notice> notices);

    // ==================== 创建映射 ====================

    /**
     * 将公告请求转换为实体
     *
     * @param request 公告请求
     *
     * @return 公告实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "status", ignore = true)
    Notice toEntity(NoticeRequest request);

    // ==================== 更新映射 ====================

    /**
     * 将公告请求映射到现有实体
     *
     * @param request 公告请求
     * @param notice  现有公告实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromRequest(NoticeRequest request, @MappingTarget Notice notice);

}
