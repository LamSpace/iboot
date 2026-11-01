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
 * {@link JobApiClient} 的契约测试。
 *
 * <p>用 MockRestServiceServer 断言调度服务的路径、身份头与错误映射。备份接口已随拆分
 * 迁出管理端，因此这些路径不再由 {@link AdminApiClientTest} 覆盖。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class JobApiClientTest {

    private MockRestServiceServer server;

    private JobApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new JobApiClient(builder.baseUrl("http://job.test").build());
    }

    /**
     * 备份概览携带令牌与版本头。
     */
    @Test
    void backupSummarySendsTokenAndVersion() {
        server.expect(requestTo("http://job.test/api/monitor/backup/summary"))
                .andExpect(header("Authorization", "Bearer tk-1"))
                .andExpect(header("Accept-Version", "1"))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"enabled\":true}}",
                        MediaType.APPLICATION_JSON));

        String body = client.backupSummary("tk-1");

        assertThat(body).contains("\"enabled\":true");
        server.verify();
    }

    /**
     * 触发备份使用 POST。
     */
    @Test
    void triggerBackupUsesPost() {
        server.expect(requestTo("http://job.test/api/monitor/backup/records"))
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
        server.expect(requestTo("http://job.test/api/monitor/backup/summary"))
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
        assertThatThrownBy(() -> client.triggerBackup(null))
                .isInstanceOf(AdminApiException.class)
                .hasMessage("缺少登录令牌");
        server.verify();
    }
}
