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

import com.iboot.ai.infrastructure.metrics.AiMetrics;
import com.iboot.ai.infrastructure.security.PermissionScope;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link RagChatService} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RagChatServiceTest {

    private KnowledgeRetrievalService retrievalService;

    private ChatApplicationService chatApplicationService;

    private ChatClient chatClient;

    private RagChatService service;

    private PermissionScope scope;

    @BeforeEach
    void setUp() {
        retrievalService = mock(KnowledgeRetrievalService.class);
        chatApplicationService = mock(ChatApplicationService.class);
        chatClient = mock(ChatClient.class);
        service = new RagChatService(retrievalService, new RagPromptBuilder(), chatApplicationService, chatClient,
                new AiMetrics(new SimpleMeterRegistry(), "test-model"), "test-model");
        scope = new PermissionScope(1L, "tester", false, 5, 2L, List.of(), List.of(2L),
                List.of("ai:kb:query"), List.of());
        when(chatApplicationService.prepare(1L, null, "问题"))
                .thenReturn(new ChatApplicationService.PreparedChat(1L, "conv-1", "问题"));
    }

    /**
     * 命中时生成引用与带上下文提示词。
     */
    @Test
    void preparesCitationsAndPrompt() {
        when(retrievalService.search("问题", 1L, scope)).thenReturn(new RetrievalResult(List.of(
                new RetrievedChunk("10:0", 1L, 10L, "年假政策", null, "年假五天", 3, 0, 1.0)),
                false, "hybrid"));

        RagPreparation preparation = service.prepare(1L, null, "问题", 1L, scope);

        assertThat(preparation.conversationId()).isEqualTo("conv-1");
        assertThat(preparation.noHit()).isFalse();
        assertThat(preparation.citations()).hasSize(1);
        assertThat(preparation.citations().getFirst().index()).isEqualTo(1);
        assertThat(preparation.citations().getFirst().docTitle()).isEqualTo("年假政策");
        assertThat(preparation.systemPrompt()).contains("年假五天");
    }

    /**
     * 无命中时不调用模型，返回固定提示。
     */
    @Test
    void noHitDoesNotCallModel() {
        when(retrievalService.search("问题", 1L, scope)).thenReturn(RetrievalResult.empty());

        RagPreparation preparation = service.prepare(1L, null, "问题", 1L, scope);
        RagAnswer answer = service.answer(preparation);

        assertThat(preparation.noHit()).isTrue();
        assertThat(answer.content()).isEqualTo(RagChatService.NO_HIT_ANSWER);
        assertThat(answer.citations()).isEmpty();
        verifyNoInteractions(chatClient);
    }

    /**
     * 检索降级状态传递到回答。
     */
    @Test
    void propagatesDegradedFlag() {
        when(retrievalService.search("问题", 1L, scope)).thenReturn(new RetrievalResult(List.of(
                new RetrievedChunk("10:0", 1L, 10L, "年假政策", null, "年假五天", 3, 0, 1.0)),
                true, "bm25"));

        RagPreparation preparation = service.prepare(1L, null, "问题", 1L, scope);

        assertThat(preparation.degraded()).isTrue();
    }
}
