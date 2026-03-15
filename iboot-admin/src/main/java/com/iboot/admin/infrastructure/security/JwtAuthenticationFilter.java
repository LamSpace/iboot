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
import com.iboot.admin.common.constant.Constants;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.infrastructure.config.JwtSecurityConfig;
import com.iboot.admin.infrastructure.config.SecurityWhitelistConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器
 *
 * @author iBoot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtSecurityConfig jwtSecurityConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 检查是否为白名单路径（使用统一配置的匹配方法）
        String requestURI = request.getRequestURI();
        boolean isWhiteListPath = SecurityWhitelistConfig.isWhitelisted(requestURI);

        // 如果是白名单路径，直接放行
        if (isWhiteListPath) {
            chain.doFilter(request, response);
            return;
        }

        // 获取 token
        String token = getTokenFromRequest(request);

        if (StringUtils.isNotBlank(token)) {
            try {
                // 验证 token 签名（过期时间由 Redis 控制）
                if (jwtTokenUtil.validateTokenSignature(token)) {
                    // 从 Redis 中获取用户信息
                    String redisKey = Constants.LOGIN_TOKEN_KEY + token;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> loginUser = (Map<String, Object>) redisTemplate.opsForValue().get(redisKey);

                    if (loginUser != null) {
                        // 构建认证信息
                        Long userId = Long.valueOf(loginUser.get(Constants.USER_ID).toString());
                        String username = loginUser.get("username").toString();

                        // 刷新 token 过期时间（从配置读取）
                        long sessionTimeoutMinutes = jwtSecurityConfig.getSessionTimeout();
                        redisTemplate.expire(redisKey, sessionTimeoutMinutes, TimeUnit.MINUTES);

                        // 同步刷新 online_token 映射的过期时间
                        String onlineKey = Constants.ONLINE_TOKEN_KEY + userId;
                        redisTemplate.expire(onlineKey, sessionTimeoutMinutes, TimeUnit.MINUTES);

                        @SuppressWarnings("unchecked")
                        List<String> permissions = (List<String>) loginUser.get(Constants.PERMISSIONS);
                        List<SimpleGrantedAuthority> authorities = permissions.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(loginUser, null, authorities);

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                log.error("JWT 认证失败：{}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, "认证失败，请重新登录")));
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * 从请求中获取 Token
     * 优先从 Authorization 请求头获取，其次从 URL 参数获取（用于文件下载、SSE 连接等场景）
     *
     * @param request HTTP 请求
     * @return JWT 令牌字符串
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 优先从请求头获取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith(Constants.TOKEN_PREFIX)) {
            return bearerToken.substring(Constants.TOKEN_PREFIX.length());
        }
        // 其次从 URL 参数获取（用于文件下载、SSE 连接等场景）
        // 支持 "token" 和 "t" 两种参数名
        String tokenParam = request.getParameter("token");
        if (StringUtils.isNotBlank(tokenParam)) {
            return tokenParam;
        }
        String shortTokenParam = request.getParameter("t");
        if (StringUtils.isNotBlank(shortTokenParam)) {
            return shortTokenParam;
        }
        return null;
    }
}
