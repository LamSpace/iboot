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

import com.iboot.admin.domain.system.model.Menu;
import com.iboot.admin.interfaces.dto.export.MenuExportVO;
import com.iboot.admin.interfaces.dto.request.MenuRequest;
import com.iboot.admin.interfaces.dto.response.MenuResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * 菜单 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface MenuMapper {

    // ==================== 查询映射 ====================

    /**
     * 将菜单实体转换为响应 DTO
     *
     * @param menu 菜单实体
     *
     * @return 菜单响应 DTO
     */
    @Mapping(target = "children", ignore = true)
    MenuResponse toResponse(Menu menu);

    /**
     * 将菜单实体列表转换为树形响应 DTO 列表
     *
     * @param menus 菜单实体列表
     *
     * @return 菜单树形响应 DTO 列表
     */
    default List<MenuResponse> toTreeResponseList(List<Menu> menus) {
        return menus.stream().map(this::toTreeResponse).toList();
    }

    /**
     * 将菜单实体转换为导出 VO
     *
     * @param menu 菜单实体
     *
     * @return 菜单导出 VO
     */
    MenuExportVO toExportVO(Menu menu);

    /**
     * 将菜单实体列表转换为导出 VO 列表
     *
     * @param menus 菜单实体列表
     *
     * @return 菜单导出 VO 列表
     */
    List<MenuExportVO> toExportVOList(List<Menu> menus);

    // ==================== 创建映射 ====================

    /**
     * 将菜单请求转换为实体
     *
     * @param request 菜单请求
     *
     * @return 菜单实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "children", ignore = true)
    Menu toEntity(MenuRequest request);

    // ==================== 更新映射 ====================

    /**
     * 将菜单请求映射到现有实体
     *
     * @param request 菜单请求
     * @param menu    现有菜单实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateEntityFromRequest(MenuRequest request, @MappingTarget Menu menu);

    // ==================== 树形结构映射 ====================

    /**
     * 将菜单实体转换为树形响应 DTO（递归处理子菜单）
     *
     * @param menu 菜单实体
     *
     * @return 菜单树形响应 DTO
     */
    @Named("toTreeResponse")
    default MenuResponse toTreeResponse(Menu menu) {
        MenuResponse response = toResponse(menu);
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            response.setChildren(menu.getChildren().stream().map(this::toTreeResponse).toList());
        }
        return response;
    }

}
