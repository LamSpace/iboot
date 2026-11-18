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
import com.iboot.cli.http.ApiResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 分页合并逻辑测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class PaginationTest {

    /**
     * `--all` 应合并全部页并保持 total 与 data 结构。
     */
    @Test
    void allMergesPages() {
        TestCommand command = new TestCommand();
        command.pages.put("1", page(100, 250, "u1"));
        command.pages.put("2", page(100, 250, "u101"));
        command.pages.put("3", page(50, 250, "u201"));
        PageOptions options = new PageOptions();
        options.all = true;

        ApiResponse response = command.fetch(options);

        JsonNode data = response.data();
        assertThat(data.get("total").asLong()).isEqualTo(250);
        assertThat(data.get("pageSize").asInt()).isEqualTo(100);
        assertThat(data.get("data").size()).isEqualTo(250);
        assertThat(command.calls).hasSize(3);
        assertThat(command.calls.get(0).get("pageSize")).isEqualTo("100");
        assertThat(command.calls.get(2).get("pageNum")).isEqualTo("3");
    }

    /**
     * 超过安全上限时应拒绝，不返回不完整结果。
     */
    @Test
    void allRejectsOverLimit() {
        TestCommand command = new TestCommand();
        command.pages.put("1", page(100, 10_001, "u1"));
        PageOptions options = new PageOptions();
        options.all = true;

        assertThatThrownBy(() -> command.fetch(options))
                .isInstanceOf(CliException.class)
                .extracting(e -> ((CliException) e).exitCode())
                .isEqualTo(ExitCode.USAGE);
    }

    /**
     * 非分页响应不支持 `--all`。
     */
    @Test
    void allRejectsNonPageResponse() {
        TestCommand command = new TestCommand();
        command.pages.put("1", new ApiResponse(200, "ok",
                JsonNodeFactory.instance.arrayNode().add("x"), null));
        PageOptions options = new PageOptions();
        options.all = true;

        assertThatThrownBy(() -> command.fetch(options))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("分页");
    }

    /**
     * 未指定 `--all` 时保持单页请求。
     */
    @Test
    void withoutAllRequestsSinglePage() {
        TestCommand command = new TestCommand();
        command.pages.put("2", page(20, 100, "u21"));
        PageOptions options = new PageOptions();
        options.page = 2;
        options.size = 20;

        ApiResponse response = command.fetch(options);

        assertThat(command.calls).hasSize(1);
        assertThat(command.calls.get(0).get("pageNum")).isEqualTo("2");
        assertThat(command.calls.get(0).get("pageSize")).isEqualTo("20");
        assertThat(response.data().get("data").size()).isEqualTo(20);
    }

    private ApiResponse page(int size, long total, String firstId) {
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put("total", total);
        data.put("pageNum", 1);
        data.put("pageSize", 100);
        ArrayNode rows = data.putArray("data");
        for (int i = 0; i < size; i++) {
            rows.add(firstId + "-" + i);
        }
        return new ApiResponse(200, "查询成功", data, null);
    }

    /**
     * 拦截 HTTP 的测试命令。
     */
    static class TestCommand extends BaseCommand {

        private final Map<String, ApiResponse> pages = new LinkedHashMap<>();

        private final List<Map<String, String>> calls = new ArrayList<>();

        @Override
        protected ApiResponse get(String path, Map<String, String> query) {
            calls.add(new LinkedHashMap<>(query));
            return pages.get(query.get("pageNum"));
        }

        @Override
        public void run() {
            // 测试只调用 fetch。
        }

        ApiResponse fetch(PageOptions options) {
            return getPaged("/api/demo", options, Map.of());
        }
    }
}
