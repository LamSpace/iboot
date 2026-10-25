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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

/**
 * 模型接入配置的启动校验。
 *
 * <p>Spike 已确认 OpenAI starter 会在 base-url 后拼接 {@code /chat/completions}，因此 base-url
 * 必须以 {@code /v1} 结尾；同时 API Key 与模型标识不能为空。校验失败直接终止启动，避免服务带着
 * 必然 404 的配置上线。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class AiConfigValidator implements InitializingBean {

    private final String baseUrl;

    private final String model;

    private final String apiKey;

    /**
     * 创建校验器。
     *
     * @param baseUrl 对话端点地址
     * @param model   对话模型标识
     * @param apiKey  API Key
     */
    public AiConfigValidator(String baseUrl, String model, String apiKey) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
    }

    /**
     * 校验配置，失败时抛出异常终止启动。
     */
    @Override
    public void afterPropertiesSet() {
        String normalized = StringUtils.removeEnd(StringUtils.trim(baseUrl), "/");
        if (!StringUtils.endsWith(normalized, "/v1")) {
            throw new IllegalStateException(
                    "spring.ai.openai.chat.base-url 必须以 /v1 结尾，当前值为：" + baseUrl);
        }
        if (StringUtils.isBlank(model)) {
            throw new IllegalStateException("spring.ai.openai.chat.options.model 不能为空");
        }
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException(
                    "spring.ai.openai.chat.api-key 不能为空，请通过 DASHSCOPE_CHAT_API_KEY 注入");
        }
    }
}
