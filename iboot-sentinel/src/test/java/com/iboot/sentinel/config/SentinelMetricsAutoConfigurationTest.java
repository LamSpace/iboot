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

package com.iboot.sentinel.config;

import com.alibaba.csp.sentinel.metric.extension.MetricExtension;
import com.alibaba.csp.sentinel.metric.extension.MetricExtensionProvider;
import com.iboot.sentinel.metrics.SentinelMetricsExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 治理指标自动配置的行为测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class SentinelMetricsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SentinelMetricsAutoConfiguration.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    /**
     * 同时具备 Sentinel 与 Micrometer 时创建并注册一次扩展。
     */
    @Test
    void registersExtensionOnceWhenDependenciesPresent() {
        runner.withPropertyValues("spring.application.name=auto-config-one")
                .run(context -> {
                    assertThat(context).hasSingleBean(SentinelMetricsExtension.class);
                    assertThat(registeredCount("auto-config-one")).isEqualTo(1);
                });
    }

    /**
     * 重复创建同应用上下文不会重复注册。
     */
    @Test
    void doesNotRegisterTwiceForSameApplication() {
        runner.withPropertyValues("spring.application.name=auto-config-twice")
                .run(context -> assertThat(context).hasSingleBean(SentinelMetricsExtension.class));

        runner.withPropertyValues("spring.application.name=auto-config-twice")
                .run(context -> {
                    assertThat(context).hasSingleBean(SentinelMetricsExtension.class);
                    assertThat(registeredCount("auto-config-twice")).isEqualTo(1);
                });
    }

    /**
     * 没有 Sentinel 时不激活。
     */
    @Test
    void doesNotActivateWithoutSentinel() {
        runner.withClassLoader(new FilteredClassLoader(MetricExtension.class))
                .withPropertyValues("spring.application.name=auto-config-no-sentinel")
                .run(context -> assertThat(context).doesNotHaveBean(SentinelMetricsExtension.class));
    }

    private long registeredCount(final String application) {
        return MetricExtensionProvider.getMetricExtensions().stream()
                .filter(SentinelMetricsExtension.class::isInstance)
                .map(SentinelMetricsExtension.class::cast)
                .filter(metrics -> metrics.application().equals(application))
                .count();
    }
}
