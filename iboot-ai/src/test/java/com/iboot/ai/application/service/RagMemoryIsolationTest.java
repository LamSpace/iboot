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
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RAG 会话记忆隔离测试。
 *
 * <p>确认检索上下文只进入 system message，不写入对话记忆。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RagMemoryIsolationTest {

    /**
     * 记忆只保留用户问题与最终回答。
     */
    @Test
    void doesNotStoreRetrievalContextInMemory() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().build());
        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(
                new Generation(new AssistantMessage("回答")))));
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();

        KnowledgeRetrievalService retrievalService = mock(KnowledgeRetrievalService.class);
        ChatApplicationService chatApplicationService = mock(ChatApplicationService.class);
        when(chatApplicationService.prepare(1L, null, "问题"))
                .thenReturn(new ChatApplicationService.PreparedChat(1L, "conv-1", "问题"));
        PermissionScope scope = new PermissionScope(1L, "tester", false, 5, 2L, List.of(), List.of(2L),
                List.of("ai:kb:query"), List.of());
        when(retrievalService.search("问题", 1L, scope)).thenReturn(new RetrievalResult(List.of(
                new RetrievedChunk("10:0", 1L, 10L, "年假政策", null, "年假五天", 3, 0, 1.0)),
                false, "hybrid"));
        RagChatService service = new RagChatService(retrievalService, new RagPromptBuilder(),
                chatApplicationService, chatClient, new AiMetrics(new SimpleMeterRegistry(), "test-model"),
                "test-model");

        RagPreparation preparation = service.prepare(1L, null, "问题", 1L, scope);
        service.answer(preparation);

        List<Message> messages = memory.get("conv-1");
        assertThat(messages).isNotEmpty();
        assertThat(messages).allSatisfy(message -> assertThat(message.getText()).doesNotContain("年假五天"));
        assertThat(messages).anySatisfy(message -> assertThat(message.getText()).contains("回答"));
    }
}
