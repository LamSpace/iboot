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

package com.iboot.gateway.config;

import com.iboot.authz.ServiceAuthorizationProperties;
import com.iboot.authz.ServiceAuthorizationWebFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关的服务间授权配置。
 *
 * <p>网关以 WebFlux 运行，不能复用 Servlet 过滤器；这里注册
 * {@link ServiceAuthorizationWebFilter}，在路由与 JWT 处理之前只允许门户证书
 * 访问 {@code /api/**}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(ServiceAuthorizationProperties.class)
public class GatewayServiceAuthorizationConfig {

    /**
     * 创建 WebFlux 授权过滤器。
     *
     * @param properties 授权配置
     * @return 授权过滤器
     */
    @Bean
    public ServiceAuthorizationWebFilter serviceAuthorizationWebFilter(
            final ServiceAuthorizationProperties properties) {
        return new ServiceAuthorizationWebFilter(properties);
    }
}
