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

import com.iboot.ai.domain.knowledge.ChunkDraft;
import com.iboot.ai.domain.knowledge.DocumentChunk;
import com.iboot.ai.domain.knowledge.KnowledgeDocument;
import com.iboot.ai.domain.knowledge.KnowledgeStorage;
import com.iboot.ai.domain.knowledge.OcrParser;
import com.iboot.ai.domain.knowledge.ParsedBlock;
import com.iboot.ai.domain.knowledge.ParsedDocument;
import com.iboot.ai.infrastructure.config.RagProperties;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import com.iboot.ai.infrastructure.ocr.OcrException;
import com.iboot.ai.infrastructure.ocr.OcrQuotaService;
import com.iboot.ai.infrastructure.ocr.PdfPageRenderer;
import com.iboot.ai.infrastructure.parsing.DocumentParseException;
import com.iboot.ai.infrastructure.parsing.DocumentParserRegistry;
import com.iboot.ai.infrastructure.persistence.KnowledgeRepository;
import com.iboot.ai.infrastructure.search.IndexException;
import com.iboot.ai.infrastructure.search.KnowledgeChunkIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 知识库文档入库应用服务。
 *
 * <p>负责上传去重、对象存储写入、解析、OCR 补全、分块、向量化、索引与状态机推进。
 * 失败按原因码决定是否自动重试；权限类错误不做无意义重试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private static final Set<String> PERMANENT_FAILURES = Set.of(
            "UNSUPPORTED_TYPE", "EMPTY_FILE", "NO_TEXT", "ENCRYPTED", "PARSE_FAILED",
            "OCR_CONFIG", "OCR_QUOTA", "OCR_LIMIT", "DIMENSION_MISMATCH", "EMBEDDING_COUNT_MISMATCH");

    private final KnowledgeRepository repository;

    private final KnowledgeStorage storage;

    private final DocumentParserRegistry parserRegistry;

    private final ChunkSplitter chunkSplitter;

    private final ChunkEmbedder chunkEmbedder;

    private final KnowledgeChunkIndexer indexer;

    private final PdfPageRenderer pageRenderer;

    private final OcrParser ocrParser;

    private final OcrQuotaService ocrQuota;

    private final RagProperties properties;

    private final AiMetrics metrics;

    /**
     * 创建入库服务。
     *
     * @param repository     元数据仓储
     * @param storage        对象存储
     * @param parserRegistry 解析器注册表
     * @param chunkSplitter  分块器
     * @param chunkEmbedder  向量化服务
     * @param indexer        索引器
     * @param pageRenderer   PDF 页面渲染器
     * @param ocrParser      OCR 解析器
     * @param ocrQuota       OCR 配额
     * @param properties     知识库配置
     * @param metrics        指标
     */
    public KnowledgeIngestionService(KnowledgeRepository repository, KnowledgeStorage storage,
                                     DocumentParserRegistry parserRegistry, ChunkSplitter chunkSplitter,
                                     ChunkEmbedder chunkEmbedder, KnowledgeChunkIndexer indexer,
                                     PdfPageRenderer pageRenderer, OcrParser ocrParser,
                                     OcrQuotaService ocrQuota, RagProperties properties, AiMetrics metrics) {
        this.repository = repository;
        this.storage = storage;
        this.parserRegistry = parserRegistry;
        this.chunkSplitter = chunkSplitter;
        this.chunkEmbedder = chunkEmbedder;
        this.indexer = indexer;
        this.pageRenderer = pageRenderer;
        this.ocrParser = ocrParser;
        this.ocrQuota = ocrQuota;
        this.properties = properties;
        this.metrics = metrics;
    }

    /**
     * 上传结果。
     *
     * @param documentId 文档 ID
     * @param duplicate  是否为重复内容
     */
    public record SubmitResult(long documentId, boolean duplicate) {
    }

    /**
     * 提交文档。
     *
     * @param kbId        知识库 ID
     * @param fileName    原始文件名
     * @param content     文件内容
     * @param mimeType    MIME 类型
     * @param ownerUserId 上传人用户 ID
     * @param ownerDeptId 上传人部门 ID
     * @return 上传结果
     */
    public SubmitResult submit(long kbId, String fileName, byte[] content, String mimeType, long ownerUserId,
                               long ownerDeptId) {
        if (content == null || content.length == 0) {
            throw new DocumentParseException("EMPTY_FILE", "文件内容为空");
        }
        if (!parserRegistry.supports(fileName)) {
            throw new DocumentParseException("UNSUPPORTED_TYPE", "不支持的文件类型：" + fileName);
        }
        String contentHash = sha256(content);
        Optional<KnowledgeDocument> existing = repository.findActiveDocumentByHash(kbId, contentHash);
        if (existing.isPresent()) {
            return new SubmitResult(existing.get().id(), true);
        }
        String storageKey = "kb/" + kbId + "/doc/" + UUID.randomUUID() + "/" + sanitize(fileName);
        storage.put(storageKey, content, mimeType);
        long documentId = repository.insertDocument(kbId, fileName, extensionOf(fileName), content.length, mimeType,
                storageKey, contentHash, ownerUserId, ownerDeptId);
        return new SubmitResult(documentId, false);
    }

    /**
     * 处理一条文档。
     *
     * @param docId 文档 ID
     */
    public void process(long docId) {
        KnowledgeDocument document = repository.findDocument(docId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在：" + docId));
        if (document.status() == com.iboot.ai.domain.knowledge.DocumentStatus.READY) {
            return;
        }
        try {
            doProcess(document);
        } catch (RuntimeException e) {
            handleFailure(document, e);
        }
    }

    /**
     * 手动重试文档。
     *
     * @param docId 文档 ID
     */
    public void retry(long docId) {
        repository.resetForRetry(docId);
    }

    /**
     * 删除文档及其索引与对象。
     *
     * @param docId 文档 ID
     */
    public void deleteDocument(long docId) {
        repository.findDocument(docId).ifPresent(document -> {
            deleteIndexQuietly(document);
            deleteObjectQuietly(document.storageKey());
            repository.softDeleteDocument(docId);
        });
    }

    /**
     * 删除知识库及其索引与对象。
     *
     * @param kbId 知识库 ID
     */
    public void deleteKnowledgeBase(long kbId) {
        for (KnowledgeDocument document : repository.findDocumentsByKb(kbId)) {
            deleteObjectQuietly(document.storageKey());
        }
        try {
            indexer.deleteByKb(kbId);
        } catch (RuntimeException e) {
            log.warn("删除知识库索引失败，kbId: {}, 原因: {}", kbId, e.getMessage());
        }
        repository.softDeleteKnowledgeBase(kbId);
    }

    private void doProcess(KnowledgeDocument document) {
        byte[] content = storage.get(document.storageKey());
        ParsedDocument parsed = parserRegistry.parse(document.fileName(), content);
        parsed = applyOcr(parsed, content);
        if (!parsed.hasText()) {
            throw new DocumentParseException("NO_TEXT", "文档中没有可提取文本");
        }
        List<ChunkDraft> chunks = chunkSplitter.split(parsed);
        if (chunks.isEmpty()) {
            throw new DocumentParseException("NO_TEXT", "文档分块为空");
        }
        List<float[]> vectors = chunkEmbedder.embed(chunks.stream().map(ChunkDraft::content).toList());
        List<DocumentChunk> entities = chunks.stream()
                .map(chunk -> new DocumentChunk(null, document.id(), chunk.chunkIndex(), chunk.pageNo(),
                        chunk.headingPath(), chunk.content(), chunk.tokenCount()))
                .toList();
        repository.saveChunks(document.id(), entities);
        indexer.index(document, chunks, vectors);
        repository.markReady(document.id(), chunks.size());
    }

    private ParsedDocument applyOcr(ParsedDocument parsed, byte[] content) {
        if (!properties.getOcr().isEnabled()) {
            return parsed;
        }
        List<ParsedBlock> blocks = new ArrayList<>();
        int processedPages = 0;
        for (ParsedBlock block : parsed.blocks()) {
            if (block.text() != null && !block.text().isBlank()) {
                blocks.add(block);
                continue;
            }
            if (block.pageNo() == null) {
                blocks.add(block);
                continue;
            }
            if (processedPages >= properties.getOcr().getMaxPages()) {
                throw new OcrException("OCR_LIMIT", "单文档 OCR 页数超过上限");
            }
            if (!ocrQuota.tryAcquire(1)) {
                metrics.ocrQuotaRejected();
                throw new OcrException("OCR_QUOTA", "OCR 日页数配额已耗尽");
            }
            byte[] image = pageRenderer.render(content, block.pageNo(), properties.getOcr().getDpi());
            long ocrStart = System.nanoTime();
            String text;
            try {
                text = ocrParser.recognize(image, "image/jpeg");
                metrics.ocrPages(1, "success");
                metrics.ocrTokens(text.length());
                metrics.ocrDuration(Duration.ofNanos(System.nanoTime() - ocrStart), "success");
            } catch (RuntimeException e) {
                metrics.ocrPages(1, "failed");
                metrics.ocrDuration(Duration.ofNanos(System.nanoTime() - ocrStart), "failed");
                throw e;
            }
            blocks.add(new ParsedBlock(block.pageNo(), block.headingPath(), text));
            processedPages++;
        }
        return new ParsedDocument(blocks);
    }

    private void handleFailure(KnowledgeDocument document, RuntimeException error) {
        String code = failureCode(error);
        String message = truncate(error.getMessage());
        int attempts = repository.findDocument(document.id())
                .map(KnowledgeDocument::attempts)
                .orElse(document.attempts());
        LocalDateTime nextAttemptAt = null;
        if (!PERMANENT_FAILURES.contains(code) && attempts < properties.getIngest().getMaxAttempts()) {
            nextAttemptAt = LocalDateTime.now().plusMinutes(backoffMinutes(attempts));
        }
        repository.markFailed(document.id(), code, message, nextAttemptAt);
        log.warn("文档入库失败，docId: {}, code: {}, attempts: {}", document.id(), code, attempts);
    }

    private String failureCode(RuntimeException error) {
        if (error instanceof DocumentParseException parseException) {
            return parseException.code();
        }
        if (error instanceof OcrException ocrException) {
            return ocrException.code();
        }
        if (error instanceof EmbeddingException embeddingException) {
            return embeddingException.code();
        }
        if (error instanceof IndexException indexException) {
            return indexException.code();
        }
        return "PROCESSING_FAILED";
    }

    private long backoffMinutes(int attempts) {
        if (attempts <= 1) {
            return 1L;
        }
        if (attempts == 2) {
            return 5L;
        }
        return 15L;
    }

    private void deleteIndexQuietly(KnowledgeDocument document) {
        try {
            indexer.deleteByDoc(document.id());
        } catch (RuntimeException e) {
            log.warn("删除文档索引失败，docId: {}, 原因: {}", document.id(), e.getMessage());
        }
    }

    private void deleteObjectQuietly(String storageKey) {
        try {
            storage.delete(storageKey);
        } catch (RuntimeException e) {
            log.warn("删除对象存储失败，key: {}, 原因: {}", storageKey, e.getMessage());
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 || dot == fileName.length() - 1 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    private String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[\\\\/]", "_");
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
