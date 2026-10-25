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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ChunkEmbedder} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ChunkEmbedderTest {

    private EmbeddingModel model;

    private RagProperties properties;

    private SimpleMeterRegistry registry;

    private ChunkEmbedder embedder;

    @BeforeEach
    void setUp() {
        model = mock(EmbeddingModel.class);
        properties = new RagProperties();
        properties.getEmbedding().setBatchSize(2);
        properties.getEmbedding().setDimensions(4);
        registry = new SimpleMeterRegistry();
        embedder = new ChunkEmbedder(model, properties, new AiMetrics(registry, "test-model"));
    }

    /**
     * 按批次调用并保持顺序。
     */
    @Test
    void embedsInBatches() {
        when(model.embedForResponse(List.of("a", "b"))).thenReturn(response(4, 4));
        when(model.embedForResponse(List.of("c"))).thenReturn(response(4));

        List<float[]> vectors = embedder.embed(List.of("a", "b", "c"));

        assertThat(vectors).hasSize(3);
        assertThat(vectors).allSatisfy(vector -> assertThat(vector).hasSize(4));
        verify(model).embedForResponse(List.of("a", "b"));
        verify(model).embedForResponse(List.of("c"));
        assertThat(registry.get("iboot.ai.rag.embedding.tokens").tag("stage", "document").counter().count())
                .isGreaterThan(0);
    }

    /**
     * 维度不匹配时失败。
     */
    @Test
    void rejectsDimensionMismatch() {
        when(model.embedForResponse(List.of("a"))).thenReturn(response(3));

        assertThatThrownBy(() -> embedder.embed(List.of("a")))
                .isInstanceOf(EmbeddingException.class)
                .extracting(error -> ((EmbeddingException) error).code())
                .isEqualTo("DIMENSION_MISMATCH");
    }

    /**
     * 返回数量不匹配时失败。
     */
    @Test
    void rejectsCountMismatch() {
        when(model.embedForResponse(List.of("a", "b"))).thenReturn(response(4));

        assertThatThrownBy(() -> embedder.embed(List.of("a", "b")))
                .isInstanceOf(EmbeddingException.class)
                .extracting(error -> ((EmbeddingException) error).code())
                .isEqualTo("EMBEDDING_COUNT_MISMATCH");
    }

    /**
     * 空输入不调用模型。
     */
    @Test
    void skipsEmptyInput() {
        assertThat(embedder.embed(List.of())).isEmpty();
        verifyNoInteractions(model);
    }

    private EmbeddingResponse response(int... dimensions) {
        List<Embedding> embeddings = new java.util.ArrayList<>();
        for (int i = 0; i < dimensions.length; i++) {
            embeddings.add(new Embedding(new float[dimensions[i]], i));
        }
        return new EmbeddingResponse(embeddings);
    }
}
