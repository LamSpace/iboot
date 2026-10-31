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

import com.iboot.admin.application.service.RoleApplicationService;
import com.iboot.kernel.logging.Log;
import com.iboot.kernel.logging.BusinessTypeEnum;
import com.iboot.kernel.web.PageResult;
import com.iboot.kernel.web.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.system.model.Role;
import com.iboot.admin.interfaces.dto.export.RoleExportVO;
import com.iboot.admin.interfaces.dto.request.RoleRequest;
import com.iboot.admin.interfaces.dto.response.RoleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理控制器。
 *
 * <p>提供角色的分页查询、详情、新增、修改、删除、状态切换、数据权限设置和导出接口，
 * 对应 {@code role:*} 权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see RoleApplicationService
 */
@Tag(name = "角色管理", description = "角色管理相关接口")
@RestController
@RequestMapping("/api/role")
public class RoleController {

    private final RoleApplicationService roleApplicationService;

    @SuppressWarnings("all")
    public RoleController(final RoleApplicationService roleApplicationService) {
        this.roleApplicationService = roleApplicationService;
    }

    /**
     * 分页查询角色列表。
     *
     * <p>任一带条件参数非空时走条件查询，否则查询全部角色。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @param roleName 角色名称关键字，可为 {@code null}
     * @param roleCode 角色编码关键字，可为 {@code null}
     * @param status 状态：0-停用，1-正常；可为 {@code null}
     * @return 角色分页结果
     */
    @Operation(summary = "查询角色列表")
    @GetMapping(version = "1", value = "/list")
    @PreAuthorize("hasAuthority('role:list')")
    public Result<PageResult<RoleResponse>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String roleName,
                                                 @RequestParam(required = false) String roleCode,
                                                 @RequestParam(required = false) Integer status) {
        List<Role> roles;
        long total;
        if (roleName != null || roleCode != null || status != null) {
            roles = roleApplicationService.getRolePageByCondition(roleName, roleCode, status, pageNum, pageSize);
            total = roleApplicationService.countRolesByCondition(roleName, roleCode, status);
        } else {
            roles = roleApplicationService.getRolePage(pageNum, pageSize);
            total = roleApplicationService.countRoles();
        }
        List<RoleResponse> responses = roles.stream().map(this::convertToResponse).collect(Collectors.toList());
        PageResult<RoleResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 查询所有角色。
     *
     * @return 角色列表；没有数据时返回空列表
     */
    @Operation(summary = "查询所有角色")
    @GetMapping(version = "1", value = "/all")
    @PreAuthorize("hasAuthority('role:list')")
    public Result<List<RoleResponse>> all() {
        List<Role> roles = roleApplicationService.getAllRoles();
        List<RoleResponse> responses = roles.stream().map(this::convertToResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    /**
     * 查询角色详情。
     *
     * <p>响应中同时包含角色关联的菜单 ID 和数据权限部门 ID。
     *
     * @param id 角色 ID
     * @return 角色详情
     * @throws com.iboot.kernel.web.BusinessException 角色不存在时抛出
     */
    @Operation(summary = "查询角色详情")
    @GetMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('role:query')")
    public Result<RoleResponse> getById(@PathVariable Long id) {
        Role role = roleApplicationService.getRoleById(id);
        RoleResponse response = convertToResponse(role);
        // 获取角色的菜单ID和部门ID
        response.setMenuIds(roleApplicationService.getRoleMenuIds(id));
        response.setDeptIds(roleApplicationService.getRoleDeptIds(id));
        return Result.success(response);
    }

    /**
     * 新增角色。
     *
     * <p>创建角色后，按请求中的 {@code menuIds} 分配菜单权限；
     * 当数据范围为自定义（2）时，同时设置部门数据权限。
     *
     * @param request 角色创建请求
     * @return 创建后的角色详情
     */
    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasAuthority('role:add')")
    @Log(title = "角色管理", businessType = BusinessTypeEnum.INSERT)
    public Result<RoleResponse> add(@Valid @RequestBody RoleRequest request) {
        Role role = convertToEntity(request);
        Role createdRole = roleApplicationService.createRole(role);
        // 分配菜单权限
        if (request.getMenuIds() != null && !request.getMenuIds().isEmpty()) {
            roleApplicationService.assignMenus(createdRole.getId(), request.getMenuIds());
        }
        // 设置数据权限
        if (request.getDataScope() != null && request.getDataScope() == 2 && request.getDeptIds() != null
                && !request.getDeptIds().isEmpty()) {
            roleApplicationService.setDataPermission(createdRole.getId(), request.getDataScope(), request.getDeptIds());
        }
        return Result.success(convertToResponse(createdRole));
    }

    /**
     * 修改角色。
     *
     * <p>更新角色基本信息；请求包含 {@code menuIds} 时同时覆盖菜单权限。
     *
     * @param request 角色更新请求，{@code id} 不能为空
     * @return 无数据的成功响应
     */
    @Operation(summary = "修改角色")
    @PutMapping
    @PreAuthorize("hasAuthority('role:edit')")
    @Log(title = "角色管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> update(@Valid @RequestBody RoleRequest request) {
        Role role = convertToEntity(request);
        role.setId(request.getId());
        roleApplicationService.updateRole(role);
        // 更新菜单权限
        if (request.getMenuIds() != null) {
            roleApplicationService.assignMenus(request.getId(), request.getMenuIds());
        }
        return Result.success();
    }

    /**
     * 删除角色。
     *
     * @param id 角色 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "删除角色")
    @DeleteMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('role:remove')")
    @Log(title = "角色管理", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        roleApplicationService.deleteRole(id);
        return Result.success();
    }

    /**
     * 修改角色状态。
     *
     * @param id 角色 ID
     * @param status 目标状态：1-启用，0-停用
     * @return 无数据的成功响应
     */
    @Operation(summary = "修改角色状态")
    @PutMapping(version = "1", value = "/changeStatus")
    @PreAuthorize("hasAuthority('role:edit')")
    @Log(title = "角色管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> changeStatus(@RequestParam Long id, @RequestParam Integer status) {
        roleApplicationService.changeStatus(id, status);
        return Result.success();
    }

    /**
     * 设置角色数据权限。
     *
     * @param request 角色请求，包含角色 ID、数据范围和自定义部门 ID 列表
     * @return 无数据的成功响应
     */
    @Operation(summary = "设置数据权限")
    @PutMapping(version = "1", value = "/dataScope")
    @PreAuthorize("hasAuthority('role:edit')")
    @Log(title = "角色管理", businessType = BusinessTypeEnum.GRANT)
    public Result<Void> dataScope(@RequestBody RoleRequest request) {
        roleApplicationService.setDataPermission(request.getId(), request.getDataScope(), request.getDeptIds());
        return Result.success();
    }

    /**
     * 导出角色列表。
     *
     * @param response HTTP 响应，用于写入 Excel 文件
     * @param roleName 角色名称关键字，可为 {@code null}
     * @param roleCode 角色编码关键字，可为 {@code null}
     * @param status 状态：0-停用，1-正常；可为 {@code null}
     * @throws IOException 写入响应流失败时抛出
     */
    @Operation(summary = "导出角色列表")
    @GetMapping(version = "1", value = "/export")
    @PreAuthorize("hasAuthority('role:export')")
    @Log(title = "角色管理", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response, @RequestParam(required = false) String roleName,
                       @RequestParam(required = false) String roleCode, @RequestParam(required = false) Integer status)
            throws IOException {
        List<Role> roles;
        if (roleName != null || roleCode != null || status != null) {
            roles = roleApplicationService.getAllRolesByCondition(roleName, roleCode, status);
        } else {
            roles = roleApplicationService.getAllRoles();
        }
        List<RoleExportVO> exportList = roles.stream().map(this::convertToExportVO).collect(Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, RoleExportVO.class, "角色列表", "角色数据");
    }

    private Role convertToEntity(RoleRequest request) {
        return Role.builder()
                .roleCode(request.getRoleCode())
                .roleName(request.getRoleName())
                .roleSort(request.getOrderNum())
                .dataScope(request.getDataScope())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
    }

    private RoleResponse convertToResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .orderNum(role.getRoleSort())
                .dataScope(role.getDataScope())
                .status(role.getStatus())
                .remark(role.getRemark())
                .createTime(role.getCreateTime())
                .build();
    }

    private RoleExportVO convertToExportVO(Role role) {
        return RoleExportVO.builder()
                .id(role.getId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .orderNum(role.getRoleSort())
                .dataScope(role.getDataScope())
                .status(role.getStatus())
                .createTime(role.getCreateTime())
                .remark(role.getRemark())
                .build();
    }

}
