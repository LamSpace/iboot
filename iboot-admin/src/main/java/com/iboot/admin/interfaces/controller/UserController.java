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

import com.iboot.admin.application.mapper.system.UserMapper;
import com.iboot.admin.application.service.ConfigApplicationService;
import com.iboot.admin.application.service.DeptApplicationService;
import com.iboot.admin.application.service.PostApplicationService;
import com.iboot.admin.application.service.RoleApplicationService;
import com.iboot.admin.application.service.UserApplicationService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import com.iboot.admin.interfaces.dto.export.UserExportVO;
import com.iboot.admin.interfaces.dto.request.CreateUserRequest;
import com.iboot.admin.interfaces.dto.request.UpdatePasswordRequest;
import com.iboot.admin.interfaces.dto.request.UpdateProfileRequest;
import com.iboot.admin.interfaces.dto.request.UpdateUserRequest;
import com.iboot.admin.interfaces.dto.response.ProfileResponse;
import com.iboot.admin.interfaces.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户管理控制器
 *
 * @author iBoot
 */
@Tag(name = "用户管理", description = "用户管理相关接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final UserApplicationService userApplicationService;
    private final ConfigApplicationService configApplicationService;
    private final DeptApplicationService deptApplicationService;
    private final PostApplicationService postApplicationService;
    private final RoleApplicationService roleApplicationService;

    private static final String CONFIG_KEY_INIT_PASSWORD = "sys.user.initPassword";
    private static final String DEFAULT_INIT_PASSWORD = "123456";

    @Operation(summary = "查询用户列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('user:list')")
    public Result<PageResult<UserResponse>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {

        List<User> users;
        long total;

        if (username != null || phone != null || status != null) {
            users = userApplicationService.getUserPageByCondition(username, phone, status, pageNum, pageSize);
            total = userApplicationService.countUsersByCondition(username, phone, status);
        } else {
            users = userApplicationService.getUserPage(pageNum, pageSize);
            total = userApplicationService.countUsers();
        }

        List<UserResponse> userResponses = userMapper.toResponseList(users);

        PageResult<UserResponse> pageResult = new PageResult<>(userResponses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/profile")
    public Result<ProfileResponse> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userApplicationService.getUserById(userId);
        return Result.success(convertToProfileResponse(user));
    }

    @Operation(summary = "更新当前用户信息")
    @PutMapping("/profile")
    @Log(title = "个人中心", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = User.builder()
                .id(userId)
                .nickname(request.getNickname())
                .email(request.getEmail())
                .phone(request.getPhone())
                .gender(request.getGender())
                .build();
        userApplicationService.updateUser(user);
        return Result.success();
    }

    @Operation(summary = "修改当前用户密码")
    @PutMapping("/profile/password")
    @Log(title = "个人中心", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userApplicationService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:query')")
    public Result<UserResponse> getById(@PathVariable Long id) {
        User user = userApplicationService.getUserById(id);
        return Result.success(userMapper.toResponse(user));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasAuthority('user:add')")
    @Log(title = "用户管理", businessType = BusinessTypeEnum.INSERT)
    public Result<UserResponse> add(@Valid @RequestBody CreateUserRequest request) {
        // 如果密码为空，使用系统配置的默认密码
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            String initPassword = configApplicationService.getConfigValue(CONFIG_KEY_INIT_PASSWORD);
            request.setPassword(initPassword != null ? initPassword : DEFAULT_INIT_PASSWORD);
        }
        User user = userMapper.toEntity(request);
        User createdUser = userApplicationService.createUser(user);
        return Result.success(userMapper.toResponse(createdUser));
    }

    @Operation(summary = "修改用户")
    @PutMapping
    @PreAuthorize("hasAuthority('user:edit')")
    @Log(title = "用户管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> update(@Valid @RequestBody UpdateUserRequest request) {
        User user = userApplicationService.getUserById(request.getId());
        userMapper.updateEntityFromRequest(request, user);
        userApplicationService.updateUser(user);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:remove')")
    @Log(title = "用户管理", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (id.equals(currentUserId)) {
            throw new BusinessException("不允许删除当前登录用户");
        }
        userApplicationService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "修改用户状态")
    @PutMapping("/changeStatus")
    @PreAuthorize("hasAuthority('user:edit')")
    @Log(title = "用户管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> changeStatus(@RequestParam Long id, @RequestParam Integer status) {
        if (status == 0) {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (id.equals(currentUserId)) {
                throw new BusinessException("不允许停用当前登录用户");
            }
        }
        userApplicationService.changeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/resetPassword")
    @PreAuthorize("hasAuthority('user:resetPwd')")
    @Log(title = "用户管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> resetPassword(@RequestParam Long id, @RequestParam(required = false) String newPassword) {
        // 如果新密码为空，使用系统配置的默认密码
        String password = newPassword;
        if (password == null || password.isBlank()) {
            password = configApplicationService.getConfigValue(CONFIG_KEY_INIT_PASSWORD);
            if (password == null) {
                password = DEFAULT_INIT_PASSWORD;
            }
        }
        userApplicationService.resetPassword(id, password);
        return Result.success();
    }

    @Operation(summary = "导出用户列表")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('user:export')")
    @Log(title = "用户管理", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) String phone,
                       @RequestParam(required = false) Integer status) throws IOException {
        List<User> users;
        if (username != null || phone != null || status != null) {
            users = userApplicationService.getAllUsersByCondition(username, phone, status);
        } else {
            users = userApplicationService.getAllUsers();
        }

        List<UserExportVO> exportList = userMapper.toExportVOList(users);

        ExcelExportUtil.exportExcel(response, exportList, UserExportVO.class, "用户列表", "用户数据");
    }

    /**
     * 将领域实体转换为个人信息响应 DTO
     *
     * @param user 用户领域实体
     * @return 个人信息响应 DTO
     */
    private ProfileResponse convertToProfileResponse(User user) {
        // 获取部门名称
        String deptName = null;
        if (user.getDeptId() != null) {
            try {
                deptName = deptApplicationService.getDeptById(user.getDeptId()).getDeptName();
            } catch (Exception e) {
                // 部门不存在时忽略
            }
        }

        // 获取岗位名称列表
        List<String> postNames = new ArrayList<>();
        if (user.getPostIds() != null) {
            for (Long postId : user.getPostIds()) {
                try {
                    postNames.add(postApplicationService.getPostById(postId).getPostName());
                } catch (Exception e) {
                    // 岗位不存在时忽略
                }
            }
        }

        // 获取角色名称列表
        List<String> roleNames = new ArrayList<>();
        if (user.getRoleIds() != null) {
            for (Long roleId : user.getRoleIds()) {
                try {
                    roleNames.add(roleApplicationService.getRoleById(roleId).getRoleName());
                } catch (Exception e) {
                    // 角色不存在时忽略
                }
            }
        }

        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .avatar(user.getAvatar())
                .deptName(deptName)
                .postNames(postNames)
                .roleNames(roleNames)
                .createTime(user.getCreateTime())
                .build();
    }
}
