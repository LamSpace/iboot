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

package com.iboot.job.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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
 * {@link PermissionScopeClient} 的行为测试。
 *
 * <p>覆盖三条对外承诺：带调用者令牌请求管理端、结果按用户缓存、下游不可用时失败关闭。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class PermissionScopeClientTest {

    private static final String SCOPE_URL = "http://admin.test/api/auth/scope";

    private static final String SCOPE_BODY = """
            {"type":"com.iboot.response","data":{"code":200,"data":{
              "dataScope":2,"deptId":3,"visibleDeptIds":[3,4],"superAdmin":false}}}
            """;

    private MockRestServiceServer server;

    private PermissionScopeClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PermissionScopeClient(builder, SCOPE_URL, 60);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(Map.of("userId", 1L), null, List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer tk-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 解析数据权限范围，并携带调用者令牌。
     */
    @Test
    void parsesScopeWithCallerToken() {
        server.expect(requestTo(SCOPE_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tk-1"))
                .andRespond(withSuccess(SCOPE_BODY, MediaType.APPLICATION_JSON));

        JobPermissionScope scope = client.fetchCurrentUserScope();

        assertThat(scope.dataScope()).isEqualTo(2);
        assertThat(scope.deptId()).isEqualTo(3L);
        assertThat(scope.visibleDeptIds()).containsExactly(3L, 4L);
        assertThat(scope.superAdmin()).isFalse();
        server.verify();
    }

    /**
     * 同一用户的第二次查询命中缓存，不再发起请求。
     */
    @Test
    void cachesPerUser() {
        server.expect(requestTo(SCOPE_URL))
                .andRespond(withSuccess(SCOPE_BODY, MediaType.APPLICATION_JSON));

        client.fetchCurrentUserScope();
        client.fetchCurrentUserScope();

        server.verify();
    }

    /**
     * 下游不可用时失败关闭，不返回未过滤的结果。
     */
    @Test
    void failsClosedWhenDownstreamUnavailable() {
        server.expect(requestTo(SCOPE_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.fetchCurrentUserScope())
                .isInstanceOf(ScopeUnavailableException.class);
    }
}
