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

import com.iboot.admin.domain.system.model.Role;
import com.iboot.admin.interfaces.dto.export.RoleExportVO;
import com.iboot.admin.interfaces.dto.request.RoleRequest;
import com.iboot.admin.interfaces.dto.response.RoleResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 角色 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface RoleMapper {

    // ==================== 查询映射 ====================

    /**
     * 将角色实体转换为响应 DTO
     *
     * @param role 角色实体
     *
     * @return 角色响应 DTO
     */
    RoleResponse toResponse(Role role);

    /**
     * 将角色实体列表转换为响应 DTO 列表
     *
     * @param roles 角色实体列表
     *
     * @return 角色响应 DTO 列表
     */
    List<RoleResponse> toResponseList(List<Role> roles);

    /**
     * 将角色实体转换为导出 VO
     *
     * @param role 角色实体
     *
     * @return 角色导出 VO
     */
    RoleExportVO toExportVO(Role role);

    /**
     * 将角色实体列表转换为导出 VO 列表
     *
     * @param roles 角色实体列表
     *
     * @return 角色导出 VO 列表
     */
    List<RoleExportVO> toExportVOList(List<Role> roles);

    // ==================== 创建映射 ====================

    /**
     * 将角色请求转换为实体
     *
     * @param request 角色请求
     *
     * @return 角色实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Role toEntity(RoleRequest request);

    // ==================== 更新映射 ====================

    /**
     * 将角色请求映射到现有实体
     *
     * @param request 角色请求
     * @param role    现有角色实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromRequest(RoleRequest request, @MappingTarget Role role);

}
