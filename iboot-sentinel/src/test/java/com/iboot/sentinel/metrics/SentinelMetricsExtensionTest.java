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

package com.iboot.sentinel.metrics;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 治理指标桥接的行为测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class SentinelMetricsExtensionTest {

    private MeterRegistry registry;

    private SentinelMetricsExtension extension;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        extension = new SentinelMetricsExtension(registry, "iboot-gateway");
    }

    /**
     * 通过数按资源与服务累加。
     */
    @Test
    void recordsPassCountWithResourceAndApplicationTags() {
        extension.addPass("workflow", 2);
        extension.addPass("workflow", 1);

        Counter counter = registry.get("sentinel.pass")
                .tag("resource", "workflow")
                .tag("application", "iboot-gateway")
                .counter();

        assertThat(counter.count()).isEqualTo(3.0);
    }

    /**
     * 阻塞数按资源、服务与规则类型累加。
     */
    @Test
    void recordsBlockCountWithRuleTag() {
        extension.addBlock("workflow", 1, "default", new FlowException("workflow"));
        extension.addBlock("workflow", 2, "default", new DegradeException("workflow"));

        assertThat(registry.get("sentinel.block")
                .tag("resource", "workflow")
                .tag("application", "iboot-gateway")
                .tag("rule", "flow")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("sentinel.block")
                .tag("resource", "workflow")
                .tag("application", "iboot-gateway")
                .tag("rule", "degrade")
                .counter().count()).isEqualTo(2.0);
    }

    /**
     * 异常数按资源与服务累加。
     */
    @Test
    void recordsExceptionCount() {
        extension.addException("iboot-demo:resource", 1, new IllegalStateException("boom"));

        assertThat(registry.get("sentinel.exception")
                .tag("resource", "iboot-demo:resource")
                .tag("application", "iboot-gateway")
                .counter().count()).isEqualTo(1.0);
    }

    /**
     * 响应时间进入 Timer。
     */
    @Test
    void recordsResponseTime() {
        extension.addRt("workflow", 120);
        extension.addRt("workflow", 80);

        Timer timer = registry.get("sentinel.rt")
                .tag("resource", "workflow")
                .tag("application", "iboot-gateway")
                .timer();

        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(200.0);
    }

    /**
     * 线程数随进入与离开增减。
     */
    @Test
    void tracksThreadCount() {
        extension.increaseThreadNum("workflow");
        extension.increaseThreadNum("workflow");
        extension.decreaseThreadNum("workflow");

        assertThat(registry.get("sentinel.threads")
                .tag("resource", "workflow")
                .tag("application", "iboot-gateway")
                .gauge().value()).isEqualTo(1.0);
    }

    /**
     * 资源名缺失时使用稳定占位符，避免 Micrometer 拒绝空标签。
     */
    @Test
    void usesStableResourceNameWhenMissing() {
        extension.addPass(null, 1);

        assertThat(registry.get("sentinel.pass")
                .tag("resource", "unknown")
                .counter().count()).isEqualTo(1.0);
    }
}
