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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 智能体工具的下游客户端配置。
 *
 * <p>为 admin 代理单独提供带超时的 {@link RestClient}，与权限范围客户端各用各的连接配置，
 * 避免工具执行超时影响权限查询。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class AgentToolConfig {

    /**
     * 创建指向 admin 的 REST 客户端。
     *
     * @param builder    REST 客户端构建器
     * @param properties AI 配置
     * @return 下游客户端
     */
    @Bean("adminRestClient")
    public RestClient adminRestClient(RestClient.Builder builder, AiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(Math.max(1, properties.getAgent().getStepTimeoutSeconds())));
        return builder
                .baseUrl(properties.getAgent().getAdminBaseUrl())
                .requestFactory(factory)
                .build();
    }
}
