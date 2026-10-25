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

import com.iboot.admin.application.service.AuthApplicationService;
import com.iboot.admin.application.service.BusinessMetricsService;
import com.iboot.admin.common.util.MonitorUtils;
import com.iboot.admin.infrastructure.config.ApiVersionConfig;
import com.iboot.admin.interfaces.dto.response.UserScopeResponse;
import com.iboot.admin.infrastructure.config.JwtSecurityConfig;
import com.iboot.admin.infrastructure.security.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link AuthController} 只读 scope 接口的单元测试。
 *
 * <p>验证当前用户的数据权限范围以统一响应结构返回，且请求中的 {@code userId} 参数不会改变
 * 查询对象。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@WebMvcTest(controllers = AuthController.class)
@Import({ApiVersionConfig.class, MonitorUtils.class, AuthControllerScopeTest.SecurityTestConfig.class})
class AuthControllerScopeTest {

    private static final String VERSION_HEADER = "Accept-Version";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthApplicationService service;

    @MockitoBean
    private BusinessMetricsService businessMetricsService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private JwtSecurityConfig jwtSecurityConfig;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * scope 接口返回当前用户范围，忽略外部传入的 userId。
     *
     * @throws Exception MockMvc 执行失败时抛出
     */
    @Test
    void scopeReturnsCurrentUserScope() throws Exception {
        when(service.getCurrentUserScope()).thenReturn(new UserScopeResponse(101L, "rag_dept", false, 4, 2L,
                List.of(2L, 3L), List.of(2L, 3L, 4L), List.of("ai:kb:query"), List.of("ROLE_RAG_DEPT")));

        mockMvc.perform(get("/api/auth/scope")
                        .header(VERSION_HEADER, "1")
                        .param("userId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data.userId").value(101))
                .andExpect(jsonPath("$.data.data.username").value("rag_dept"))
                .andExpect(jsonPath("$.data.data.dataScope").value(4))
                .andExpect(jsonPath("$.data.data.deptId").value(2))
                .andExpect(jsonPath("$.data.data.visibleDeptIds[0]").value(2))
                .andExpect(jsonPath("$.data.data.visibleDeptIds[1]").value(3))
                .andExpect(jsonPath("$.data.data.kbVisibleDeptIds[2]").value(4))
                .andExpect(jsonPath("$.data.data.permissions[0]").value("ai:kb:query"))
                .andExpect(jsonPath("$.data.data.superAdmin").value(false));
    }

    /**
     * 测试用安全配置。
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
