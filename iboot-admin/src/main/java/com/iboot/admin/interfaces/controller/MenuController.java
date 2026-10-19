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

package com.iboot.admin.interfaces.controller;

import com.iboot.admin.application.service.MenuApplicationService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.constant.Constants;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.system.model.Menu;
import com.iboot.admin.interfaces.dto.export.MenuExportVO;
import com.iboot.admin.interfaces.dto.request.MenuRequest;
import com.iboot.admin.interfaces.dto.response.MenuResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单管理控制器。
 *
 * <p>提供菜单树、列表、详情、当前用户菜单树与权限、增删改和导出接口；
 * 管理接口对应 {@code menu:*} 权限，当前用户接口允许已登录用户访问。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see MenuApplicationService
 */
@Tag(name = "菜单管理", description = "菜单管理相关接口")
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuApplicationService menuApplicationService;

    @SuppressWarnings("all")
    public MenuController(final MenuApplicationService menuApplicationService) {
        this.menuApplicationService = menuApplicationService;
    }

    /**
     * 查询菜单树形结构。
     *
     * @return 顶级菜单列表，子菜单挂在 {@code children} 中
     */
    @Operation(summary = "查询菜单树形结构")
    @GetMapping(version = "1", value = "/tree")
    @PreAuthorize("hasAuthority('menu:list')")
    public Result<List<MenuResponse>> tree() {
        List<Menu> menus = menuApplicationService.getMenuTree();
        List<MenuResponse> responses = menus.stream().map(this::convertToTreeResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    /**
     * 查询菜单列表。
     *
     * @return 菜单列表；没有数据时返回空列表
     */
    @Operation(summary = "查询菜单列表")
    @GetMapping(version = "1", value = "/list")
    @PreAuthorize("hasAuthority('menu:list')")
    public Result<List<MenuResponse>> list() {
        List<Menu> menus = menuApplicationService.getAllMenus();
        List<MenuResponse> responses = menus.stream().map(this::convertToResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    /**
     * 查询菜单详情。
     *
     * @param id 菜单 ID
     * @return 菜单详情
     * @throws com.iboot.admin.common.exception.BusinessException 菜单不存在时抛出
     */
    @Operation(summary = "查询菜单详情")
    @GetMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('menu:query')")
    public Result<MenuResponse> getById(@PathVariable Long id) {
        Menu menu = menuApplicationService.getMenuById(id);
        return Result.success(convertToResponse(menu));
    }

    /**
     * 获取当前登录用户的菜单树。
     *
     * @return 当前用户可见的菜单树
     * @throws com.iboot.admin.common.exception.BusinessException 无法获取当前用户信息时抛出
     */
    @Operation(summary = "获取当前用户菜单树")
    @GetMapping(version = "1", value = "/user/tree")
    public Result<List<MenuResponse>> userMenuTree() {
        Long userId = getCurrentUserId();
        List<Menu> menus = menuApplicationService.getUserMenuTree(userId);
        List<MenuResponse> responses = menus.stream().map(this::convertToTreeResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    /**
     * 获取当前登录用户的权限列表。
     *
     * @return 权限标识集合；没有权限时返回空集合
     * @throws com.iboot.admin.common.exception.BusinessException 无法获取当前用户信息时抛出
     */
    @Operation(summary = "获取当前用户权限列表")
    @GetMapping(version = "1", value = "/user/perms")
    public Result<Set<String>> userPermissions() {
        Long userId = getCurrentUserId();
        List<String> permsList = menuApplicationService.getUserPermissions(userId);
        Set<String> perms = new java.util.HashSet<>(permsList);
        return Result.success(perms);
    }

    /**
     * 新增菜单。
     *
     * @param request 菜单创建请求
     * @return 创建后的菜单详情
     */
    @Operation(summary = "新增菜单")
    @PostMapping
    @PreAuthorize("hasAuthority('menu:add')")
    @Log(title = "菜单管理", businessType = BusinessTypeEnum.INSERT)
    public Result<MenuResponse> add(@Valid @RequestBody MenuRequest request) {
        Menu menu = convertToEntity(request);
        Menu createdMenu = menuApplicationService.createMenu(menu);
        return Result.success(convertToResponse(createdMenu));
    }

    /**
     * 修改菜单。
     *
     * @param request 菜单更新请求，{@code id} 不能为空
     * @return 无数据的成功响应
     */
    @Operation(summary = "修改菜单")
    @PutMapping
    @PreAuthorize("hasAuthority('menu:edit')")
    @Log(title = "菜单管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> update(@Valid @RequestBody MenuRequest request) {
        Menu menu = convertToEntity(request);
        menu.setId(request.getId());
        menuApplicationService.updateMenu(menu);
        return Result.success();
    }

    /**
     * 删除菜单。
     *
     * @param id 菜单 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "删除菜单")
    @DeleteMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('menu:remove')")
    @Log(title = "菜单管理", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        menuApplicationService.deleteMenu(id);
        return Result.success();
    }

    /**
     * 导出菜单列表。
     *
     * <p>按菜单树递归展平，并按名称和状态过滤后写入 Excel。
     *
     * @param response HTTP 响应，用于写入 Excel 文件
     * @param menuName 菜单名称关键字，可为 {@code null}
     * @param status 状态：0-停用，1-正常；可为 {@code null}
     * @throws IOException 写入响应流失败时抛出
     */
    @Operation(summary = "导出菜单列表")
    @GetMapping(version = "1", value = "/export")
    @PreAuthorize("hasAuthority('menu:export')")
    @Log(title = "菜单管理", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response, @RequestParam(required = false) String menuName,
                       @RequestParam(required = false) Integer status) throws IOException {
        List<Menu> menus = menuApplicationService.getMenuTree();
        List<MenuExportVO> exportList = new ArrayList<>();
        flattenMenuTree(menus, exportList, 1, menuName, status);
        ExcelExportUtil.exportExcel(response, exportList, MenuExportVO.class, "菜单列表", "菜单数据");
    }

    /**
     * 递归展平菜单树并添加层级信息
     */
    private void flattenMenuTree(List<Menu> menus, List<MenuExportVO> exportList, int level, String menuName,
                                 Integer status) {
        for (Menu menu : menus) {
            boolean nameMatch = menuName == null || menuName.isEmpty()
                    || (menu.getMenuName() != null && menu.getMenuName().contains(menuName));
            boolean statusMatch = status == null || status.equals(menu.getStatus());
            if (nameMatch && statusMatch) {
                exportList.add(convertToExportVO(menu, level));
            }
            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                flattenMenuTree(menu.getChildren(), exportList, level + 1, menuName, status);
            }
        }
    }

    private MenuExportVO convertToExportVO(Menu menu, int level) {
        return MenuExportVO.builder()
                .id(menu.getId())
                .level(level)
                .menuName(menu.getMenuName())
                .menuType(menu.getMenuType())
                .path(menu.getPath())
                .component(menu.getComponent())
                .perms(menu.getPermission())
                .icon(menu.getIcon())
                .orderNum(menu.getOrderNum())
                .isFrame(menu.getIsFrame())
                .isCache(menu.getIsCache())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .createTime(menu.getCreateTime())
                .build();
    }

    private Menu convertToEntity(MenuRequest request) {
        return Menu.builder()
                .parentId(request.getParentId())
                .menuName(request.getMenuName())
                .menuType(request.getMenuType())
                .path(request.getPath())
                .component(request.getComponent())
                .permission(request.getPerms())
                .icon(request.getIcon())
                .orderNum(request.getOrderNum())
                .isFrame(request.getIsFrame())
                .isCache(request.getIsCache())
                .visible(request.getVisible())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
    }

    private MenuResponse convertToResponse(Menu menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .menuName(menu.getMenuName())
                .menuType(menu.getMenuType())
                .path(menu.getPath())
                .component(menu.getComponent())
                .perms(menu.getPermission())
                .icon(menu.getIcon())
                .orderNum(menu.getOrderNum())
                .isFrame(menu.getIsFrame())
                .isCache(menu.getIsCache())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .remark(menu.getRemark())
                .createTime(menu.getCreateTime())
                .build();
    }

    private MenuResponse convertToTreeResponse(Menu menu) {
        MenuResponse response = convertToResponse(menu);
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            response
                    .setChildren(menu.getChildren().stream().map(this::convertToTreeResponse).collect(Collectors.toList()));
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Map) {
                var principalMap = (Map<String, Object>) principal;
                Object userId = principalMap.get(Constants.USER_ID);
                if (userId != null) {
                    return Long.valueOf(userId.toString());
                }
            }
        }
        throw new com.iboot.admin.common.exception.BusinessException("无法获取当前用户信息");
    }

}
