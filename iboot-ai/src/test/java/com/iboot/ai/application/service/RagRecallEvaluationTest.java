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
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.iboot.ai.infrastructure.config.RagProperties;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import com.iboot.ai.infrastructure.ocr.OcrQuotaService;
import com.iboot.ai.infrastructure.ocr.PdfPageRenderer;
import com.iboot.ai.infrastructure.parsing.DocxParser;
import com.iboot.ai.infrastructure.parsing.DocumentParserRegistry;
import com.iboot.ai.infrastructure.parsing.PlainTextParser;
import com.iboot.ai.infrastructure.parsing.TextPdfParser;
import com.iboot.ai.infrastructure.persistence.KnowledgeRepository;
import com.iboot.ai.infrastructure.search.KnowledgeChunkIndexer;
import com.iboot.ai.infrastructure.search.KnowledgeIndexInitializer;
import com.iboot.ai.infrastructure.security.PermissionScope;
import com.iboot.ai.support.FakeOcrParser;
import com.iboot.ai.support.InMemoryKnowledgeStorage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 50 篇合成语料的 Top-5 召回评测。
 *
 * <p>只在 {@code cloud} 分组运行，使用真实百炼 Embedding；默认 {@code surefire.excludedGroups=cloud}
 * 会排除本测试，避免日常测试产生费用。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Tag("cloud")
@Testcontainers(disabledWithoutDocker = true)
class RagRecallEvaluationTest {

    private static final int MYSQL_PORT = 3306;

    private static final int ES_PORT = 9200;

    private static final String INDEX = "iboot-kb-recall-test";

    private static final int DIMENSIONS = 1024;

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_ai_recall")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

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

    @BeforeAll
    static void setUpElasticsearch() {
        restClient = Rest5Client.builder(
                URI.create("http://" + ELASTICSEARCH.getHost() + ":" + ELASTICSEARCH.getMappedPort(ES_PORT)))
                .build();
        transport = new Rest5ClientTransport(restClient, new Jackson3JsonpMapper());
        client = new ElasticsearchClient(transport);
    }

    @AfterAll
    static void tearDownElasticsearch() throws IOException {
        if (transport != null) {
            transport.close();
        }
        if (restClient != null) {
            restClient.close();
        }
    }

    /**
     * 入库 50 篇语料并计算 50 个问题的 Recall@5。
     *
     * @throws Exception 迁移或读取语料失败时抛出
     */
    @Test
    void evaluatesRecallAtFive() throws Exception {
        String apiKey = System.getenv("DASHSCOPE_EMBEDDING_API_KEY");
        assumeTrue(apiKey != null && !apiKey.isBlank(), "缺少 DASHSCOPE_EMBEDDING_API_KEY");

        DriverManagerDataSource dataSource = dataSource();
        migrateWithRetry(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM ai_document_chunk");
        jdbc.update("DELETE FROM ai_document");
        jdbc.update("DELETE FROM ai_kb_share_dept");
        jdbc.update("DELETE FROM ai_knowledge_base");
        KnowledgeRepository repository = new KnowledgeRepository(new NamedParameterJdbcTemplate(dataSource), jdbc);

        RagProperties properties = new RagProperties();
        properties.setIndexName(INDEX);
        properties.getEmbedding().setDimensions(DIMENSIONS);
        properties.getEmbedding().setBatchSize(10);
        properties.getOcr().setEnabled(false);

        try {
            client.indices().delete(d -> d.index(INDEX));
        } catch (ElasticsearchException ignored) {
            // 首次运行时索引不存在
        }
        new KnowledgeIndexInitializer(client, properties).ensureIndex();

        InMemoryKnowledgeStorage storage = new InMemoryKnowledgeStorage();
        FakeOcrParser ocrParser = new FakeOcrParser();
        OcrQuotaService ocrQuota = mock(OcrQuotaService.class);
        when(ocrQuota.tryAcquire(1)).thenReturn(true);
        EmbeddingModel embeddingModel = cloudEmbeddingModel(apiKey);
        AiMetrics metrics = new AiMetrics(new SimpleMeterRegistry(), "text-embedding-v4");
        ChunkEmbedder embedder = new ChunkEmbedder(embeddingModel, properties, metrics);
        KnowledgeIngestionService ingestionService = new KnowledgeIngestionService(
                repository,
                storage,
                new DocumentParserRegistry(List.of(new TextPdfParser(), new DocxParser(), new PlainTextParser())),
                new ChunkSplitter(properties),
                embedder,
                new KnowledgeChunkIndexer(client, properties),
                new PdfPageRenderer(),
                ocrParser,
                ocrQuota,
                properties,
                metrics);
        KnowledgeRetrievalService retrievalService = new KnowledgeRetrievalService(client, properties, embedder,
                new KnowledgeAccessService(), repository);

        long kbId = repository.insertKnowledgeBase("eval-kb", "recall evaluation", 1, 1L, 1L, "eval", List.of());
        JsonNode corpus = loadCorpus();
        Map<Integer, Long> documentIds = new LinkedHashMap<>();
        for (JsonNode document : corpus.path("documents")) {
            int corpusId = document.path("id").asInt();
            byte[] content = document.path("content").asString().getBytes(StandardCharsets.UTF_8);
            KnowledgeIngestionService.SubmitResult submitted = ingestionService.submit(kbId,
                    "doc-" + corpusId + ".txt", content, "text/plain", 1L, 1L);
            ingestionService.process(submitted.documentId());
            var stored = repository.findDocument(submitted.documentId()).orElseThrow();
            assertThat(stored.status().name())
                    .as("文档 %s 入库状态", corpusId)
                    .isEqualTo("READY");
            documentIds.put(corpusId, submitted.documentId());
        }

        PermissionScope scope = new PermissionScope(1L, "eval", false, 1, 1L, List.of(), List.of(1L),
                List.of("ai:kb:query"), List.of());
        List<Map<String, Object>> results = new ArrayList<>();
        List<Long> latenciesMs = new ArrayList<>();
        int hits = 0;
        for (JsonNode question : corpus.path("questions")) {
            String text = question.path("question").asString();
            int expectedCorpusId = question.path("expectedDocumentId").asInt();
            Long expectedDbId = documentIds.get(expectedCorpusId);
            long start = System.nanoTime();
            RetrievalResult result = retrievalService.search(text, kbId, scope);
            latenciesMs.add((System.nanoTime() - start) / 1_000_000L);
            List<Long> retrieved = result.chunks().stream().map(RetrievedChunk::docId).toList();
            boolean hit = retrieved.contains(expectedDbId);
            if (hit) {
                hits++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("question", text);
            row.put("expectedDocumentId", expectedCorpusId);
            row.put("expectedDbId", expectedDbId);
            row.put("retrievedDbIds", retrieved);
            row.put("hit", hit);
            results.add(row);
        }

        double recall = hits / (double) results.size();
        latenciesMs.sort(Long::compareTo);
        long p95 = latenciesMs.get(Math.max(0, (int) Math.ceil(latenciesMs.size() * 0.95) - 1));
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("documents", documentIds.size());
        report.put("questions", results.size());
        report.put("hits", hits);
        report.put("recallAt5", recall);
        report.put("retrievalLatencyMsP95", p95);
        report.put("retrievalLatencyMsMax", latenciesMs.getLast());
        report.put("mode", "hybrid");
        report.put("results", results);
        Files.writeString(Path.of("target/rag-recall-report.json"),
                MAPPER.writeValueAsString(report), StandardCharsets.UTF_8);
        System.out.printf("RAG Recall@5: %.2f%% (%d/%d)%n", recall * 100, hits, results.size());

        assertThat(recall).isGreaterThanOrEqualTo(0.80);
    }

    private DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/iboot_ai_recall?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        return dataSource;
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

    private JsonNode loadCorpus() throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream("rag/eval-corpus.json")) {
            if (input == null) {
                throw new IllegalStateException("缺少 rag/eval-corpus.json");
            }
            return MAPPER.readTree(input.readAllBytes());
        }
    }

    private EmbeddingModel cloudEmbeddingModel(String apiKey) {
        String baseUrl = System.getenv().getOrDefault("DASHSCOPE_BASE_URL",
                "https://dashscope.aliyuncs.com/compatible-mode");
        String endpoint = baseUrl.replaceAll("/+$", "") + "/v1/embeddings";
        RestClient restClient = RestClient.builder().build();
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embedForResponse(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            ObjectNode request = MAPPER.createObjectNode();
            request.put("model", "text-embedding-v4");
            request.put("dimensions", DIMENSIONS);
            ArrayNode input = request.putArray("input");
            texts.forEach(input::add);
            String body = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(MAPPER.writeValueAsString(request))
                    .retrieve()
                    .body(String.class);
            JsonNode data = MAPPER.readTree(body).path("data");
            List<Embedding> embeddings = new ArrayList<>();
            int index = 0;
            for (JsonNode item : data) {
                JsonNode vectorNode = item.path("embedding");
                float[] vector = new float[vectorNode.size()];
                for (int i = 0; i < vector.length; i++) {
                    vector[i] = (float) vectorNode.get(i).asDouble();
                }
                embeddings.add(new Embedding(vector, index++));
            }
            if (embeddings.size() != texts.size()) {
                throw new IllegalStateException("Embedding 返回数量不一致");
            }
            return new EmbeddingResponse(embeddings);
        });
        return model;
    }
}
