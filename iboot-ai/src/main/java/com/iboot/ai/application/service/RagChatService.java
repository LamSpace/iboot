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
import com.iboot.ai.interfaces.dto.UsageDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库 RAG 问答服务。
 *
 * <p>先做配额检查与会话解析，再执行权限前置过滤的检索，最后把编号上下文放入 system message
 * 调用对话模型。无命中时不调用模型；检索上下文不写入会话记忆。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagChatService {

    /**
     * 无命中提示。
     */
    public static final String NO_HIT_ANSWER = "未在知识库中找到相关内容。";

    private final KnowledgeRetrievalService retrievalService;

    private final RagPromptBuilder promptBuilder;

    private final ChatApplicationService chatApplicationService;

    private final ChatClient chatClient;

    private final AiMetrics metrics;

    private final String model;

    /**
     * 创建 RAG 问答服务。
     *
     * @param retrievalService    检索服务
     * @param promptBuilder       提示词构造器
     * @param chatApplicationService 对话应用服务
     * @param chatClient          带记忆 advisor 的客户端
     * @param metrics             指标
     * @param model               模型标识
     */
    public RagChatService(KnowledgeRetrievalService retrievalService, RagPromptBuilder promptBuilder,
                          ChatApplicationService chatApplicationService, ChatClient chatClient, AiMetrics metrics,
                          @Value("${spring.ai.openai.chat.options.model}") String model) {
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
        this.chatApplicationService = chatApplicationService;
        this.chatClient = chatClient;
        this.metrics = metrics;
        this.model = model;
    }

    /**
     * 准备一次 RAG 问答。
     *
     * @param userId         用户 ID
     * @param conversationId 会话 ID；为空时新建
     * @param question       用户问题
     * @param knowledgeBaseId 选择的知识库；为空时检索全部可见知识库
     * @param scope          当前用户权限范围
     * @return 准备好的上下文
     */
    public RagPreparation prepare(Long userId, String conversationId, String question, Long knowledgeBaseId,
                                  PermissionScope scope) {
        ChatApplicationService.PreparedChat prepared = chatApplicationService.prepare(userId, conversationId,
                question);
        RetrievalResult retrieval = retrievalService.search(question, knowledgeBaseId, scope);
        if (retrieval.chunks().isEmpty()) {
            metrics.ragEmptyResults();
        }
        return new RagPreparation(prepared.userId(), prepared.conversationId(), question,
                promptBuilder.build(retrieval.chunks()), citations(retrieval.chunks()), retrieval.degraded(),
                retrieval.chunks().isEmpty());
    }

    /**
     * 非流式回答。
     *
     * @param preparation 准备好的上下文
     * @return 回答
     */
    public RagAnswer answer(RagPreparation preparation) {
        if (preparation.noHit()) {
            metrics.ragRequest("no_hit", preparation.degraded());
            return new RagAnswer(preparation.conversationId(), NO_HIT_ANSWER, model, new UsageDto(0, 0, 0),
                    List.of(), preparation.degraded());
        }
        long start = System.nanoTime();
        try {
            ChatResponse response = chatClient.prompt()
                    .system(preparation.systemPrompt())
                    .user(preparation.question())
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, preparation.conversationId()))
                    .call()
                    .chatResponse();
            UsageDto usage = ChatApplicationService.usageOf(response);
            if (usage != null) {
                chatApplicationService.recordUsage(preparation.userId(), usage);
            }
            metrics.request("success");
            metrics.duration(Duration.ofNanos(System.nanoTime() - start));
            metrics.ragRequest("success", preparation.degraded());
            return new RagAnswer(preparation.conversationId(), ChatApplicationService.textOf(response), model,
                    usage == null ? new UsageDto(0, 0, 0) : usage, preparation.citations(),
                    preparation.degraded());
        } catch (RuntimeException e) {
            ChatErrorMapper.AiError error = ChatErrorMapper.toError(e);
            metrics.request("error");
            metrics.error(error.code());
            metrics.duration(Duration.ofNanos(System.nanoTime() - start));
            metrics.ragRequest("error", preparation.degraded());
            throw new ModelCallException(error, e);
        }
    }

    /**
     * 流式回答。
     *
     * @param preparation 准备好的上下文
     * @return 响应流
     */
    public Flux<ChatResponse> stream(RagPreparation preparation) {
        return chatClient.prompt()
                .system(preparation.systemPrompt())
                .user(preparation.question())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, preparation.conversationId()))
                .stream()
                .chatResponse();
    }

    /**
     * 记录流式回答的用量。
     *
     * @param userId 用户 ID
     * @param usage  用量
     */
    public void recordUsage(Long userId, UsageDto usage) {
        chatApplicationService.recordUsage(userId, usage);
    }

    private List<RagCitation> citations(List<RetrievedChunk> chunks) {
        List<RagCitation> citations = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            citations.add(new RagCitation(i + 1, chunk.kbId(), chunk.docId(), chunk.docTitle(), chunk.pageNo(),
                    chunk.chunkIndex(), snippet(chunk.content())));
        }
        return citations;
    }

    private String snippet(String content) {
        if (content == null) {
            return "";
        }
        return content.length() <= 200 ? content : content.substring(0, 200) + "…";
    }
}
