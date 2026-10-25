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

package com.iboot.ai.application.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 错误映射测试。
 */
class ChatErrorMapperTest {

    @Test
    void mapsTimeout() {
        var error = ChatErrorMapper.toError(new TimeoutException("timed out"));
        assertThat(error.code()).isEqualTo("timeout");
        assertThat(error.retryable()).isTrue();
    }

    @Test
    void mapsUnauthorized() {
        var error = ChatErrorMapper.toError(new RuntimeException("401 Unauthorized"));
        assertThat(error.code()).isEqualTo("unauthorized");
        assertThat(error.retryable()).isFalse();
    }

    @Test
    void mapsRateLimit() {
        var error = ChatErrorMapper.toError(new RuntimeException("429 rate limit"));
        assertThat(error.code()).isEqualTo("rate_limited");
        assertThat(error.retryable()).isTrue();
    }

    @Test
    void defaultsToUpstreamError() {
        var error = ChatErrorMapper.toError(new RuntimeException("boom"));
        assertThat(error.code()).isEqualTo("upstream_error");
        assertThat(error.retryable()).isTrue();
    }
}
