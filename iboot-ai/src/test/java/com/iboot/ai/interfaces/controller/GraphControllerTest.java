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
import com.iboot.ai.domain.graph.GraphStatus;
import com.iboot.ai.domain.graph.GraphSyncSummary;
import com.iboot.ai.infrastructure.graph.Neo4jGraphRepository;
import com.iboot.kernel.security.JwtTokenUtil;
import com.iboot.ai.interfaces.advisor.AiExceptionHandler;
import org.springframework.data.redis.core.RedisTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link GraphController} 在启用态下的接口测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@WebMvcTest(controllers = GraphController.class)
@Import({AiExceptionHandler.class, GraphTestSecurityConfig.class})
@TestPropertySource(properties = "iboot.ai.graph.enabled=true")
class GraphControllerTest {

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
    void syncsWithGraphPermission() throws Exception {
        when(syncService.sync("test-token"))
                .thenReturn(new GraphSyncSummary("sync-1", Instant.parse("2026-10-28T10:00:00Z"),
                        3, 2, 1, 4, 9, 1));

        mockMvc.perform(post("/api/ai/graph/sync")
                        .with(authentication(auth("ai:graph:use")))
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departments").value(3))
                .andExpect(jsonPath("$.removed").value(1));
    }

    @Test
    void reportsStatus() throws Exception {
        when(repository.loadStatus()).thenReturn(new GraphStatus(true, Instant.parse("2026-10-28T10:00:00Z"),
                3, 2, 1, 4, 9));

        mockMvc.perform(get("/api/ai/graph/status")
                        .with(authentication(auth("ai:graph:use"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(true))
                .andExpect(jsonPath("$.users").value(2));
    }

    @Test
    void rejectsWithoutGraphPermission() throws Exception {
        mockMvc.perform(get("/api/ai/graph/status")
                        .with(authentication(auth("ai:chat"))))
                .andExpect(status().isForbidden());
    }

    private Authentication auth(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                Map.of("userId", 1L, "username", "tester"), null,
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }
}
