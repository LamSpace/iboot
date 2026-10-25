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

package com.iboot.ai.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.iboot.ai.domain.knowledge.ChunkDraft;
import com.iboot.ai.domain.knowledge.DocumentStatus;
import com.iboot.ai.domain.knowledge.KnowledgeDocument;
import com.iboot.ai.infrastructure.config.RagProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link KnowledgeChunkIndexer} 的集成测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeChunkIndexerTest {

    private static final int ES_PORT = 9200;

    private static final String INDEX = "iboot-kb-chunk-test-indexer";

    @Container
    static final GenericContainer<?> ELASTICSEARCH = new GenericContainer<>(
            "docker.elastic.co/elasticsearch/elasticsearch:9.2.3")
            .withExposedPorts(ES_PORT)
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("xpack.security.enrollment.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .waitingFor(Wait.forHttp("/_cluster/health")
                    .forPort(ES_PORT)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private static Rest5Client restClient;

    private static ElasticsearchTransport transport;

    private static ElasticsearchClient client;

    private static KnowledgeChunkIndexer indexer;

    @BeforeAll
    static void setUp() {
        restClient = Rest5Client.builder(
                URI.create("http://" + ELASTICSEARCH.getHost() + ":" + ELASTICSEARCH.getMappedPort(ES_PORT)))
                .build();
        transport = new Rest5ClientTransport(restClient, new Jackson3JsonpMapper());
        client = new ElasticsearchClient(transport);
        RagProperties properties = new RagProperties();
        properties.setIndexName(INDEX);
        properties.getEmbedding().setDimensions(4);
        indexer = new KnowledgeChunkIndexer(client, properties);
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (transport != null) {
            transport.close();
        }
        if (restClient != null) {
            restClient.close();
        }
    }

    @BeforeEach
    void resetIndex() throws IOException {
        try {
            client.indices().delete(d -> d.index(INDEX));
        } catch (ElasticsearchException ignored) {
            // 首次运行时索引不存在
        }
        RagProperties properties = new RagProperties();
        properties.setIndexName(INDEX);
        properties.getEmbedding().setDimensions(4);
        new KnowledgeIndexInitializer(client, properties).ensureIndex();
    }

    /**
     * 写入、重建幂等、按文档与知识库删除。
     *
     * @throws IOException 查询失败时抛出
     */
    @Test
    void indexesIdempotentlyAndDeletes() throws IOException {
        KnowledgeDocument document = document(10L, 1L);
        indexer.index(document, List.of(
                new ChunkDraft(0, 1, "标题", "第一段", 10),
                new ChunkDraft(1, 1, "标题", "第二段", 10)),
                List.of(vector(0.1f), vector(0.2f)));
        assertThat(count()).isEqualTo(2);

        indexer.index(document, List.of(new ChunkDraft(0, 1, "标题", "重建", 10)),
                List.of(vector(0.3f)));
        assertThat(count()).isEqualTo(1);

        indexer.deleteByDoc(10L);
        assertThat(count()).isZero();

        indexer.index(document, List.of(new ChunkDraft(0, 1, null, "内容", 10)), List.of(vector(0.4f)));
        indexer.index(document(11L, 2L), List.of(new ChunkDraft(0, null, null, "另一库", 10)),
                List.of(vector(0.5f)));
        indexer.deleteByKb(2L);
        assertThat(count()).isEqualTo(1);
    }

    private long count() throws IOException {
        return client.count(c -> c.index(INDEX)).count();
    }

    private float[] vector(float value) {
        return new float[]{value, value, value, value};
    }

    private KnowledgeDocument document(long id, long kbId) {
        LocalDateTime now = LocalDateTime.now();
        return new KnowledgeDocument(id, kbId, "doc.pdf", "pdf", 10L, "application/pdf", "kb/doc.pdf",
                "hash", 100L, 2L, DocumentStatus.READY, null, null, 0, 0, null, now, now);
    }
}
