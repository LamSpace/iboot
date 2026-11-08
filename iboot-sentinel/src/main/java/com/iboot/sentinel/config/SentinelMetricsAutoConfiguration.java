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
import com.iboot.sentinel.metrics.SentinelMetricsExtension;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 治理指标桥接的自动配置。
 *
 * <p>只有同时存在 Sentinel 与 Micrometer 时才创建扩展；未接入 Sentinel 的服务不会装载。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration")
@ConditionalOnClass({MetricExtension.class, MeterRegistry.class})
public class SentinelMetricsAutoConfiguration {

    /**
     * 创建并注册指标扩展。
     *
     * @param registry    Micrometer 注册表
     * @param application 服务标识
     * @return 指标扩展
     */
    @Bean
    @ConditionalOnMissingBean(SentinelMetricsExtension.class)
    public SentinelMetricsExtension sentinelMetricsExtension(
            final MeterRegistry registry,
            @Value("${spring.application.name:unknown}") final String application) {
        SentinelMetricsExtension extension = new SentinelMetricsExtension(registry, application);
        extension.register();
        return extension;
    }
}
