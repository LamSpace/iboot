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

package com.iboot.workflow.infrastructure.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.dbinit.SchemaMigrator;
import com.iboot.workflow.application.service.notification.NotificationCommand;
import com.iboot.workflow.domain.notification.model.NotificationEventType;
import com.iboot.workflow.domain.notification.model.NotificationOutboxEvent;
import com.iboot.workflow.domain.notification.model.NotificationOutboxStatus;
import com.iboot.workflow.domain.notification.repository.NotificationOutboxRepository;
import com.iboot.workflow.infrastructure.config.MyBatisConfig;
import com.iboot.workflow.infrastructure.config.OutboxProperties;
import com.iboot.workflow.infrastructure.notification.OutboxNotificationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通知 outbox 仓储的集成测试。
 *
 * <p>使用 Testcontainers MySQL 与全量迁移，验证状态流转、退避重试、死信、
 * {@code SKIP LOCKED} 并发抢占、SENDING 租约重置与唯一键约束。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringJUnitConfig(NotificationOutboxRepositoryTest.RepositoryTestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class NotificationOutboxRepositoryTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_repository")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @Autowired
    private NotificationOutboxRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboxNotificationPort notificationPort;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanOutbox() {
        jdbcTemplate.update("DELETE FROM biz_notification_outbox");
    }

    @Test
    void enqueueRollsBackWithCallerTransaction() {
        assertThatThrownBy(() -> transactionTemplate.execute(status -> {
            notificationPort.enqueue(new NotificationCommand("evt-" + UUID.randomUUID(),
                    NotificationEventType.APPROVAL_TODO, "APPROVAL_TODO", 2L, java.util.Map.of()));
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(repository.countByStatus(NotificationOutboxStatus.PENDING)).isZero();
    }

    @Test
    void claimsPendingEventThenMarksSent() {
        NotificationOutboxEvent event = repository.save(event("evt-" + UUID.randomUUID()));
        LocalDateTime now = LocalDateTime.now();

        List<NotificationOutboxEvent> claimed = repository.claimBatch(10, now);

        assertThat(claimed).extracting(NotificationOutboxEvent::getId).containsExactly(event.getId());
        assertThat(claimed.get(0).getStatus()).isEqualTo(NotificationOutboxStatus.SENDING);
        assertThat(repository.markSent(event.getId(), "msg-1", now)).isTrue();
        assertThat(repository.countByStatus(NotificationOutboxStatus.SENT)).isEqualTo(1);
    }

    @Test
    void retriesWithBackoffAndEventuallyMarksDead() {
        NotificationOutboxEvent event = repository.save(event("evt-" + UUID.randomUUID()));
        LocalDateTime now = LocalDateTime.now();

        repository.claimBatch(10, now);
        assertThat(repository.markRetry(event.getId(), 1, now.plusSeconds(5), "boom", now)).isTrue();
        assertThat(repository.claimBatch(10, now)).isEmpty();
        assertThat(repository.claimBatch(10, now.plusSeconds(6)))
                .extracting(NotificationOutboxEvent::getId).containsExactly(event.getId());

        assertThat(repository.markDead(event.getId(), 10, "final", now)).isTrue();
        assertThat(repository.countByStatus(NotificationOutboxStatus.DEAD)).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateEventId() {
        String eventId = "evt-" + UUID.randomUUID();
        repository.save(event(eventId));

        assertThatThrownBy(() -> repository.save(event(eventId)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void concurrentClaimsDoNotOverlap() throws Exception {
        int total = 5;
        for (int i = 0; i < total; i++) {
            repository.save(event("evt-" + UUID.randomUUID()));
        }
        LocalDateTime now = LocalDateTime.now();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<List<Long>> first = executor.submit(() -> claimAfter(start, now));
            Future<List<Long>> second = executor.submit(() -> claimAfter(start, now));
            start.countDown();

            List<Long> ids = Stream.concat(first.get().stream(), second.get().stream())
                    .collect(Collectors.toList());
            assertThat(ids).doesNotHaveDuplicates();
            assertThat(ids).hasSize(total);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void resetsStaleSendingAndDeletesExpiredSent() {
        NotificationOutboxEvent event = repository.save(event("evt-" + UUID.randomUUID()));
        LocalDateTime now = LocalDateTime.now();
        repository.claimBatch(10, now);

        assertThat(repository.resetStaleSending(now.plusMinutes(1), now)).isEqualTo(1);
        assertThat(repository.claimBatch(10, now)).extracting(NotificationOutboxEvent::getId)
                .containsExactly(event.getId());

        LocalDateTime sentAt = now.minusDays(8);
        repository.markSent(event.getId(), "msg-old", sentAt);
        assertThat(repository.deleteSentBefore(now.minusDays(7))).isEqualTo(1);
        assertThat(repository.countByStatus(NotificationOutboxStatus.SENT)).isZero();
    }

    private List<Long> claimAfter(CountDownLatch start, LocalDateTime now) throws InterruptedException {
        start.await();
        return repository.claimBatch(10, now).stream().map(NotificationOutboxEvent::getId).toList();
    }

    private NotificationOutboxEvent event(String eventId) {
        NotificationOutboxEvent event = new NotificationOutboxEvent();
        event.setEventId(eventId);
        event.setEventType("APPROVAL_TODO");
        event.setPayload("{\"eventId\":\"" + eventId + "\"}");
        event.setStatus(NotificationOutboxStatus.PENDING);
        event.setAttempts(0);
        event.setMaxAttempts(10);
        event.setCreateTime(LocalDateTime.now());
        return event;
    }

    /**
     * 仓储测试上下文：数据源、Flyway、事务与 MyBatis。
     */
    @Configuration
    @EnableTransactionManagement
    @EnableConfigurationProperties(OutboxProperties.class)
    @Import({MyBatisConfig.class, NotificationOutboxRepositoryImpl.class, OutboxNotificationPort.class})
    static class RepositoryTestConfig {

        /**
         * 执行 Flyway 迁移并返回数据源。
         *
         * @return 数据源
         */
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                    + "/iboot_repository?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            dataSource.setUsername("root");
            dataSource.setPassword("root");
            migrateWithRetry(dataSource);
            return dataSource;
        }

        private static void migrateWithRetry(DataSource dataSource) {
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

        /**
         * 提供声明式事务管理器。
         *
         * @param dataSource 数据源
         * @return 事务管理器
         */
        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        /**
         * 提供测试清理用的 JdbcTemplate。
         *
         * @param dataSource 数据源
         * @return JdbcTemplate
         */
        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        /**
         * 提供序列化器。
         *
         * @return ObjectMapper
         */
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        /**
         * 提供测试用事务模板。
         *
         * @param transactionManager 事务管理器
         * @return 事务模板
         */
        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }
    }
}
