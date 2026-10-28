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

package com.iboot.admin.interfaces.controller;

import com.iboot.admin.application.service.BusinessMetricsService;
import com.iboot.admin.application.service.StatisticsApplicationService;
import com.iboot.admin.common.util.MonitorUtils;
import com.iboot.admin.infrastructure.config.ApiVersionConfig;
import com.iboot.admin.infrastructure.config.JwtSecurityConfig;
import com.iboot.admin.infrastructure.security.JwtTokenUtil;
import com.iboot.admin.interfaces.dto.response.StatisticsResponse.ReportResponse;
import com.iboot.admin.interfaces.dto.response.StatisticsResponse.UsageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link StatisticsController} 的接口测试。
 *
 * <p>验证统计接口在 1.3 中新增的显式权限校验：具备对应权限时放行，缺失时返回 403。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@WebMvcTest(controllers = StatisticsController.class)
@Import({ApiVersionConfig.class, MonitorUtils.class, StatisticsControllerTest.SecurityTestConfig.class})
class StatisticsControllerTest {

    private static final String VERSION_HEADER = "Accept-Version";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticsApplicationService statisticsApplicationService;

    @MockitoBean
    private BusinessMetricsService businessMetricsService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private JwtSecurityConfig jwtSecurityConfig;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 验证具备 {@code report:view} 时报表接口返回 200。
     *
     * @throws Exception 请求执行失败时抛出
     */
    @Test
    void reportAllowsReportViewPermission() throws Exception {
        when(statisticsApplicationService.getReportStatistics(any(), any())).thenReturn(new ReportResponse());

        mockMvc.perform(get("/api/statistics/report")
                        .with(authentication(auth("report:view")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isOk());
    }

    /**
     * 验证缺少 {@code report:view} 时报表接口返回 403。
     *
     * @throws Exception 请求执行失败时抛出
     */
    @Test
    void reportRequiresReportViewPermission() throws Exception {
        mockMvc.perform(get("/api/statistics/report")
                        .with(authentication(auth("usage:view")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isForbidden());
    }

    /**
     * 验证具备 {@code usage:view} 时使用分析接口返回 200。
     *
     * @throws Exception 请求执行失败时抛出
     */
    @Test
    void usageAllowsUsageViewPermission() throws Exception {
        when(statisticsApplicationService.getUsageAnalysis(any(), any())).thenReturn(new UsageResponse());

        mockMvc.perform(get("/api/statistics/usage")
                        .with(authentication(auth("usage:view")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isOk());
    }

    /**
     * 验证缺少 {@code usage:view} 时使用分析接口返回 403。
     *
     * @throws Exception 请求执行失败时抛出
     */
    @Test
    void usageRequiresUsageViewPermission() throws Exception {
        mockMvc.perform(get("/api/statistics/usage")
                        .with(authentication(auth("report:view")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isForbidden());
    }

    private Authentication auth(final String... authorities) {
        Map<String, Object> principal = Map.of("userId", 1L, "username", "alice");
        List<SimpleGrantedAuthority> granted = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, null, granted);
    }

    /**
     * 测试安全配置：放开过滤链，方法级权限仍由 {@code @PreAuthorize} 校验。
     */
    @org.springframework.boot.test.context.TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityTestConfig {

        /**
         * 提供内存指标注册表，避免监控过滤器在测试中空指针。
         *
         * @return 指标注册表
         */
        @Bean
        io.micrometer.core.instrument.MeterRegistry meterRegistry() {
            return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        }

        /**
         * 提供 Jackson 2 的 ObjectMapper，供 JWT 过滤器使用。
         *
         * @return Jackson 2 ObjectMapper
         */
        @Bean
        com.fasterxml.jackson.databind.ObjectMapper jackson2ObjectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }

        /**
         * 允许所有请求进入控制器。
         *
         * @param http 安全配置
         * @return 过滤链
         * @throws Exception 配置失败时抛出
         */
        @Bean
        SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
            return http.build();
        }
    }
}
