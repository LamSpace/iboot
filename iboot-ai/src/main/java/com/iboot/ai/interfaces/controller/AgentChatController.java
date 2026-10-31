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

import com.iboot.ai.application.service.agent.AgentConfirmResult;
import com.iboot.ai.application.service.agent.AgentRunEvent;
import com.iboot.ai.application.service.agent.AgentRunService;
import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.kernel.security.SecurityUtils;
import com.iboot.ai.interfaces.dto.AgentChatRequest;
import com.iboot.ai.interfaces.dto.AgentConfirmRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 智能体对话与确认接口。
 *
 * <p>流式事件为 {@code meta → step → tool_result → token* → done | error}；
 * 写操作停在待确认时额外发送 {@code pending_approval} 并以 {@code done{status=PENDING_APPROVAL}} 收尾。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ai/agent")
@ConditionalOnProperty(prefix = "iboot.ai.agent", name = "enabled", havingValue = "true")
public class AgentChatController {

    private final AgentRunService runService;

    private final AiProperties aiProperties;

    /**
     * 创建控制器。
     *
     * @param runService   运行服务
     * @param aiProperties AI 配置
     */
    public AgentChatController(AgentRunService runService, AiProperties aiProperties) {
        this.runService = runService;
        this.aiProperties = aiProperties;
    }

    /**
     * 流式执行一次智能体运行。
     *
     * @param request 请求
     * @return SSE 连接
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('ai:agent:use')")
    public SseEmitter stream(@Valid @RequestBody AgentChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String token = SecurityUtils.getCurrentBearerToken();
        Set<String> permissions = SecurityUtils.getCurrentPermissions();
        String conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? UUID.randomUUID().toString()
                : request.conversationId();
        AgentContext context = AgentContext.start(userId, token, permissions, action -> { });

        SseEmitter emitter = new SseEmitter(aiProperties.getChat().getSseTimeoutMs());
        AtomicReference<Disposable> subscription = new AtomicReference<>();
        emitter.onCompletion(() -> dispose(subscription));
        emitter.onTimeout(() -> {
            dispose(subscription);
            emitter.complete();
        });
        emitter.onError(error -> dispose(subscription));

        subscription.set(runService.stream(context, request.agentId(), conversationId, request.content())
                .subscribe(event -> send(emitter, event),
                        error -> completeWithError(emitter, error),
                        emitter::complete));
        return emitter;
    }

    /**
     * 确认或拒绝待确认的写操作。
     *
     * @param runId   运行 ID
     * @param request 确认请求
     * @return 确认结果
     */
    @PostMapping("/runs/{runId}/confirm")
    @PreAuthorize("hasAuthority('ai:agent:use')")
    public AgentConfirmResult confirm(@PathVariable long runId, @RequestBody AgentConfirmRequest request) {
        return runService.confirm(runId, SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentBearerToken(),
                SecurityUtils.getCurrentPermissions(), request.approve());
    }

    private void send(SseEmitter emitter, AgentRunEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.name()).data(event.data()));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void completeWithError(SseEmitter emitter, Throwable error) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data(Map.of("code", "agent_error", "message", "智能体运行失败", "retryable", false)));
        } catch (IOException ignored) {
            // 客户端已断开
        }
        emitter.completeWithError(error);
    }

    private void dispose(AtomicReference<Disposable> subscription) {
        Disposable disposable = subscription.get();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
