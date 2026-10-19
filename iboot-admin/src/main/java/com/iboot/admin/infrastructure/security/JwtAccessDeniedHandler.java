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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 授权失败处理器。
 *
 * <p>当已认证用户访问无权限资源（例如 {@code @PreAuthorize} 校验失败）时，
 * 返回状态码 403 和统一的 JSON 错误响应，而不是 Spring Security 默认的错误页。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    @SuppressWarnings("all")
    public JwtAccessDeniedHandler(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    /**
     * 处理授权失败。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param accessDeniedException 授权异常
     * @throws IOException 写入响应失败时抛出
     */
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.error("授权失败: {}", accessDeniedException.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> result = Result.error(403, "无权限访问");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

}
