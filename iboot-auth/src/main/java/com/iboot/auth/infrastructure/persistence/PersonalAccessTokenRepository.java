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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 个人访问令牌元数据仓储。
 *
 * <p>读写系统域登记的 {@code sys_user_pat} 表，只保存令牌标识与生命周期信息，
 * 不保存令牌原文。认证服务只在签发与撤销时写入该表，其余为只读查询。
 *
 * <p>退出条件：{@code iboot-system} 拆分后，本仓储改为调用系统服务的内部接口，
 * 认证服务不再直连该表。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Repository
public class PersonalAccessTokenRepository {

    private static final String SELECT_COLUMNS =
            "SELECT id, user_id, name, token_id, expires_at, revoked, revoked_at, create_time FROM sys_user_pat ";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建仓储。
     *
     * @param jdbcTemplate 共享库访问模板
     */
    public PersonalAccessTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 插入一条令牌元数据。
     *
     * @param userId 所属用户 ID
     * @param name 令牌名称
     * @param tokenId 令牌唯一标识
     * @param expiresAt 过期时间
     */
    public void insert(Long userId, String name, String tokenId, LocalDateTime expiresAt) {
        jdbcTemplate.update(
                "INSERT INTO sys_user_pat (user_id, name, token_id, expires_at, revoked, create_time) "
                        + "VALUES (?, ?, ?, ?, 0, NOW())",
                userId, name, tokenId, expiresAt);
    }

    /**
     * 按令牌标识查询元数据。
     *
     * @param tokenId 令牌唯一标识
     * @return 令牌记录；不存在时返回空
     */
    public Optional<PersonalAccessTokenRecord> findByTokenId(String tokenId) {
        return jdbcTemplate.query(SELECT_COLUMNS + "WHERE token_id = ?",
                        (rs, rowNum) -> map(rs), tokenId)
                .stream()
                .findFirst();
    }

    /**
     * 查询用户全部令牌元数据，按创建时间倒序。
     *
     * @param userId 用户 ID
     * @return 令牌记录列表
     */
    public List<PersonalAccessTokenRecord> findByUserId(Long userId) {
        return jdbcTemplate.query(SELECT_COLUMNS + "WHERE user_id = ? ORDER BY create_time DESC, id DESC",
                (rs, rowNum) -> map(rs), userId);
    }

    /**
     * 按记录 ID 与用户 ID 查询令牌，避免跨用户访问。
     *
     * @param id 记录 ID
     * @param userId 用户 ID
     * @return 令牌记录；不存在时返回空
     */
    public Optional<PersonalAccessTokenRecord> findByIdAndUserId(Long id, Long userId) {
        return jdbcTemplate.query(SELECT_COLUMNS + "WHERE id = ? AND user_id = ?",
                        (rs, rowNum) -> map(rs), id, userId)
                .stream()
                .findFirst();
    }

    /**
     * 统计用户当前有效的令牌数量。
     *
     * @param userId 用户 ID
     * @param now 当前时间，用于排除已过期记录
     * @return 有效令牌数量
     */
    public int countActive(Long userId, LocalDateTime now) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_pat WHERE user_id = ? AND revoked = 0 AND expires_at > ?",
                Integer.class, userId, now);
        return count == null ? 0 : count;
    }

    /**
     * 撤销一条尚未撤销的令牌。
     *
     * @param id 记录 ID
     * @param userId 用户 ID
     * @param revokedAt 撤销时间
     * @return 记录存在且本次被撤销时返回 {@code true}
     */
    public boolean revoke(Long id, Long userId, LocalDateTime revokedAt) {
        return jdbcTemplate.update(
                "UPDATE sys_user_pat SET revoked = 1, revoked_at = ? "
                        + "WHERE id = ? AND user_id = ? AND revoked = 0",
                revokedAt, id, userId) > 0;
    }

    /**
     * 把结果集映射为令牌记录。
     *
     * @param rs 结果集
     * @return 令牌记录
     * @throws SQLException 读取列失败时抛出
     */
    private PersonalAccessTokenRecord map(ResultSet rs) throws SQLException {
        return new PersonalAccessTokenRecord(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("name"),
                rs.getString("token_id"),
                rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getBoolean("revoked"),
                rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toLocalDateTime(),
                rs.getTimestamp("create_time").toLocalDateTime());
    }
}
