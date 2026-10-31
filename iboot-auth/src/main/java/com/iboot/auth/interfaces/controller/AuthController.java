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

package com.iboot.auth.interfaces.controller;

import com.iboot.auth.application.service.AuthApplicationService;
import com.iboot.auth.interfaces.dto.request.RegisterRequest;
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.web.BusinessException;
import com.iboot.kernel.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器。
 *
 * <p>提供登出与注册接口。登录已改为 OAuth2 授权码流程，由授权服务器端点承担，
 * 不再有用户名密码登录接口。两个接口都在安全白名单中，允许匿名访问。
 *
 * <p>数据权限范围接口（{@code GET /api/auth/scope}）仍在管理端，属于系统域，不在本服务范围。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see AuthApplicationService
 */
@Tag(name = "认证管理", description = "用户登录、登出、注册相关接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    /**
     * 创建认证控制器。
     *
     * @param authApplicationService 认证应用服务
     */
    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    /**
     * 用户登出。
     *
     * <p>从请求头解析 Bearer 令牌并使对应会话失效；未携带令牌时按成功处理。
     *
     * @param request HTTP 请求，用于读取 Authorization 头
     * @return 无数据的成功响应
     */
    @Operation(summary = "用户登出")
    @PostMapping(version = "1", value = "/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        authApplicationService.logout(token);
        return Result.success("登出成功", null);
    }

    /**
     * 用户注册。
     *
     * <p>要求系统开启注册功能；两次输入的密码必须一致，用户名唯一性由应用服务校验。
     *
     * @param request 注册请求，包含用户名、密码、确认密码和昵称
     * @return 无数据的成功响应
     * @throws BusinessException 两次密码不一致、注册未开启或用户名已存在时抛出
     */
    @Operation(summary = "用户注册", description = "用户自主注册账号，需要系统开启注册功能")
    @PostMapping(version = "1", value = "/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        authApplicationService.register(request.getUsername(), request.getPassword(), request.getNickname());
        return Result.success("注册成功", null);
    }

    /**
     * 从请求中解析客户端真实 IP，兼容常见反向代理头。
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 从 Authorization 头中提取 Bearer 令牌。
     *
     * @param request HTTP 请求
     * @return 令牌字符串；请求头缺失或格式不符时返回 {@code null}
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith(Constants.TOKEN_PREFIX)) {
            return bearerToken.substring(Constants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
