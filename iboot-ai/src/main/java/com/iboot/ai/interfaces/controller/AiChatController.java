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

package com.iboot.ai.interfaces.controller;

import com.iboot.ai.application.service.ChatApplicationService;
import com.iboot.ai.application.service.ChatErrorMapper;
import com.iboot.ai.application.service.QuotaService;
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import com.iboot.ai.infrastructure.security.SecurityUtils;
import com.iboot.ai.interfaces.dto.ChatRequest;
import com.iboot.ai.interfaces.dto.ChatResponseDto;
import com.iboot.ai.interfaces.dto.ConversationSummaryDto;
import com.iboot.ai.interfaces.dto.MessageDto;
import com.iboot.ai.interfaces.dto.UsageDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 对话接口。
 *
 * <p>流式接口使用 POST + SSE，事件约定为 {@code meta → token* → done}，异常时发送
 * {@code error} 且不发送 {@code done}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final ChatApplicationService chatService;

    private final QuotaService quotaService;

    private final AiMetrics aiMetrics;

    private final AiProperties aiProperties;

    private final String model;

    /**
     * 创建控制器。
     *
     * @param chatService  对话服务
     * @param quotaService 配额服务
     * @param aiMetrics    指标
     * @param aiProperties AI 配置
     * @param model        模型标识
     */
    public AiChatController(ChatApplicationService chatService,
                            QuotaService quotaService,
                            AiMetrics aiMetrics,
                            AiProperties aiProperties,
                            @Value("${spring.ai.openai.chat.options.model}") String model) {
        this.chatService = chatService;
        this.quotaService = quotaService;
        this.aiMetrics = aiMetrics;
        this.aiProperties = aiProperties;
        this.model = model;
    }

    /**
     * 非流式对话。
     *
     * @param request 对话请求
     * @return 回答
     */
    @PostMapping("/chat")
    public ChatResponseDto chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chatOnce(SecurityUtils.getCurrentUserId(),
                request.conversationId(), request.content());
    }

    /**
     * 流式对话。
     *
     * @param request 对话请求
     * @return SSE 连接
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ChatApplicationService.PreparedChat prepared =
                chatService.prepare(userId, request.conversationId(), request.content());

        SseEmitter emitter = new SseEmitter(aiProperties.getChat().getSseTimeoutMs());
        SubscriptionRegistry subscription = new SubscriptionRegistry();
        AtomicBoolean firstToken = new AtomicBoolean(true);
        AtomicReference<UsageDto> lastUsage = new AtomicReference<>();
        long start = System.nanoTime();

        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(() -> {
            subscription.dispose();
            emitter.complete();
        });
        emitter.onError(error -> subscription.dispose());

        try {
            emitter.send(SseEmitter.event()
                    .name("meta")
                    .data(Map.of("conversationId", prepared.conversationId())));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        subscription.attach(chatService.stream(prepared).subscribe(
                response -> onChunk(emitter, subscription, firstToken, lastUsage, response, start),
                error -> onError(emitter, error, start),
                () -> onDone(emitter, prepared, lastUsage.get(), start)));
        return emitter;
    }

    /**
     * 列出当前用户的会话。
     *
     * @return 会话摘要
     */
    @GetMapping("/conversations")
    public List<ConversationSummaryDto> conversations() {
        return chatService.conversations(SecurityUtils.getCurrentUserId());
    }

    /**
     * 查询会话消息。
     *
     * @param conversationId 会话 ID
     * @return 消息列表
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageDto> messages(@PathVariable String conversationId) {
        return chatService.messages(SecurityUtils.getCurrentUserId(), conversationId);
    }

    /**
     * 查询当前用户当天用量。
     *
     * @return 用量快照
     */
    @GetMapping("/usage")
    public QuotaService.UsageSnapshot usage() {
        return quotaService.usage(SecurityUtils.getCurrentUserId());
    }

    private void onChunk(SseEmitter emitter, SubscriptionRegistry subscription,
                         AtomicBoolean firstToken, AtomicReference<UsageDto> lastUsage,
                         ChatResponse response, long start) {
        UsageDto usage = ChatApplicationService.usageOf(response);
        if (usage != null) {
            lastUsage.set(usage);
        }
        String delta = ChatApplicationService.textOf(response);
        if (delta.isEmpty()) {
            return;
        }
        if (firstToken.compareAndSet(true, false)) {
            aiMetrics.firstToken(Duration.ofNanos(System.nanoTime() - start));
        }
        try {
            emitter.send(SseEmitter.event().name("token").data(Map.of("delta", delta)));
        } catch (IOException e) {
            subscription.dispose();
            emitter.completeWithError(e);
        }
    }

    private void onError(SseEmitter emitter, Throwable error, long start) {
        ChatErrorMapper.AiError aiError = ChatErrorMapper.toError(error);
        log.warn("流式对话失败：{}", aiError.code());
        aiMetrics.request("error");
        aiMetrics.error(aiError.code());
        aiMetrics.duration(Duration.ofNanos(System.nanoTime() - start));
        try {
            emitter.send(SseEmitter.event().name("error").data(aiError));
        } catch (IOException ignored) {
            // 客户端已断开，无需再写
        }
        emitter.complete();
    }

    private void onDone(SseEmitter emitter, ChatApplicationService.PreparedChat prepared,
                        UsageDto usage, long start) {
        UsageDto effective = usage == null ? new UsageDto(0, 0, 0) : usage;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("conversationId", prepared.conversationId());
        data.put("model", model);
        data.put("usage", effective);
        try {
            emitter.send(SseEmitter.event().name("done").data(data));
        } catch (IOException ignored) {
            // 客户端已断开，无需再写
        }
        emitter.complete();
        aiMetrics.request("success");
        aiMetrics.duration(Duration.ofNanos(System.nanoTime() - start));
        chatService.recordUsage(prepared.userId(), effective);
    }

}
