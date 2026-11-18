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

package com.iboot.system.application.service;

import com.iboot.kernel.security.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 个人访问令牌批量撤销器测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class PersonalAccessTokenRevokerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 撤销应同时清理撤销记录与最近使用时间，并标记元数据。
     */
    @Test
    void revokesAllTokensOfUser() {
        PersonalAccessTokenRevoker revoker = new PersonalAccessTokenRevoker(jdbcTemplate, redisTemplate);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(7L)))
                .thenReturn(List.of("jti-1", "jti-2"));

        revoker.revokeAll(7L);

        verify(redisTemplate).delete(List.of(
                Constants.PAT_TOKEN_KEY + "jti-1", Constants.PAT_LAST_USED_KEY + "jti-1",
                Constants.PAT_TOKEN_KEY + "jti-2", Constants.PAT_LAST_USED_KEY + "jti-2"));
        verify(jdbcTemplate).update(anyString(), eq(7L));
    }

    /**
     * 用户没有活动令牌时不应触碰共享存储或数据库。
     */
    @Test
    void doesNothingWhenNoActiveToken() {
        PersonalAccessTokenRevoker revoker = new PersonalAccessTokenRevoker(jdbcTemplate, redisTemplate);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(7L))).thenReturn(List.of());

        revoker.revokeAll(7L);

        verify(redisTemplate, never()).delete(org.mockito.ArgumentMatchers.anyCollection());
        verify(jdbcTemplate, never()).update(anyString(), eq(7L));
    }

    /**
     * Redis 删除失败时必须抛出，让调用方的事务回滚。
     */
    @Test
    void failsClosedWhenSharedStorageUnavailable() {
        PersonalAccessTokenRevoker revoker = new PersonalAccessTokenRevoker(jdbcTemplate, redisTemplate);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(7L)))
                .thenReturn(List.of("jti-1"));
        when(redisTemplate.delete(org.mockito.ArgumentMatchers.anyCollection()))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThatThrownBy(() -> revoker.revokeAll(7L))
                .isInstanceOf(RedisConnectionFailureException.class);
        verify(jdbcTemplate, never()).update(anyString(), eq(7L));
    }
}
