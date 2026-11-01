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

package com.iboot.monitor.application.service;

import com.iboot.kernel.security.Constants;
import com.iboot.kernel.web.BusinessException;
import com.iboot.monitor.infrastructure.directory.DeptNameReader;
import com.iboot.monitor.interfaces.dto.response.OnlineUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OnlineUserApplicationService} 的行为测试。
 *
 * <p>覆盖在线列表的字段补全与强制下线的作用范围：只删除目标会话，
 * 不误伤同一用户的其它会话，也不误删他人的在线映射。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class OnlineUserApplicationServiceTest {

    private static final String TOKEN_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1";

    private static final String TOKEN_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb2";

    private RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> valueOperations;

    private DeptNameReader deptNameReader;

    private OnlineUserApplicationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        deptNameReader = mock(DeptNameReader.class);
        when(deptNameReader.allDeptNames()).thenReturn(Map.of(2L, "研发部"));
        service = new OnlineUserApplicationService(redisTemplate, deptNameReader);
    }

    /**
     * 在线列表补齐部门名称并按登录时间倒序。
     */
    @Test
    void listFillsDeptName() {
        when(redisTemplate.keys(Constants.LOGIN_TOKEN_KEY + "*"))
                .thenReturn(Set.of(Constants.LOGIN_TOKEN_KEY + TOKEN_A));
        when(valueOperations.get(Constants.LOGIN_TOKEN_KEY + TOKEN_A)).thenReturn(session(1L, TOKEN_A, 2L));

        List<OnlineUserResponse> users = service.getOnlineUsers(null, null);

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getDeptName()).isEqualTo("研发部");
        assertThat(users.get(0).getUsername()).isEqualTo("admin");
    }

    /**
     * 强制下线只删除目标会话；同一用户的其它会话不受影响。
     */
    @Test
    void forceLogoutDeletesOnlyTargetSession() {
        when(redisTemplate.keys(Constants.LOGIN_TOKEN_KEY + "*"))
                .thenReturn(Set.of(Constants.LOGIN_TOKEN_KEY + TOKEN_A, Constants.LOGIN_TOKEN_KEY + TOKEN_B));
        when(valueOperations.get(Constants.LOGIN_TOKEN_KEY + TOKEN_A)).thenReturn(session(1L, TOKEN_A, 2L));
        when(valueOperations.get(Constants.ONLINE_TOKEN_KEY + 1L)).thenReturn(TOKEN_B);

        service.forceLogout(mask(TOKEN_A));

        verify(redisTemplate).delete(Constants.LOGIN_TOKEN_KEY + TOKEN_A);
        verify(redisTemplate, never()).delete(Constants.LOGIN_TOKEN_KEY + TOKEN_B);
        // 在线映射指向的是另一个会话，不应被删除
        verify(redisTemplate, never()).delete(Constants.ONLINE_TOKEN_KEY + 1L);
    }

    /**
     * 会话不存在时给出明确错误，而不是静默成功。
     */
    @Test
    void forceLogoutRejectsUnknownSession() {
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        assertThatThrownBy(() -> service.forceLogout("not-exist"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("会话不存在");
    }

    /**
     * 在线人数按会话键数量统计。
     */
    @Test
    void countReflectsSessionKeys() {
        when(redisTemplate.keys(Constants.LOGIN_TOKEN_KEY + "*"))
                .thenReturn(Set.of(Constants.LOGIN_TOKEN_KEY + TOKEN_A, Constants.LOGIN_TOKEN_KEY + TOKEN_B));

        assertThat(service.getOnlineUserCount()).isEqualTo(2);
    }

    private Map<String, Object> session(Long userId, String token, Long deptId) {
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.USER_ID, userId);
        map.put("username", "admin");
        map.put("nickname", "管理员");
        map.put("deptId", deptId);
        map.put("loginIp", "127.0.0.1");
        map.put("loginTime", "2026-11-01 10:00:00");
        map.put("roles", List.of("admin"));
        return map;
    }

    private String mask(String token) {
        return token.substring(0, 8) + "****" + token.substring(token.length() - 8);
    }
}
