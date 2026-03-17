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

import com.iboot.admin.common.constant.Constants;
import com.iboot.admin.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

/**
 * 安全工具类
 *
 * @author iBoot
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

}
