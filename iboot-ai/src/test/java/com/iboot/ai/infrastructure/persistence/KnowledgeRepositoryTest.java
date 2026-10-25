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

import com.iboot.ai.domain.knowledge.DocumentChunk;
import com.iboot.ai.domain.knowledge.DocumentStatus;
import com.iboot.ai.domain.knowledge.KnowledgeBase;
import com.iboot.ai.domain.knowledge.KnowledgeDocument;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link KnowledgeRepository} 的集成测试。
 *
 * <p>使用 Testcontainers MySQL 8.0.45 与真实 Flyway 迁移，覆盖知识库、文档状态机、分块与级联删除。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeRepositoryTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_ai_repo")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private KnowledgeRepository repository;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/iboot_ai_repo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        migrateWithRetry(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM ai_document_chunk");
        jdbc.update("DELETE FROM ai_document");
        jdbc.update("DELETE FROM ai_kb_share_dept");
        jdbc.update("DELETE FROM ai_knowledge_base");
        repository = new KnowledgeRepository(new NamedParameterJdbcTemplate(dataSource), jdbc);
    }

    private void migrateWithRetry(DriverManagerDataSource dataSource) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
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
     * 知识库与共享部门可写入并读回。
     */
    @Test
    void insertsAndReadsKnowledgeBase() {
        long id = repository.insertKnowledgeBase("kb", "desc", 4, 100L, 2L, "tester", List.of(2L, 3L));

        KnowledgeBase base = repository.findKnowledgeBase(id).orElseThrow();
        assertThat(base.name()).isEqualTo("kb");
        assertThat(base.visibility()).isEqualTo(4);
        assertThat(base.sharedDeptIds()).containsExactly(2L, 3L);
        assertThat(repository.findKnowledgeBaseSummaries()).hasSize(1);
    }

    /**
     * 文档状态机与认领查询可用。
     */
    @Test
    void managesDocumentLifecycle() {
        long kbId = repository.insertKnowledgeBase("kb", null, 1, 100L, 2L, "tester", List.of());
        long docId = repository.insertDocument(kbId, "a.pdf", "pdf", 10L, "application/pdf", "kb/a.pdf",
                "a".repeat(64), 100L, 2L);

        KnowledgeDocument pending = repository.findDocument(docId).orElseThrow();
        assertThat(pending.status()).isEqualTo(DocumentStatus.PENDING);
        assertThat(repository.findActiveDocumentByHash(kbId, "a".repeat(64))).isPresent();

        LocalDateTime now = LocalDateTime.now();
        assertThat(repository.findClaimable(now, now.minusMinutes(15), 3, 1)).hasSize(1);
        assertThat(repository.markProcessing(docId, now)).isTrue();
        repository.markReady(docId, 3);

        KnowledgeDocument ready = repository.findDocument(docId).orElseThrow();
        assertThat(ready.status()).isEqualTo(DocumentStatus.READY);
        assertThat(ready.chunkCount()).isEqualTo(3);
    }

    /**
     * 处理中超时与失败待重试的文档可被重新认领。
     */
    @Test
    void claimsStaleAndRetryableDocuments() {
        long kbId = repository.insertKnowledgeBase("kb", null, 1, 100L, 2L, "tester", List.of());
        long docId = repository.insertDocument(kbId, "a.pdf", "pdf", 10L, "application/pdf", "kb/a.pdf",
                "d".repeat(64), 100L, 2L);
        LocalDateTime now = LocalDateTime.now();
        assertThat(repository.markProcessing(docId, now)).isTrue();

        jdbc.update("UPDATE ai_document SET update_time = ? WHERE id = ?", now.minusMinutes(30), docId);
        assertThat(repository.findClaimable(now, now.minusMinutes(15), 3, 1)).hasSize(1);

        repository.markFailed(docId, "OCR_FAILED", "重试", now.minusMinutes(1));
        assertThat(repository.findClaimable(now, now.minusMinutes(15), 3, 1)).hasSize(1);

        repository.markFailed(docId, "OCR_FAILED", "等待", now.plusMinutes(5));
        assertThat(repository.findClaimable(now, now.minusMinutes(15), 3, 1)).isEmpty();

        repository.markFailed(docId, "OCR_FAILED", "耗尽", now.minusMinutes(1));
        jdbc.update("UPDATE ai_document SET attempts = 3 WHERE id = ?", docId);
        assertThat(repository.findClaimable(now, now.minusMinutes(15), 3, 1)).isEmpty();
    }

    /**
     * 分块保存是幂等的，删除文档会清理分块。
     */
    @Test
    void savesChunksAndCascadesOnDelete() {
        long kbId = repository.insertKnowledgeBase("kb", null, 1, 100L, 2L, "tester", List.of());
        long docId = repository.insertDocument(kbId, "a.pdf", "pdf", 10L, "application/pdf", "kb/a.pdf",
                "b".repeat(64), 100L, 2L);
        repository.saveChunks(docId, List.of(
                new DocumentChunk(null, docId, 0, 1, "标题", "第一段", 10),
                new DocumentChunk(null, docId, 1, 1, "标题", "第二段", 10)));
        repository.saveChunks(docId, List.of(new DocumentChunk(null, docId, 0, 1, "标题", "覆盖", 10)));

        assertThat(repository.findChunks(docId)).hasSize(1);
        assertThat(repository.findChunks(docId).getFirst().content()).isEqualTo("覆盖");

        repository.softDeleteDocument(docId);
        assertThat(repository.findDocument(docId)).isEmpty();
        assertThat(repository.findChunks(docId)).isEmpty();
    }

    /**
     * 删除知识库级联清理文档与共享部门。
     */
    @Test
    void softDeleteKnowledgeBaseCascades() {
        long kbId = repository.insertKnowledgeBase("kb", null, 2, 100L, 2L, "tester", List.of(2L));
        long docId = repository.insertDocument(kbId, "a.pdf", "pdf", 10L, "application/pdf", "kb/a.pdf",
                "c".repeat(64), 100L, 2L);
        repository.saveChunks(docId, List.of(new DocumentChunk(null, docId, 0, 1, null, "内容", 10)));

        repository.softDeleteKnowledgeBase(kbId);

        assertThat(repository.findKnowledgeBase(kbId)).isEmpty();
        assertThat(repository.findDocumentsByKb(kbId)).isEmpty();
        assertThat(repository.findChunks(docId)).isEmpty();
    }
}
