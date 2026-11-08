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

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 服务间调用客户端配置。
 *
 * <p>审批服务向 notify 登记通知时按注册中心的服务名寻址，不写死主机与端口，也不回穿网关。
 * 独立的 Bean 名称避免与 Spring Boot 自动配置的默认构建器混淆。客户端设置连接与读取超时，
 * 使下游不可用时尽快失败，避免异步投递线程长时间占用。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class ServiceClientConfig {

    /**
     * 创建按服务名寻址的 REST 客户端构建器。
     *
     * @return 带负载均衡能力的构建器
     */
    @Bean("loadBalancedRestClientBuilder")
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 下游不可用时尽快失败，让 Sentinel 熔断有机会在重试耗尽前打开
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(factory);
    }
}
