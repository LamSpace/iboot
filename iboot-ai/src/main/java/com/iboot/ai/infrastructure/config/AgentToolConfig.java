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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 智能体工具的下游客户端配置。
 *
 * <p>为工具代理单独提供带超时的 {@link RestClient}，与权限范围客户端各用各的连接配置，
 * 避免工具执行超时影响权限查询。
 *
 * <p>服务之间的调用按注册中心里的服务名寻址，不回穿网关：网关只负责外部流量如何进入，
 * 若工具调用经网关，改一条路由就会静默改变工具的目标，且内部调用会被计入边缘限流。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class AgentToolConfig {

    /**
     * 创建按服务名寻址的 REST 客户端构建器。
     *
     * @return 带负载均衡能力的构建器
     */
    @Bean("loadBalancedRestClientBuilder")
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    /**
     * 创建指向管理端的 REST 客户端。
     *
     * @param builder    负载均衡构建器
     * @param properties AI 配置
     * @return 下游客户端
     */
    @Bean("adminRestClient")
    public RestClient adminRestClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
                                      AiProperties properties) {
        return builder
                .baseUrl(properties.getAgent().getAdminBaseUrl())
                .requestFactory(requestFactory(properties))
                .build();
    }

    /**
     * 创建指向调度服务的 REST 客户端。
     *
     * @param builder    负载均衡构建器
     * @param properties AI 配置
     * @return 下游客户端
     */
    @Bean("jobRestClient")
    public RestClient jobRestClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
                                    AiProperties properties) {
        return builder
                .baseUrl(properties.getAgent().getJobBaseUrl())
                .requestFactory(requestFactory(properties))
                .build();
    }

    /**
     * 构造与工具执行超时对齐的请求工厂。
     *
     * @param properties AI 配置
     * @return 请求工厂
     */
    private SimpleClientHttpRequestFactory requestFactory(final AiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(Math.max(1, properties.getAgent().getStepTimeoutSeconds())));
        return factory;
    }
}
