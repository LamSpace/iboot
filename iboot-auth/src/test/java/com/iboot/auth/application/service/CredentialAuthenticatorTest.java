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

import com.iboot.auth.domain.model.AuthUser;
import com.iboot.auth.infrastructure.metrics.LoginMetrics;
import com.iboot.auth.infrastructure.persistence.AuthDirectory;
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.web.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 凭据认证器的规则测试。
 *
 * <p>登录入口从 MVC 控制器搬到 OAuth2 授权服务器后，锁定、禁用、失败计数与登录日志这些规则仍然
 * 由本组件承担，因此断言保持与拆分前一致。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class CredentialAuthenticatorTest {

    private AuthDirectory authDirectory;

    private PasswordEncoder passwordEncoder;

    private RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> valueOperations;

    private LoginMetrics loginMetrics;

    private CredentialAuthenticator authenticator;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        authDirectory = mock(AuthDirectory.class);
        passwordEncoder = mock(PasswordEncoder.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        loginMetrics = mock(LoginMetrics.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        authenticator = new CredentialAuthenticator(authDirectory, passwordEncoder, redisTemplate, loginMetrics);
    }

    private AuthUser user(Integer status) {
        return new AuthUser(7L, "alice", "encoded", "Alice", "avatar.png", 3L, status, List.of(11L));
    }

    @Test
    @DisplayName("认证成功：更新最后登录信息、记录成功日志并返回登录上下文")
    void verifySuccess() {
        when(authDirectory.findUserByUsername("alice")).thenReturn(Optional.of(user(1)));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

        CredentialAuthenticator.AuthenticatedUser result =
                authenticator.verify("alice", "secret", "127.0.0.1", "Mozilla/5.0 Chrome/120");

        assertThat(result.user().id()).isEqualTo(7L);
        assertThat(result.browser()).isEqualTo("Chrome");
        assertThat(result.os()).isEqualTo("Unknown");
        assertThat(result.loginTime()).isNotNull();
        verify(authDirectory).updateLoginInfo(eq(7L), eq("127.0.0.1"), any());
        verify(loginMetrics).recordLoginSuccess();
        verify(authDirectory).insertLoginLog(eq("alice"), eq("127.0.0.1"), eq(""), eq("Chrome"), eq("Unknown"),
                eq(1), eq("登录成功"), any());
    }

    @Test
    @DisplayName("密码错误：累加失败次数、记录失败指标并抛出业务异常")
    void verifyWrongPassword() {
        when(authDirectory.findUserByUsername("alice")).thenReturn(Optional.of(user(1)));
        when(passwordEncoder.matches("bad", "encoded")).thenReturn(false);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> authenticator.verify("alice", "bad", "127.0.0.1", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");

        verify(valueOperations).increment(Constants.LOGIN_RETRY_KEY + "alice");
        verify(loginMetrics).recordLoginFailed();
        verify(authDirectory, never()).updateLoginInfo(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("用户不存在：与密码错误返回同一提示，避免暴露账号是否存在")
    void verifyUnknownUser() {
        when(authDirectory.findUserByUsername("nobody")).thenReturn(Optional.empty());
        when(valueOperations.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> authenticator.verify("nobody", "secret", "127.0.0.1", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    @DisplayName("账号被禁用：拒绝通过且不更新登录信息")
    void verifyDisabledUser() {
        when(authDirectory.findUserByUsername("alice")).thenReturn(Optional.of(user(0)));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> authenticator.verify("alice", "secret", "127.0.0.1", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户已被禁用");

        verify(authDirectory, never()).updateLoginInfo(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("失败次数达到阈值：锁定期间即使密码正确也拒绝登录")
    void verifyLockedAccount() {
        when(authDirectory.findConfigValue("sys.login.maxRetry")).thenReturn("5");
        when(valueOperations.get(Constants.LOGIN_RETRY_KEY + "alice")).thenReturn(5);

        assertThatThrownBy(() -> authenticator.verify("alice", "secret", "127.0.0.1", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账户锁定");

        verify(authDirectory, never()).findUserByUsername(anyString());
    }

    @Test
    @DisplayName("独占会话：开启后清除该用户的旧会话")
    void evictPreviousSession() {
        when(authDirectory.findConfigValue("sys.session.exclusive")).thenReturn("true");
        when(valueOperations.get(Constants.ONLINE_TOKEN_KEY + 7L)).thenReturn("old-session");

        authenticator.evictPreviousSession(7L);

        verify(redisTemplate).delete(Constants.LOGIN_TOKEN_KEY + "old-session");
        verify(redisTemplate).delete(Constants.ONLINE_TOKEN_KEY + 7L);
    }

    @Test
    @DisplayName("解析权限：超级管理员角色返回全部权限")
    void resolvePermissionsForAdminRole() {
        when(authDirectory.findRoleCode(11L)).thenReturn(Optional.of(Constants.ROLE_ADMIN_CODE));
        when(authDirectory.findAllPermissions()).thenReturn(Set.of("user:list", "file:list", "backup:list"));

        assertThat(authenticator.resolvePermissions(List.of(11L)))
                .containsExactlyInAnyOrder("user:list", "file:list", "backup:list");
    }
}
