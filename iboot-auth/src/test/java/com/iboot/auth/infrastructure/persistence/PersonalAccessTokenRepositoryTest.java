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

package com.iboot.auth.infrastructure.persistence;

import com.iboot.auth.domain.model.PersonalAccessTokenRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 个人访问令牌仓储测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PersonalAccessTokenRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    private PersonalAccessTokenRepository repository;

    /**
     * 插入应写入用户、名称、标识与过期时间，不写入令牌原文。
     */
    @Test
    void insertsMetadata() {
        repository = new PersonalAccessTokenRepository(jdbcTemplate);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

        repository.insert(7L, "ci", "jti-1", "user:list,job:run", expiresAt);

        verify(jdbcTemplate).update(anyString(), eq(7L), eq("ci"), eq("jti-1"),
                eq("user:list,job:run"), eq(expiresAt));
    }

    /**
     * 查询应把结果集映射为记录。
     *
     * @throws SQLException 映射失败时抛出
     */
    @Test
    @SuppressWarnings("unchecked")
    void mapsRowsToRecords() throws SQLException {
        repository = new PersonalAccessTokenRepository(jdbcTemplate);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        LocalDateTime createTime = LocalDateTime.now();
        when(resultSet.getLong("id")).thenReturn(11L);
        when(resultSet.getLong("user_id")).thenReturn(7L);
        when(resultSet.getString("name")).thenReturn("ci");
        when(resultSet.getString("token_id")).thenReturn("jti-1");
        when(resultSet.getString("scopes")).thenReturn("user:list,job:run");
        when(resultSet.getTimestamp("expires_at")).thenReturn(Timestamp.valueOf(expiresAt));
        when(resultSet.getBoolean("revoked")).thenReturn(false);
        when(resultSet.getTimestamp("revoked_at")).thenReturn(null);
        when(resultSet.getTimestamp("create_time")).thenReturn(Timestamp.valueOf(createTime));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenAnswer(invocation -> {
                    RowMapper<PersonalAccessTokenRecord> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        List<PersonalAccessTokenRecord> records = repository.findByUserId(7L);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).id()).isEqualTo(11L);
        assertThat(records.get(0).userId()).isEqualTo(7L);
        assertThat(records.get(0).name()).isEqualTo("ci");
        assertThat(records.get(0).tokenId()).isEqualTo("jti-1");
        assertThat(records.get(0).scopes()).containsExactly("user:list", "job:run");
        assertThat(records.get(0).expiresAt()).isEqualTo(expiresAt);
        assertThat(records.get(0).revoked()).isFalse();
    }

    /**
     * 查询单条记录应把用户 ID 作为条件，避免跨用户读取。
     */
    @Test
    @SuppressWarnings("unchecked")
    void scopesSingleLookupToOwner() {
        repository = new PersonalAccessTokenRepository(jdbcTemplate);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(11L), eq(7L)))
                .thenReturn(List.of());

        Optional<PersonalAccessTokenRecord> record = repository.findByIdAndUserId(11L, 7L);

        assertThat(record).isEmpty();
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(11L), eq(7L));
    }

    /**
     * 有效令牌数量统计应排除已撤销与已过期记录。
     */
    @Test
    void countsOnlyActiveTokens() {
        repository = new PersonalAccessTokenRepository(jdbcTemplate);
        LocalDateTime now = LocalDateTime.now();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(7L), eq(now))).thenReturn(2);

        assertThat(repository.countActive(7L, now)).isEqualTo(2);
    }

    /**
     * 撤销只更新属于该用户且尚未撤销的记录。
     */
    @Test
    void revokesOwnActiveToken() {
        repository = new PersonalAccessTokenRepository(jdbcTemplate);
        LocalDateTime revokedAt = LocalDateTime.now();
        when(jdbcTemplate.update(anyString(), eq(revokedAt), eq(11L), eq(7L))).thenReturn(1);

        assertThat(repository.revoke(11L, 7L, revokedAt)).isTrue();
    }
}
