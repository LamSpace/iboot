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
import com.iboot.ai.interfaces.dto.ChatResponseDto;
import com.iboot.ai.interfaces.dto.ConversationSummaryDto;
import com.iboot.ai.interfaces.dto.MessageDto;
import com.iboot.ai.interfaces.dto.UsageDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 对话应用服务。
 *
 * <p>负责会话解析、配额前置检查、模型调用与指标计量。会话记忆由 ChatClient 的 advisor 维护，
 * 调用方只需传入会话 ID。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class ChatApplicationService {

    private final ChatClient chatClient;

    private final ChatMemory chatMemory;

    private final ConversationService conversationService;

    private final QuotaService quotaService;

    private final AiMetrics aiMetrics;

    private final String model;

    /**
     * 创建对话服务。
     *
     * @param chatClient         带记忆 advisor 的客户端
     * @param chatMemory         会话记忆
     * @param conversationService 会话索引
     * @param quotaService       配额
     * @param aiMetrics          指标
     * @param model              模型标识
     */
    public ChatApplicationService(ChatClient chatClient,
                                  ChatMemory chatMemory,
                                  ConversationService conversationService,
                                  QuotaService quotaService,
                                  AiMetrics aiMetrics,
                                  @Value("${spring.ai.openai.chat.options.model}") String model) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.conversationService = conversationService;
        this.quotaService = quotaService;
        this.aiMetrics = aiMetrics;
        this.model = model;
    }

    /**
     * 准备好的对话上下文。
     *
     * @param userId         用户 ID
     * @param conversationId 会话 ID
     * @param content        用户消息
     */
    public record PreparedChat(Long userId, String conversationId, String content) {
    }

    /**
     * 校验配额并解析会话。
     *
     * @param userId         用户 ID
     * @param conversationId 会话 ID；为空时新建
     * @param content        用户消息
     * @return 准备好的上下文
     */
    public PreparedChat prepare(Long userId, String conversationId, String content) {
        quotaService.assertWithinQuota(userId);
        String resolved = resolveConversation(userId, conversationId, content);
        return new PreparedChat(userId, resolved, content);
    }

    /**
     * 非流式对话。
     *
     * @param userId         用户 ID
     * @param conversationId 会话 ID
     * @param content        用户消息
     * @return 回答
     */
    public ChatResponseDto chatOnce(Long userId, String conversationId, String content) {
        PreparedChat prepared = prepare(userId, conversationId, content);
        long start = System.nanoTime();
        try {
            ChatResponse response = chatClient.prompt()
                    .user(prepared.content())
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, prepared.conversationId()))
                    .call()
                    .chatResponse();

            UsageDto usage = usageOf(response);
            aiMetrics.request("success");
            aiMetrics.duration(Duration.ofNanos(System.nanoTime() - start));
            if (usage != null) {
                recordUsage(prepared.userId(), usage);
            }
            return new ChatResponseDto(prepared.conversationId(), textOf(response), model, usage);
        } catch (RuntimeException e) {
            ChatErrorMapper.AiError error = ChatErrorMapper.toError(e);
            aiMetrics.request("error");
            aiMetrics.error(error.code());
            throw new ModelCallException(error, e);
        }
    }

    /**
     * 流式对话。
     *
     * @param prepared 准备好的上下文
     * @return 响应流
     */
    public Flux<ChatResponse> stream(PreparedChat prepared) {
        return chatClient.prompt()
                .user(prepared.content())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, prepared.conversationId()))
                .stream()
                .chatResponse();
    }

    /**
     * 列出用户会话。
     *
     * @param userId 用户 ID
     * @return 会话摘要
     */
    public List<ConversationSummaryDto> conversations(Long userId) {
        return conversationService.list(userId).stream()
                .map(summary -> new ConversationSummaryDto(
                        summary.conversationId(), summary.title(), summary.updatedAt()))
                .toList();
    }

    /**
     * 查询会话消息。
     *
     * @param userId         用户 ID
     * @param conversationId 会话 ID
     * @return 消息列表
     */
    public List<MessageDto> messages(Long userId, String conversationId) {
        if (!conversationService.belongsTo(userId, conversationId)) {
            throw new ConversationNotFoundException(conversationId);
        }
        return chatMemory.get(conversationId).stream()
                .map(message -> new MessageDto(message.getMessageType().name(), message.getText()))
                .toList();
    }

    /**
     * 记录一次成功用量的配额与指标。
     *
     * @param userId 用户 ID
     * @param usage  用量
     */
    public void recordUsage(Long userId, UsageDto usage) {
        quotaService.recordUsage(userId, usage.promptTokens(), usage.completionTokens());
        aiMetrics.tokens(usage.promptTokens(), usage.completionTokens());
    }

    /**
     * 提取响应文本。
     *
     * @param response 模型响应
     * @return 文本；无内容时返回空串
     */
    public static String textOf(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    /**
     * 提取响应用量。
     *
     * @param response 模型响应
     * @return 用量；缺失时返回 {@code null}
     */
    public static UsageDto usageOf(ChatResponse response) {
        if (response == null || response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return null;
        }
        Usage usage = response.getMetadata().getUsage();
        return new UsageDto(
                usage.getPromptTokens() == null ? 0 : usage.getPromptTokens(),
                usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens(),
                usage.getTotalTokens() == null ? 0 : usage.getTotalTokens());
    }

    private String resolveConversation(Long userId, String conversationId, String content) {
        if (StringUtils.isBlank(conversationId)) {
            String created = UUID.randomUUID().toString();
            conversationService.touch(userId, created, content);
            return created;
        }
        if (!conversationService.belongsTo(userId, conversationId)) {
            throw new ConversationNotFoundException(conversationId);
        }
        conversationService.touch(userId, conversationId, content);
        return conversationId;
    }
}
