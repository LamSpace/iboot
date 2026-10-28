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

package com.iboot.ai.infrastructure.security;

import com.iboot.ai.infrastructure.config.AiProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link PermissionScopeClient} 的单元测试。
 *
 * <p>覆盖 scope 响应解析、JWT 转发、60 秒缓存与失败关闭。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class PermissionScopeClientTest {

    private static final String SCOPE_URL = "http://localhost:8080/api/auth/scope";

    private static final String RESPONSE = """
            {"type":"com.iboot.response.success","source":"/api/auth/scope","data":{"code":200,
             "data":{"userId":101,"username":"rag_dept","superAdmin":false,"dataScope":4,"deptId":2,
             "visibleDeptIds":[2,3],"kbVisibleDeptIds":[2,3,4],
             "permissions":["ai:kb:query"],"roleCodes":["ROLE_RAG_DEPT"]},
             "message":"ok"}}
            """;

    private MockRestServiceServer server;

    private PermissionScopeClient client;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.getPermission().setScopeUrl(SCOPE_URL);
        properties.getPermission().setCacheSeconds(60);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PermissionScopeClient(builder, properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                Map.of("userId", 101L, "username", "rag_dept"), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    /**
     * 正确解析 admin 的 scope 响应并转发 JWT。
     */
    @Test
    void fetchesAndParsesCurrentUserScope() {
        server.expect(requestTo(SCOPE_URL))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        PermissionScope scope = client.fetchCurrentUserScope();

        assertThat(scope.userId()).isEqualTo(101L);
        assertThat(scope.dataScope()).isEqualTo(4);
        assertThat(scope.deptId()).isEqualTo(2L);
        assertThat(scope.visibleDeptIds()).containsExactly(2L, 3L);
        assertThat(scope.kbVisibleDeptIds()).containsExactly(2L, 3L, 4L);
        assertThat(scope.permissions()).containsExactly("ai:kb:query");
        server.verify();
    }

    /**
     * 同一用户重复调用命中缓存，不重复请求 admin。
     */
    @Test
    void cachesScopeForSameUser() {
        server.expect(requestTo(SCOPE_URL))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        client.fetchCurrentUserScope();
        client.fetchCurrentUserScope();

        server.verify();
    }

    /**
     * admin 返回错误时失败关闭。
     */
    @Test
    void failsClosedWhenAdminReturnsError() {
        server.expect(requestTo(SCOPE_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(client::fetchCurrentUserScope)
                .isInstanceOf(PermissionScopeUnavailableException.class);
    }

    /**
     * 缺少 JWT 时失败关闭。
     */
    @Test
    void failsClosedWhenTokenMissing() {
        RequestContextHolder.resetRequestAttributes();

        assertThatThrownBy(client::fetchCurrentUserScope)
                .isInstanceOf(PermissionScopeUnavailableException.class);
    }

    /**
     * 不在请求线程上时可用显式令牌取范围。
     */
    @Test
    void fetchesScopeWithExplicitTokenOutsideRequestThread() {
        RequestContextHolder.resetRequestAttributes();
        server.expect(requestTo(SCOPE_URL))
                .andExpect(header("Authorization", "Bearer explicit-token"))
                .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        PermissionScope scope = client.fetchScope(101L, "explicit-token");

        assertThat(scope.userId()).isEqualTo(101L);
        assertThat(scope.visibleDeptIds()).containsExactly(2L, 3L);
        server.verify();
    }

    /**
     * 显式令牌路径与请求线程路径共用缓存，同一用户只请求一次。
     */
    @Test
    void explicitTokenReusesCache() {
        server.expect(requestTo(SCOPE_URL)).andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));

        client.fetchScope(101L, "explicit-token");
        client.fetchCurrentUserScope();

        server.verify();
    }
}
