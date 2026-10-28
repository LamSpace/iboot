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
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 把 admin 响应裁剪为可进入模型上下文的文本。
 *
 * <p>负责三件事：去掉 {@code Result} 外层包装、限制行数与字符数、递归屏蔽敏感字段。
 * 工具返回的任何内容都必须经过本类处理。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class ToolResultTrimmer {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwd", "pwd", "oldpassword", "newpassword", "salt",
            "token", "accesstoken", "refreshtoken", "authorization", "secret", "apikey", "accesskey");

    private static final String MASK = "***";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final int maxChars;

    private final int maxRows;

    /**
     * 创建裁剪器。
     *
     * @param properties AI 配置
     */
    public ToolResultTrimmer(AiProperties properties) {
        this.maxChars = Math.max(200, properties.getAgent().getMaxResultChars());
        this.maxRows = Math.max(1, properties.getAgent().getMaxResultRows());
    }

    /**
     * 返回允许的最大行数，供工具限制分页大小。
     *
     * @return 最大行数
     */
    public int maxRows() {
        return maxRows;
    }

    /**
     * 裁剪响应文本。
     *
     * @param body admin 原始响应 JSON
     * @return 面向模型的裁剪结果
     */
    public String trim(String body) {
        if (body == null || body.isBlank()) {
            return "{}";
        }
        JsonNode payload;
        try {
            JsonNode root = MAPPER.readTree(body);
            payload = unwrap(root);
        } catch (RuntimeException e) {
            return truncate(body);
        }
        JsonNode scrubbed = scrub(payload);
        String text = MAPPER.writeValueAsString(scrubbed);
        return truncate(text);
    }

    private JsonNode unwrap(JsonNode root) {
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            return root;
        }
        JsonNode rows = data.path("data");
        if (rows.isArray()) {
            ObjectNode page = MAPPER.createObjectNode();
            page.put("total", data.path("total").asLong(rows.size()));
            page.put("pageNum", data.path("pageNum").asInt(1));
            page.put("pageSize", data.path("pageSize").asInt(rows.size()));
            ArrayNode limited = MAPPER.createArrayNode();
            int count = 0;
            for (JsonNode row : rows) {
                if (count++ >= maxRows) {
                    break;
                }
                limited.add(row);
            }
            page.set("rows", limited);
            return page;
        }
        return data;
    }

    private JsonNode scrub(JsonNode node) {
        if (node.isObject()) {
            ObjectNode copy = MAPPER.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_KEYS.contains(field.getKey().toLowerCase(Locale.ROOT))) {
                    copy.put(field.getKey(), MASK);
                } else {
                    copy.set(field.getKey(), scrub(field.getValue()));
                }
            }
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = MAPPER.createArrayNode();
            int count = 0;
            for (JsonNode item : node) {
                if (count++ >= maxRows) {
                    break;
                }
                copy.add(scrub(item));
            }
            return copy;
        }
        return node;
    }

    private String truncate(String text) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "…(已截断)";
    }
}
