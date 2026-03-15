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

import com.iboot.admin.domain.system.model.DictType;
import com.iboot.admin.interfaces.dto.export.DictTypeExportVO;
import com.iboot.admin.interfaces.dto.request.DictTypeRequest;
import com.iboot.admin.interfaces.dto.response.DictTypeResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 字典类型 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(
    componentModel = "spring",
    
    
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface DictMapper {

    // ==================== 字典类型映射 ====================

    /**
     * 将字典类型实体转换为响应 DTO
     *
     * @param dictType 字典类型实体
     * @return 字典类型响应 DTO
     */
    DictTypeResponse typeToResponse(DictType dictType);

    /**
     * 将字典类型实体列表转换为响应 DTO 列表
     *
     * @param dictTypes 字典类型实体列表
     * @return 字典类型响应 DTO 列表
     */
    List<DictTypeResponse> typeToResponseList(List<DictType> dictTypes);

    /**
     * 将字典类型实体转换为导出 VO
     *
     * @param dictType 字典类型实体
     * @return 字典类型导出 VO
     */
    DictTypeExportVO typeToExportVO(DictType dictType);

    /**
     * 将字典类型实体列表转换为导出 VO 列表
     *
     * @param dictTypes 字典类型实体列表
     * @return 字典类型导出 VO 列表
     */
    List<DictTypeExportVO> typeToExportVOList(List<DictType> dictTypes);

    /**
     * 将字典类型请求转换为实体
     *
     * @param request 字典类型请求
     * @return 字典类型实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    DictType toEntity(DictTypeRequest request);

    /**
     * 将字典类型请求映射到现有实体
     *
     * @param request  字典类型请求
     * @param dictType 现有字典类型实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromRequest(DictTypeRequest request, @MappingTarget DictType dictType);
}
