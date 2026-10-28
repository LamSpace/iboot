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

package com.iboot.ai.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 智能体 schema 迁移测试。
 *
 * <p>使用 Testcontainers MySQL 8.0.45 验证智能体两张表、关键索引、名称唯一约束，
 * 以及在同一 schema 上重复执行迁移的幂等性。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class AgentSchemaMigrationTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_ai_agent_test")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private DriverManagerDataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void migrate() {
        dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/iboot_ai_agent_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        migrateWithRetry();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private void migrateWithRetry() {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .load()
                        .migrate();
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
     * 两张表与关键索引存在。
     */
    @Test
    void createsAgentTablesAndIndexes() {
        assertThat(tableExists("ai_agent")).isTrue();
        assertThat(tableExists("ai_agent_run")).isTrue();
        assertThat(indexExists("ai_agent", "uk_agent_name")).isTrue();
        assertThat(indexExists("ai_agent_run", "idx_run_user_time")).isTrue();
        assertThat(indexExists("ai_agent_run", "idx_run_pending")).isTrue();
    }

    /**
     * 同一名称的未删除智能体唯一，逻辑删除后可重名。
     */
    @Test
    void enforcesAgentNameUniqueness() {
        insertAgent(1L, "ops-helper");
        assertThatThrownBy(() -> insertAgent(2L, "ops-helper"))
                .isInstanceOf(DuplicateKeyException.class);

        jdbcTemplate.update("UPDATE ai_agent SET deleted = id WHERE id = 1");
        insertAgent(2L, "ops-helper");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_agent", Integer.class)).isEqualTo(2);
    }

    /**
     * 重复执行迁移不报错，且版本停留在最新。
     */
    @Test
    void repeatedMigrationIsIdempotent() {
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class)).isEqualTo(2);
    }

    private boolean tableExists(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table) > 0;
    }

    private boolean indexExists(String table, String index) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE()"
                        + " AND table_name = ? AND index_name = ?",
                Integer.class, table, index) > 0;
    }

    private void insertAgent(Long id, String name) {
        jdbcTemplate.update("""
                INSERT INTO ai_agent (id, name, system_prompt, tool_allowlist_json, created_by)
                VALUES (?, ?, '你是运维助手', '["query_users"]', 'test')
                """, id, name);
    }
}
