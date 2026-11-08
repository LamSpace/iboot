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

package com.iboot.notify.interfaces.controller;

import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通知服务限流响应的行为测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class SentinelBlockExceptionHandlerTest {

    /**
     * 被限流时返回项目统一的 JSON 信封。
     */
    @Test
    void writesUnifiedJsonWhenBlocked() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SentinelBlockExceptionHandler handler = new SentinelBlockExceptionHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, "/internal/notifications",
                new FlowException("/internal/notifications"));

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.path("code").asInt()).isEqualTo(429);
        assertThat(body.path("message").asText()).isEqualTo("请求过于频繁");
    }
}
