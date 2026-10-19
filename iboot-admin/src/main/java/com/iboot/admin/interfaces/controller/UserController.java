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
import com.iboot.admin.application.service.*;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户管理控制器。
 *
 * <p>提供用户列表、详情、新增、修改、删除、状态切换、密码重置、个人中心和导出接口。
 * 控制器只负责 HTTP 参数解析、权限校验和 DTO 转换，业务规则由
 * {@link UserApplicationService} 及部门、岗位、角色相关应用服务承担。
 *
 * <p>除个人中心接口外，管理接口通过 {@code @PreAuthorize} 校验 {@code user:*} 权限；
 * 写操作通过 {@code @Log} 记录操作日志。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see UserApplicationService
 */
@Tag(name = "用户管理", description = "用户管理相关接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    /**
     * 系统配置项：新建用户或重置密码时使用的初始密码。
     */
    private static final String CONFIG_KEY_INIT_PASSWORD = "sys.user.initPassword";

    /**
     * 未配置初始密码时使用的兜底密码。
     */
    private static final String DEFAULT_INIT_PASSWORD = "123456";

    private final UserMapper userMapper;

    private final UserApplicationService userApplicationService;

    private final ConfigApplicationService configApplicationService;

    private final DeptApplicationService deptApplicationService;

    private final PostApplicationService postApplicationService;

    private final RoleApplicationService roleApplicationService;

    @SuppressWarnings("all")
    public UserController(final UserMapper userMapper, final UserApplicationService userApplicationService,
                          final ConfigApplicationService configApplicationService,
                          final DeptApplicationService deptApplicationService, final PostApplicationService postApplicationService,
                          final RoleApplicationService roleApplicationService) {
        this.userMapper = userMapper;
        this.userApplicationService = userApplicationService;
        this.configApplicationService = configApplicationService;
        this.deptApplicationService = deptApplicationService;
        this.postApplicationService = postApplicationService;
        this.roleApplicationService = roleApplicationService;
    }

    /**
     * 分页查询用户列表。
     *
     * <p>任一带条件参数非空时走条件查询，否则查询全部未删除用户；
     * 查询条件与总数统计使用同一组筛选条件。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @param username 用户名关键字，可为 {@code null}
     * @param phone 手机号关键字，可为 {@code null}
     * @param status 状态：0-停用，1-启用；可为 {@code null}
     * @return 分页结果，包含当前页用户列表和符合条件的总数
     */
    @Operation(summary = "查询用户列表")
    @GetMapping(version = "1", value = "/list")
    @PreAuthorize("hasAuthority('user:list')")
    public Result<PageResult<UserResponse>> list(@RequestParam(defaultValue = "1") Integer pageNum,
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

    /**
     * 查询当前登录用户的个人信息。
     *
     * <p>用户 ID 从当前安全上下文获取；部门、岗位、角色名称查询失败时忽略对应项。
     *
     * @return 当前登录用户的个人信息
     * @throws BusinessException 当前用户不存在时抛出
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping(version = "1", value = "/profile")
    public Result<ProfileResponse> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userApplicationService.getUserById(userId);
        return Result.success(convertToProfileResponse(user));
    }

    /**
     * 更新当前登录用户的个人资料。
     *
     * <p>只更新昵称、邮箱、手机号和性别；用户名、状态、角色等字段不受请求影响。
     *
     * @param request 个人资料更新请求
     * @return 无数据的成功响应
     * @throws BusinessException 邮箱或手机号已被占用时抛出
     */
    @Operation(summary = "更新当前用户信息")
    @PutMapping(version = "1", value = "/profile")
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

    /**
     * 修改当前登录用户的密码。
     *
     * <p>需要提供旧密码；新密码由应用服务加密后保存。
     *
     * @param request 包含旧密码和新密码的请求
     * @return 无数据的成功响应
     * @throws BusinessException 旧密码不正确时抛出
     */
    @Operation(summary = "修改当前用户密码")
    @PutMapping(version = "1", value = "/profile/password")
    @Log(title = "个人中心", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userApplicationService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    /**
     * 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户详情
     * @throws BusinessException 用户不存在时抛出
     */
    @Operation(summary = "查询用户详情")
    @GetMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('user:query')")
    public Result<UserResponse> getById(@PathVariable Long id) {
        User user = userApplicationService.getUserById(id);
        return Result.success(userMapper.toResponse(user));
    }

    /**
     * 新增用户。
     *
     * <p>请求未提供密码时，依次使用系统配置 {@code sys.user.initPassword} 和兜底密码
     * {@value #DEFAULT_INIT_PASSWORD}。
     *
     * @param request 创建用户请求
     * @return 创建后的用户详情
     * @throws BusinessException 用户名、邮箱或手机号已存在时抛出
     */
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

    /**
     * 修改用户。
     *
     * <p>先加载用户，再将请求中的可修改字段合并到实体，最后交由应用服务校验并持久化。
     *
     * @param request 更新用户请求
     * @return 无数据的成功响应
     * @throws BusinessException 用户不存在，或邮箱、手机号已被占用时抛出
     */
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

    /**
     * 删除用户。
     *
     * <p>不允许删除当前登录用户；系统内置用户的删除限制由应用服务校验。
     *
     * @param id 用户 ID
     * @return 无数据的成功响应
     * @throws BusinessException 尝试删除当前登录用户，或用户不存在、为系统内置用户时抛出
     */
    @Operation(summary = "删除用户")
    @DeleteMapping(version = "1", value = "/{id}")
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

    /**
     * 修改用户状态。
     *
     * <p>停用当前登录用户会被拒绝；系统内置用户的停用限制由应用服务校验。
     *
     * @param id 用户 ID
     * @param status 目标状态：1-启用，0-停用
     * @return 无数据的成功响应
     * @throws BusinessException 尝试停用当前登录用户，或用户不存在、为系统内置用户时抛出
     */
    @Operation(summary = "修改用户状态")
    @PutMapping(version = "1", value = "/changeStatus")
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

    /**
     * 重置用户密码。
     *
     * <p>未提供新密码时，依次使用系统配置 {@code sys.user.initPassword} 和兜底密码
     * {@value #DEFAULT_INIT_PASSWORD}；重置操作不校验旧密码。
     *
     * @param id 用户 ID
     * @param newPassword 新密码；为 {@code null} 或空白时使用初始密码
     * @return 无数据的成功响应
     * @throws BusinessException 用户不存在时抛出
     */
    @Operation(summary = "重置密码")
    @PutMapping(version = "1", value = "/resetPassword")
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

    /**
     * 导出用户列表。
     *
     * <p>带筛选条件时导出符合条件的用户，否则导出全部未删除用户；
     * 文件通过响应流直接写出，不返回 JSON。
     *
     * @param response HTTP 响应，用于写入 Excel 文件
     * @param username 用户名关键字，可为 {@code null}
     * @param phone 手机号关键字，可为 {@code null}
     * @param status 状态：0-停用，1-启用；可为 {@code null}
     * @throws IOException 写入响应流失败时抛出
     */
    @Operation(summary = "导出用户列表")
    @GetMapping(version = "1", value = "/export")
    @PreAuthorize("hasAuthority('user:export')")
    @Log(title = "用户管理", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response, @RequestParam(required = false) String username,
                       @RequestParam(required = false) String phone, @RequestParam(required = false) Integer status)
            throws IOException {
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
     * 将领域实体转换为个人信息响应 DTO。
     *
     * <p>部门、岗位、角色名称查询失败时忽略对应项，不影响个人信息返回。
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
            }
        }
        // 部门不存在时忽略
        // 获取岗位名称列表
        List<String> postNames = new ArrayList<>();
        if (user.getPostIds() != null) {
            for (Long postId : user.getPostIds()) {
                try {
                    postNames.add(postApplicationService.getPostById(postId).getPostName());
                } catch (Exception e) {
                }
                // 岗位不存在时忽略
            }
        }
        // 获取角色名称列表
        List<String> roleNames = new ArrayList<>();
        if (user.getRoleIds() != null) {
            for (Long roleId : user.getRoleIds()) {
                try {
                    roleNames.add(roleApplicationService.getRoleById(roleId).getRoleName());
                } catch (Exception e) {
                }
                // 角色不存在时忽略
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
