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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 指标记录的单元测试。
 */
class AiMetricsTest {

    @Test
    void recordsCountersTimersAndTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiMetrics metrics = new AiMetrics(registry, "qwen3.8-flash");

        metrics.request("success");
        metrics.tokens(10, 5);
        metrics.firstToken(Duration.ofMillis(120));
        metrics.duration(Duration.ofMillis(800));
        metrics.error("timeout");
        metrics.quotaRejected("user");

        assertThat(registry.get("iboot.ai.chat.requests").tag("outcome", "success").counter().count())
                .isEqualTo(1.0);
        assertThat(registry.get("iboot.ai.tokens").tag("type", "prompt").counter().count())
                .isEqualTo(10.0);
        assertThat(registry.get("iboot.ai.tokens").tag("type", "completion").counter().count())
                .isEqualTo(5.0);
        assertThat(registry.get("iboot.ai.first_token").timer().count()).isEqualTo(1);
        assertThat(registry.get("iboot.ai.chat.duration").timer().count()).isEqualTo(1);
        assertThat(registry.get("iboot.ai.errors").tag("reason", "timeout").counter().count())
                .isEqualTo(1.0);
        assertThat(registry.get("iboot.ai.quota.rejected").tag("scope", "user").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void recordsGraphMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiMetrics metrics = new AiMetrics(registry, "qwen3.8-flash");

        metrics.graphQuery("colleagues", "success", Duration.ofMillis(12));
        metrics.graphQuery("dept_members", "error", Duration.ofMillis(30));
        metrics.graphSync("success", Duration.ofMillis(200));
        metrics.graphSync("failure", Duration.ofMillis(150));

        assertThat(registry.get("iboot.ai.graph.queries").tag("template", "colleagues")
                .tag("outcome", "success").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("iboot.ai.graph.queries").tag("template", "dept_members")
                .tag("outcome", "error").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("iboot.ai.graph.query.duration").tag("template", "colleagues").timer().count())
                .isEqualTo(1);
        assertThat(registry.get("iboot.ai.graph.sync").tag("outcome", "success").counter().count())
                .isEqualTo(1.0);
        assertThat(registry.get("iboot.ai.graph.sync.duration").tag("outcome", "failure").timer().count())
                .isEqualTo(1);
    }
}
