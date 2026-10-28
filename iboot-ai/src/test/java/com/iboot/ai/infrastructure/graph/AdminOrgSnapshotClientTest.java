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

package com.iboot.ai.infrastructure.graph;

import com.iboot.ai.application.service.graph.GraphSyncException;
import com.iboot.ai.domain.graph.GraphSnapshot;
import com.iboot.ai.infrastructure.config.GraphProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link AdminOrgSnapshotClient} 的单元测试。
 *
 * <p>覆盖四类数据解析、分页终止、最大翻页保护与下游错误。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class AdminOrgSnapshotClientTest {

    private static final String BASE_URL = "http://localhost:8080";

    private static final String DEPTS = """
            {"type":"com.iboot.response.success","source":"/api/dept/list","data":{"code":200,
             "data":[{"id":1,"parentId":null,"deptCode":"HQ","deptName":"总经办","status":1},
                     {"id":2,"parentId":1,"deptCode":"RD","deptName":"研发部","status":1}],
             "message":"ok"}}
            """;

    private static final String MENUS = """
            {"type":"com.iboot.response.success","source":"/api/menu/list","data":{"code":200,
             "data":[{"id":100,"parentId":null,"menuName":"用户管理","menuType":"C","perms":"user:list","status":1}],
             "message":"ok"}}
            """;

    private static final String ROLES = """
            {"type":"com.iboot.response.success","source":"/api/role/list","data":{"code":200,
             "data":{"total":1,"pageNum":1,"pageSize":200,"code":200,
                     "data":[{"id":5,"roleCode":"admin","roleName":"管理员","dataScope":1,"status":1}],
                     "message":"查询成功"},"message":"ok"}}
            """;

    private static final String ROLE_DETAIL = """
            {"type":"com.iboot.response.success","source":"/api/role/5","data":{"code":200,
             "data":{"id":5,"roleCode":"admin","roleName":"管理员","dataScope":1,"status":1,
                     "menuIds":[100],"deptIds":[2]},"message":"ok"}}
            """;

    private static final String USERS = """
            {"type":"com.iboot.response.success","source":"/api/user/list","data":{"code":200,
             "data":{"total":1,"pageNum":1,"pageSize":200,"code":200,
                     "data":[{"id":10,"username":"alice","nickname":"爱丽丝","deptId":2,"status":1,
                              "roleIds":[5]}],"message":"查询成功"},"message":"ok"}}
            """;

    private MockRestServiceServer server;

    private AdminOrgSnapshotClient client;

    private GraphProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GraphProperties();
        properties.getSync().setPageSize(200);
        properties.getSync().setMaxPages(50);
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AdminOrgSnapshotClient(builder.build(), properties);
    }

    @Test
    void buildsSnapshotFromAdminResponses() {
        expectBaseRequests();

        GraphSnapshot snapshot = client.fetch("test-token");

        assertThat(snapshot.departments()).hasSize(2);
        assertThat(snapshot.departments().get(1).parentId()).isEqualTo(1L);
        assertThat(snapshot.menus()).singleElement()
                .satisfies(menu -> assertThat(menu.permission()).isEqualTo("user:list"));
        assertThat(snapshot.roles()).singleElement()
                .satisfies(role -> {
                    assertThat(role.menuIds()).containsExactly(100L);
                    assertThat(role.deptIds()).containsExactly(2L);
                });
        assertThat(snapshot.users()).singleElement()
                .satisfies(user -> {
                    assertThat(user.deptId()).isEqualTo(2L);
                    assertThat(user.roleIds()).containsExactly(5L);
                });
        server.verify();
    }

    @Test
    void forwardsBearerToken() {
        expectBaseRequests();

        client.fetch("test-token");

        server.verify();
    }

    @Test
    void readsAllUserPages() {
        properties.getSync().setPageSize(1);
        server.expect(requestTo(BASE_URL + "/api/dept/list")).andRespond(withSuccess(DEPTS, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/menu/list")).andRespond(withSuccess(MENUS, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/role/list?pageNum=1&pageSize=1"))
                .andRespond(withSuccess(ROLES, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/role/5"))
                .andRespond(withSuccess(ROLE_DETAIL, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/user/list?pageNum=1&pageSize=1"))
                .andRespond(withSuccess(page(1, 2), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/user/list?pageNum=2&pageSize=1"))
                .andRespond(withSuccess(page(2, 2), MediaType.APPLICATION_JSON));

        GraphSnapshot snapshot = client.fetch("test-token");

        assertThat(snapshot.users()).hasSize(2);
        server.verify();
    }

    @Test
    void failsWhenMaxPagesExceeded() {
        properties.getSync().setPageSize(1);
        properties.getSync().setMaxPages(1);
        server.expect(requestTo(BASE_URL + "/api/dept/list")).andRespond(withSuccess(DEPTS, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/menu/list")).andRespond(withSuccess(MENUS, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/role/list?pageNum=1&pageSize=1"))
                .andRespond(withSuccess(ROLES, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/role/5"))
                .andRespond(withSuccess(ROLE_DETAIL, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/user/list?pageNum=1&pageSize=1"))
                .andRespond(withSuccess(page(1, 5), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetch("test-token"))
                .isInstanceOf(GraphSyncException.class)
                .hasMessageContaining("翻页");
    }

    @Test
    void failsWhenDownstreamRejectsPermission() {
        server.expect(requestTo(BASE_URL + "/api/dept/list"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.fetch("test-token"))
                .isInstanceOf(GraphSyncException.class)
                .hasMessageContaining("权限");
    }

    private void expectBaseRequests() {
        server.expect(requestTo(BASE_URL + "/api/dept/list"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess(DEPTS, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/menu/list")).andRespond(withSuccess(MENUS, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/role/list?pageNum=1&pageSize=200"))
                .andRespond(withSuccess(ROLES, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/role/5"))
                .andRespond(withSuccess(ROLE_DETAIL, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/user/list?pageNum=1&pageSize=200"))
                .andRespond(withSuccess(USERS, MediaType.APPLICATION_JSON));
    }

    private String page(int pageNum, int total) {
        return """
                {"type":"com.iboot.response.success","source":"/api/user/list","data":{"code":200,
                 "data":{"total":%d,"pageNum":%d,"pageSize":1,"code":200,
                         "data":[{"id":%d,"username":"user%d","nickname":"用户%d","deptId":2,"status":1,
                                  "roleIds":[5]}],"message":"查询成功"},"message":"ok"}}
                """.formatted(total, pageNum, 10 + pageNum, pageNum, pageNum);
    }
}
