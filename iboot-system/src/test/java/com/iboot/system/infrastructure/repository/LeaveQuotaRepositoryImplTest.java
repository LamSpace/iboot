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
import com.iboot.system.domain.system.model.LeaveQuota;
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
 * 请假额度仓储测试。
 *
 * <p>在真实 MySQL 上验证懒创建、条件扣减、幂等日志与并发扣减。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class LeaveQuotaRepositoryImplTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_leave_quota")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private LeaveQuotaRepositoryImpl repository;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/iboot_leave_quota?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        migrateWithRetry(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("DELETE FROM sys_user_leave_quota");
        jdbcTemplate.update("DELETE FROM sys_leave_quota_log");
        repository = new LeaveQuotaRepositoryImpl(jdbcTemplate);
    }

    /**
     * 验证首次扣减懒创建额度行，并在额度耗尽后拒绝继续扣减。
     */
    @Test
    void createsOnFirstDeductionAndStopsAtLimit() {
        assertThat(repository.findByUserIdAndYear(1L, 2026)).isEmpty();

        repository.ensureQuota(1L, 2026, 2);
        assertThat(repository.deductOne(1L, 2026)).isEqualTo(1);
        assertThat(repository.deductOne(1L, 2026)).isEqualTo(1);
        assertThat(repository.deductOne(1L, 2026)).isZero();

        LeaveQuota quota = repository.findByUserIdAndYear(1L, 2026).orElseThrow();
        assertThat(quota.getUsedCount()).isEqualTo(2);
        assertThat(quota.remainingCount()).isZero();
    }

    /**
     * 验证同一申请 ID 只写入一次变更日志。
     */
    @Test
    void changeLogIsIdempotent() {
        assertThat(repository.insertChange("leave-1", 1L, 2026, 1, "DEDUCT")).isTrue();
        assertThat(repository.insertChange("leave-1", 1L, 2026, 1, "DEDUCT")).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_leave_quota_log", Integer.class))
                .isEqualTo(1);
    }

    /**
     * 验证并发扣减不会超卖。
     */
    @Test
    void concurrentDeductionDoesNotOversell() throws Exception {
        repository.ensureQuota(1L, 2026, 1);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = pool.submit(() -> {
                start.await();
                return repository.deductOne(1L, 2026);
            });
            Future<Integer> second = pool.submit(() -> {
                start.await();
                return repository.deductOne(1L, 2026);
            });
            start.countDown();
            assertThat(first.get() + second.get()).isEqualTo(1);
            assertThat(repository.findByUserIdAndYear(1L, 2026).orElseThrow().getUsedCount())
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
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
