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
}
