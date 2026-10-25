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

import com.iboot.ai.infrastructure.config.RagProperties;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 分块向量化服务。
 *
 * <p>按配置批量调用 Spring AI {@link EmbeddingModel}，校验返回数量与维度，并记录 token 用量。
 * 维度不一致时拒绝写入，避免索引中出现错误维度的向量。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChunkEmbedder {

    private final EmbeddingModel embeddingModel;

    private final RagProperties properties;

    private final AiMetrics metrics;

    /**
     * 创建向量化服务。
     *
     * @param embeddingModel Spring AI 向量模型
     * @param properties     知识库配置
     * @param metrics        指标
     */
    public ChunkEmbedder(EmbeddingModel embeddingModel, RagProperties properties, AiMetrics metrics) {
        this.embeddingModel = embeddingModel;
        this.properties = properties;
        this.metrics = metrics;
    }

    /**
     * 批量向量化文本。
     *
     * @param texts 文本列表
     * @return 与文本顺序一致的向量
     * @throws EmbeddingException 调用失败、数量或维度不匹配时抛出
     */
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        int batchSize = Math.max(1, properties.getEmbedding().getBatchSize());
        int dimensions = properties.getEmbedding().getDimensions();
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += batchSize) {
            List<String> batch = texts.subList(start, Math.min(texts.size(), start + batchSize));
            EmbeddingResponse response;
            try {
                response = embeddingModel.embedForResponse(batch);
            } catch (RuntimeException e) {
                throw new EmbeddingException("EMBEDDING_FAILED", "Embedding 调用失败", e);
            }
            List<Embedding> results = response == null ? List.of() : response.getResults();
            if (results.size() != batch.size()) {
                throw new EmbeddingException("EMBEDDING_COUNT_MISMATCH",
                        "Embedding 返回数量与输入不一致：" + results.size() + "/" + batch.size());
            }
            for (Embedding embedding : results) {
                float[] vector = embedding.getOutput();
                if (vector == null || vector.length != dimensions) {
                    throw new EmbeddingException("DIMENSION_MISMATCH",
                            "Embedding 维度不匹配，期望 " + dimensions + "，实际 "
                                    + (vector == null ? 0 : vector.length));
                }
                vectors.add(vector);
            }
            metrics.embeddingTokens(tokensOf(response, batch), "document");
        }
        return vectors;
    }

    private long tokensOf(EmbeddingResponse response, List<String> batch) {
        if (response != null && response.getMetadata() != null) {
            Usage usage = response.getMetadata().getUsage();
            if (usage != null && usage.getTotalTokens() != null && usage.getTotalTokens() > 0) {
                return usage.getTotalTokens();
            }
        }
        return batch.stream().mapToLong(String::length).sum();
    }
}
