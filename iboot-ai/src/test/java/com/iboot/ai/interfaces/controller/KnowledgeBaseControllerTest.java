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

package com.iboot.ai.interfaces.controller;

import com.iboot.ai.application.service.KnowledgeBaseApplicationService;
import com.iboot.kernel.security.JwtTokenUtil;
import com.iboot.ai.infrastructure.security.PermissionScope;
import com.iboot.ai.infrastructure.security.PermissionScopeClient;
import com.iboot.ai.interfaces.advisor.AiExceptionHandler;
import com.iboot.ai.interfaces.dto.KnowledgeBaseRequest;
import com.iboot.ai.interfaces.dto.KnowledgeBaseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link KnowledgeBaseController} 的接口测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@WebMvcTest(controllers = KnowledgeBaseController.class)
@Import({AiExceptionHandler.class, KnowledgeBaseControllerTest.SecurityTestConfig.class})
class KnowledgeBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeBaseApplicationService service;

    @MockitoBean
    private PermissionScopeClient permissionScopeClient;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 有查询权限时返回知识库列表。
     *
     * @throws Exception MockMvc 执行失败时抛出
     */
    @Test
    void listsVisibleKnowledgeBases() throws Exception {
        PermissionScope scope = scope();
        when(permissionScopeClient.fetchCurrentUserScope()).thenReturn(scope);
        when(service.list(scope)).thenReturn(List.of(new KnowledgeBaseResponse(1L, "kb", "desc", 5,
                1L, 2L, List.of(), LocalDateTime.now())));

        mockMvc.perform(get("/api/ai/knowledge-bases")
                        .with(authentication(auth("ai:kb:query"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("kb"))
                .andExpect(jsonPath("$[0].visibility").value(5));
    }

    /**
     * 有管理权限时可以创建知识库。
     *
     * @throws Exception MockMvc 执行失败时抛出
     */
    @Test
    void createsKnowledgeBaseWithManagePermission() throws Exception {
        PermissionScope scope = scope();
        when(permissionScopeClient.fetchCurrentUserScope()).thenReturn(scope);
        when(service.create(eq(scope), any(KnowledgeBaseRequest.class)))
                .thenReturn(new KnowledgeBaseResponse(1L, "kb", null, 5, 1L, 2L, List.of(), LocalDateTime.now()));

        mockMvc.perform(post("/api/ai/knowledge-bases")
                        .with(authentication(auth("ai:kb:manage")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"kb\",\"visibility\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("kb"));
    }

    /**
     * 缺少查询权限时返回 403。
     *
     * @throws Exception MockMvc 执行失败时抛出
     */
    @Test
    void rejectsWithoutQueryPermission() throws Exception {
        mockMvc.perform(get("/api/ai/knowledge-bases")
                        .with(authentication(auth("ai:chat"))))
                .andExpect(status().isForbidden());
    }

    private PermissionScope scope() {
        return new PermissionScope(1L, "tester", false, 5, 2L, List.of(), List.of(2L),
                List.of("ai:kb:query"), List.of());
    }

    private Authentication auth(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                Map.of("userId", 1L, "username", "tester"), null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }

    /**
     * 测试安全配置。
     */
    @org.springframework.boot.test.context.TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityTestConfig {

        /**
         * 提供内存指标注册表。
         *
         * @return 指标注册表
         */
        @Bean
        io.micrometer.core.instrument.MeterRegistry meterRegistry() {
            return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        }

        /**
         * 提供 Jackson 2 ObjectMapper。
         *
         * @return ObjectMapper
         */
        @Bean
        com.fasterxml.jackson.databind.ObjectMapper jackson2ObjectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }

        /**
         * 放行请求，权限由方法级注解校验。
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
