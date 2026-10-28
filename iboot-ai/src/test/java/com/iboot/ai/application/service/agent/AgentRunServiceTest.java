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

package com.iboot.ai.application.service.agent;

import com.iboot.ai.application.service.QuotaExceededException;
import com.iboot.ai.application.service.QuotaService;
import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.AgentDefinition;
import com.iboot.ai.domain.agent.AgentRunRecord;
import com.iboot.ai.domain.agent.AgentRunStatus;
import com.iboot.ai.domain.agent.AgentRuntimeEvent;
import com.iboot.ai.domain.agent.AgentTool;
import com.iboot.ai.domain.agent.ApprovalGrant;
import com.iboot.ai.domain.agent.PendingAction;
import com.iboot.ai.domain.agent.ToolOutcome;
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import com.iboot.ai.infrastructure.persistence.AgentRepository;
import com.iboot.ai.infrastructure.tool.AgentToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AgentRunService} 的单元测试。
 *
 * <p>用假运行时驱动事件流，覆盖只读往返、待确认中断、配额按调用检查、确认执行、
 * 过期与重放拒绝、审计不落敏感值。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class AgentRunServiceTest {

    private static final long AGENT_ID = 1L;

    private static final long USER_ID = 7L;

    private static final long RUN_ID = 99L;

    private static final String CONVERSATION = "c-1";

    private AgentRepository repository;

    private AgentToolRegistry tools;

    private QuotaService quotaService;

    private AiProperties properties;

    private AgentRunService service;

    @BeforeEach
    void setUp() {
        repository = mock(AgentRepository.class);
        tools = mock(AgentToolRegistry.class);
        quotaService = mock(QuotaService.class);
        properties = new AiProperties();
        properties.getAgent().setEnabled(true);
        properties.getAgent().setApprovalTtlSeconds(300);
        when(repository.findEnabledAgent(AGENT_ID)).thenReturn(Optional.of(definition()));
        when(repository.insertRun(AGENT_ID, USER_ID, CONVERSATION)).thenReturn(RUN_ID);
        service = service(events -> Flux.empty());
    }

    /**
     * 只读工具往返产生完整事件序列，并按一次调用计量。
     */
    @Test
    void streamsReadOnlyRoundTrip() {
        service = service(events -> Flux.just(
                AgentRuntimeEvent.modelStart(),
                AgentRuntimeEvent.text("你好"),
                AgentRuntimeEvent.toolCall("query_users"),
                AgentRuntimeEvent.toolResult("query_users", "SUCCESS"),
                AgentRuntimeEvent.usage(10, 5),
                AgentRuntimeEvent.completed()));

        List<AgentRunEvent> out = service.stream(context(), AGENT_ID, CONVERSATION, "查用户").collectList().block();

        assertThat(out).extracting(AgentRunEvent::name)
                .containsExactly("meta", "token", "step", "tool_result", "done");
        assertThat(out.getLast().data()).containsEntry("status", "COMPLETED");
        assertThat((Map<String, Object>) out.getLast().data().get("usage"))
                .containsEntry("promptTokens", 10L).containsEntry("completionTokens", 5L);
        verify(quotaService).assertWithinQuota(USER_ID);
        verify(quotaService).recordUsage(USER_ID, 10, 5);
        verify(repository).markFinished(eq(RUN_ID), eq(AgentRunStatus.COMPLETED), anyString(), eq(2),
                eq(10L), eq(5L), isNull());
    }

    /**
     * 写工具触发待确认后立即收尾，不继续生成文本。
     */
    @Test
    void stopsAtPendingApproval() {
        service = service(events -> Flux.concat(
                Flux.just(AgentRuntimeEvent.modelStart(), AgentRuntimeEvent.toolCall("trigger_backup")),
                Flux.defer(() -> {
                    events.pendingSink().submit(new PendingAction("trigger_backup", "{}", "digest-1"));
                    return Flux.just(AgentRuntimeEvent.toolResult("trigger_backup", "SUCCESS"));
                }),
                Flux.just(AgentRuntimeEvent.text("不应出现"))));

        List<AgentRunEvent> out = service.stream(context(), AGENT_ID, CONVERSATION, "备份").collectList().block();

        assertThat(out).extracting(AgentRunEvent::name)
                .containsExactly("meta", "step", "tool_result", "pending_approval", "done");
        assertThat(out.getLast().data()).containsEntry("status", "PENDING_APPROVAL");
        assertThat(out).noneMatch(event -> "token".equals(event.name()));
        verify(repository).markPending(eq(RUN_ID), any(PendingAction.class), any(), eq(2), anyString(),
                eq(0L), eq(0L));
        verify(repository, never()).markFinished(anyLong(), any(), anyString(), anyInt(), anyLong(), anyLong(),
                any());
    }

    /**
     * 每次模型调用前检查配额。
     */
    @Test
    void checksQuotaPerModelCall() {
        service = service(events -> Flux.just(
                AgentRuntimeEvent.modelStart(),
                AgentRuntimeEvent.usage(3, 1),
                AgentRuntimeEvent.modelStart(),
                AgentRuntimeEvent.usage(4, 2),
                AgentRuntimeEvent.completed()));

        service.stream(context(), AGENT_ID, CONVERSATION, "问题").collectList().block();

        verify(quotaService, times(2)).assertWithinQuota(USER_ID);
        verify(quotaService, times(2)).recordUsage(eq(USER_ID), anyInt(), anyInt());
    }

    /**
     * 确认后按记录参数执行工具，并把授权传给工具。
     */
    @Test
    void confirmExecutesWithStoredGrant() {
        when(repository.findRun(RUN_ID)).thenReturn(Optional.of(pendingRun(LocalDateTime.now().plusMinutes(5))));
        when(repository.claimForApproval(RUN_ID, USER_ID)).thenReturn(1);
        AtomicReference<AgentContext> captured = new AtomicReference<>();
        AgentTool tool = mock(AgentTool.class);
        when(tool.name()).thenReturn("trigger_backup");
        when(tool.execute(any(), any())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return ToolOutcome.ok("备份已触发");
        });
        when(tools.find("trigger_backup")).thenReturn(Optional.of(tool));

        AgentConfirmResult result = service.confirm(RUN_ID, USER_ID, "tk-1",
                Set.of("backup:record:create"), true);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(captured.get().grant().matches("trigger_backup", "digest-1")).isTrue();
        verify(repository).markFinished(eq(RUN_ID), eq(AgentRunStatus.COMPLETED), anyString(), anyInt(),
                anyLong(), anyLong(), isNull());
    }

    /**
     * 过期的待确认动作转为 EXPIRED，不执行工具。
     */
    @Test
    void confirmRejectsExpired() {
        when(repository.findRun(RUN_ID)).thenReturn(Optional.of(pendingRun(LocalDateTime.now().minusMinutes(1))));

        assertThatThrownBy(() -> service.confirm(RUN_ID, USER_ID, "tk-1", Set.of(), true))
                .isInstanceOf(AgentRunExpiredException.class);
        verify(repository).markFinished(eq(RUN_ID), eq(AgentRunStatus.EXPIRED), any(), anyInt(), anyLong(),
                anyLong(), eq("approval_expired"));
        verify(tools, never()).find(anyString());
    }

    /**
     * 他人的运行按不存在处理。
     */
    @Test
    void confirmRejectsOtherUser() {
        when(repository.findRun(RUN_ID)).thenReturn(Optional.of(pendingRun(LocalDateTime.now().plusMinutes(5))));

        assertThatThrownBy(() -> service.confirm(RUN_ID, 8L, "tk-1", Set.of(), true))
                .isInstanceOf(AgentNotFoundException.class);
    }

    /**
     * 已被处理的运行不能重复确认。
     */
    @Test
    void confirmRejectsReplay() {
        when(repository.findRun(RUN_ID)).thenReturn(Optional.of(pendingRun(LocalDateTime.now().plusMinutes(5))));
        when(repository.claimForApproval(RUN_ID, USER_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.confirm(RUN_ID, USER_ID, "tk-1", Set.of(), true))
                .isInstanceOf(AgentRunConflictException.class);
        verify(tools, never()).find(anyString());
    }

    /**
     * 审计轨迹只保存步骤摘要，不包含模型输出的敏感文本。
     */
    @Test
    void auditDoesNotPersistSensitiveText() {
        service = service(events -> Flux.just(
                AgentRuntimeEvent.text("password=super-secret"),
                AgentRuntimeEvent.toolCall("query_users"),
                AgentRuntimeEvent.toolResult("query_users", "SUCCESS"),
                AgentRuntimeEvent.completed()));

        service.stream(context(), AGENT_ID, CONVERSATION, "问题").collectList().block();

        ArgumentCaptor<String> steps = ArgumentCaptor.forClass(String.class);
        verify(repository).markFinished(anyLong(), any(), steps.capture(), anyInt(), anyLong(), anyLong(), any());
        assertThat(steps.getValue()).doesNotContain("super-secret").doesNotContain("password");
    }


    /**
     * 配额不足以可辨识原因结束，不出现完成事件。
     */
    @Test
    void failsWithQuotaCode() {
        doThrow(new QuotaExceededException("user", "配额不足")).when(quotaService).assertWithinQuota(USER_ID);
        service = service(events -> Flux.just(AgentRuntimeEvent.modelStart(),
                AgentRuntimeEvent.completed()));

        List<AgentRunEvent> out = service.stream(context(), AGENT_ID, CONVERSATION, "问题").collectList().block();

        assertThat(out.getLast().name()).isEqualTo("error");
        assertThat(out.getLast().data()).containsEntry("code", "quota_exceeded");
        verify(repository).markFinished(eq(RUN_ID), eq(AgentRunStatus.FAILED), anyString(), anyInt(),
                anyLong(), anyLong(), eq("quota_exceeded"));
    }

    /**
     * 步数超限以 step_limit 结束。
     */
    @Test
    void failsWithStepLimitCode() {
        service = service(events -> Flux.just(AgentRuntimeEvent.stepLimit()));

        List<AgentRunEvent> out = service.stream(context(), AGENT_ID, CONVERSATION, "问题").collectList().block();

        assertThat(out.getLast().data()).containsEntry("code", "step_limit");
    }

    /**
     * 运行时错误保留原因码。
     */
    @Test
    void failsWithRuntimeCode() {
        service = service(events -> Flux.just(AgentRuntimeEvent.error("model_error")));

        List<AgentRunEvent> out = service.stream(context(), AGENT_ID, CONVERSATION, "问题").collectList().block();

        assertThat(out.getLast().data()).containsEntry("code", "model_error");
    }

    private AgentRunService service(java.util.function.Function<AgentContext, Flux<AgentRuntimeEvent>> runtime) {
        AgentRuntime agentRuntime = (ctx, agent, message) -> runtime.apply(ctx);
        return new AgentRunService(agentRuntime, repository, tools, quotaService,
                new AiMetrics(new SimpleMeterRegistry(), "test-model"), properties);
    }

    private AgentContext context() {
        return new AgentContext(USER_ID, "tk-1", Set.of("user:list"), null, null, action -> { });
    }

    private AgentDefinition definition() {
        return new AgentDefinition(AGENT_ID, "ops", "运维助手", "你是运维助手", null,
                List.of("query_users", "trigger_backup"));
    }

    private AgentRunRecord pendingRun(LocalDateTime expiresAt) {
        return new AgentRunRecord(RUN_ID, CONVERSATION, AGENT_ID, USER_ID, AgentRunStatus.PENDING_APPROVAL,
                "trigger_backup", "{}", "digest-1", expiresAt, 2, "[]", 10L, 5L, null,
                LocalDateTime.now().minusMinutes(1), null);
    }
}
