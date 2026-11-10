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

package com.iboot.workflow.infrastructure.quota;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.kernel.web.BusinessException;
import com.iboot.workflow.infrastructure.config.SeataXidClientInterceptor;
import com.iboot.workflow.infrastructure.config.WorkflowLeaveQuotaProperties;
import org.apache.seata.core.context.RootContext;
import org.junit.jupiter.api.AfterEach;
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
 * 额度划转 HTTP 客户端测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class HttpLeaveQuotaTransferClientTest {

    private static final String OK_JSON = """
            {"code":200,"message":"ok","data":{"transferId":"t-1","status":"TRYING"}}
            """;

    private MockRestServiceServer server;

    private HttpLeaveQuotaTransferClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://iboot-system")
                .requestInterceptor(new SeataXidClientInterceptor());
        server = MockRestServiceServer.bindTo(builder).build();
        WorkflowLeaveQuotaProperties properties = new WorkflowLeaveQuotaProperties();
        properties.setInternalToken("secret");
        client = new HttpLeaveQuotaTransferClient(builder.build(), properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        RootContext.unbind();
        server.verify();
    }

    /**
     * 验证全局事务内划转会透传 XID 与内部令牌。
     */
    @Test
    void transferPropagatesXidAndToken() {
        RootContext.bind("127.0.0.1:8091:1");
        server.expect(requestTo("http://iboot-system/internal/leave/quota/transfer/try"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("TX_XID", "127.0.0.1:8091:1"))
                .andExpect(header("X-Internal-Token", "secret"))
                .andRespond(withSuccess(OK_JSON, MediaType.APPLICATION_JSON));

        client.transfer("t-1", 1L, 2L, 2026, 1);
    }

    /**
     * 验证没有全局事务时失败关闭且不发起请求。
     */
    @Test
    void transferWithoutXidFailsClosed() {
        assertThatThrownBy(() -> client.transfer("t-1", 1L, 2L, 2026, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(503));
    }

    /**
     * 验证额度不足映射为 400。
     */
    @Test
    void transferMapsInsufficientQuota() {
        RootContext.bind("127.0.0.1:8091:2");
        server.expect(requestTo("http://iboot-system/internal/leave/quota/transfer/try"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":400,\"message\":\"请假额度不足\"}"));

        assertThatThrownBy(() -> client.transfer("t-1", 1L, 2L, 2026, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(400));
    }

    /**
     * 验证取消墓碑映射为 409。
     */
    @Test
    void transferMapsCancelledBranch() {
        RootContext.bind("127.0.0.1:8091:3");
        server.expect(requestTo("http://iboot-system/internal/leave/quota/transfer/try"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":409,\"message\":\"划转已取消\"}"));

        assertThatThrownBy(() -> client.transfer("t-1", 1L, 2L, 2026, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(409));
    }

    /**
     * 验证下游 503 映射为额度服务不可用。
     */
    @Test
    void transferMapsServiceUnavailable() {
        RootContext.bind("127.0.0.1:8091:4");
        server.expect(requestTo("http://iboot-system/internal/leave/quota/transfer/try"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":503,\"message\":\"系统服务未启用分布式事务\"}"));

        assertThatThrownBy(() -> client.transfer("t-1", 1L, 2L, 2026, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(503));
    }
}
