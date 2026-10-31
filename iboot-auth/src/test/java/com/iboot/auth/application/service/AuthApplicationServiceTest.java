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
import com.iboot.auth.interfaces.dto.response.LoginResponse;
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.security.JwtSecurityConfig;
import com.iboot.kernel.security.JwtTokenUtil;
import com.iboot.kernel.web.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
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
 * {@link AuthApplicationService} 的行为测试。
 *
 * <p>覆盖登录成功与失败、禁用账号、失败锁定、独占会话、登出幂等与注册开关等关键分支。
 * 依赖以 Mock 替代，测试不接触真实数据库与 Redis。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class AuthApplicationServiceTest {

    private static final String TOKEN = "token-abc";

    private AuthDirectory authDirectory;

    private PasswordEncoder passwordEncoder;

    private JwtTokenUtil jwtTokenUtil;

    private JwtSecurityConfig jwtSecurityConfig;

    private RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> valueOperations;

    private LoginMetrics loginMetrics;

    private AuthApplicationService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        authDirectory = mock(AuthDirectory.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenUtil = mock(JwtTokenUtil.class);
        jwtSecurityConfig = mock(JwtSecurityConfig.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        loginMetrics = mock(LoginMetrics.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtSecurityConfig.getExpiration()).thenReturn(604800L);
        when(jwtTokenUtil.createToken(any(), anyLong())).thenReturn(TOKEN);

        service = new AuthApplicationService(authDirectory, passwordEncoder, jwtTokenUtil, jwtSecurityConfig,
                redisTemplate, loginMetrics);
    }

    private AuthUser user(Integer status) {
        return new AuthUser(7L, "alice", "encoded", "Alice", "avatar.png", 3L, status, List.of(11L));
    }

    @Test
    @DisplayName("登录成功：签发令牌、写入共享会话并返回权限与角色")
    void loginSuccessWritesSession() {
        when(authDirectory.findUserByUsername("alice")).thenReturn(Optional.of(user(1)));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(authDirectory.findRoleCode(11L)).thenReturn(Optional.of("ROLE_USER"));
        when(authDirectory.findPermissionsByRoleIds(List.of(11L))).thenReturn(Set.of("user:list", "file:list"));

        LoginResponse response = service.login("alice", "secret", "127.0.0.1", "Mozilla/5.0 Chrome/120");

        assertThat(response.getToken()).isEqualTo(TOKEN);
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getPermissions()).containsExactlyInAnyOrder("user:list", "file:list");
        assertThat(response.getRoles()).containsExactly("ROLE_USER");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(valueOperations).set(eq(Constants.LOGIN_TOKEN_KEY + TOKEN), captor.capture(), anyLong(), any());
        assertThat(captor.getValue()).containsEntry("deptId", 3L).containsEntry("username", "alice");

        verify(authDirectory).updateLoginInfo(eq(7L), eq("127.0.0.1"), any());
        verify(loginMetrics).recordLoginSuccess();
        verify(authDirectory).insertLoginLog(eq("alice"), eq("127.0.0.1"), eq(""), eq("Chrome"), eq("Unknown"),
                eq(1), eq("登录成功"), any());
    }

    @Test
    @DisplayName("密码错误：累加失败次数、记录失败指标并抛出业务异常")
    void loginWrongPassword() {
        when(authDirectory.findUserByUsername("alice")).thenReturn(Optional.of(user(1)));
        when(passwordEncoder.matches("bad", "encoded")).thenReturn(false);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> service.login("alice", "bad", "127.0.0.1", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");

        verify(valueOperations).increment(Constants.LOGIN_RETRY_KEY + "alice");
        verify(loginMetrics).recordLoginFailed();
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("用户不存在：与密码错误返回同一提示，避免暴露账号是否存在")
    void loginUnknownUser() {
        when(authDirectory.findUserByUsername("nobody")).thenReturn(Optional.empty());
        when(valueOperations.increment(anyString())).thenReturn(1L);

        assertThatThrownBy(() -> service.login("nobody", "secret", "127.0.0.1", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    @DisplayName("账号被禁用：拒绝登录且不签发令牌")
    void loginDisabledUser() {
        when(authDirectory.findUserByUsername("alice")).thenReturn(Optional.of(user(0)));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> service.login("alice", "secret", "127.0.0.1", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户已被禁用");

        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("失败次数达到阈值：锁定期间即使密码正确也拒绝登录")
    void loginLockedAccount() {
        when(authDirectory.findConfigValue("sys.login.maxRetry")).thenReturn("5");
        when(valueOperations.get(Constants.LOGIN_RETRY_KEY + "alice")).thenReturn(5);

        assertThatThrownBy(() -> service.login("alice", "secret", "127.0.0.1", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账户锁定");

        verify(authDirectory, never()).findUserByUsername(anyString());
    }

    @Test
    @DisplayName("独占会话：开启后清除该用户的旧令牌")
    void loginEvictsPreviousSession() {
        when(authDirectory.findUserByUsername("alice")).thenReturn(Optional.of(user(1)));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(authDirectory.findConfigValue("sys.session.exclusive")).thenReturn("true");
        when(valueOperations.get(Constants.ONLINE_TOKEN_KEY + 7L)).thenReturn("old-token");

        service.login("alice", "secret", "127.0.0.1", null);

        verify(redisTemplate).delete(Constants.LOGIN_TOKEN_KEY + "old-token");
        verify(redisTemplate).delete(Constants.ONLINE_TOKEN_KEY + 7L);
    }

    @Test
    @DisplayName("登出：删除会话与在线映射")
    void logoutRemovesSession() {
        Map<String, Object> loginUser = Map.of(Constants.USER_ID, 7L);
        when(valueOperations.get(Constants.LOGIN_TOKEN_KEY + TOKEN)).thenReturn(loginUser);
        when(valueOperations.get(Constants.ONLINE_TOKEN_KEY + 7L)).thenReturn(TOKEN);

        service.logout(TOKEN);

        verify(redisTemplate).delete(Constants.LOGIN_TOKEN_KEY + TOKEN);
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
