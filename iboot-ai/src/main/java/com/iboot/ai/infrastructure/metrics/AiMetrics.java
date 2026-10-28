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

package com.iboot.ai.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 业务指标。
 *
 * <p>补充 Spring AI 自带 observation 之外的业务语义：请求结果、token、首 token 延迟、错误原因
 * 与配额拒绝。所有指标都以 {@code iboot.ai.} 为前缀，便于 Prometheus 抓取与告警。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class AiMetrics {

    private final MeterRegistry registry;

    private final String model;

    /**
     * 创建指标组件。
     *
     * @param registry Micrometer 注册表
     * @param model    当前对话模型
     */
    public AiMetrics(MeterRegistry registry, @Value("${spring.ai.openai.chat.options.model}") String model) {
        this.registry = registry;
        this.model = model;
    }

    /**
     * 记录一次对话请求结果。
     *
     * @param outcome 结果标签
     */
    public void request(String outcome) {
        Counter.builder("iboot.ai.chat.requests")
                .tag("model", model)
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    /**
     * 记录 token 用量。
     *
     * @param promptTokens     输入 token
     * @param completionTokens 输出 token
     */
    public void tokens(int promptTokens, int completionTokens) {
        Counter.builder("iboot.ai.tokens").tag("type", "prompt").register(registry).increment(promptTokens);
        Counter.builder("iboot.ai.tokens").tag("type", "completion").register(registry).increment(completionTokens);
    }

    /**
     * 记录一次知识库问答结果。
     *
     * @param outcome  结果：success、error 或 no_hit
     * @param degraded 是否降级
     */
    public void ragRequest(String outcome, boolean degraded) {
        Counter.builder("iboot.ai.rag.requests")
                .tag("outcome", outcome)
                .tag("degraded", Boolean.toString(degraded))
                .register(registry)
                .increment();
    }

    /**
     * 记录一次知识库空结果。
     */
    public void ragEmptyResults() {
        Counter.builder("iboot.ai.rag.empty_results").register(registry).increment();
    }

    /**
     * 记录 OCR 页数。
     *
     * @param pages   页数
     * @param outcome 结果：success 或 failed
     */
    public void ocrPages(int pages, String outcome) {
        Counter.builder("iboot.ai.rag.ocr.pages")
                .tag("outcome", outcome)
                .register(registry)
                .increment(pages);
    }

    /**
     * 记录一次 OCR 配额拒绝。
     */
    public void ocrQuotaRejected() {
        Counter.builder("iboot.ai.rag.ocr.quota_rejected").register(registry).increment();
    }

    /**
     * 记录 OCR 估算 token 用量。
     *
     * @param tokens token 数
     */
    public void ocrTokens(long tokens) {
        Counter.builder("iboot.ai.rag.ocr.tokens").register(registry).increment(tokens);
    }

    /**
     * 记录一次 OCR 耗时。
     *
     * @param duration 耗时
     * @param outcome  结果：success 或 failed
     */
    public void ocrDuration(Duration duration, String outcome) {
        Timer.builder("iboot.ai.rag.ocr.duration")
                .tag("outcome", outcome)
                .publishPercentiles(0.5, 0.95)
                .register(registry)
                .record(duration);
    }

    /**
     * 记录 Embedding token 用量。
     *
     * @param tokens token 数
     * @param stage  阶段：document 或 query
     */
    public void embeddingTokens(long tokens, String stage) {
        Counter.builder("iboot.ai.rag.embedding.tokens")
                .tag("stage", stage)
                .register(registry)
                .increment(tokens);
    }

    /**
     * 记录首 token 延迟。
     *
     * @param duration 延迟
     */
    public void firstToken(Duration duration) {
        Timer.builder("iboot.ai.first_token")
                .tag("model", model)
                .publishPercentiles(0.5, 0.95)
                .register(registry)
                .record(duration);
    }

    /**
     * 记录一次完整对话耗时。
     *
     * @param duration 耗时
     */
    public void duration(Duration duration) {
        Timer.builder("iboot.ai.chat.duration")
                .tag("model", model)
                .publishPercentiles(0.5, 0.95)
                .register(registry)
                .record(duration);
    }

    /**
     * 记录一次失败。
     *
     * @param reason 失败原因
     */
    public void error(String reason) {
        Counter.builder("iboot.ai.errors").tag("reason", reason).register(registry).increment();
    }

    /**
     * 记录一次配额拒绝。
     *
     * @param scope 拒绝范围（global / user）
     */
    public void quotaRejected(String scope) {
        Counter.builder("iboot.ai.quota.rejected").tag("scope", scope).register(registry).increment();
    }

    /**
     * 记录一次智能体运行结果。
     *
     * @param outcome 结果标签（completed / pending / error）
     */
    public void agentRun(String outcome) {
        Counter.builder("iboot.ai.agent.runs").tag("outcome", outcome).register(registry).increment();
    }

    /**
     * 记录一次工具调用。
     *
     * @param tool    工具名
     * @param outcome 结果标签
     * @param took    耗时
     */
    public void agentToolCall(String tool, String outcome, Duration took) {
        Counter.builder("iboot.ai.agent.tool.calls")
                .tag("tool", tool == null ? "unknown" : tool)
                .tag("outcome", outcome)
                .register(registry)
                .increment();
        Timer.builder("iboot.ai.agent.tool.duration")
                .tag("tool", tool == null ? "unknown" : tool)
                .register(registry)
                .record(took);
    }

    /**
     * 记录一次确认决策。
     *
     * @param decision 决策标签（approved / rejected）
     */
    public void agentApproval(String decision) {
        Counter.builder("iboot.ai.agent.approvals").tag("decision", decision).register(registry).increment();
    }

    /**
     * 记录一次被拦截的写操作绕过尝试。
     */
    public void agentBypassBlocked() {
        Counter.builder("iboot.ai.agent.approval.bypass.blocked").register(registry).increment();
    }

    /**
     * 记录一次运行的模型调用步数。
     *
     * @param steps 步数
     */
    public void agentSteps(int steps) {
        Counter.builder("iboot.ai.agent.steps").register(registry).increment(steps);
    }

    /**
     * 记录一次图查询。
     *
     * @param template 查询模板
     * @param outcome  结果标签
     * @param took     耗时
     */
    public void graphQuery(String template, String outcome, Duration took) {
        Counter.builder("iboot.ai.graph.queries")
                .tag("template", template == null ? "unknown" : template)
                .tag("outcome", outcome)
                .register(registry)
                .increment();
        Timer.builder("iboot.ai.graph.query.duration")
                .tag("template", template == null ? "unknown" : template)
                .register(registry)
                .record(took);
    }

    /**
     * 记录一次图同步。
     *
     * @param outcome 结果标签
     * @param took    耗时
     */
    public void graphSync(String outcome, Duration took) {
        Counter.builder("iboot.ai.graph.sync").tag("outcome", outcome).register(registry).increment();
        Timer.builder("iboot.ai.graph.sync.duration").tag("outcome", outcome).register(registry).record(took);
    }
}
