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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务基础配置。
 *
 * <p>启用 {@link AiProperties} 绑定，并注册模型接入配置的启动校验器。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties({AiProperties.class, RagProperties.class})
public class AiConfiguration {

    /**
     * 注册配置校验器。
     *
     * @param baseUrl 对话端点地址
     * @param model   对话模型标识
     * @param apiKey  API Key
     * @return 校验器
     */
    @Bean
    public AiConfigValidator aiConfigValidator(
            @Value("${spring.ai.openai.chat.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model}") String model,
            @Value("${spring.ai.openai.chat.api-key:}") String apiKey) {
        return new AiConfigValidator(baseUrl, model, apiKey);
    }

    /**
     * 注册知识库配置校验器。
     *
     * @param properties 知识库配置
     * @return 校验器
     */
    @Bean
    public RagConfigValidator ragConfigValidator(RagProperties properties) {
        return new RagConfigValidator(properties);
    }
}
