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

import com.iboot.ai.infrastructure.config.AiProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ToolResultTrimmer} 的单元测试。
 *
 * <p>验证分页包装展开、行数上限、敏感字段屏蔽与字符截断。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ToolResultTrimmerTest {

    /**
     * 分页响应展开为 total/pageNum/pageSize/rows。
     */
    @Test
    void unwrapsPageEnvelope() {
        String body = """
                {"code":200,"message":"ok","data":{"code":200,"data":[{"id":1,"username":"alice"}],
                 "total":1,"pageNum":1,"pageSize":10}}
                """;

        String trimmed = trimmer(2_000, 20).trim(body);

        assertThat(trimmed).contains("\"total\":1").contains("\"rows\"").contains("alice");
        assertThat(trimmed).doesNotContain("\"code\"");
    }

    /**
     * 行数超过上限时只保留前 N 行。
     */
    @Test
    void limitsRows() {
        String body = """
                {"code":200,"data":{"data":[{"id":1},{"id":2},{"id":3}],"total":3,"pageNum":1,"pageSize":3}}
                """;

        String trimmed = trimmer(2_000, 2).trim(body);

        assertThat(trimmed).contains("\"id\":1").contains("\"id\":2").doesNotContain("\"id\":3");
        assertThat(trimmed).contains("\"total\":3");
    }

    /**
     * 密码与令牌字段被替换为掩码。
     */
    @Test
    void masksSensitiveFields() {
        String body = """
                {"code":200,"data":{"username":"alice","password":"secret-hash","accessToken":"tk-1",
                 "profile":{"secret":"s"}}}
                """;

        String trimmed = trimmer(2_000, 20).trim(body);

        assertThat(trimmed).doesNotContain("secret-hash").doesNotContain("tk-1");
        assertThat(trimmed).contains("\"password\":\"***\"").contains("\"accessToken\":\"***\"");
    }

    /**
     * 超出字符上限时截断并标记。
     */
    @Test
    void truncatesLongText() {
        String body = "{\"code\":200,\"data\":{\"text\":\"" + "a".repeat(500) + "\"}}";

        String trimmed = trimmer(200, 20).trim(body);

        assertThat(trimmed.length()).isLessThanOrEqualTo(220);
        assertThat(trimmed).endsWith("…(已截断)");
    }

    private ToolResultTrimmer trimmer(int maxChars, int maxRows) {
        AiProperties properties = new AiProperties();
        properties.getAgent().setMaxResultChars(maxChars);
        properties.getAgent().setMaxResultRows(maxRows);
        return new ToolResultTrimmer(properties);
    }
}
