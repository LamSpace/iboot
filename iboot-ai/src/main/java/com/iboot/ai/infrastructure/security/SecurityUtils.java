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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

/**
 * 安全上下文读取工具。
 *
 * <p>认证主体是 admin 登录时写入 Redis 的会话 Map，其中包含 {@code userId}、{@code username}
 * 与权限列表。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 用户 ID
     * @throws IllegalStateException 未认证时抛出
     */
    public static Long getCurrentUserId() {
        Object userId = principal().get("userId");
        if (userId == null) {
            throw new IllegalStateException("当前登录会话缺少 userId");
        }
        return Long.valueOf(userId.toString());
    }

    /**
     * 获取当前用户名。
     *
     * @return 用户名
     */
    public static String getCurrentUsername() {
        Object username = principal().get("username");
        return username == null ? "" : username.toString();
    }

    private static Map<?, ?> principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Map<?, ?> map)) {
            throw new IllegalStateException("当前请求未认证");
        }
        return map;
    }
}
