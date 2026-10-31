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

import com.iboot.kernel.web.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 安全上下文工具。
 *
 * <p>从 Spring Security 的 {@link org.springframework.security.core.context.SecurityContextHolder}
 * 读取当前登录用户 ID、用户名等信息，供控制器和应用服务使用。
 *
 * <p>未登录时的异常语义以管理端为准（抛 {@link BusinessException}，由全局异常处理器转成统一响应）；
 * AI 服务迁移前抛 {@code IllegalStateException}，差异已登记在变更说明中。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    private SecurityUtils() {
        // 私有构造函数，防止实例化
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID
     *
     * @throws BusinessException 当用户未登录时抛出
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("用户未登录");
        }
        Object principal = authentication.getPrincipal();
        // 情况 1:principal 是 Map 类型 (JWT 认证过滤器设置)
        if (principal instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> principalMap = (Map<String, Object>) principal;
            Object userId = principalMap.get(Constants.USER_ID);
            if (userId != null) {
                return Long.valueOf(userId.toString());
            }
        }
        // 情况 2：principal 是 UserDetails 或其他类型，尝试从 details 获取
        Object details = authentication.getDetails();
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> detailsMap = (Map<String, Object>) details;
            Object userId = detailsMap.get(Constants.USER_ID);
            if (userId != null) {
                return Long.valueOf(userId.toString());
            }
        }
        // 情况 3：principal 是字符串，说明认证信息不完整（可能是 Spring Security 默认行为）
        if (principal instanceof String) {
            log.warn("认证信息不完整，principal 为 String 类型，用户可能未通过 JWT 认证");
            throw new BusinessException("用户未登录或会话已过期");
        }
        throw new BusinessException("无法获取用户信息");
    }

    /**
     * 获取当前登录用户名
     *
     * @return 用户名
     *
     * @throws BusinessException 当用户未登录时抛出
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("用户未登录");
        }
        Object principal = authentication.getPrincipal();
        // 情况 1:principal 是 Map 类型 (JWT 认证过滤器设置)
        if (principal instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> principalMap = (Map<String, Object>) principal;
            Object username = principalMap.get("username");
            if (username != null) {
                return username.toString();
            }
        }
        // 情况 2:principal 是字符串或其他类型
        return authentication.getName();
    }

    /**
     * 获取当前认证信息
     *
     * @return 认证信息
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 判断用户是否已登录
     *
     * @return 是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * 获取当前登录用户部门 ID
     *
     * @return 部门 ID，无法获取时返回 null
     */
    public static Long getCurrentDeptId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return null;
            }
            Object principal = authentication.getPrincipal();
            // 情况 1:principal 是 Map 类型 (JWT 认证过滤器设置)
            if (principal instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> principalMap = (Map<String, Object>) principal;
                Object deptId = principalMap.get(Constants.DEPT_ID);
                if (deptId != null) {
                    return Long.valueOf(deptId.toString());
                }
            }
            // 情况 2：尝试从 details 获取
            Object details = authentication.getDetails();
            if (details instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> detailsMap = (Map<String, Object>) details;
                Object deptId = detailsMap.get(Constants.DEPT_ID);
                if (deptId != null) {
                    return Long.valueOf(deptId.toString());
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前会话的权限码集合。
     *
     * @return 权限码集合；未认证时为空集合
     */
    public static Set<String> getCurrentPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 读取当前请求的 Bearer 令牌，供服务间以调用者身份代理下游请求。
     *
     * <p>只能在请求线程调用；异步线程不保留请求上下文。
     *
     * @return 令牌字符串
     * @throws BusinessException 缺少请求上下文或 {@code Authorization} 头时抛出
     */
    public static String getCurrentBearerToken() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new BusinessException("缺少请求上下文");
        }
        String header = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException("缺少 Authorization 请求头");
        }
        return header.substring("Bearer ".length());
    }

}
