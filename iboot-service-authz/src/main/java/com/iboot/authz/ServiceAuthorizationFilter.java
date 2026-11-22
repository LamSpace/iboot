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

package com.iboot.authz;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet 服务间授权过滤器。
 *
 * <p>在 Spring Security 之前执行：命中路径规则且调用方不在白名单时直接返回 403，
 * 不进入 JWT 认证与业务逻辑；未命中规则时放行。过滤器自身不解析令牌，也不改写
 * 认证上下文。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ServiceAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuthorizationFilter.class);

    private static final String DENIED_BODY = "{\"code\":403,\"message\":\"调用方无权访问\"}";

    private final ServiceAuthorizationProperties properties;

    private final ServiceAuthorizationPolicy policy;

    /**
     * 创建过滤器。
     *
     * @param properties 授权配置
     */
    public ServiceAuthorizationFilter(final ServiceAuthorizationProperties properties) {
        this.properties = properties;
        this.policy = new ServiceAuthorizationPolicy(properties);
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String caller = ServletClientCertificateIdentity.from(request);
        if (policy.isDenied(path, caller)) {
            log.warn("拒绝服务间调用，caller={}, path={}", caller, path);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(DENIED_BODY);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
