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

package com.iboot.ai.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RagConfigValidator} 的单元测试。
 *
 * <p>验证 OCR 关闭时不要求 Key，启用时缺少 Key/模型或索引参数非法会阻止启动。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RagConfigValidatorTest {

    /**
     * OCR 关闭时缺少 Key 不影响启动。
     */
    @Test
    void disabledOcrDoesNotRequireKey() {
        RagProperties properties = properties(false, "", "qwen3.5-ocr");
        assertThatCode(() -> new RagConfigValidator(properties).afterPropertiesSet()).doesNotThrowAnyException();
    }

    /**
     * OCR 启用但缺少 Key 时启动失败。
     */
    @Test
    void enabledOcrRequiresApiKey() {
        RagProperties properties = properties(true, "", "qwen3.5-ocr");
        assertThatThrownBy(() -> new RagConfigValidator(properties).afterPropertiesSet())
                .hasMessageContaining("OCR")
                .hasMessageContaining("api-key");
    }

    /**
     * OCR 启用但缺少模型时启动失败。
     */
    @Test
    void enabledOcrRequiresModel() {
        RagProperties properties = properties(true, "test-key", "");
        assertThatThrownBy(() -> new RagConfigValidator(properties).afterPropertiesSet())
                .hasMessageContaining("model");
    }

    /**
     * OCR 启用且 Key/模型齐全时通过。
     */
    @Test
    void enabledOcrWithKeyPasses() {
        RagProperties properties = properties(true, "test-key", "qwen3.5-ocr");
        assertThatCode(() -> new RagConfigValidator(properties).afterPropertiesSet()).doesNotThrowAnyException();
    }

    /**
     * 向量维度非法时启动失败。
     */
    @Test
    void invalidEmbeddingDimensionFails() {
        RagProperties properties = properties(false, "", "qwen3.5-ocr");
        properties.getEmbedding().setDimensions(0);
        assertThatThrownBy(() -> new RagConfigValidator(properties).afterPropertiesSet())
                .hasMessageContaining("dimensions");
    }

    /**
     * 分块重叠不小于最大长度时启动失败。
     */
    @Test
    void invalidChunkOverlapFails() {
        RagProperties properties = properties(false, "", "qwen3.5-ocr");
        properties.setChunkMaxChars(100);
        properties.setChunkOverlapChars(100);
        assertThatThrownBy(() -> new RagConfigValidator(properties).afterPropertiesSet())
                .hasMessageContaining("chunk-overlap-chars");
    }

    /**
     * 索引名为空时启动失败。
     */
    @Test
    void blankIndexNameFails() {
        RagProperties properties = properties(false, "", "qwen3.5-ocr");
        properties.setIndexName(" ");
        assertThatThrownBy(() -> new RagConfigValidator(properties).afterPropertiesSet())
                .hasMessageContaining("index-name");
    }

    private RagProperties properties(boolean ocrEnabled, String apiKey, String model) {
        RagProperties properties = new RagProperties();
        properties.getOcr().setEnabled(ocrEnabled);
        properties.getOcr().setApiKey(apiKey);
        properties.getOcr().setModel(model);
        return properties;
    }
}
