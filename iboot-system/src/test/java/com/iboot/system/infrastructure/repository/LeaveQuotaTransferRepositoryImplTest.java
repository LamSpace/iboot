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

import com.iboot.dbinit.SchemaMigrator;
import com.iboot.system.domain.system.model.LeaveQuotaTransfer;
import com.iboot.system.domain.system.model.LeaveQuotaTransferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 额度划转 TCC 仓储测试。
 *
 * <p>在真实 MySQL 上验证冻结、结算、释放、唯一键幂等、终态迁移与并发冻结。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class LeaveQuotaTransferRepositoryImplTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_leave_transfer")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private LeaveQuotaTransferRepositoryImpl repository;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/iboot_leave_transfer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        migrateWithRetry(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("DELETE FROM sys_leave_quota_transfer");
        jdbcTemplate.update("DELETE FROM sys_user_leave_quota");
        repository = new LeaveQuotaTransferRepositoryImpl(jdbcTemplate);
    }

    /**
     * 验证冻结、结算与释放的额度变化。
     */
    @Test
    void freezesSettlesAndReleases() {
        repository.ensureQuota(1L, 2026, 10);
        repository.ensureQuota(2L, 2026, 10);

        assertThat(repository.freeze(1L, 2026, 3)).isEqualTo(1);
        assertThat(repository.freeze(1L, 2026, 8)).isZero();

        assertThat(repository.settleFrozen(1L, 2026, 3)).isEqualTo(1);
        assertThat(repository.increaseTotal(2L, 2026, 3)).isEqualTo(1);

        assertThat(total(1L)).isEqualTo(7);
        assertThat(frozen(1L)).isZero();
        assertThat(total(2L)).isEqualTo(13);
    }

    /**
     * 验证释放只扣减冻结，不改变总额度。
     */
    @Test
    void releaseKeepsTotal() {
        repository.ensureQuota(1L, 2026, 10);
        repository.freeze(1L, 2026, 4);

        assertThat(repository.releaseFrozen(1L, 2026, 4)).isEqualTo(1);
        assertThat(repository.releaseFrozen(1L, 2026, 1)).isZero();
        assertThat(total(1L)).isEqualTo(10);
        assertThat(frozen(1L)).isZero();
    }

    /**
     * 验证划转标识与 XID 的唯一键保证 Try 幂等。
     */
    @Test
    void insertTryingIsIdempotentByBothKeys() {
        LeaveQuotaTransfer first = transfer("t-1", "xid-1");
        assertThat(repository.insertTrying(first)).isTrue();
        assertThat(repository.insertTrying(transfer("t-1", "xid-2"))).isFalse();
        assertThat(repository.insertTrying(transfer("t-2", "xid-1"))).isFalse();
        assertThat(repository.findByXid("xid-1")).isPresent();
        assertThat(repository.findByTransferId("t-1")).isPresent();
    }

    /**
     * 验证只有 TRYING 可以迁移到终态，重复迁移不再生效。
     */
    @Test
    void markTerminalOnlyFromTrying() {
        repository.insertTrying(transfer("t-1", "xid-1"));

        assertThat(repository.markTerminal("xid-1", LeaveQuotaTransferStatus.CONFIRMED)).isEqualTo(1);
        assertThat(repository.markTerminal("xid-1", LeaveQuotaTransferStatus.CANCELLED)).isZero();
        assertThat(repository.findByXid("xid-1").orElseThrow().status())
                .isEqualTo(LeaveQuotaTransferStatus.CONFIRMED);
    }

    /**
     * 验证同一划出方的并发冻结不会超过剩余额度。
     */
    @Test
    void concurrentFreezeDoesNotOversell() throws Exception {
        repository.ensureQuota(1L, 2026, 1);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = pool.submit(() -> {
                start.await();
                return repository.freeze(1L, 2026, 1);
            });
            Future<Integer> second = pool.submit(() -> {
                start.await();
                return repository.freeze(1L, 2026, 1);
            });
            start.countDown();
            assertThat(first.get() + second.get()).isEqualTo(1);
            assertThat(frozen(1L)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private LeaveQuotaTransfer transfer(final String transferId, final String xid) {
        return new LeaveQuotaTransfer(transferId, xid, 1L, 2L, 2026, 1, LeaveQuotaTransferStatus.TRYING);
    }

    private int total(final Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT total_count FROM sys_user_leave_quota WHERE user_id = ? AND `year` = 2026",
                Integer.class, userId);
    }

    private int frozen(final Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT frozen_count FROM sys_user_leave_quota WHERE user_id = ? AND `year` = 2026",
                Integer.class, userId);
    }

    private void migrateWithRetry(final DriverManagerDataSource dataSource) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                SchemaMigrator.migrate(dataSource);
                return;
            } catch (RuntimeException e) {
                last = e;
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw last;
    }
}
