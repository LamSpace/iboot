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

package com.iboot.cli.command;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 任务执行等待器测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class JobRunWaiterTest {

    /**
     * 出现成功日志时返回该日志。
     */
    @Test
    void returnsSuccessLog() {
        List<JsonNode> responses = new ArrayList<>();
        responses.add(null);
        responses.add(log(1, null));

        JsonNode result = waiter(responses).awaitTerminal();

        assertThat(result.get("status").asInt()).isEqualTo(1);
    }

    /**
     * 失败日志应以业务错误结束并带出异常信息。
     */
    @Test
    void throwsOnFailedLog() {
        List<JsonNode> responses = new ArrayList<>();
        responses.add(log(0, "boom"));

        assertThatThrownBy(() -> waiter(responses).awaitTerminal())
                .isInstanceOf(CliException.class)
                .hasMessageContaining("boom")
                .extracting(e -> ((CliException) e).exitCode())
                .isEqualTo(ExitCode.BUSINESS);
    }

    /**
     * 超时后停止轮询。
     */
    @Test
    void throwsOnTimeout() {
        List<JsonNode> responses = new ArrayList<>();
        responses.add(null);
        responses.add(null);

        assertThatThrownBy(() -> new JobRunWaiter(() -> responses.remove(0),
                Duration.ofMillis(1), Duration.ZERO, millis -> { }).awaitTerminal())
                .isInstanceOf(CliException.class)
                .hasMessageContaining("超时");
    }

    private JobRunWaiter waiter(List<JsonNode> responses) {
        return new JobRunWaiter(() -> responses.remove(0),
                Duration.ofMillis(1), Duration.ofSeconds(10), millis -> { });
    }

    private JsonNode log(int status, String exceptionInfo) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("id", 9L);
        node.put("status", status);
        if (exceptionInfo != null) {
            node.put("exceptionInfo", exceptionInfo);
        }
        return node;
    }
}
