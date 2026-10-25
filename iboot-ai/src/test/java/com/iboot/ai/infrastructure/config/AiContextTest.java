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
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 应用上下文与配置绑定的冒烟测试。
 *
 * <p>使用 profile {@code test} 注入假 Key 与不可达端点；不发起任何模型调用。
 */
@SpringBootTest
@ActiveProfiles("test")
class AiContextTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private OpenAiEmbeddingModel openAiEmbeddingModel;

    @Autowired
    private AiProperties aiProperties;

    @Test
    void exposesEmbeddingModelConfiguredForDashScope() {
        assertThat(embeddingModel).isNotNull();
        assertThat(openAiEmbeddingModel.getOptions().getModel()).isEqualTo("text-embedding-v4");
    }

    @Test
    void bindsQuotaAndChatDefaults() {
        assertThat(aiProperties.getQuota().getGlobalDailyTokens()).isEqualTo(20_000_000L);
        assertThat(aiProperties.getQuota().getUserDailyTokens()).isEqualTo(200_000L);
        assertThat(aiProperties.getChat().getMaxMessages()).isEqualTo(20);
        assertThat(aiProperties.getChat().getConversationTtlHours()).isEqualTo(24L);
    }
}
