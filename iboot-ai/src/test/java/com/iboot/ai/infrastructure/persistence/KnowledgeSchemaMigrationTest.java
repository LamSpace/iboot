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
 * 知识库 schema 迁移测试。
 *
 * <p>使用 Testcontainers MySQL 8.0.45 验证四张表、索引与内容哈希唯一约束。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeSchemaMigrationTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_ai_test")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void migrate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/iboot_ai_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        migrateWithRetry(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private void migrateWithRetry(DriverManagerDataSource dataSource) {
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
     * 四张表与关键索引存在。
     */
    @Test
    void createsKnowledgeTablesAndIndexes() {
        assertThat(tableExists("ai_knowledge_base")).isTrue();
        assertThat(tableExists("ai_kb_share_dept")).isTrue();
        assertThat(tableExists("ai_document")).isTrue();
        assertThat(tableExists("ai_document_chunk")).isTrue();
        assertThat(indexExists("ai_document", "uk_doc_hash")).isTrue();
        assertThat(indexExists("ai_document", "idx_doc_claim")).isTrue();
        assertThat(indexExists("ai_document_chunk", "uk_chunk")).isTrue();
    }

    /**
     * 同一知识库内容哈希唯一，删除后可重新插入。
     */
    @Test
    void enforcesContentHashUniqueness() {
        insertKnowledgeBase(10L);
        insertDocument(20L, 10L, "a".repeat(64));
        assertThatThrownBy(() -> insertDocument(21L, 10L, "a".repeat(64)))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);

        jdbcTemplate.update("UPDATE ai_document SET deleted = id WHERE id = 20");
        insertDocument(21L, 10L, "a".repeat(64));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_document", Integer.class)).isEqualTo(2);
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

    private void insertKnowledgeBase(Long id) {
        jdbcTemplate.update("""
                INSERT INTO ai_knowledge_base (id, name, visibility, owner_user_id, owner_dept_id, created_by)
                VALUES (?, 'test-kb', 1, 1, 1, 'test')
                """, id);
    }

    private void insertDocument(Long id, Long kbId, String hash) {
        jdbcTemplate.update("""
                INSERT INTO ai_document (id, kb_id, file_name, file_ext, file_size, mime_type, storage_key,
                                         content_hash, owner_user_id, owner_dept_id, status)
                VALUES (?, ?, 'test.pdf', 'pdf', 1, 'application/pdf', 'kb/test.pdf', ?, 1, 1, 'PENDING')
                """, id, kbId, hash);
    }
}
