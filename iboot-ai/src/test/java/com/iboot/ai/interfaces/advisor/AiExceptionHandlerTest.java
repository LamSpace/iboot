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

package com.iboot.ai.interfaces.advisor;

import com.iboot.ai.application.service.ChatErrorMapper;
import com.iboot.ai.application.service.ConversationNotFoundException;
import com.iboot.ai.application.service.ModelCallException;
import com.iboot.ai.application.service.QuotaExceededException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异常到 HTTP 状态码的映射测试。
 */
class AiExceptionHandlerTest {

    private final AiExceptionHandler handler = new AiExceptionHandler();

    @Test
    void quotaMapsTo429() {
        var response = handler.handleQuota(new QuotaExceededException("user", "quota used up"));
        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getBody()).containsEntry("code", "quota_exceeded");
    }

    @Test
    void missingConversationMapsTo404() {
        var response = handler.handleNotFound(new ConversationNotFoundException("c1"));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void modelFailureMapsTo503() {
        var response = handler.handleModelCall(new ModelCallException(
                new ChatErrorMapper.AiError("upstream_error", "unavailable", true),
                new RuntimeException("boom")));
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).containsEntry("retryable", true);
    }
}
