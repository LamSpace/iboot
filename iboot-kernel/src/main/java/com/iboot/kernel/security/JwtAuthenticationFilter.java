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

package com.iboot.kernel.security;

import com.iboot.kernel.web.Result;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证过滤器。
 *
 * <p>从请求头 {@code Authorization} 解析 Bearer 令牌，校验通过后将认证信息写入
 * 安全上下文；白名单路径直接放行，未携带或校验失败时交给后续过滤器处理。
 *
 * <p>支持两种令牌：自包含令牌用认证服务公钥验签、声明直接建模，共享存储中的
 * {@code login_tokens:<jti>} 只用于判断会话是否被撤销并做滑动续期；旧的对称签名令牌
 * 仍按「令牌原文即会话键」的方式读取，直到双栈窗口关闭。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * 401 响应体的序列化器；不依赖宿主的 ObjectMapper，避免内核隐式耦合。
     */
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final String USERNAME_CLAIM = "username";

    private final JwtTokenVerifier tokenVerifier;

    private final RedisTemplate<String, Object> redisTemplate;

    private final JwtSecurityConfig jwtSecurityConfig;

    private final SecurityWhitelistProperties whitelistProperties;

    /**
     * 创建过滤器。
     *
     * @param tokenVerifier 令牌校验器
     * @param redisTemplate 共享会话所在的 Redis 模板
     * @param jwtSecurityConfig 会话超时等配置
     * @param whitelistProperties 宿主白名单
     */
    public JwtAuthenticationFilter(final JwtTokenVerifier tokenVerifier,
                                   final RedisTemplate<String, Object> redisTemplate,
                                   final JwtSecurityConfig jwtSecurityConfig,
                                   final SecurityWhitelistProperties whitelistProperties) {
        this.tokenVerifier = tokenVerifier;
        this.redisTemplate = redisTemplate;
        this.jwtSecurityConfig = jwtSecurityConfig;
        this.whitelistProperties = whitelistProperties;
    }

    @Override
    /**
     * 解析令牌并建立安全上下文。
     *
     * <p>白名单路径直接放行；请求未携带有效令牌时也继续过滤器链，
     * 由后续的安全组件决定是否拒绝。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param chain 过滤器链
     * @throws ServletException 过滤器处理失败时抛出
     * @throws IOException 读取请求或写入响应失败时抛出
     */
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (whitelistProperties.isWhitelisted(requestURI)) {
            chain.doFilter(request, response);
            return;
        }
        String token = getTokenFromRequest(request);
        if (StringUtils.isNotBlank(token)) {
            try {
                JwtTokenVerifier.VerifiedToken verified = tokenVerifier.verify(token);
                if (verified != null) {
                    if (verified.legacy()) {
                        authenticateLegacySession(token);
                    } else {
                        authenticateStateless(verified.claims());
                    }
                }
            } catch (TokenVersionRetiredException e) {
                log.warn("令牌版本已停用：{}", e.getMessage());
                writeUnauthorized(response, Constants.TOKEN_VERSION_RETIRED, "登录凭据版本已过期，请重新登录");
                return;
            } catch (Exception e) {
                log.error("JWT 认证失败：{}", e.getMessage());
                writeUnauthorized(response, 401, "认证失败，请重新登录");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 按旧方式从共享存储读取会话并建立认证上下文。
     *
     * @param token 令牌原文，同时是共享存储的键后缀
     */
    @SuppressWarnings("unchecked")
    private void authenticateLegacySession(String token) {
        String redisKey = Constants.LOGIN_TOKEN_KEY + token;
        Map<String, Object> loginUser = (Map<String, Object>) redisTemplate.opsForValue().get(redisKey);
        if (loginUser == null || loginUser.get(Constants.USER_ID) == null) {
            return;
        }
        Long userId = Long.valueOf(loginUser.get(Constants.USER_ID).toString());
        slideSession(redisKey, userId);
        List<String> permissions = (List<String>) loginUser.get(Constants.PERMISSIONS);
        applyAuthentication(loginUser, permissions);
    }

    /**
     * 按自包含令牌的声明建立认证上下文，共享存储只用于撤销判定与滑动续期。
     *
     * @param claims 令牌声明
     */
    @SuppressWarnings("unchecked")
    private void authenticateStateless(Claims claims) {
        String tokenId = claims.getId();
        String subject = claims.getSubject();
        if (StringUtils.isBlank(tokenId) || StringUtils.isBlank(subject)) {
            return;
        }
        String redisKey = Constants.LOGIN_TOKEN_KEY + tokenId;
        if (redisTemplate.opsForValue().get(redisKey) == null) {
            return;
        }
        Long userId = Long.valueOf(subject);
        slideSession(redisKey, userId);
        Object permissionsClaim = claims.get(Constants.PERMISSIONS);
        List<String> permissions = permissionsClaim instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        applyAuthentication(buildPrincipal(claims, userId), permissions);
    }

    /**
     * 把令牌声明转换为与旧会话结构一致的认证主体。
     *
     * <p>下游通过 {@link SecurityUtils} 读取 {@code userId}、{@code username}、
     * {@code deptId} 等键，因此这里保持同名，避免业务代码感知令牌格式变化。
     *
     * @param claims 令牌声明
     * @param userId 用户 ID
     * @return 认证主体
     */
    private Map<String, Object> buildPrincipal(Claims claims, Long userId) {
        Map<String, Object> principal = new HashMap<>();
        principal.put(Constants.USER_ID, userId);
        principal.put(USERNAME_CLAIM, claims.get(USERNAME_CLAIM));
        principal.put(Constants.PERMISSIONS, claims.get(Constants.PERMISSIONS));
        principal.put(Constants.ROLES, claims.get(Constants.ROLES));
        principal.put(Constants.DEPT_ID, claims.get(Constants.DEPT_ID));
        principal.put("nickname", claims.get("nickname"));
        principal.put("jti", claims.getId());
        return principal;
    }

    /**
     * 写入安全上下文。
     *
     * @param principal 认证主体
     * @param permissions 权限码集合
     */
    private void applyAuthentication(Map<String, Object> principal, List<String> permissions) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (permissions != null) {
            for (String permission : permissions) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 刷新会话与在线映射的过期时间。
     *
     * @param redisKey 会话键
     * @param userId 用户 ID
     */
    private void slideSession(String redisKey, Long userId) {
        long sessionTimeoutMinutes = jwtSecurityConfig.getSessionTimeout();
        redisTemplate.expire(redisKey, sessionTimeoutMinutes, TimeUnit.MINUTES);
        redisTemplate.expire(Constants.ONLINE_TOKEN_KEY + userId, sessionTimeoutMinutes, TimeUnit.MINUTES);
    }

    /**
     * 写出 401 响应。
     *
     * @param response HTTP 响应
     * @param code 业务状态码
     * @param message 提示消息
     * @throws IOException 写入失败时抛出
     */
    private void writeUnauthorized(HttpServletResponse response, Integer code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.writeValueAsString(Result.error(code, message)));
    }

    /**
     * 从请求中获取 Token 优先从 Authorization 请求头获取，其次从 URL 参数获取（用于文件下载、SSE 连接等场景）
     *
     * @param request HTTP 请求
     *
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
