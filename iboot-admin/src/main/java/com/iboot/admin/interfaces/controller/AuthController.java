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

import com.iboot.admin.application.service.AuthApplicationService;
import com.iboot.admin.interfaces.dto.response.UserScopeResponse;
import com.iboot.kernel.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据权限范围控制器。
 *
 * <p>只保留 {@code GET /api/auth/scope}：登录、登出与注册已迁至 {@code iboot-auth}。
 * 本接口返回的可见部门范围来自系统域的部门、角色与用户数据，管理端的数据权限拦截器
 * 本来就在本地解析同一份结果，因此不随认证服务迁移；{@code iboot-system} 拆分时随系统域迁走。
 *
 * <p>返回的是当前会话对应的范围，不接受用户标识参数。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see AuthApplicationService
 */
@Tag(name = "认证管理", description = "当前用户的数据权限范围")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    /**
     * 创建控制器。
     *
     * @param authApplicationService 认证应用服务
     */
    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    /**
     * 查询当前登录用户的数据权限范围。
     *
     * <p>只读接口，不接受用户标识参数，始终返回当前会话对应的用户范围，供文件服务与 AI 服务
     * 做检索前权限过滤。
     *
     * @return 当前用户的数据权限范围
     */
    @Operation(summary = "查询当前用户数据权限范围")
    @GetMapping(version = "1", value = "/scope")
    public Result<UserScopeResponse> scope() {
        return Result.success(authApplicationService.getCurrentUserScope());
    }
}
