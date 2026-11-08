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

package com.iboot.notify.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通知服务 RocketMQ 配置绑定测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RocketMqPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsDefaults() {
        runner.run(context -> {
            RocketMqProperties properties = context.getBean(RocketMqProperties.class);
            assertThat(properties.getEndpoints()).isEqualTo("localhost:18081");
            assertThat(properties.isSslEnabled()).isFalse();
            assertThat(properties.getTopic()).isEqualTo("iboot-notification");
            assertThat(properties.getConsumerGroup()).isEqualTo("iboot-notify-consumer");
            assertThat(properties.isConsumerEnabled()).isTrue();
            assertThat(properties.getRetryInterval()).isEqualTo(Duration.ofSeconds(10));
        });
    }

    @Test
    void bindsOverrides() {
        runner.withPropertyValues(
                        "iboot.rocketmq.endpoints=rocketmq-proxy:8081",
                        "iboot.rocketmq.ssl-enabled=true",
                        "iboot.rocketmq.topic=custom-topic",
                        "iboot.rocketmq.consumer-group=custom-group",
                        "iboot.rocketmq.consumer-enabled=false",
                        "iboot.rocketmq.retry-interval=3s")
                .run(context -> {
                    RocketMqProperties properties = context.getBean(RocketMqProperties.class);
                    assertThat(properties.getEndpoints()).isEqualTo("rocketmq-proxy:8081");
                    assertThat(properties.isSslEnabled()).isTrue();
                    assertThat(properties.getTopic()).isEqualTo("custom-topic");
                    assertThat(properties.getConsumerGroup()).isEqualTo("custom-group");
                    assertThat(properties.isConsumerEnabled()).isFalse();
                    assertThat(properties.getRetryInterval()).isEqualTo(Duration.ofSeconds(3));
                });
    }

    /**
     * 测试用配置，只启用配置属性绑定。
     */
    @Configuration
    @EnableConfigurationProperties(RocketMqProperties.class)
    static class TestConfig {
    }
}
