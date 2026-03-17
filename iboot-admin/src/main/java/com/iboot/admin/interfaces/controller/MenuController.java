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
 * 菜单管理控制器
 *
 * @author iBoot
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

    @Operation(summary = "查询菜单树形结构")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('menu:list')")
    public Result<List<MenuResponse>> tree() {
        List<Menu> menus = menuApplicationService.getMenuTree();
        List<MenuResponse> responses = menus.stream().map(this::convertToTreeResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "查询菜单列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('menu:list')")
    public Result<List<MenuResponse>> list() {
        List<Menu> menus = menuApplicationService.getAllMenus();
        List<MenuResponse> responses = menus.stream().map(this::convertToResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "查询菜单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:query')")
    public Result<MenuResponse> getById(@PathVariable Long id) {
        Menu menu = menuApplicationService.getMenuById(id);
        return Result.success(convertToResponse(menu));
    }

    @Operation(summary = "获取当前用户菜单树")
    @GetMapping("/user/tree")
    public Result<List<MenuResponse>> userMenuTree() {
        Long userId = getCurrentUserId();
        List<Menu> menus = menuApplicationService.getUserMenuTree(userId);
        List<MenuResponse> responses = menus.stream().map(this::convertToTreeResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "获取当前用户权限列表")
    @GetMapping("/user/perms")
    public Result<Set<String>> userPermissions() {
        Long userId = getCurrentUserId();
        List<String> permsList = menuApplicationService.getUserPermissions(userId);
        Set<String> perms = new java.util.HashSet<>(permsList);
        return Result.success(perms);
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @PreAuthorize("hasAuthority('menu:add')")
    @Log(title = "菜单管理", businessType = BusinessTypeEnum.INSERT)
    public Result<MenuResponse> add(@Valid @RequestBody MenuRequest request) {
        Menu menu = convertToEntity(request);
        Menu createdMenu = menuApplicationService.createMenu(menu);
        return Result.success(convertToResponse(createdMenu));
    }

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

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:remove')")
    @Log(title = "菜单管理", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        menuApplicationService.deleteMenu(id);
        return Result.success();
    }

    @Operation(summary = "导出菜单列表")
    @GetMapping("/export")
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
