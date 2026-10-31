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

import com.iboot.auth.application.service.CredentialAuthenticator;
import com.iboot.auth.domain.model.AuthUser;
import com.iboot.kernel.security.Constants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 访问令牌定制器。
 *
 * <p>把用户标识、用户名、昵称、部门、角色与权限写入令牌声明，使资源服务无需读取共享会话
 * 即可完成鉴权；同时以授权记录 ID 作为令牌 {@code jti}，把会话写入共享 Redis 作为撤销与
 * 在线名单。
 *
 * <p>{@code jti} 在同一个登录会话内保持稳定：刷新令牌时沿用同一授权记录，因此不会产生
 * 孤儿会话，在线用户列表也只显示一条记录。
 *
 * <p>独占会话在授权码换取令牌时执行；刷新令牌时只续期，不驱逐自身。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class PortalTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final RedisTemplate<String, Object> redisTemplate;

    private final CredentialAuthenticator credentialAuthenticator;

    /**
     * 创建令牌定制器。
     *
     * @param redisTemplate 共享会话 Redis 模板
     * @param credentialAuthenticator 凭据认证器，用于读取会话配置与独占会话策略
     */
    public PortalTokenCustomizer(RedisTemplate<String, Object> redisTemplate,
                                 CredentialAuthenticator credentialAuthenticator) {
        this.redisTemplate = redisTemplate;
        this.credentialAuthenticator = credentialAuthenticator;
    }

    /**
     * 定制访问令牌声明，并写入共享会话。
     *
     * @param context 令牌编码上下文
     */
    @Override
    public void customize(JwtEncodingContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return;
        }
        PortalUserPrincipal principal = resolvePrincipal(context);
        if (principal == null) {
            return;
        }
        OAuth2Authorization authorization = context.getAuthorization();
        String sessionId = authorization != null ? authorization.getId() : null;
        if (sessionId == null) {
            return;
        }

        AuthUser user = principal.user();
        context.getClaims()
                .id(sessionId)
                .subject(String.valueOf(user.id()))
                .claim("userId", user.id())
                .claim("username", user.username())
                .claim("nickname", user.nickname())
                .claim("deptId", user.deptId())
                .claim(Constants.ROLES, principal.roles())
                .claim(Constants.PERMISSIONS, principal.permissions())
                .claim("sid", sessionId);

        boolean newLogin = AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getAuthorizationGrantType());
        if (newLogin) {
            credentialAuthenticator.evictPreviousSession(user.id());
        }
        writeSession(sessionId, principal);
    }

    /**
     * 从认证上下文或授权记录中解析用户主体。
     *
     * <p>刷新令牌时当前的 {@code principal} 是客户端，用户主体保存在授权记录中。
     *
     * @param context 令牌编码上下文
     * @return 用户主体；无法确定时返回 {@code null}
     */
    private PortalUserPrincipal resolvePrincipal(JwtEncodingContext context) {
        PortalUserPrincipal principal = asPortalUser(context.getPrincipal());
        if (principal != null) {
            return principal;
        }
        OAuth2Authorization authorization = context.getAuthorization();
        if (authorization == null) {
            return null;
        }
        return asPortalUser(authorization.getAttribute(Principal.class.getName()));
    }

    /**
     * 把对象转换为用户主体。
     *
     * @param candidate 认证对象或主体对象
     * @return 用户主体；类型不匹配时返回 {@code null}
     */
    private PortalUserPrincipal asPortalUser(Object candidate) {
        if (candidate instanceof PortalUserPrincipal portalUserPrincipal) {
            return portalUserPrincipal;
        }
        if (candidate instanceof Authentication authentication
                && authentication.getPrincipal() instanceof PortalUserPrincipal portalUserPrincipal) {
            return portalUserPrincipal;
        }
        return null;
    }

    /**
     * 写入共享会话与在线映射，并续期。
     *
     * @param sessionId 会话标识
     * @param principal 用户主体
     */
    private void writeSession(String sessionId, PortalUserPrincipal principal) {
        AuthUser user = principal.user();
        Map<String, Object> session = new HashMap<>();
        session.put(Constants.USER_ID, user.id());
        session.put("username", user.username());
        session.put(Constants.PERMISSIONS, principal.permissions());
        session.put(Constants.ROLES, principal.roles());
        session.put("loginIp", principal.loginIp());
        session.put("browser", principal.browser());
        session.put("os", principal.os());
        session.put("loginTime", principal.loginTime().toString());
        session.put("deptId", user.deptId());
        session.put("nickname", user.nickname());

        long sessionTimeoutMinutes = credentialAuthenticator.sessionTimeoutMinutes();
        redisTemplate.opsForValue().set(Constants.LOGIN_TOKEN_KEY + sessionId, session,
                sessionTimeoutMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(Constants.ONLINE_TOKEN_KEY + user.id(), sessionId,
                sessionTimeoutMinutes, TimeUnit.MINUTES);
    }
}
