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

import com.iboot.admin.application.service.OnlineUserApplicationService;
import com.iboot.kernel.logging.Log;
import com.iboot.kernel.logging.BusinessTypeEnum;
import com.iboot.kernel.web.PageResult;
import com.iboot.kernel.web.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.interfaces.dto.export.OnlineUserExportVO;
import com.iboot.admin.interfaces.dto.response.OnlineUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 在线用户监控控制器。
 *
 * <p>提供在线用户分页查询、数量统计、强制退出和导出接口，需要
 * {@code online:list}、{@code online:forceLogout}、{@code online:export} 权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see OnlineUserApplicationService
 */
@Tag(name = "在线用户监控", description = "在线用户监控相关接口")
@RestController
@RequestMapping("/api/online")
public class OnlineUserController {

    private final OnlineUserApplicationService onlineUserApplicationService;

    @SuppressWarnings("all")
    public OnlineUserController(final OnlineUserApplicationService onlineUserApplicationService) {
        this.onlineUserApplicationService = onlineUserApplicationService;
    }

    /**
     * 分页查询在线用户。
     *
     * <p>先按条件筛选全部在线用户，再在内存中分页。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @param username 用户名关键字，可为 {@code null}
     * @param loginIp 登录 IP 关键字，可为 {@code null}
     * @return 在线用户分页结果
     */
    @Operation(summary = "查询在线用户列表")
    @GetMapping(version = "1", value = "/list")
    @PreAuthorize("hasAuthority('online:list')")
    public Result<PageResult<OnlineUserResponse>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                                       @RequestParam(required = false) String username,
                                                       @RequestParam(required = false) String loginIp) {
        List<OnlineUserResponse> allUsers = onlineUserApplicationService.getOnlineUsers(username, loginIp);
        // 内存分页
        long total = allUsers.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, allUsers.size());
        int toIndex = Math.min(fromIndex + pageSize, allUsers.size());
        List<OnlineUserResponse> pageData = allUsers.subList(fromIndex, toIndex);
        PageResult<OnlineUserResponse> pageResult = new PageResult<>(pageData, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 获取在线用户总数。
     *
     * @return 在线用户数量
     */
    @Operation(summary = "获取在线用户总数")
    @GetMapping(version = "1", value = "/count")
    @PreAuthorize("hasAuthority('online:list')")
    public Result<Long> count() {
        return Result.success(onlineUserApplicationService.getOnlineUserCount());
    }

    /**
     * 强制退出指定在线用户。
     *
     * @param tokenId 在线会话 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "强制退出用户")
    @DeleteMapping(version = "1", value = "/{tokenId}")
    @PreAuthorize("hasAuthority('online:forceLogout')")
    @Log(title = "在线用户", businessType = BusinessTypeEnum.FORCE_LOGOUT)
    public Result<Void> forceLogout(@PathVariable String tokenId) {
        onlineUserApplicationService.forceLogout(tokenId);
        return Result.success("强退成功", null);
    }

    /**
     * 导出在线用户列表。
     *
     * @param response HTTP 响应，用于写入 Excel 文件
     * @param username 用户名关键字，可为 {@code null}
     * @param loginIp 登录 IP 关键字，可为 {@code null}
     * @throws IOException 写入响应流失败时抛出
     */
    @Operation(summary = "导出在线用户列表")
    @GetMapping(version = "1", value = "/export")
    @PreAuthorize("hasAuthority('online:export')")
    @Log(title = "在线用户", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response, @RequestParam(required = false) String username,
                       @RequestParam(required = false) String loginIp) throws IOException {
        List<OnlineUserResponse> allUsers = onlineUserApplicationService.getOnlineUsers(username, loginIp);
        List<OnlineUserExportVO> exportList = allUsers.stream()
                .map(this::convertToExportVO)
                .collect(java.util.stream.Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, OnlineUserExportVO.class, "在线用户", "在线用户数据");
    }

    private OnlineUserExportVO convertToExportVO(OnlineUserResponse user) {
        return OnlineUserExportVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .deptName(user.getDeptName())
                .loginIp(user.getLoginIp())
                .browser(user.getBrowser())
                .os(user.getOs())
                .loginTime(user.getLoginTime())
                .roles(user.getRoles() != null ? String.join(", ", user.getRoles()) : "")
                .build();
    }

}
