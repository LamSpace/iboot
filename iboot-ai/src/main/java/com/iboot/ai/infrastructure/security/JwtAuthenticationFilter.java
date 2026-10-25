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

package com.iboot.ai.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 共享登录态过滤器。
 *
 * <p>验签 JWT 后读取 admin 写入 Redis 的 {@code login_tokens:<token>}，建立安全上下文，并按
 * admin 的滑动过期语义刷新会话 TTL。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * 登录会话 key 前缀。
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 用户在线映射 key 前缀。
     */
    public static final String ONLINE_TOKEN_KEY = "online_token:";

    private static final String USER_ID = "userId";

    private static final String PERMISSIONS = "permissions";

    private final JwtTokenUtil jwtTokenUtil;

    private final RedisTemplate<String, Object> redisTemplate;

    private final long sessionTimeoutMinutes;

    /**
     * 创建过滤器。
     *
     * @param jwtTokenUtil          JWT 验签工具
     * @param redisTemplate         共享 Redis 模板
     * @param sessionTimeoutMinutes 会话滑动过期分钟数
     */
    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil,
                                   RedisTemplate<String, Object> redisTemplate,
                                   @Value("${jwt.session-timeout:30}") long sessionTimeoutMinutes) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.redisTemplate = redisTemplate;
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityWhitelistConfig.isWhitelisted(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (StringUtils.isBlank(token) || !jwtTokenUtil.validateTokenSignature(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String loginKey = LOGIN_TOKEN_KEY + token;
            @SuppressWarnings("unchecked")
            Map<String, Object> loginUser = (Map<String, Object>) redisTemplate.opsForValue().get(loginKey);
            if (loginUser == null) {
                chain.doFilter(request, response);
                return;
            }

            Long userId = Long.valueOf(loginUser.get(USER_ID).toString());
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) loginUser.get(PERMISSIONS);
            List<SimpleGrantedAuthority> authorities = permissions == null
                    ? List.of()
                    : permissions.stream().map(SimpleGrantedAuthority::new).toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            redisTemplate.expire(loginKey, sessionTimeoutMinutes, TimeUnit.MINUTES);
            redisTemplate.expire(ONLINE_TOKEN_KEY + userId, sessionTimeoutMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("AI 服务认证失败：{}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"认证失败，请重新登录\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring("Bearer ".length());
        }
        String token = request.getParameter("token");
        if (StringUtils.isNotBlank(token)) {
            return token;
        }
        return request.getParameter("t");
    }
}
