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
import com.iboot.kernel.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户名密码认证提供者。
 *
 * <p>把 {@link CredentialAuthenticator} 的业务规则接入 Spring Security：锁定、禁用与密码错误的
 * 提示文案保持与拆分前一致，失败时抛出对应的标准异常，由登录页原样展示。
 *
 * <p>只在请求线程内可用；登录页与授权服务器在同一进程，因此可以直接读取当前请求的
 * IP 与 User-Agent 写入登录日志。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class CredentialAuthenticationProvider implements AuthenticationProvider {

    private static final String UNKNOWN_IP = "unknown";

    private final CredentialAuthenticator credentialAuthenticator;

    /**
     * 创建认证提供者。
     *
     * @param credentialAuthenticator 凭据认证器
     */
    public CredentialAuthenticationProvider(CredentialAuthenticator credentialAuthenticator) {
        this.credentialAuthenticator = credentialAuthenticator;
    }

    /**
     * 校验用户名与密码。
     *
     * @param authentication 用户名密码认证请求
     * @return 已认证的令牌，主体为 {@link PortalUserPrincipal}
     * @throws AuthenticationException 锁定、禁用或密码错误时抛出
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString();
        HttpServletRequest request = currentRequest();
        String loginIp = request != null ? clientIp(request) : UNKNOWN_IP;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        CredentialAuthenticator.AuthenticatedUser authenticated;
        try {
            authenticated = credentialAuthenticator.verify(username, password, loginIp, userAgent);
        } catch (BusinessException e) {
            throw translate(e);
        }

        AuthUser user = authenticated.user();
        List<String> permissions = credentialAuthenticator.resolvePermissions(user.roleIds());
        List<String> roles = credentialAuthenticator.resolveRoleCodes(user.roleIds());
        PortalUserPrincipal principal = new PortalUserPrincipal(user, permissions, roles, loginIp,
                authenticated.browser(), authenticated.os(), authenticated.loginTime());

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }

    /**
     * 是否支持用户名密码认证。
     *
     * @param authentication 认证类型
     * @return 用户名密码认证返回 {@code true}
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * 把业务异常转换为 Spring Security 的认证异常，保留原始提示文案。
     *
     * @param e 业务异常
     * @return 对应的认证异常
     */
    private AuthenticationException translate(BusinessException e) {
        String message = e.getMessage() == null ? "登录失败" : e.getMessage();
        if (message.contains("锁定")) {
            return new LockedException(message);
        }
        if (message.contains("禁用")) {
            return new DisabledException(message);
        }
        return new BadCredentialsException(message);
    }

    /**
     * 读取当前请求。
     *
     * @return 当前请求；不在请求线程时返回 {@code null}
     */
    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 取客户端 IP，优先使用代理转发的地址。
     *
     * @param request 当前请求
     * @return 客户端 IP
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
