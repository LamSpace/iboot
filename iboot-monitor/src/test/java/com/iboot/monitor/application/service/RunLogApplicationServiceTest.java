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

package com.iboot.monitor.application.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * {@link RunLogApplicationService} 的行为测试。
 *
 * <p>覆盖检索后端不可用时的行为：传输层故障向上抛出明确的错误（由内核全局异常处理转成
 * 业务错误），而不是返回一个「成功但为空」的假列表——后者会让运维误以为真的没有日志。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RunLogApplicationServiceTest {

    /**
     * Elasticsearch 不可达时抛出明确异常，不返回伪造的空结果。
     */
    @Test
    void searchSurfacesBackendFailure() {
        ElasticsearchClient client = mock(ElasticsearchClient.class, invocation -> {
            throw new IllegalStateException("connection refused");
        });
        RunLogApplicationService service = new RunLogApplicationService(client);

        assertThatThrownBy(() -> service.searchRunLogs(null, null, null, null, null, null, 1, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("connection refused");
        assertThatThrownBy(() -> service.countRunLogs(null, null, null, null, null, null))
                .isInstanceOf(IllegalStateException.class);
    }
}
