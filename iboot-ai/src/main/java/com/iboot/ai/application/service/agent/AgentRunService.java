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

import com.iboot.ai.application.service.ModelCallException;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 智能体运行服务。
 *
 * <p>负责运行状态机、配额与指标记账、审计轨迹、以及写操作的两段式确认。
 * 第一段只产生待确认动作并结束流；确认后由本服务按记录的原始参数执行工具，
 * 授权事实只来自服务端记录，不来自模型输出。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
@ConditionalOnProperty(prefix = "iboot.ai.agent", name = "enabled", havingValue = "true")
public class AgentRunService {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final AgentRuntime runtime;

    private final AgentRepository repository;

    private final AgentToolRegistry tools;

    private final QuotaService quotaService;

    private final AiMetrics metrics;

    private final AiProperties properties;

    /**
     * 创建服务。
     *
     * @param runtime      运行时
     * @param repository   仓储
     * @param tools        工具注册表
     * @param quotaService 配额服务
     * @param metrics      指标
     * @param properties   AI 配置
     */
    public AgentRunService(AgentRuntime runtime, AgentRepository repository, AgentToolRegistry tools,
                           QuotaService quotaService, AiMetrics metrics, AiProperties properties) {
        this.runtime = runtime;
        this.repository = repository;
        this.tools = tools;
        this.quotaService = quotaService;
        this.metrics = metrics;
        this.properties = properties;
    }

    /**
     * 执行一次智能体运行。
     *
     * @param context        调用者上下文
     * @param agentId        智能体 ID
     * @param conversationId 会话 ID
     * @param message        用户消息
     * @return 运行事件流
     * @throws AgentNotFoundException 智能体不存在或已停用
     */
    public Flux<AgentRunEvent> stream(AgentContext context, long agentId, String conversationId, String message) {
        AgentDefinition agent = repository.findEnabledAgent(agentId)
                .orElseThrow(() -> new AgentNotFoundException("智能体不存在或已停用"));
        long runId = repository.insertRun(agentId, context.userId(), conversationId);
        RunState state = new RunState(runId, context.userId(), properties.getAgent().getApprovalTtlSeconds());
        AgentContext runContext = new AgentContext(context.userId(), context.bearerToken(), context.permissions(),
                runId, null, state.pending::set);

        Flux<AgentRunEvent> body = Flux.just(AgentRunEvent.meta(runId, conversationId, agentId))
                .concatWith(runtime.stream(runContext, agent, message)
                        .takeUntil(event -> state.pending.get() != null)
                        .concatMap(event -> handle(state, event)));

        return body
                .concatWith(Flux.defer(() -> state.finish(repository, metrics)))
                .onErrorResume(error -> Mono.just(state.fail(repository, metrics, error)));
    }

    /**
     * 确认或拒绝待确认的写操作。
     *
     * @param runId       运行 ID
     * @param userId      当前用户 ID
     * @param token       当前登录令牌
     * @param permissions 当前会话权限
     * @param approve     是否批准
     * @return 确认结果
     * @throws AgentNotFoundException    运行不存在或不属于当前用户
     * @throws AgentRunConflictException 状态不允许确认
     * @throws AgentRunExpiredException  待确认动作已过期
     */
    public AgentConfirmResult confirm(long runId, Long userId, String token, Set<String> permissions,
                                      boolean approve) {
        AgentRunRecord run = repository.findRun(runId)
                .orElseThrow(() -> new AgentNotFoundException("运行不存在"));
        if (!run.userId().equals(userId)) {
            throw new AgentNotFoundException("运行不存在");
        }
        if (run.status() == AgentRunStatus.PENDING_APPROVAL
                && run.pendingExpiresAt() != null
                && run.pendingExpiresAt().isBefore(LocalDateTime.now())) {
            repository.markFinished(runId, AgentRunStatus.EXPIRED, run.stepsJson(), run.stepCount(),
                    run.inputTokens(), run.outputTokens(), "approval_expired");
            throw new AgentRunExpiredException();
        }
        if (run.status() != AgentRunStatus.PENDING_APPROVAL) {
            throw new AgentRunConflictException("运行不在待确认状态");
        }
        if (!approve) {
            repository.markFinished(runId, AgentRunStatus.REJECTED,
                    appendStep(run.stepsJson(), approvalStep(run, "REJECTED", null)),
                    run.stepCount(), run.inputTokens(), run.outputTokens(), null);
            metrics.agentApproval("rejected");
            return new AgentConfirmResult(runId, AgentRunStatus.REJECTED.name(), "已拒绝执行");
        }
        if (repository.claimForApproval(runId, userId) == 0) {
            throw new AgentRunConflictException("运行已被处理");
        }
        AgentTool tool = tools.find(run.pendingTool())
                .orElseThrow(() -> new AgentRunConflictException("待确认工具已不可用"));
        Map<String, Object> arguments = parseArguments(run.pendingArgsJson());
        AgentContext execContext = new AgentContext(userId, token, permissions, runId,
                new ApprovalGrant(run.pendingTool(), run.pendingArgsDigest()), action -> { });
        long start = System.nanoTime();
        ToolOutcome outcome = tool.execute(execContext, arguments);
        metrics.agentToolCall(tool.name(), outcome.status().name().toLowerCase(),
                Duration.ofNanos(System.nanoTime() - start));
        if (outcome.status() == ToolOutcome.Status.PENDING) {
            metrics.agentBypassBlocked();
        }
        AgentRunStatus status = outcome.status() == ToolOutcome.Status.OK
                ? AgentRunStatus.COMPLETED : AgentRunStatus.FAILED;
        repository.markFinished(runId, status,
                appendStep(run.stepsJson(), approvalStep(run, "APPROVED", outcome.status().name())),
                run.stepCount(), run.inputTokens(), run.outputTokens(),
                status == AgentRunStatus.FAILED ? "tool_failed" : null);
        metrics.agentApproval("approved");
        return new AgentConfirmResult(runId, status.name(), outcome.content());
    }

    private Mono<AgentRunEvent> handle(RunState state, AgentRuntimeEvent event) {
        return switch (event.type()) {
            case MODEL_START -> {
                quotaService.assertWithinQuota(state.userId);
                yield Mono.empty();
            }
            case TEXT -> Mono.just(AgentRunEvent.token(event.text()));
            case TOOL_CALL -> {
                int seq = state.nextSeq();
                state.steps.add(step("tool_call", event.toolName()));
                state.toolStartedAt = System.nanoTime();
                state.currentTool = event.toolName();
                yield Mono.just(AgentRunEvent.step(seq, event.toolName()));
            }
            case TOOL_RESULT -> {
                int seq = state.nextSeq();
                String toolState = state.pending.get() != null
                        ? "PENDING"
                        : (event.toolState() == null ? "UNKNOWN" : event.toolState());
                state.steps.add(step("tool_result", event.toolName(), toolState));
                long took = state.toolStartedAt == 0 ? 0 : System.nanoTime() - state.toolStartedAt;
                metrics.agentToolCall(event.toolName(), toolState.toLowerCase(), Duration.ofNanos(took));
                if (state.currentTool == null) {
                    state.currentTool = event.toolName();
                }
                yield Mono.just(AgentRunEvent.toolResult(seq, event.toolName(), toolState));
            }
            case USAGE -> {
                state.inputTokens += Math.max(0, event.inputTokens());
                state.outputTokens += Math.max(0, event.outputTokens());
                quotaService.recordUsage(state.userId, event.inputTokens(), event.outputTokens());
                metrics.tokens(event.inputTokens(), event.outputTokens());
                yield Mono.empty();
            }
            case STEP_LIMIT -> Mono.error(new StepLimitException());
            case ERROR -> Mono.error(new AgentRuntimeFailureException(event.errorCode()));
            case COMPLETED -> Mono.empty();
        };
    }

    private Map<String, Object> parseArguments(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        JsonNode node = MAPPER.readTree(json);
        Map<String, Object> arguments = new LinkedHashMap<>();
        node.properties().forEach(entry -> arguments.put(entry.getKey(), MAPPER.treeToValue(entry.getValue(),
                Object.class)));
        return arguments;
    }

    private Map<String, Object> step(String type, String tool, String state) {
        Map<String, Object> step = step(type, tool);
        step.put("state", state);
        return step;
    }

    private Map<String, Object> step(String type, String tool) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("type", type);
        if (tool != null) {
            step.put("tool", tool);
        }
        return step;
    }

    private Map<String, Object> approvalStep(AgentRunRecord run, String decision, String outcome) {
        Map<String, Object> step = step("approval", run.pendingTool());
        step.put("decision", decision);
        if (outcome != null) {
            step.put("outcome", outcome);
        }
        return step;
    }

    private String appendStep(String existing, Map<String, Object> step) {
        ArrayNode array = existing == null || existing.isBlank()
                ? MAPPER.createArrayNode()
                : (ArrayNode) MAPPER.readTree(existing);
        array.add(MAPPER.valueToTree(step));
        return MAPPER.writeValueAsString(array);
    }

    /**
     * 一次运行的累计状态。
     */
    private static final class RunState {

        private final long runId;

        private final Long userId;

        private final long approvalTtlSeconds;

        private final List<Map<String, Object>> steps = Collections.synchronizedList(new ArrayList<>());

        private final AtomicReference<PendingAction> pending = new AtomicReference<>();

        private int seq;

        private long inputTokens;

        private long outputTokens;

        private long toolStartedAt;

        private String currentTool;

        private RunState(long runId, Long userId, long approvalTtlSeconds) {
            this.runId = runId;
            this.userId = userId;
            this.approvalTtlSeconds = approvalTtlSeconds;
        }

        private int nextSeq() {
            return ++seq;
        }

        private String stepsJson() {
            return MAPPER.writeValueAsString(steps);
        }

        private Flux<AgentRunEvent> finish(AgentRepository repository, AiMetrics metrics) {
            PendingAction action = pending.get();
            if (action != null) {
                LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(Math.max(1, approvalTtlSeconds));
                repository.markPending(runId, action, expiresAt, seq, stepsJson(), inputTokens, outputTokens);
                metrics.agentRun("pending");
                metrics.agentSteps(seq);
                return Flux.just(
                        AgentRunEvent.pendingApproval(runId, action.toolName(), action.argsDigest()),
                        AgentRunEvent.done(AgentRunStatus.PENDING_APPROVAL.name(), inputTokens, outputTokens));
            }
            repository.markFinished(runId, AgentRunStatus.COMPLETED, stepsJson(), seq,
                    inputTokens, outputTokens, null);
            metrics.agentRun("completed");
            metrics.agentSteps(seq);
            return Flux.just(AgentRunEvent.done(AgentRunStatus.COMPLETED.name(), inputTokens, outputTokens));
        }

        private AgentRunEvent fail(AgentRepository repository, AiMetrics metrics, Throwable error) {
            String code = errorCode(error);
            repository.markFinished(runId, AgentRunStatus.FAILED, stepsJson(), seq,
                    inputTokens, outputTokens, code);
            metrics.agentRun("error");
            metrics.error(code);
            return AgentRunEvent.error(code, messageOf(code), !"step_limit".equals(code));
        }

        private static String errorCode(Throwable error) {
            if (error instanceof QuotaExceededException) {
                return "quota_exceeded";
            }
            if (error instanceof StepLimitException) {
                return "step_limit";
            }
            if (error instanceof AgentRuntimeFailureException failure && failure.code != null) {
                return failure.code;
            }
            if (error instanceof ModelCallException) {
                return "model_error";
            }
            return "agent_error";
        }

        private static String messageOf(String code) {
            return switch (code) {
                case "quota_exceeded" -> "配额不足，运行已终止";
                case "step_limit" -> "达到步数上限，运行已终止";
                case "model_error" -> "模型不可用，运行已终止";
                default -> "智能体运行失败";
            };
        }
    }

    /**
     * 步数超限。
     */
    private static final class StepLimitException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    /**
     * 运行时返回的失败。
     */
    private static final class AgentRuntimeFailureException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final String code;

        private AgentRuntimeFailureException(String code) {
            super(code);
            this.code = code;
        }
    }
}
