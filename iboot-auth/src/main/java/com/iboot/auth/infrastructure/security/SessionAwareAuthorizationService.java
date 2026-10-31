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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

/**
 * 带会话撤销的授权记录服务。
 *
 * <p>授权记录被移除（登出、令牌撤销）时同步删除共享会话，使自包含令牌在下一次请求时立即失效。
 * 令牌本身无法撤销，撤销名单是唯一的即时失效手段。
 *
 * <p>会话标识沿用授权记录 ID，与令牌定制器写入的 {@code jti} 一致。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class SessionAwareAuthorizationService implements OAuth2AuthorizationService {

    private final OAuth2AuthorizationService delegate;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 创建服务。
     *
     * @param delegate 实际的授权记录存储
     * @param redisTemplate 共享会话 Redis 模板
     */
    public SessionAwareAuthorizationService(OAuth2AuthorizationService delegate,
                                            RedisTemplate<String, Object> redisTemplate) {
        this.delegate = delegate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存授权记录；访问令牌已被置为失效时同步撤销共享会话。
     *
     * <p>登出接口通过「失效令牌后再保存」表达撤销，因此不能只在移除时处理。
     *
     * @param authorization 授权记录
     */
    @Override
    public void save(OAuth2Authorization authorization) {
        delegate.save(authorization);
        if (authorization != null && isAccessTokenInvalidated(authorization)) {
            revokeSession(authorization);
        }
    }

    /**
     * 判断访问令牌是否已被显式失效。
     *
     * @param authorization 授权记录
     * @return 已失效时返回 {@code true}
     */
    private boolean isAccessTokenInvalidated(OAuth2Authorization authorization) {
        OAuth2Authorization.Token<?> accessToken = authorization.getAccessToken();
        return accessToken != null && !accessToken.isActive();
    }

    /**
     * 移除授权记录，同时撤销共享会话。
     *
     * @param authorization 授权记录
     */
    @Override
    public void remove(OAuth2Authorization authorization) {
        delegate.remove(authorization);
        if (authorization != null) {
            revokeSession(authorization);
        }
    }

    /**
     * 按 ID 查询授权记录。
     *
     * @param id 授权记录 ID
     * @return 授权记录；不存在时返回 {@code null}
     */
    @Override
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    /**
     * 按令牌查询授权记录。
     *
     * @param token 令牌
     * @param tokenType 令牌类型
     * @return 授权记录；不存在时返回 {@code null}
     */
    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return delegate.findByToken(token, tokenType);
    }

    /**
     * 删除会话与在线映射。
     *
     * @param authorization 授权记录
     */
    @SuppressWarnings("unchecked")
    private void revokeSession(OAuth2Authorization authorization) {
        String sessionId = authorization.getId();
        redisTemplate.delete(Constants.LOGIN_TOKEN_KEY + sessionId);

        OAuth2Authorization.Token<?> accessToken = authorization.getAccessToken();
        if (accessToken == null) {
            return;
        }
        Object userId = accessToken.getClaims().get("userId");
        if (userId == null) {
            return;
        }
        String onlineKey = Constants.ONLINE_TOKEN_KEY + userId;
        Object mapped = redisTemplate.opsForValue().get(onlineKey);
        if (sessionId.equals(mapped)) {
            redisTemplate.delete(onlineKey);
        }
    }
}
