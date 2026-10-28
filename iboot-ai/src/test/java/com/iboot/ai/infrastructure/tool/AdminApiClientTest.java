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

package com.iboot.ai.infrastructure.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link AdminApiClient} 的契约测试。
 *
 * <p>用 MockRestServiceServer 断言下游路径、查询参数、身份头与错误映射。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class AdminApiClientTest {

    private MockRestServiceServer server;

    private AdminApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AdminApiClient(builder.baseUrl("http://admin.test").build());
    }

    /**
     * 查询用户携带令牌、版本头与分页参数。
     */
    @Test
    void queryUsersSendsTokenAndPaging() {
        server.expect(requestTo("http://admin.test/api/user/list?pageNum=1&pageSize=10&username=ali&status=1"))
                .andExpect(header("Authorization", "Bearer tk-1"))
                .andExpect(header("Accept-Version", "1"))
                .andRespond(withSuccess("{\"code\":200,\"data\":[]}", MediaType.APPLICATION_JSON));

        String body = client.queryUsers("tk-1", "ali", 1, 1, 10);

        assertThat(body).contains("\"code\":200");
        server.verify();
    }


    /**
     * 登录日志查询携带筛选条件。
     */
    @Test
    void queryLoginLogsSendsFilters() {
        server.expect(requestTo("http://admin.test/api/log/login/list?pageNum=1&pageSize=10"
                        + "&username=ali&ipAddress=10.0.0.1&status=0"))
                .andExpect(header("Authorization", "Bearer tk-1"))
                .andRespond(withSuccess("{\"code\":200,\"data\":[]}", MediaType.APPLICATION_JSON));

        client.queryLoginLogs("tk-1", "ali", "10.0.0.1", 0, null, null, 1, 10);

        server.verify();
    }

    /**
     * 触发备份使用 POST。
     */
    @Test
    void triggerBackupUsesPost() {
        server.expect(requestTo("http://admin.test/api/monitor/backup/records"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tk-1"))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"id\":7}}", MediaType.APPLICATION_JSON));

        String body = client.triggerBackup("tk-1");

        assertThat(body).contains("\"id\":7");
        server.verify();
    }

    /**
     * 下游 403 映射为可辨识异常。
     */
    @Test
    void mapsForbiddenToException() {
        server.expect(requestTo("http://admin.test/api/monitor/backup/summary"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.backupSummary("tk-1"))
                .isInstanceOf(AdminApiException.class)
                .hasMessage("无权访问下游接口");
    }

    /**
     * 缺少令牌时不发起请求。
     */
    @Test
    void rejectsMissingToken() {
        assertThatThrownBy(() -> client.queryUsers(null, null, null, 1, 10))
                .isInstanceOf(AdminApiException.class)
                .hasMessage("缺少登录令牌");
        server.verify();
    }
}
