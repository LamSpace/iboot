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
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import com.iboot.ai.infrastructure.config.RagProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 知识库分块索引初始化器。
 *
 * <p>启动时幂等创建索引与 mapping；Elasticsearch 不可用时只记录警告，不阻止服务启动，
 * 首次检索或入库时再暴露明确错误。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexInitializer.class);

    private final ElasticsearchClient client;

    private final RagProperties properties;

    /**
     * 创建初始化器。
     *
     * @param client     ES 客户端
     * @param properties 知识库配置
     */
    public KnowledgeIndexInitializer(ElasticsearchClient client, RagProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * 启动时幂等创建索引。
     */
    @PostConstruct
    public void initialize() {
        try {
            ensureIndex();
        } catch (Exception e) {
            log.warn("知识库索引初始化失败，将在首次使用时重试：{}", e.getMessage());
        }
    }

    /**
     * 幂等创建索引与 mapping。
     *
     * @throws java.io.IOException 与 Elasticsearch 通信失败时抛出
     */
    public void ensureIndex() throws java.io.IOException {
        String indexName = properties.getIndexName();
        if (client.indices().exists(e -> e.index(indexName)).value()) {
            return;
        }
        try {
            client.indices().create(c -> c.index(indexName).mappings(m -> m
                    .properties("chunkId", p -> p.keyword(k -> k))
                    .properties("kbId", p -> p.long_(l -> l))
                    .properties("docId", p -> p.long_(l -> l))
                    .properties("docTitle", p -> p.text(t -> t.analyzer("cjk")
                            .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                    .properties("headingPath", p -> p.text(t -> t.analyzer("cjk")))
                    .properties("content", p -> p.text(t -> t.analyzer("cjk")))
                    .properties("embedding", p -> p.denseVector(d -> d
                            .dims(properties.getEmbedding().getDimensions())
                            .similarity(DenseVectorSimilarity.Cosine)))
                    .properties("pageNo", p -> p.integer(i -> i))
                    .properties("chunkIndex", p -> p.integer(i -> i))
                    .properties("ownerUserId", p -> p.long_(l -> l))
                    .properties("ownerDeptId", p -> p.long_(l -> l))
                    .properties("updatedAt", p -> p.date(d -> d))));
            log.info("知识库索引已创建：{}", indexName);
        } catch (ElasticsearchException e) {
            if (!isResourceAlreadyExists(e)) {
                throw e;
            }
        }
    }

    private boolean isResourceAlreadyExists(ElasticsearchException e) {
        return e.getMessage() != null && e.getMessage().contains("resource_already_exists_exception");
    }
}
