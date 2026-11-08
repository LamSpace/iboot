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

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关限流响应配置。
 *
 * <p>把 Sentinel 的路由级阻塞映射为与 {@code GatewayErrorHandler} 同构的 JSON：
 * {@code code}、{@code message} 与 {@code path}。客户端据此可以区分「被限流」与
 * 「上游不可用」。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class SentinelGatewayConfig {

    /**
     * 创建 Sentinel 网关阻塞响应处理器。
     *
     * @return 阻塞响应处理器
     */
    @Bean
    public BlockRequestHandler sentinelGatewayBlockRequestHandler() {
        return (exchange, throwable) -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", HttpStatus.TOO_MANY_REQUESTS.value());
            body.put("message", "请求过于频繁");
            body.put("path", exchange.getRequest().getPath().value());
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body);
        };
    }
}
