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

import com.iboot.ai.application.service.RagAnswer;
import com.iboot.ai.application.service.RagChatService;
import com.iboot.ai.application.service.RagCitation;
import com.iboot.ai.application.service.RagPreparation;
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import com.iboot.ai.infrastructure.security.JwtTokenUtil;
import com.iboot.ai.infrastructure.security.PermissionScope;
import com.iboot.ai.infrastructure.security.PermissionScopeClient;
import com.iboot.ai.infrastructure.security.PermissionScopeUnavailableException;
import com.iboot.ai.interfaces.advisor.AiExceptionHandler;
import com.iboot.ai.interfaces.dto.UsageDto;
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

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link RagChatController} 的接口测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@WebMvcTest(controllers = RagChatController.class,
        properties = "spring.ai.openai.chat.options.model=test-model")
@Import({AiExceptionHandler.class, RagChatControllerTest.SecurityTestConfig.class})
class RagChatControllerTest {

    private static final String VERSION_HEADER = "Accept-Version";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagChatService ragChatService;

    @MockitoBean
    private PermissionScopeClient permissionScopeClient;

    @MockitoBean
    private AiProperties aiProperties;

    @MockitoBean
    private AiMetrics aiMetrics;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 有权限时返回回答与引用。
     *
     * @throws Exception MockMvc 执行失败时抛出
     */
    @Test
    void returnsAnswerWithCitations() throws Exception {
        PermissionScope scope = new PermissionScope(1L, "tester", false, 5, 2L, List.of(), List.of(2L),
                List.of("ai:kb:query"), List.of());
        RagCitation citation = new RagCitation(1, 1L, 10L, "年假政策", 3, 0, "年假五天");
        RagPreparation preparation = new RagPreparation(1L, "conv-1", "问题", "system",
                List.of(citation), false, false);
        when(permissionScopeClient.fetchCurrentUserScope()).thenReturn(scope);
        when(ragChatService.prepare(1L, null, "问题", 1L, scope)).thenReturn(preparation);
        when(ragChatService.answer(preparation)).thenReturn(new RagAnswer("conv-1", "回答", "test-model",
                new UsageDto(10, 2, 12), List.of(citation), false));

        mockMvc.perform(post("/api/ai/rag/chat")
                        .with(authentication(auth("ai:kb:query")))
                        .header(VERSION_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"问题\",\"knowledgeBaseId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value("conv-1"))
                .andExpect(jsonPath("$.content").value("回答"))
                .andExpect(jsonPath("$.citations[0].docTitle").value("年假政策"));
    }

    /**
     * 缺少知识库查询权限时返回 403。
     *
     * @throws Exception MockMvc 执行失败时抛出
     */
    @Test
    void rejectsWithoutPermission() throws Exception {
        mockMvc.perform(post("/api/ai/rag/chat")
                        .with(authentication(auth("ai:chat")))
                        .header(VERSION_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"问题\"}"))
                .andExpect(status().isForbidden());
    }

    /**
     * 权限范围不可用时返回 503。
     *
     * @throws Exception MockMvc 执行失败时抛出
     */
    @Test
    void returnsServiceUnavailableWhenScopeMissing() throws Exception {
        when(permissionScopeClient.fetchCurrentUserScope())
                .thenThrow(new PermissionScopeUnavailableException("down"));

        mockMvc.perform(post("/api/ai/rag/chat")
                        .with(authentication(auth("ai:kb:query")))
                        .header(VERSION_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"问题\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("retrieval_unavailable"));
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
