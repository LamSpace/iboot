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

package com.iboot.kernel.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 鉴权自动配置的注册测试。
 *
 * <p>宿主服务的组件扫描覆盖不到内核包；若自动配置漏注册，应用照样启动、接口照样返回 200，
 * 只是过滤器不在链上——这是静默的安全失效。这里把「过滤器确实进了过滤链」单独断言。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class KernelSecurityAutoConfigurationTest {

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KernelSecurityAutoConfiguration.class))
            .withBean("redisTemplate", RedisTemplate.class, () -> redisTemplate)
            .withPropertyValues("jwt.secret=iboot-admin-secret-key-for-jwt-token-generation-minimum-256-bits");

    /**
     * 只加依赖时，内核过滤器应出现在 Spring Security 的过滤链中。
     */
    @Test
    void registersJwtFilterIntoFilterChain() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(JwtAuthenticationFilter.class);
            assertThat(context).hasSingleBean(SecurityFilterChain.class);

            FilterChainProxy proxy = context.getBean(FilterChainProxy.class);
            boolean present = proxy.getFilterChains().stream()
                    .flatMap(chain -> chain.getFilters().stream())
                    .anyMatch(JwtAuthenticationFilter.class::isInstance);
            assertThat(present).isTrue();
        });
    }

    /**
     * 宿主自定义过滤链时，内核的默认过滤链应让位。
     */
    @Test
    void backsOffWhenHostDefinesOwnFilterChain() {
        runner.withBean("hostChain", SecurityFilterChain.class, () -> mock(SecurityFilterChain.class))
                .run(context -> assertThat(context.getBeansOfType(SecurityFilterChain.class)).hasSize(1));
    }
}
