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

import com.iboot.admin.domain.system.model.Dept;
import com.iboot.admin.interfaces.dto.export.DeptExportVO;
import com.iboot.admin.interfaces.dto.request.DeptRequest;
import com.iboot.admin.interfaces.dto.response.DeptResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * 部门 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface DeptMapper {

    // ==================== 查询映射 ====================

    /**
     * 将部门实体转换为响应 DTO
     *
     * @param dept 部门实体
     *
     * @return 部门响应 DTO
     */
    @Mapping(target = "children", ignore = true)
    DeptResponse toResponse(Dept dept);

    /**
     * 将部门实体列表转换为树形响应 DTO 列表
     *
     * @param depts 部门实体列表
     *
     * @return 部门树形响应 DTO 列表
     */
    default List<DeptResponse> toTreeResponseList(List<Dept> depts) {
        return depts.stream().map(this::toTreeResponse).toList();
    }

    /**
     * 将部门实体转换为导出 VO
     *
     * @param dept 部门实体
     *
     * @return 部门导出 VO
     */
    @Mapping(target = "level", ignore = true)
    DeptExportVO toExportVO(Dept dept);

    /**
     * 将部门实体列表转换为导出 VO 列表
     *
     * @param depts 部门实体列表
     *
     * @return 部门导出 VO 列表
     */
    @Mapping(target = "level", ignore = true)
    List<DeptExportVO> toExportVOList(List<Dept> depts);

    // ==================== 创建映射 ====================

    /**
     * 将部门请求转换为实体
     *
     * @param request 部门请求
     *
     * @return 部门实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "children", ignore = true)
    Dept toEntity(DeptRequest request);

    // ==================== 更新映射 ====================

    /**
     * 将部门请求映射到现有实体
     *
     * @param request 部门请求
     * @param dept    现有部门实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateEntityFromRequest(DeptRequest request, @MappingTarget Dept dept);

    // ==================== 树形结构映射 ====================

    /**
     * 将部门实体转换为树形响应 DTO（递归处理子部门）
     *
     * @param dept 部门实体
     *
     * @return 部门树形响应 DTO
     */
    @Named("toTreeResponse")
    default DeptResponse toTreeResponse(Dept dept) {
        DeptResponse response = toResponse(dept);
        if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
            response.setChildren(dept.getChildren().stream().map(this::toTreeResponse).toList());
        }
        return response;
    }

}
