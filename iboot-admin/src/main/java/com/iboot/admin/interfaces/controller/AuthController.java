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
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.web.BusinessException;
import com.iboot.kernel.web.Result;
import com.iboot.admin.interfaces.dto.request.LoginRequest;
import com.iboot.admin.interfaces.dto.request.RegisterRequest;
import com.iboot.admin.interfaces.dto.response.LoginResponse;
import com.iboot.admin.interfaces.dto.response.UserScopeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器。
 *
 * <p>提供登录、登出和注册接口。登录时从请求中提取客户端 IP 和 User-Agent，
 * 交由 {@link AuthApplicationService} 完成认证、令牌签发和会话记录。
 * 这些接口位于安全白名单中，允许匿名访问。
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

    @SuppressWarnings("all")
    public AuthController(final AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    /**
     * 用户登录。
     *
     * <p>校验用户名和密码，成功后返回 JWT 令牌和用户信息；失败时记录登录日志、
     * 累加失败次数，并抛出业务异常。
     *
     * @param request 登录请求，包含用户名和密码
     * @param httpRequest HTTP 请求，用于提取客户端 IP 和 User-Agent
     * @return 登录结果，包含令牌和用户信息
     * @throws BusinessException 用户名或密码错误、账号被锁定或用户被禁用时抛出
     */
    @Operation(summary = "用户登录")
    @PostMapping(version = "1", value = "/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String loginIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse response = authApplicationService.login(request.getUsername(), request.getPassword(), loginIp,
                userAgent);
        return Result.success("登录成功", response);
    }

    /**
     * 查询当前登录用户的数据权限范围。
     *
     * <p>只读接口，不接受用户标识参数，始终返回当前会话对应的用户范围，供 AI 服务做检索前
     * 权限过滤。
     *
     * @return 当前用户的数据权限范围
     */
    @Operation(summary = "查询当前用户数据权限范围")
    @GetMapping(version = "1", value = "/scope")
    public Result<UserScopeResponse> scope() {
        return Result.success(authApplicationService.getCurrentUserScope());
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
     * 从请求中获取客户端IP
     *
     * @param request HTTP请求
     *
     * @return 客户端IP地址
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
        // 多个代理时，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 从请求中获取Token
     *
     * @param request HTTP请求
     *
     * @return JWT令牌字符串
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith(Constants.TOKEN_PREFIX)) {
            return bearerToken.substring(Constants.TOKEN_PREFIX.length());
        }
        return null;
    }

}
