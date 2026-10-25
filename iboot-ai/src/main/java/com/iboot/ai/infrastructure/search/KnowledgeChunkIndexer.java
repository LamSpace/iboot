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
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.iboot.ai.domain.knowledge.ChunkDraft;
import com.iboot.ai.domain.knowledge.KnowledgeDocument;
import com.iboot.ai.infrastructure.config.RagProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库分块索引器。
 *
 * <p>写入前按文档删除旧分块，使用确定性 ID 保证重建幂等；删除按 {@code docId} 或
 * {@code kbId} 执行 delete-by-query。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeChunkIndexer {

    private final ElasticsearchClient client;

    private final RagProperties properties;

    /**
     * 创建索引器。
     *
     * @param client     ES 客户端
     * @param properties 知识库配置
     */
    public KnowledgeChunkIndexer(ElasticsearchClient client, RagProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * 写入文档分块。
     *
     * @param document 文档
     * @param chunks   分块
     * @param vectors  与分块顺序一致的向量
     */
    public void index(KnowledgeDocument document, List<ChunkDraft> chunks, List<float[]> vectors) {
        if (chunks.size() != vectors.size()) {
            throw new IndexException("CHUNK_VECTOR_MISMATCH", "分块数与向量数不一致", null);
        }
        deleteByDoc(document.id());
        if (chunks.isEmpty()) {
            return;
        }
        String updatedAt = Instant.now().toString();
        List<BulkOperation> operations = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkDraft chunk = chunks.get(i);
            KnowledgeChunkDocument source = new KnowledgeChunkDocument(
                    document.id() + ":" + chunk.chunkIndex(),
                    document.kbId(),
                    document.id(),
                    document.fileName(),
                    chunk.headingPath(),
                    chunk.content(),
                    vectors.get(i),
                    chunk.pageNo(),
                    chunk.chunkIndex(),
                    document.ownerUserId(),
                    document.ownerDeptId(),
                    updatedAt);
            operations.add(BulkOperation.of(b -> b.index(index -> index
                    .index(properties.getIndexName())
                    .id(source.chunkId())
                    .document(source))));
        }
        try {
            BulkResponse response = client.bulk(b -> b.operations(operations).refresh(Refresh.WaitFor));
            if (response.errors()) {
                throw new IndexException("INDEX_FAILED", "分块批量写入失败", null);
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof IndexException indexException) {
                throw indexException;
            }
            throw new IndexException("INDEX_FAILED", "分块写入 Elasticsearch 失败", e);
        }
    }

    /**
     * 删除文档的全部分块。
     *
     * @param docId 文档 ID
     */
    public void deleteByDoc(long docId) {
        try {
            client.deleteByQuery(q -> q.index(properties.getIndexName())
                    .query(query -> query.term(t -> t.field("docId").value(docId)))
                    .refresh(true));
        } catch (IOException | RuntimeException e) {
            throw new IndexException("INDEX_FAILED", "删除文档分块失败", e);
        }
    }

    /**
     * 删除知识库的全部分块。
     *
     * @param kbId 知识库 ID
     */
    public void deleteByKb(long kbId) {
        try {
            client.deleteByQuery(q -> q.index(properties.getIndexName())
                    .query(query -> query.term(t -> t.field("kbId").value(kbId)))
                    .refresh(true));
        } catch (IOException | RuntimeException e) {
            throw new IndexException("INDEX_FAILED", "删除知识库分块失败", e);
        }
    }
}
