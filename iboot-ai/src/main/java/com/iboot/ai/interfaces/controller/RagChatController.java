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
import com.iboot.ai.application.service.RagAnswer;
import com.iboot.ai.application.service.RagChatService;
import com.iboot.ai.application.service.RagPreparation;
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import com.iboot.ai.infrastructure.security.PermissionScope;
import com.iboot.ai.infrastructure.security.PermissionScopeClient;
import com.iboot.ai.infrastructure.security.SecurityUtils;
import com.iboot.ai.interfaces.dto.RagChatRequest;
import com.iboot.ai.interfaces.dto.UsageDto;
import jakarta.validation.Valid;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 知识库 RAG 问答接口。
 *
 * <p>流式事件为 {@code meta → citations → token* → done}，异常时为 {@code error}。
 * 与普通对话接口完全分离，未选择知识库的普通对话行为不变。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ai/rag")
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagChatController {

    private final RagChatService ragChatService;

    private final PermissionScopeClient permissionScopeClient;

    private final AiProperties aiProperties;

    private final AiMetrics aiMetrics;

    private final String model;

    /**
     * 创建控制器。
     *
     * @param ragChatService        RAG 问答服务
     * @param permissionScopeClient 权限范围客户端
     * @param aiProperties          AI 配置
     * @param aiMetrics             指标
     * @param model                 模型标识
     */
    public RagChatController(RagChatService ragChatService, PermissionScopeClient permissionScopeClient,
                             AiProperties aiProperties, AiMetrics aiMetrics,
                             @Value("${spring.ai.openai.chat.options.model}") String model) {
        this.ragChatService = ragChatService;
        this.permissionScopeClient = permissionScopeClient;
        this.aiProperties = aiProperties;
        this.aiMetrics = aiMetrics;
        this.model = model;
    }

    /**
     * 非流式知识库问答。
     *
     * @param request 请求
     * @return 回答
     */
    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('ai:kb:query')")
    public RagAnswer chat(@Valid @RequestBody RagChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        PermissionScope scope = permissionScopeClient.fetchCurrentUserScope();
        RagPreparation prepared = ragChatService.prepare(userId, request.conversationId(), request.content(),
                request.knowledgeBaseId(), scope);
        return ragChatService.answer(prepared);
    }

    /**
     * 流式知识库问答。
     *
     * @param request 请求
     * @return SSE 连接
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('ai:kb:query')")
    public SseEmitter stream(@Valid @RequestBody RagChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        PermissionScope scope = permissionScopeClient.fetchCurrentUserScope();
        RagPreparation prepared = ragChatService.prepare(userId, request.conversationId(), request.content(),
                request.knowledgeBaseId(), scope);

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
            emitter.send(SseEmitter.event().name("citations").data(prepared.citations()));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        if (prepared.noHit()) {
            aiMetrics.ragRequest("no_hit", prepared.degraded());
            try {
                emitter.send(SseEmitter.event().name("token")
                        .data(Map.of("delta", RagChatService.NO_HIT_ANSWER)));
                emitter.send(SseEmitter.event().name("done")
                        .data(doneData(prepared, new UsageDto(0, 0, 0))));
            } catch (IOException ignored) {
                // 客户端已断开
            }
            emitter.complete();
            return emitter;
        }

        subscription.attach(ragChatService.stream(prepared).subscribe(
                response -> onChunk(emitter, subscription, firstToken, lastUsage, response, start),
                error -> onError(emitter, error, prepared.degraded(), start),
                () -> onDone(emitter, prepared, lastUsage.get(), start)));
        return emitter;
    }

    private void onChunk(SseEmitter emitter, SubscriptionRegistry subscription, AtomicBoolean firstToken,
                         AtomicReference<UsageDto> lastUsage, ChatResponse response, long start) {
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

    private void onError(SseEmitter emitter, Throwable error, boolean degraded, long start) {
        ChatErrorMapper.AiError aiError = ChatErrorMapper.toError(error);
        aiMetrics.request("error");
        aiMetrics.error(aiError.code());
        aiMetrics.duration(Duration.ofNanos(System.nanoTime() - start));
        aiMetrics.ragRequest("error", degraded);
        try {
            emitter.send(SseEmitter.event().name("error").data(aiError));
        } catch (IOException ignored) {
            // 客户端已断开
        }
        emitter.complete();
    }

    private void onDone(SseEmitter emitter, RagPreparation prepared, UsageDto usage, long start) {
        UsageDto effective = usage == null ? new UsageDto(0, 0, 0) : usage;
        ragChatService.recordUsage(prepared.userId(), effective);
        aiMetrics.request("success");
        aiMetrics.duration(Duration.ofNanos(System.nanoTime() - start));
        aiMetrics.ragRequest("success", prepared.degraded());
        try {
            emitter.send(SseEmitter.event().name("done").data(doneData(prepared, effective)));
        } catch (IOException ignored) {
            // 客户端已断开
        }
        emitter.complete();
    }

    private Map<String, Object> doneData(RagPreparation prepared, UsageDto usage) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("conversationId", prepared.conversationId());
        data.put("model", model);
        data.put("usage", usage);
        data.put("degraded", prepared.degraded());
        return data;
    }
}
