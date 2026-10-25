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

package com.iboot.ai.application.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.iboot.ai.domain.knowledge.KnowledgeBaseSummary;
import com.iboot.ai.infrastructure.config.RagProperties;
import com.iboot.ai.infrastructure.persistence.KnowledgeRepository;
import com.iboot.ai.infrastructure.search.KnowledgeChunkDocument;
import com.iboot.ai.infrastructure.search.KnowledgeIndexInitializer;
import com.iboot.ai.infrastructure.security.PermissionScope;
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
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeRetrievalService} 的集成测试。
 *
 * <p>使用真实 Elasticsearch 验证 BM25 + kNN + RRF，并用五级数据权限构造红队场景，
 * 确认越权文档不会出现在任一检索路径或结果中。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeRetrievalServiceTest {

    private static final int ES_PORT = 9200;

    private static final String INDEX = "iboot-kb-chunk-test-retrieval";

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

    private ChunkEmbedder embedder;

    private KnowledgeRetrievalService service;

    private RagProperties properties;

    private KnowledgeRepository repository;

    @BeforeAll
    static void setUpClient() {
        restClient = Rest5Client.builder(
                URI.create("http://" + ELASTICSEARCH.getHost() + ":" + ELASTICSEARCH.getMappedPort(ES_PORT)))
                .build();
        transport = new Rest5ClientTransport(restClient, new Jackson3JsonpMapper());
        client = new ElasticsearchClient(transport);
    }

    @AfterAll
    static void tearDownClient() throws IOException {
        if (transport != null) {
            transport.close();
        }
        if (restClient != null) {
            restClient.close();
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        properties = new RagProperties();
        properties.setIndexName(INDEX);
        properties.getEmbedding().setDimensions(4);
        properties.setTopK(5);
        properties.setCandidateK(20);
        properties.setNumCandidates(100);
        properties.setRankConstant(60);
        properties.setRankWindowSize(50);
        properties.setMaxChunksPerDoc(2);
        try {
            client.indices().delete(d -> d.index(INDEX));
        } catch (ElasticsearchException ignored) {
            // 首次运行时索引不存在
        }
        new KnowledgeIndexInitializer(client, properties).ensureIndex();

        embedder = mock(ChunkEmbedder.class);
        when(embedder.embed(List.of("年假"))).thenReturn(List.of(new float[]{1f, 0f, 0f, 0f}));
        repository = mock(KnowledgeRepository.class);
        when(repository.findKnowledgeBaseSummaries()).thenReturn(List.of(
                new KnowledgeBaseSummary(1L, 1, 0L, 0L, List.of()),
                new KnowledgeBaseSummary(2L, 5, 103L, 4L, List.of())));
        service = new KnowledgeRetrievalService(client, properties, embedder, new KnowledgeAccessService(),
                repository);

        index(
                chunk("10:0", 1L, 10L, "年假政策", "员工年假政策：入职满一年享受五天年假", 100L, 2L, 1f),
                chunk("11:0", 1L, 11L, "年假政策", "财务部年假政策：年假申请需提前一周", 101L, 3L, 1f),
                chunk("12:0", 1L, 12L, "报销流程", "差旅报销流程", 102L, 2L, 0f),
                chunk("20:0", 2L, 20L, "人事制度", "年假与调休", 103L, 4L, 1f));
    }

    /**
     * 仅本人数据权限只能召回自己的文档。
     */
    @Test
    void selfScopeOnlySeesOwnDocuments() {
        RetrievalResult result = service.search("年假", null,
                scope(5, 2L, 100L, List.of(), List.of(2L), false));

        assertThat(result.chunks()).extracting(RetrievedChunk::docId).containsExactly(10L);
    }

    /**
     * 本部门只召回本部门文档。
     */
    @Test
    void deptScopeOnlySeesOwnDept() {
        RetrievalResult result = service.search("年假", null,
                scope(3, 2L, 100L, List.of(2L), List.of(2L), false));

        assertThat(result.chunks()).extracting(RetrievedChunk::docId)
                .contains(10L)
                .doesNotContain(11L);
        assertThat(result.chunks()).allSatisfy(chunk ->
                assertThat(chunk.docId()).isIn(10L, 12L));
    }

    /**
     * 自定义部门只召回授权部门文档。
     */
    @Test
    void customScopeOnlySeesAuthorizedDepts() {
        RetrievalResult result = service.search("年假", null,
                scope(2, 3L, 100L, List.of(2L), List.of(2L), false));

        assertThat(result.chunks()).extracting(RetrievedChunk::docId).doesNotContain(11L);
        assertThat(result.chunks()).allSatisfy(chunk ->
                assertThat(chunk.docId()).isIn(10L, 12L));
    }

    /**
     * 全部数据权限也不能看到未共享的仅本人知识库。
     */
    @Test
    void allScopeStillRespectsKnowledgeBaseVisibility() {
        RetrievalResult result = service.search("年假", null,
                scope(1, 2L, 100L, List.of(), List.of(2L), false));

        assertThat(result.chunks()).extracting(RetrievedChunk::docId).doesNotContain(20L);
    }

    /**
     * 显式选择不可见知识库返回 403。
     */
    @Test
    void rejectsInvisibleKnowledgeBase() {
        assertThatThrownBy(() -> service.search("年假", 2L,
                scope(1, 2L, 100L, List.of(), List.of(2L), false)))
                .isInstanceOf(KnowledgeAccessDeniedException.class);
    }

    /**
     * 自定义部门为空时拒绝全部。
     */
    @Test
    void emptyCustomDeptsDeniesAll() {
        RetrievalResult result = service.search("年假", null,
                scope(2, 3L, 100L, List.of(), List.of(), false));

        assertThat(result.chunks()).isEmpty();
    }

    /**
     * Embedding 不可用时降级为 BM25 并标记。
     */
    @Test
    void degradesToBm25WhenEmbeddingFails() {
        when(embedder.embed(List.of("年假"))).thenThrow(new EmbeddingException("EMBEDDING_FAILED", "down"));

        RetrievalResult result = service.search("年假", null,
                scope(3, 2L, 100L, List.of(2L), List.of(2L), false));

        assertThat(result.degraded()).isTrue();
        assertThat(result.mode()).isEqualTo("bm25");
        assertThat(result.chunks()).extracting(RetrievedChunk::docId).contains(10L).doesNotContain(11L);
    }

    /**
     * Elasticsearch 不可用时返回 503 语义。
     */
    @Test
    void failsWhenElasticsearchUnavailable() throws Exception {
        when(embedder.embed(List.of("年假"))).thenReturn(List.of(new float[]{1f, 0f, 0f, 0f}));
        Rest5Client badRestClient = Rest5Client.builder(URI.create("http://127.0.0.1:1")).build();
        ElasticsearchTransport badTransport = new Rest5ClientTransport(badRestClient, new Jackson3JsonpMapper());
        try {
            KnowledgeRetrievalService badService = new KnowledgeRetrievalService(
                    new ElasticsearchClient(badTransport), properties, embedder, new KnowledgeAccessService(),
                    repository);

            assertThatThrownBy(() -> badService.search("年假", null,
                    scope(3, 2L, 100L, List.of(2L), List.of(2L), false)))
                    .isInstanceOf(RetrievalUnavailableException.class);
        } finally {
            badTransport.close();
            badRestClient.close();
        }
    }

    private void index(KnowledgeChunkDocument... documents) throws IOException {
        client.bulk(b -> b.operations(Arrays.stream(documents)
                        .map(document -> BulkOperation.of(op -> op.index(i -> i.index(INDEX)
                                .id(document.chunkId()).document(document))))
                        .toList())
                .refresh(Refresh.WaitFor));
    }

    private KnowledgeChunkDocument chunk(String chunkId, long kbId, long docId, String title, String content,
                                          long ownerUserId, long ownerDeptId, float firstDimension) {
        return new KnowledgeChunkDocument(chunkId, kbId, docId, title, null, content,
                new float[]{firstDimension, 1f - firstDimension, 0f, 0f}, 1, 0, ownerUserId, ownerDeptId,
                "2026-10-25T00:00:00Z");
    }

    private PermissionScope scope(int dataScope, long deptId, long userId, List<Long> visibleDeptIds,
                                  List<Long> kbVisibleDeptIds, boolean superAdmin) {
        return new PermissionScope(userId, "test", superAdmin, dataScope, deptId, visibleDeptIds,
                kbVisibleDeptIds, List.of("ai:kb:query"), List.of());
    }
}
