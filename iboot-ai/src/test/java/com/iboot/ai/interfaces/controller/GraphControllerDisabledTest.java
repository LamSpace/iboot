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

import com.iboot.ai.application.service.graph.GraphSyncService;
import com.iboot.ai.infrastructure.graph.Neo4jGraphRepository;
import com.iboot.ai.infrastructure.security.JwtTokenUtil;
import com.iboot.ai.interfaces.advisor.AiExceptionHandler;
import org.springframework.data.redis.core.RedisTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link GraphController} 在关闭态下的接口测试。
 *
 * <p>默认配置下能力关闭，两个端点都应返回 503 与可辨识的 {@code graph_disabled}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@WebMvcTest(controllers = GraphController.class)
@Import({AiExceptionHandler.class, GraphTestSecurityConfig.class})
class GraphControllerDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GraphSyncService syncService;

    @MockitoBean
    private Neo4jGraphRepository repository;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void syncReturnsGraphDisabled() throws Exception {
        mockMvc.perform(post("/api/ai/graph/sync")
                        .with(authentication(auth("ai:graph:use")))
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("graph_disabled"));
    }

    @Test
    void statusReturnsGraphDisabled() throws Exception {
        mockMvc.perform(get("/api/ai/graph/status")
                        .with(authentication(auth("ai:graph:use"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("graph_disabled"));
    }

    private Authentication auth(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                Map.of("userId", 1L, "username", "tester"), null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }
}
