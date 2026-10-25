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
import com.iboot.ai.domain.knowledge.DocumentStatus;
import com.iboot.ai.domain.knowledge.KnowledgeDocument;
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
import com.iboot.ai.support.FakeOcrParser;
import com.iboot.ai.support.InMemoryKnowledgeStorage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeIngestionService} 的端到端集成测试。
 *
 * <p>使用 Testcontainers MySQL + Elasticsearch、内存对象存储、假 Embedding 与假 OCR，
 * 验证上传、去重、解析、分块、向量化、索引、OCR 补全与删除级联。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeIngestionServiceTest {

    private static final int MYSQL_PORT = 3306;

    private static final int ES_PORT = 9200;

    private static final String INDEX = "iboot-kb-chunk-test-ingest";

    private static final int DIMENSIONS = 4;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_ai_ingest")
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

    private KnowledgeRepository repository;

    private InMemoryKnowledgeStorage storage;

    private FakeOcrParser ocrParser;

    private OcrQuotaService ocrQuota;

    private RagProperties properties;

    private KnowledgeIngestionService service;

    private JdbcTemplate jdbc;

    private SimpleMeterRegistry meterRegistry;

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

    @BeforeEach
    void setUp() throws IOException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/iboot_ai_ingest?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        migrateWithRetry(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM ai_document_chunk");
        jdbc.update("DELETE FROM ai_document");
        jdbc.update("DELETE FROM ai_kb_share_dept");
        jdbc.update("DELETE FROM ai_knowledge_base");
        repository = new KnowledgeRepository(new NamedParameterJdbcTemplate(dataSource), jdbc);

        properties = new RagProperties();
        properties.setIndexName(INDEX);
        properties.getEmbedding().setDimensions(DIMENSIONS);
        properties.getOcr().setEnabled(false);
        properties.getOcr().setDpi(80);

        try {
            client.indices().delete(d -> d.index(INDEX));
        } catch (ElasticsearchException ignored) {
            // 首次运行时索引不存在
        }
        new KnowledgeIndexInitializer(client, properties).ensureIndex();

        storage = new InMemoryKnowledgeStorage();
        ocrParser = new FakeOcrParser();
        ocrQuota = mock(OcrQuotaService.class);
        when(ocrQuota.tryAcquire(1)).thenReturn(true);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.embedForResponse(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            List<Embedding> embeddings = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                embeddings.add(new Embedding(new float[]{1f, 2f, 3f, 4f}, i));
            }
            return new EmbeddingResponse(embeddings);
        });
        meterRegistry = new SimpleMeterRegistry();
        AiMetrics metrics = new AiMetrics(meterRegistry, "test-model");
        service = new KnowledgeIngestionService(
                repository,
                storage,
                new DocumentParserRegistry(List.of(new TextPdfParser(), new DocxParser(), new PlainTextParser())),
                new ChunkSplitter(properties),
                new ChunkEmbedder(embeddingModel, properties, metrics),
                new KnowledgeChunkIndexer(client, properties),
                new PdfPageRenderer(),
                ocrParser,
                ocrQuota,
                properties,
                metrics);
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
     * 文本入库、去重与删除。
     *
     * @throws IOException 查询索引失败时抛出
     */
    @Test
    void ingestsDeduplicatesAndDeletes() throws IOException {
        long kbId = repository.insertKnowledgeBase("kb", null, 1, 100L, 2L, "tester", List.of());
        byte[] content = "知识库内容".getBytes(StandardCharsets.UTF_8);

        KnowledgeIngestionService.SubmitResult submitted = service.submit(kbId, "a.txt", content, "text/plain",
                100L, 2L);
        assertThat(submitted.duplicate()).isFalse();
        service.process(submitted.documentId());

        KnowledgeDocument document = repository.findDocument(submitted.documentId()).orElseThrow();
        assertThat(document.status()).isEqualTo(DocumentStatus.READY);
        assertThat(document.chunkCount()).isGreaterThan(0);
        assertThat(esCount()).isGreaterThan(0);

        KnowledgeIngestionService.SubmitResult duplicate = service.submit(kbId, "b.txt", content, "text/plain",
                100L, 2L);
        assertThat(duplicate.duplicate()).isTrue();
        assertThat(duplicate.documentId()).isEqualTo(submitted.documentId());

        service.deleteDocument(submitted.documentId());
        assertThat(repository.findDocument(submitted.documentId())).isEmpty();
        assertThat(esCount()).isZero();
    }

    /**
     * OCR 关闭时扫描件失败，启用后假 OCR 可补全并入库。
     *
     * @throws Exception 生成 PDF 失败时抛出
     */
    @Test
    void handlesScannedPdfWithOcrToggle() throws Exception {
        long kbId = repository.insertKnowledgeBase("kb", null, 1, 100L, 2L, "tester", List.of());
        KnowledgeIngestionService.SubmitResult submitted = service.submit(kbId, "scan.pdf", blankPdf(),
                "application/pdf", 100L, 2L);

        service.process(submitted.documentId());
        KnowledgeDocument failed = repository.findDocument(submitted.documentId()).orElseThrow();
        assertThat(failed.status()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.errorCode()).isEqualTo("NO_TEXT");
        assertThat(ocrParser.calls()).isZero();

        repository.resetForRetry(submitted.documentId());
        properties.getOcr().setEnabled(true);
        ocrParser.setText("扫描文字");
        service.process(submitted.documentId());

        KnowledgeDocument ready = repository.findDocument(submitted.documentId()).orElseThrow();
        assertThat(ready.status()).isEqualTo(DocumentStatus.READY);
        assertThat(ocrParser.calls()).isEqualTo(1);
        assertThat(esCount()).isGreaterThan(0);
        assertThat(meterRegistry.get("iboot.ai.rag.ocr.pages")
                .tag("outcome", "success").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("iboot.ai.rag.ocr.tokens").counter().count()).isGreaterThan(0);
    }

    /**
     * OCR 配额耗尽时不调用 OCR。
     *
     * @throws Exception 生成 PDF 失败时抛出
     */
    @Test
    void failsWhenOcrQuotaExhausted() throws Exception {
        long kbId = repository.insertKnowledgeBase("kb", null, 1, 100L, 2L, "tester", List.of());
        KnowledgeIngestionService.SubmitResult submitted = service.submit(kbId, "scan.pdf", blankPdf(),
                "application/pdf", 100L, 2L);
        properties.getOcr().setEnabled(true);
        when(ocrQuota.tryAcquire(1)).thenReturn(false);

        service.process(submitted.documentId());

        KnowledgeDocument failed = repository.findDocument(submitted.documentId()).orElseThrow();
        assertThat(failed.status()).isEqualTo(DocumentStatus.FAILED);
        assertThat(failed.errorCode()).isEqualTo("OCR_QUOTA");
        assertThat(ocrParser.calls()).isZero();
    }

    /**
     * 删除知识库级联清理文档与索引。
     *
     * @throws IOException 查询索引失败时抛出
     */
    @Test
    void deletesKnowledgeBaseCascade() throws IOException {
        long kbId = repository.insertKnowledgeBase("kb", null, 1, 100L, 2L, "tester", List.of());
        KnowledgeIngestionService.SubmitResult submitted = service.submit(kbId, "a.txt",
                "内容".getBytes(StandardCharsets.UTF_8), "text/plain", 100L, 2L);
        service.process(submitted.documentId());

        service.deleteKnowledgeBase(kbId);

        assertThat(repository.findKnowledgeBase(kbId)).isEmpty();
        assertThat(repository.findDocumentsByKb(kbId)).isEmpty();
        assertThat(esCount()).isZero();
    }

    private long esCount() throws IOException {
        return client.count(c -> c.index(INDEX)).count();
    }

    private byte[] blankPdf() throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
