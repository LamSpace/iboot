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

package com.iboot.authz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * WebFlux 服务间授权过滤器。
 *
 * <p>在网关路由前执行：命中路径规则且调用方不在白名单时直接返回 403；未命中
 * 规则时交给后续网关过滤器与路由。过滤器只读取 TLS 对端证书，不解析请求头身份。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ServiceAuthorizationWebFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuthorizationWebFilter.class);

    private static final String DENIED_BODY = "{\"code\":403,\"message\":\"调用方无权访问\"}";

    private final ServiceAuthorizationProperties properties;

    private final ServiceAuthorizationPolicy policy;

    /**
     * 创建过滤器。
     *
     * @param properties 授权配置
     */
    public ServiceAuthorizationWebFilter(final ServiceAuthorizationProperties properties) {
        this.properties = properties;
        this.policy = new ServiceAuthorizationPolicy(properties);
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getPath().value();
        String caller = WebFluxClientCertificateIdentity.from(exchange.getRequest());
        if (!policy.isDenied(path, caller)) {
            return chain.filter(exchange);
        }
        log.warn("拒绝服务间调用，caller={}, path={}", caller, path);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(DENIED_BODY.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
