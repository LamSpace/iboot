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

package com.iboot.workflow.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RocketMQ 与 outbox 配置绑定测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RocketMqPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsClientDefaults() {
        runner.run(context -> {
            RocketMqProperties properties = context.getBean(RocketMqProperties.class);
            assertThat(properties.getEndpoints()).isEqualTo("localhost:18081");
            assertThat(properties.isSslEnabled()).isFalse();
            assertThat(properties.getTopic()).isEqualTo("iboot-notification");
        });
    }

    @Test
    void bindsClientOverrides() {
        runner.withPropertyValues(
                        "iboot.rocketmq.endpoints=rocketmq-proxy:8081",
                        "iboot.rocketmq.ssl-enabled=true",
                        "iboot.rocketmq.topic=custom-topic")
                .run(context -> {
                    RocketMqProperties properties = context.getBean(RocketMqProperties.class);
                    assertThat(properties.getEndpoints()).isEqualTo("rocketmq-proxy:8081");
                    assertThat(properties.isSslEnabled()).isTrue();
                    assertThat(properties.getTopic()).isEqualTo("custom-topic");
                });
    }

    @Test
    void bindsOutboxDefaults() {
        runner.run(context -> {
            OutboxProperties properties = context.getBean(OutboxProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getBatchSize()).isEqualTo(20);
            assertThat(properties.getPollInterval()).isEqualTo(Duration.ofSeconds(1));
            assertThat(properties.getLease()).isEqualTo(Duration.ofMinutes(5));
            assertThat(properties.getMaxAttempts()).isEqualTo(10);
            assertThat(properties.getInitialBackoff()).isEqualTo(Duration.ofSeconds(5));
            assertThat(properties.getRetention()).isEqualTo(Duration.ofDays(7));
        });
    }

    @Test
    void bindsOutboxOverrides() {
        runner.withPropertyValues(
                        "iboot.workflow.outbox.enabled=false",
                        "iboot.workflow.outbox.batch-size=5",
                        "iboot.workflow.outbox.poll-interval=2s",
                        "iboot.workflow.outbox.max-attempts=3")
                .run(context -> {
                    OutboxProperties properties = context.getBean(OutboxProperties.class);
                    assertThat(properties.isEnabled()).isFalse();
                    assertThat(properties.getBatchSize()).isEqualTo(5);
                    assertThat(properties.getPollInterval()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(properties.getMaxAttempts()).isEqualTo(3);
                });
    }

    /**
     * 测试用配置，只启用配置属性绑定。
     */
    @Configuration
    @EnableConfigurationProperties({RocketMqProperties.class, OutboxProperties.class})
    static class TestConfig {
    }
}
