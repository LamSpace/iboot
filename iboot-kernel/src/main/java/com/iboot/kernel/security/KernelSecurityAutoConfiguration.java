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

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 共享内核的鉴权自动配置。
 *
 * <p>提供「资源服务器」需要的全部组件：令牌验签、共享会话读取、401/403 响应，以及一条默认的
 * 无状态过滤链。宿主只加依赖即可获得与既有服务一致的鉴权语义。
 *
 * <p>所有 Bean 都带 {@link ConditionalOnMissingBean}，定义了自有过滤链的服务（如
 * {@code iboot-admin} 与 {@code iboot-ai}）保持原有行为，不被覆盖。
 *
 * <p>过滤链与过滤器只在 Servlet Web 应用中生效：非 Web 的测试切片（例如只加载工作流
 * 相关 Bean 的上下文）不应因为启用安全链而启动失败。会话所用的
 * {@link RedisTemplate} 按 Bean 名 {@code redisTemplate} 注入，兼容各服务自建的模板。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration")
@EnableConfigurationProperties({JwtSecurityConfig.class, SecurityWhitelistProperties.class})
public class KernelSecurityAutoConfiguration {

    /**
     * 令牌验签与签发工具。
     *
     * <p>读取 {@code jwt.secret} 与 {@code jwt.expiration}；字段注入由 Spring 完成，
     * 因此这里用无参构造创建实例。
     *
     * @return 令牌工具
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil();
    }

    /**
     * 认证失败处理器。
     *
     * @return 返回 401 统一响应的处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    /**
     * 授权失败处理器。
     *
     * @return 返回 403 统一响应的处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtAccessDeniedHandler jwtAccessDeniedHandler() {
        return new JwtAccessDeniedHandler();
    }

    /**
     * 资源服务器相关组件的容器。
     *
     * <p>只在 Servlet Web 应用中生效；非 Web 上下文（如只加载工作流 Bean 的测试切片）
     * 不启用安全链，避免与切片无关的依赖缺失导致启动失败。
     */
    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableMethodSecurity
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class ResourceServerConfiguration {

        /**
         * 认证服务公钥集缓存。
         *
         * <p>地址取 {@code jwt.jwk-set-uri}，默认指向本地认证服务；进程内只保留一份，
         * 首次验签时才回源，启动阶段不阻塞。
         *
         * @param jwtSecurityConfig 令牌配置
         * @return 公钥集缓存
         */
        @Bean
        @ConditionalOnMissingBean
        public JwkSetProvider jwkSetProvider(JwtSecurityConfig jwtSecurityConfig) {
            return new JwkSetProvider(jwtSecurityConfig.getJwkSetUri());
        }

        /**
         * 令牌校验器，同时支持自包含令牌与旧的对称签名令牌。
         *
         * @param jwtSecurityConfig 令牌配置
         * @param jwkSetProvider 认证服务公钥集
         * @param secret 旧令牌使用的共享密钥
         * @return 令牌校验器
         */
        @Bean
        @ConditionalOnMissingBean
        public JwtTokenVerifier jwtTokenVerifier(JwtSecurityConfig jwtSecurityConfig,
                                                 JwkSetProvider jwkSetProvider,
                                                 @Value("${jwt.secret:"
                                                         + JwtTokenUtil.DEFAULT_SECRET + "}") String secret) {
            return new JwtTokenVerifier(secret, jwkSetProvider, jwtSecurityConfig.isLegacyEnabled());
        }

        /**
         * 资源服务器过滤器。
         *
         * @param tokenVerifier 令牌校验器
         * @param redisTemplate 共享会话所在的 Redis 模板
         * @param jwtSecurityConfig 会话超时等配置
         * @param whitelistProperties 宿主白名单
         * @return JWT 认证过滤器
         */
        @Bean
        @ConditionalOnMissingBean
        @SuppressWarnings("unchecked")
        public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenVerifier tokenVerifier,
                                                                @Qualifier("redisTemplate")
                                                                RedisTemplate redisTemplate,
                                                                JwtSecurityConfig jwtSecurityConfig,
                                                                SecurityWhitelistProperties whitelistProperties) {
            return new JwtAuthenticationFilter(tokenVerifier, redisTemplate, jwtSecurityConfig, whitelistProperties);
        }

        /**
         * 默认的无状态过滤链。
         *
         * <p>宿主自定义了 {@link SecurityFilterChain} 时不会创建，保证既有服务的过滤链不被替换。
         * ASYNC 与 ERROR 分派放行，避免 SSE 与异步错误处理被二次鉴权拦截。
         *
         * @param http HTTP 安全配置
         * @param jwtAuthenticationFilter 资源服务器过滤器
         * @param entryPoint 认证失败处理器
         * @param accessDeniedHandler 授权失败处理器
         * @param whitelistProperties 宿主白名单
         * @return 安全过滤链
         * @throws Exception 构建失败时抛出
         */
        @Bean
        @ConditionalOnMissingBean(SecurityFilterChain.class)
        public SecurityFilterChain kernelSecurityFilterChain(HttpSecurity http,
                                                             JwtAuthenticationFilter jwtAuthenticationFilter,
                                                             JwtAuthenticationEntryPoint entryPoint,
                                                             JwtAccessDeniedHandler accessDeniedHandler,
                                                             SecurityWhitelistProperties whitelistProperties)
                throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .exceptionHandling(exception -> exception.authenticationEntryPoint(entryPoint)
                            .accessDeniedHandler(accessDeniedHandler))
                    .authorizeHttpRequests(authorize -> authorize
                            .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                            .requestMatchers(whitelistProperties.getWhitelist().toArray(String[]::new)).permitAll()
                            .anyRequest().authenticated())
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            return http.build();
        }
    }
}
