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

package com.iboot.auth.application.service;

import com.iboot.auth.infrastructure.metrics.LoginMetrics;
import com.iboot.auth.infrastructure.persistence.AuthDirectory;
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.security.JwtTokenVerifier;
import com.iboot.kernel.web.BusinessException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证应用服务的语义测试。
 *
 * <p>覆盖登出（旧令牌与自包含令牌两种）与自助注册；凭据校验规则由
 * {@link CredentialAuthenticatorTest} 覆盖。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class AuthApplicationServiceTest {

    private static final String TOKEN = "token-abc";

    private static final String SESSION_ID = "session-jti";

    private AuthDirectory authDirectory;

    private PasswordEncoder passwordEncoder;

    private RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> valueOperations;

    private LoginMetrics loginMetrics;

    private JwtTokenVerifier tokenVerifier;

    private AuthApplicationService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        authDirectory = mock(AuthDirectory.class);
        passwordEncoder = mock(PasswordEncoder.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        loginMetrics = mock(LoginMetrics.class);
        tokenVerifier = mock(JwtTokenVerifier.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service = new AuthApplicationService(authDirectory, passwordEncoder, redisTemplate, loginMetrics,
                tokenVerifier);
    }

    @Test
    @DisplayName("登出（旧令牌）：删除会话与在线映射")
    void logoutRemovesLegacySession() {
        Map<String, Object> loginUser = Map.of(Constants.USER_ID, 7L);
        when(valueOperations.get(Constants.LOGIN_TOKEN_KEY + TOKEN)).thenReturn(loginUser);
        when(valueOperations.get(Constants.ONLINE_TOKEN_KEY + 7L)).thenReturn(TOKEN);

        service.logout(TOKEN);

        verify(redisTemplate).delete(Constants.LOGIN_TOKEN_KEY + TOKEN);
        verify(redisTemplate).delete(Constants.ONLINE_TOKEN_KEY + 7L);
        verify(loginMetrics).recordLogout();
    }

    @Test
    @DisplayName("登出（自包含令牌）：按 jti 撤销会话")
    void logoutRemovesStatelessSession() {
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn(SESSION_ID);
        when(tokenVerifier.verify(TOKEN)).thenReturn(new JwtTokenVerifier.VerifiedToken(claims, false));
        when(valueOperations.get(Constants.LOGIN_TOKEN_KEY + SESSION_ID))
                .thenReturn(Map.of(Constants.USER_ID, 7L));
        when(valueOperations.get(Constants.ONLINE_TOKEN_KEY + 7L)).thenReturn(SESSION_ID);

        service.logout(TOKEN);

        verify(redisTemplate).delete(Constants.LOGIN_TOKEN_KEY + SESSION_ID);
        verify(redisTemplate).delete(Constants.ONLINE_TOKEN_KEY + 7L);
        verify(loginMetrics).recordLogout();
    }

    @Test
    @DisplayName("无令牌登出：按成功处理且不触碰 Redis")
    void logoutWithoutTokenIsNoop() {
        service.logout(null);

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("注册关闭：抛出与拆分前一致的提示且不写入用户")
    void registerDisabled() {
        when(authDirectory.findConfigValue("sys.account.registerUser")).thenReturn("false");

        assertThatThrownBy(() -> service.register("bob", "secret", "Bob"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前系统未开启注册功能");

        verify(authDirectory, never()).insertRegisteredUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("用户名重复：拒绝注册")
    void registerDuplicateUsername() {
        when(authDirectory.findConfigValue("sys.account.registerUser")).thenReturn("true");
        when(authDirectory.usernameExists("bob")).thenReturn(true);

        assertThatThrownBy(() -> service.register("bob", "secret", "Bob"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名已存在");
    }

    @Test
    @DisplayName("注册成功：密码加密后写入，昵称为空时回落为用户名")
    void registerSuccess() {
        when(authDirectory.findConfigValue("sys.account.registerUser")).thenReturn("true");
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

        service.register("bob", "secret", "  ");

        verify(authDirectory).removeDeletedUsersByUsername("bob");
        verify(authDirectory).insertRegisteredUser("bob", "encoded-secret", "bob");
    }
}
