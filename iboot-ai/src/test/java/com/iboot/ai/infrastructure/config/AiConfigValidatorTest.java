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

package com.iboot.ai.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AiConfigValidator} 的测试。
 *
 * <p>验证 base-url、模型与 Key 的启动校验，不启动完整应用。
 */
class AiConfigValidatorTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AiConfiguration.class)
            .withPropertyValues(
                    "spring.ai.openai.chat.api-key=test-key",
                    "spring.ai.openai.chat.options.model=qwen3.8-flash");

    @Test
    void rejectsBaseUrlWithoutVersionSuffix() {
        runner.withPropertyValues("spring.ai.openai.chat.base-url=https://example.com/compatible-mode")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("/v1");
                });
    }

    @Test
    void rejectsBlankApiKey() {
        runner.withPropertyValues(
                        "spring.ai.openai.chat.base-url=https://example.com/compatible-mode/v1",
                        "spring.ai.openai.chat.api-key=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("api-key");
                });
    }

    @Test
    void acceptsValidConfiguration() {
        runner.withPropertyValues("spring.ai.openai.chat.base-url=https://example.com/compatible-mode/v1")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
