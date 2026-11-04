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

package com.iboot.ops.infrastructure.config;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * 运维控制台的访问控制配置。
 *
 * <p>控制台页面与实例接口统一使用 HTTP Basic 认证，避免与网关已有的 {@code /login}
 * 路由冲突；健康与指标端点保持匿名，供探针和集中监控抓取。页面通过同源代理嵌入门户，
 * 因此允许同源 iframe。控制台不参与业务服务的认证链，也不读取业务数据。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see OpsSecurityProperties
 */
@Configuration
@EnableConfigurationProperties(OpsSecurityProperties.class)
public class OpsSecurityConfig {

    /**
     * 创建密码编码器。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 创建控制台的唯一登录用户。
     *
     * <p>认证开启时密码缺失会直接抛出异常，让进程在启动阶段失败；认证关闭时返回空用户集，
     * 供本地开发免认证模式使用。
     *
     * @param properties      控制台凭证配置
     * @param passwordEncoder 密码编码器
     * @return 内存用户服务
     * @throws IllegalStateException 认证开启但密码未配置时抛出
     */
    @Bean
    public UserDetailsService opsUserDetailsService(final OpsSecurityProperties properties,
                                                    final PasswordEncoder passwordEncoder) {
        if (!properties.isEnabled()) {
            return new InMemoryUserDetailsManager();
        }
        if (!StringUtils.hasText(properties.getPassword())) {
            throw new IllegalStateException("IBOOT_OPS_PASSWORD 未配置：运维控制台必须设置访问密码");
        }
        UserDetails user = User.builder()
                .username(properties.getUsername())
                .password(passwordEncoder.encode(properties.getPassword()))
                .roles("OPS")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    /**
     * 构建控制台的安全过滤链。
     *
     * <p>认证开启时控制台页面与实例接口需要认证；认证关闭时仅用于本地开发，全部请求放行。
     * 静态资源、健康与指标端点始终匿名可达。CSRF 关闭以适配控制台 UI 的写操作，
     * 会话保持无状态。
     *
     * @param http       HTTP 安全配置
     * @param properties 控制台凭证配置
     * @return 安全过滤链
     * @throws Exception 构建失败时抛出
     */
    @Bean
    public SecurityFilterChain opsSecurityFilterChain(final HttpSecurity http,
                                                      final OpsSecurityProperties properties) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                            .requestMatchers("/actuator/health", "/actuator/prometheus", "/error").permitAll()
                            .requestMatchers("/sba/assets/**").permitAll();
                    if (properties.isEnabled()) {
                        auth.anyRequest().authenticated();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                });
        if (properties.isEnabled()) {
            http.httpBasic(withDefaults());
        }
        return http.build();
    }
}
