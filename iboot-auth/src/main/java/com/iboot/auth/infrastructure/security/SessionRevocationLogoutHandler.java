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

package com.iboot.auth.infrastructure.security;

import com.iboot.kernel.security.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * 登出时的会话撤销处理器。
 *
 * <p>删除当前用户在线映射指向的会话，使自包含令牌在下一次请求时立即失效——令牌本身无法撤销，
 * 撤销名单是唯一的即时失效手段。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class SessionRevocationLogoutHandler implements LogoutHandler {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 创建处理器。
     *
     * @param redisTemplate 共享会话 Redis 模板
     */
    public SessionRevocationLogoutHandler(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 撤销当前会话。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param authentication 当前认证信息，可为 {@code null}
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        PortalUserPrincipal principal = resolve(authentication);
        if (principal == null) {
            return;
        }
        Long userId = principal.user().id();
        String onlineKey = Constants.ONLINE_TOKEN_KEY + userId;
        Object sessionId = redisTemplate.opsForValue().get(onlineKey);
        if (sessionId != null) {
            redisTemplate.delete(Constants.LOGIN_TOKEN_KEY + sessionId);
            redisTemplate.delete(onlineKey);
        }
    }

    /**
     * 从认证信息中解析用户主体。
     *
     * @param authentication 认证信息
     * @return 用户主体；类型不匹配时返回 {@code null}
     */
    private PortalUserPrincipal resolve(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof PortalUserPrincipal principal) {
            return principal;
        }
        return null;
    }
}
