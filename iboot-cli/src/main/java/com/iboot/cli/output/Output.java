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

package com.iboot.cli.output;

import com.iboot.cli.http.ApiResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 命令输出。
 *
 * <p>默认输出人类可读表格；{@code --json} 输出解包后的业务数据；{@code --raw} 输出原始
 * 响应。列表响应会识别常见的数组字段（{@code rows}、{@code records}、{@code list}、
 * {@code items}），避免把分页元数据铺成表格。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class Output {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final int MAX_CELL_WIDTH = 40;

    private Output() {
    }

    /**
     * 输出响应。
     *
     * @param response 已解包响应
     * @param json 是否输出 JSON
     * @param raw 是否输出原始响应
     */
    public static void print(ApiResponse response, boolean json, boolean raw) {
        if (raw) {
            System.out.println(response.rawBody() == null ? "" : response.rawBody());
            return;
        }
        JsonNode data = response.data();
        if (json) {
            System.out.println(data == null
                    ? "{}"
                    : MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data));
            return;
        }
        if (data == null) {
            System.out.println(response.message() == null ? "（无数据）" : response.message());
            return;
        }
        JsonNode array = arrayField(data);
        if (array != null) {
            printArray(array);
        } else if (data.isObject()) {
            printObject(data);
        } else if (data.isArray()) {
            printArray(data);
        } else {
            System.out.println(cell(data));
        }
    }

    /**
     * 输出一条消息。
     *
     * @param message 消息
     */
    public static void message(String message) {
        System.out.println(message);
    }

    private static JsonNode arrayField(JsonNode data) {
        if (data == null || !data.isObject()) {
            return null;
        }
        for (String name : List.of("rows", "records", "list", "items")) {
            JsonNode value = data.get(name);
            if (value != null && value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private static void printArray(JsonNode array) {
        if (array.size() == 0) {
            System.out.println("（空）");
            return;
        }
        JsonNode first = array.get(0);
        if (first == null || !first.isObject()) {
            for (int i = 0; i < array.size(); i++) {
                System.out.println(cell(array.get(i)));
            }
            return;
        }
        List<String> headers = new ArrayList<>();
        for (Map.Entry<String, JsonNode> entry : first.properties()) {
            headers.add(entry.getKey());
        }
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JsonNode element = array.get(i);
            List<String> row = new ArrayList<>();
            for (String header : headers) {
                row.add(cell(element.get(header)));
            }
            rows.add(row);
        }
        printTable(headers, rows);
    }

    private static void printObject(JsonNode object) {
        for (Map.Entry<String, JsonNode> entry : object.properties()) {
            System.out.printf("%-24s %s%n", entry.getKey(), cell(entry.getValue()));
        }
    }

    private static void printTable(List<String> headers, List<List<String>> rows) {
        int columns = headers.size();
        int[] widths = new int[columns];
        for (int i = 0; i < columns; i++) {
            widths[i] = visibleWidth(headers.get(i));
        }
        for (List<String> row : rows) {
            for (int i = 0; i < columns; i++) {
                widths[i] = Math.max(widths[i], visibleWidth(row.get(i)));
            }
        }
        for (int i = 0; i < columns; i++) {
            widths[i] = Math.min(widths[i], MAX_CELL_WIDTH);
        }
        printRow(headers, widths);
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < columns; i++) {
            separator.append("-".repeat(widths[i]));
            if (i < columns - 1) {
                separator.append("  ");
            }
        }
        System.out.println(separator);
        for (List<String> row : rows) {
            printRow(row, widths);
        }
    }

    private static void printRow(List<String> row, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            String value = truncate(row.get(i), widths[i]);
            line.append(pad(value, widths[i]));
            if (i < widths.length - 1) {
                line.append("  ");
            }
        }
        System.out.println(line);
    }

    private static String cell(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isObject() || node.isArray()) {
            return node.toString();
        }
        return node.asString();
    }

    private static int visibleWidth(String value) {
        return value == null ? 0 : value.length();
    }

    private static String truncate(String value, int width) {
        if (value == null) {
            return "";
        }
        if (value.length() <= width) {
            return value;
        }
        if (width <= 3) {
            return value.substring(0, width);
        }
        return value.substring(0, width - 3) + "...";
    }

    private static String pad(String value, int width) {
        StringBuilder result = new StringBuilder(value);
        while (result.length() < width) {
            result.append(' ');
        }
        return result.toString();
    }
}
