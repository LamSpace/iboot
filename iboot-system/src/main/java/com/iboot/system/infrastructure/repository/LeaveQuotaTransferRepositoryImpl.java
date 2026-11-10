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

import com.iboot.system.domain.system.model.LeaveQuotaTransfer;
import com.iboot.system.domain.system.model.LeaveQuotaTransferStatus;
import com.iboot.system.domain.system.repository.LeaveQuotaTransferRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 额度划转 TCC 仓储实现。
 *
 * <p>使用 TCC 专用 JDBC 模板，连接来自未被 Seata AT 自动代理的数据源；所有额度
 * 调整都是带条件的原子更新。唯一键冲突表示重复 Try 或并发墓碑，按幂等处理。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see com.iboot.system.infrastructure.config.TccDataSourceConfig
 */
@Repository
public class LeaveQuotaTransferRepositoryImpl implements LeaveQuotaTransferRepository {

    private static final String SELECT_BY_XID = """
            SELECT transfer_id, xid, from_user_id, to_user_id, `year`, `count`, status
              FROM sys_leave_quota_transfer
             WHERE xid = ?
            """;

    private static final String SELECT_BY_TRANSFER_ID = """
            SELECT transfer_id, xid, from_user_id, to_user_id, `year`, `count`, status
              FROM sys_leave_quota_transfer
             WHERE transfer_id = ?
            """;

    private static final String INSERT_TRANSFER = """
            INSERT INTO sys_leave_quota_transfer
                (transfer_id, xid, from_user_id, to_user_id, `year`, `count`, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String MARK_TERMINAL = """
            UPDATE sys_leave_quota_transfer
               SET status = ?
             WHERE xid = ? AND status = 'TRYING'
            """;

    private static final String ENSURE_QUOTA = """
            INSERT INTO sys_user_leave_quota (user_id, `year`, total_count)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE user_id = user_id
            """;

    private static final String FREEZE = """
            UPDATE sys_user_leave_quota
               SET frozen_count = frozen_count + ?
             WHERE user_id = ? AND `year` = ?
               AND total_count - used_count - frozen_count >= ?
            """;

    private static final String RELEASE_FROZEN = """
            UPDATE sys_user_leave_quota
               SET frozen_count = frozen_count - ?
             WHERE user_id = ? AND `year` = ? AND frozen_count >= ?
            """;

    private static final String SETTLE_FROZEN = """
            UPDATE sys_user_leave_quota
               SET frozen_count = frozen_count - ?, total_count = total_count - ?
             WHERE user_id = ? AND `year` = ? AND frozen_count >= ? AND total_count >= ?
            """;

    private static final String INCREASE_TOTAL = """
            UPDATE sys_user_leave_quota
               SET total_count = total_count + ?
             WHERE user_id = ? AND `year` = ?
            """;

    private static final RowMapper<LeaveQuotaTransfer> ROW_MAPPER = (resultSet, rowNum) ->
            new LeaveQuotaTransfer(
                    resultSet.getString("transfer_id"),
                    resultSet.getString("xid"),
                    resultSet.getLong("from_user_id"),
                    resultSet.getLong("to_user_id"),
                    resultSet.getInt("year"),
                    resultSet.getInt("count"),
                    LeaveQuotaTransferStatus.valueOf(resultSet.getString("status")));

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建仓储。
     *
     * @param jdbcTemplate TCC 专用 JDBC 模板
     */
    public LeaveQuotaTransferRepositoryImpl(@Qualifier("tccJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<LeaveQuotaTransfer> findByXid(final String xid) {
        return jdbcTemplate.query(SELECT_BY_XID, ROW_MAPPER, xid).stream().findFirst();
    }

    @Override
    public Optional<LeaveQuotaTransfer> findByTransferId(final String transferId) {
        return jdbcTemplate.query(SELECT_BY_TRANSFER_ID, ROW_MAPPER, transferId).stream().findFirst();
    }

    @Override
    public boolean insertTrying(final LeaveQuotaTransfer transfer) {
        return insert(transfer, LeaveQuotaTransferStatus.TRYING);
    }

    @Override
    public boolean insertTombstone(final LeaveQuotaTransfer transfer) {
        return insert(transfer, LeaveQuotaTransferStatus.CANCELLED);
    }

    @Override
    public int markTerminal(final String xid, final LeaveQuotaTransferStatus target) {
        return jdbcTemplate.update(MARK_TERMINAL, target.name(), xid);
    }

    @Override
    public void ensureQuota(final Long userId, final int year, final int totalCount) {
        jdbcTemplate.update(ENSURE_QUOTA, userId, year, totalCount);
    }

    @Override
    public int freeze(final Long userId, final int year, final int count) {
        return jdbcTemplate.update(FREEZE, count, userId, year, count);
    }

    @Override
    public int releaseFrozen(final Long userId, final int year, final int count) {
        return jdbcTemplate.update(RELEASE_FROZEN, count, userId, year, count);
    }

    @Override
    public int settleFrozen(final Long userId, final int year, final int count) {
        return jdbcTemplate.update(SETTLE_FROZEN, count, count, userId, year, count, count);
    }

    @Override
    public int increaseTotal(final Long userId, final int year, final int count) {
        return jdbcTemplate.update(INCREASE_TOTAL, count, userId, year);
    }

    private boolean insert(final LeaveQuotaTransfer transfer, final LeaveQuotaTransferStatus status) {
        try {
            return jdbcTemplate.update(INSERT_TRANSFER,
                    transfer.transferId(), transfer.xid(), transfer.fromUserId(), transfer.toUserId(),
                    transfer.year(), transfer.count(), status.name()) > 0;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
