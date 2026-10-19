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

package com.iboot.admin.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.admin.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 认证失败处理器。
 *
 * <p>当请求没有有效令牌或认证失败时，返回状态码 401 和统一的 JSON 错误响应，
 * 并保持响应内容类型为 JSON。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper;

    @SuppressWarnings("all")
    public JwtAuthenticationEntryPoint(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    /**
     * 处理认证失败。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param authException 认证异常
     * @throws IOException 写入响应失败时抛出
     */
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        log.error("认证失败 - 请求路径: {}, 方法: {}, 错误信息: {}", requestURI, method, authException.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.error(401, "未授权，请先登录");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

}
