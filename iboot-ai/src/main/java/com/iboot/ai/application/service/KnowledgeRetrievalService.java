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
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.iboot.ai.domain.knowledge.DocumentScopeFilter;
import com.iboot.ai.domain.knowledge.KnowledgeBaseSummary;
import com.iboot.ai.infrastructure.config.RagProperties;
import com.iboot.ai.infrastructure.persistence.KnowledgeRepository;
import com.iboot.ai.infrastructure.search.KnowledgeChunkDocument;
import com.iboot.ai.infrastructure.security.PermissionScope;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库混合检索服务。
 *
 * <p>先按权限范围计算可见知识库与文档过滤条件，再执行 BM25 + kNN + 原生 RRF；两条检索路径
 * 共享同一套前置过滤，禁止先取 Top-K 再剔除越权结果。Embedding 不可用时降级为 BM25。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeRetrievalService {

    private static final List<String> SOURCE_FIELDS = List.of(
            "chunkId", "kbId", "docId", "docTitle", "headingPath", "content", "pageNo", "chunkIndex");

    private final ElasticsearchClient client;

    private final RagProperties properties;

    private final ChunkEmbedder chunkEmbedder;

    private final KnowledgeAccessService accessService;

    private final KnowledgeRepository repository;

    /**
     * 创建检索服务。
     *
     * @param client        ES 客户端
     * @param properties    知识库配置
     * @param chunkEmbedder 查询向量化
     * @param accessService 权限范围计算
     * @param repository    元数据仓储
     */
    public KnowledgeRetrievalService(ElasticsearchClient client, RagProperties properties,
                                     ChunkEmbedder chunkEmbedder, KnowledgeAccessService accessService,
                                     KnowledgeRepository repository) {
        this.client = client;
        this.properties = properties;
        this.chunkEmbedder = chunkEmbedder;
        this.accessService = accessService;
        this.repository = repository;
    }

    /**
     * 检索知识库分块。
     *
     * @param query      用户问题
     * @param selectedKb 显式选择的知识库；为空时检索全部可见知识库
     * @param scope      当前用户权限范围
     * @return 检索结果
     * @throws KnowledgeAccessDeniedException 显式选择不可见知识库时抛出
     * @throws RetrievalUnavailableException  检索后端不可用或权限范围缺失时抛出
     */
    public RetrievalResult search(String query, Long selectedKb, PermissionScope scope) {
        if (query == null || query.isBlank()) {
            return RetrievalResult.empty();
        }
        if (scope == null) {
            throw new RetrievalUnavailableException("缺少权限范围");
        }
        List<KnowledgeBaseSummary> summaries = repository.findKnowledgeBaseSummaries();
        List<Long> visibleKbIds = accessService.visibleKnowledgeBaseIds(scope, summaries);
        List<Long> kbIds;
        if (selectedKb != null) {
            if (!visibleKbIds.contains(selectedKb)) {
                throw new KnowledgeAccessDeniedException("无权访问知识库：" + selectedKb);
            }
            kbIds = List.of(selectedKb);
        } else {
            kbIds = visibleKbIds;
        }
        if (kbIds.isEmpty()) {
            return RetrievalResult.empty();
        }
        DocumentScopeFilter dataFilter = accessService.documentScopeFilter(scope);
        if (dataFilter.mode() == DocumentScopeFilter.Mode.DENY) {
            return RetrievalResult.empty();
        }
        List<Query> filters = filters(kbIds, dataFilter);
        Query bm25 = Query.of(q -> q.multiMatch(mm -> mm.query(query)
                .fields("docTitle^2", "headingPath^1.5", "content")));

        float[] queryVector = null;
        boolean degraded = false;
        try {
            queryVector = chunkEmbedder.embed(List.of(query)).getFirst();
        } catch (EmbeddingException e) {
            degraded = true;
        }
        try {
            if (queryVector == null) {
                return new RetrievalResult(bm25Only(bm25, filters), true, "bm25");
            }
            List<Float> vector = new ArrayList<>(queryVector.length);
            for (float value : queryVector) {
                vector.add(value);
            }
            SearchResponse<KnowledgeChunkDocument> bm25Response = client.search(s -> s
                    .index(properties.getIndexName())
                    .query(q -> q.bool(b -> b.must(bm25).filter(filters)))
                    .size(properties.getCandidateK())
                    .source(src -> src.filter(f -> f.includes(SOURCE_FIELDS))), KnowledgeChunkDocument.class);
            SearchResponse<KnowledgeChunkDocument> knnResponse = client.search(s -> s
                    .index(properties.getIndexName())
                    .knn(k -> k.field("embedding")
                            .queryVector(vector)
                            .k(properties.getCandidateK())
                            .numCandidates(properties.getNumCandidates())
                            .filter(filters))
                    .size(properties.getCandidateK())
                    .source(src -> src.filter(f -> f.includes(SOURCE_FIELDS))), KnowledgeChunkDocument.class);
            return new RetrievalResult(fuse(bm25Response, knnResponse), false, "hybrid");
        } catch (IOException | RuntimeException e) {
            throw new RetrievalUnavailableException("知识库检索不可用", e);
        }
    }

    private List<RetrievedChunk> bm25Only(Query bm25, List<Query> filters) throws IOException {
        SearchResponse<KnowledgeChunkDocument> response = client.search(s -> s
                .index(properties.getIndexName())
                .query(q -> q.bool(b -> b.must(bm25).filter(filters)))
                .size(properties.getCandidateK())
                .source(src -> src.filter(f -> f.includes(SOURCE_FIELDS))), KnowledgeChunkDocument.class);
        return toChunks(response);
    }

    private List<RetrievedChunk> fuse(SearchResponse<KnowledgeChunkDocument> bm25Response,
                                      SearchResponse<KnowledgeChunkDocument> knnResponse) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, KnowledgeChunkDocument> sources = new HashMap<>();
        addRanks(bm25Response, scores, sources);
        addRanks(knnResponse, scores, sources);
        List<Map.Entry<String, Double>> ranked = new ArrayList<>(scores.entrySet());
        ranked.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        List<RetrievedChunk> chunks = new ArrayList<>();
        Map<Long, Integer> perDoc = new HashMap<>();
        for (Map.Entry<String, Double> entry : ranked) {
            KnowledgeChunkDocument source = sources.get(entry.getKey());
            if (source == null || source.docId() == null) {
                continue;
            }
            int count = perDoc.getOrDefault(source.docId(), 0);
            if (count >= properties.getMaxChunksPerDoc()) {
                continue;
            }
            perDoc.put(source.docId(), count + 1);
            chunks.add(toChunk(source, entry.getValue()));
            if (chunks.size() >= properties.getTopK()) {
                break;
            }
        }
        return chunks;
    }

    private void addRanks(SearchResponse<KnowledgeChunkDocument> response, Map<String, Double> scores,
                          Map<String, KnowledgeChunkDocument> sources) {
        List<Hit<KnowledgeChunkDocument>> hits = response.hits().hits();
        int window = Math.min(hits.size(), properties.getRankWindowSize());
        for (int i = 0; i < window; i++) {
            KnowledgeChunkDocument source = hits.get(i).source();
            if (source == null || source.chunkId() == null) {
                continue;
            }
            scores.merge(source.chunkId(),
                    1.0 / (properties.getRankConstant() + i + 1), Double::sum);
            sources.putIfAbsent(source.chunkId(), source);
        }
    }

    private List<Query> filters(List<Long> kbIds, DocumentScopeFilter dataFilter) {
        List<Query> filters = new ArrayList<>();
        filters.add(Query.of(q -> q.terms(t -> t.field("kbId").terms(tv -> tv.value(
                kbIds.stream().map(FieldValue::of).toList())))));
        if (dataFilter.mode() == DocumentScopeFilter.Mode.SELF) {
            filters.add(Query.of(q -> q.term(t -> t.field("ownerUserId")
                    .value(FieldValue.of(dataFilter.userId())))));
        } else if (dataFilter.mode() == DocumentScopeFilter.Mode.DEPT) {
            filters.add(Query.of(q -> q.term(t -> t.field("ownerDeptId")
                    .value(FieldValue.of(dataFilter.deptId())))));
        } else if (dataFilter.mode() == DocumentScopeFilter.Mode.DEPTS) {
            filters.add(Query.of(q -> q.terms(t -> t.field("ownerDeptId").terms(tv -> tv.value(
                    dataFilter.deptIds().stream().map(FieldValue::of).toList())))));
        }
        return filters;
    }

    private List<RetrievedChunk> toChunks(SearchResponse<KnowledgeChunkDocument> response) {
        List<RetrievedChunk> chunks = new ArrayList<>();
        Map<Long, Integer> perDoc = new HashMap<>();
        for (Hit<KnowledgeChunkDocument> hit : response.hits().hits()) {
            KnowledgeChunkDocument source = hit.source();
            if (source == null || source.docId() == null) {
                continue;
            }
            int count = perDoc.getOrDefault(source.docId(), 0);
            if (count >= properties.getMaxChunksPerDoc()) {
                continue;
            }
            perDoc.put(source.docId(), count + 1);
            chunks.add(toChunk(source, hit.score()));
            if (chunks.size() >= properties.getTopK()) {
                break;
            }
        }
        return chunks;
    }

    private RetrievedChunk toChunk(KnowledgeChunkDocument source, Double score) {
        return new RetrievedChunk(source.chunkId(), source.kbId(), source.docId(), source.docTitle(),
                source.headingPath(), source.content(), source.pageNo(), source.chunkIndex(), score);
    }
}
