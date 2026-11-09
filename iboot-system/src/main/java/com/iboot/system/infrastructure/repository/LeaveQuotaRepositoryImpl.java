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

package com.iboot.system.infrastructure.repository;

import com.iboot.system.domain.system.model.LeaveQuota;
import com.iboot.system.domain.system.repository.LeaveQuotaRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 请假额度仓储实现。
 *
 * <p>使用条件更新保证并发扣减不超卖，使用唯一键保证同一申请只扣减一次。
 * 这些语句与调用方处于同一个事务（或同一个 Seata 全局事务分支）中。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Repository
public class LeaveQuotaRepositoryImpl implements LeaveQuotaRepository {

    private static final String SELECT_QUOTA = """
            SELECT user_id, `year`, total_count, used_count, frozen_count
              FROM sys_user_leave_quota
             WHERE user_id = ? AND `year` = ?
            """;

    private static final String ENSURE_QUOTA = """
            INSERT INTO sys_user_leave_quota (user_id, `year`, total_count)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE user_id = user_id
            """;

    private static final String DEDUCT_ONE = """
            UPDATE sys_user_leave_quota
               SET used_count = used_count + 1
             WHERE user_id = ? AND `year` = ?
               AND total_count - used_count - frozen_count >= 1
            """;

    private static final String INSERT_CHANGE = """
            INSERT INTO sys_leave_quota_log (request_id, user_id, `year`, delta, type)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final RowMapper<LeaveQuota> ROW_MAPPER = (resultSet, rowNum) -> new LeaveQuota(
            resultSet.getLong("user_id"),
            resultSet.getInt("year"),
            resultSet.getInt("total_count"),
            resultSet.getInt("used_count"),
            resultSet.getInt("frozen_count"));

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建仓储。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public LeaveQuotaRepositoryImpl(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<LeaveQuota> findByUserIdAndYear(final Long userId, final int year) {
        return jdbcTemplate.query(SELECT_QUOTA, ROW_MAPPER, userId, year).stream().findFirst();
    }

    @Override
    public void ensureQuota(final Long userId, final int year, final int totalCount) {
        jdbcTemplate.update(ENSURE_QUOTA, userId, year, totalCount);
    }

    @Override
    public int deductOne(final Long userId, final int year) {
        return jdbcTemplate.update(DEDUCT_ONE, userId, year);
    }

    @Override
    public boolean insertChange(final String requestId, final Long userId, final int year,
                                final int delta, final String type) {
        try {
            return jdbcTemplate.update(INSERT_CHANGE, requestId, userId, year, delta, type) > 0;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
