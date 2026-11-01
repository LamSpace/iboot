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

package com.iboot.file.infrastructure.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 服务间调用客户端配置。
 *
 * <p>文件服务对系统服务的调用按注册中心里的服务名寻址，由调用方负载均衡，不写死主机与端口，
 * 也不回穿网关：网关只负责外部流量如何进入。
 *
 * <p>命名限定是为了不与 Spring Boot 自动配置的默认 {@link RestClient.Builder} 混淆——
 * 需要访问外部固定地址的客户端仍应使用默认构建器。
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
        return RestClient.builder();
    }
}
