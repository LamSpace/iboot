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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

/**
 * 知识库配置启动校验。
 *
 * <p>OCR 关闭时不做任何校验；OCR 开启时要求 API Key 与模型非空，并校验索引名与向量维度。
 * 校验失败直接终止启动，避免服务带着必然失败的配置运行。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class RagConfigValidator implements InitializingBean {

    private final RagProperties properties;

    /**
     * 创建校验器。
     *
     * @param properties 知识库配置
     */
    public RagConfigValidator(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * 校验配置。
     */
    @Override
    public void afterPropertiesSet() {
        if (StringUtils.isBlank(properties.getIndexName())) {
            throw new IllegalStateException("iboot.ai.rag.index-name 不能为空");
        }
        if (properties.getEmbedding().getDimensions() <= 0) {
            throw new IllegalStateException("iboot.ai.rag.embedding.dimensions 必须大于 0");
        }
        if (properties.getChunkMaxChars() <= 0
                || properties.getChunkOverlapChars() < 0
                || properties.getChunkOverlapChars() >= properties.getChunkMaxChars()) {
            throw new IllegalStateException("chunk-overlap-chars 必须小于 chunk-max-chars");
        }
        if (!properties.getOcr().isEnabled()) {
            return;
        }
        if (StringUtils.isBlank(properties.getOcr().getApiKey())) {
            throw new IllegalStateException(
                    "OCR 已启用，但 iboot.ai.rag.ocr.api-key 为空，请通过 DASHSCOPE_OCR_API_KEY 注入");
        }
        if (StringUtils.isBlank(properties.getOcr().getModel())) {
            throw new IllegalStateException("OCR 已启用，但 iboot.ai.rag.ocr.model 为空");
        }
    }
}
