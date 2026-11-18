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

package com.iboot.auth.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 个人访问令牌策略配置的绑定测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class PersonalAccessTokenPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    /**
     * 默认值应覆盖常见的 CI 场景。
     */
    @Test
    void usesDefaultsWhenNotConfigured() {
        runner.run(context -> {
            PersonalAccessTokenProperties properties =
                    context.getBean(PersonalAccessTokenProperties.class);
            assertThat(properties.getDefaultDays()).isEqualTo(30);
            assertThat(properties.getMaxDays()).isEqualTo(90);
            assertThat(properties.getMaxPerUser()).isEqualTo(10);
        });
    }

    /**
     * 配置项应可从 {@code iboot.auth.pat.*} 覆盖。
     */
    @Test
    void bindsConfiguredValues() {
        runner.withPropertyValues(
                        "iboot.auth.pat.default-days=7",
                        "iboot.auth.pat.max-days=30",
                        "iboot.auth.pat.max-per-user=3")
                .run(context -> {
                    PersonalAccessTokenProperties properties =
                            context.getBean(PersonalAccessTokenProperties.class);
                    assertThat(properties.getDefaultDays()).isEqualTo(7);
                    assertThat(properties.getMaxDays()).isEqualTo(30);
                    assertThat(properties.getMaxPerUser()).isEqualTo(3);
                });
    }

    /**
     * 测试用配置类。
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PersonalAccessTokenProperties.class)
    static class TestConfig {
    }
}
