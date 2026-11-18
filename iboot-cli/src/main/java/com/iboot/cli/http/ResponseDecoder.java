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

package com.iboot.cli.http;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 响应解包与错误映射。
 *
 * <p>先按结构识别 CloudEvents：存在 {@code specversion} 与对象类型的 {@code data} 时
 * 取内部 {@code Result}；再按数字型 {@code code} 判断业务成功。直接返回 DTO 的服务
 * （例如 {@code iboot-ai}）原样保留。HTTP 401/403/5xx 与业务码非 200 都转换为带
 * 退出码的 {@link CliException}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ResponseDecoder {

    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * 解包响应。
     *
     * @param status HTTP 状态码
     * @param body 响应体
     * @param raw 是否跳过解包
     * @return 已解包响应
     * @throws CliException HTTP 或业务错误
     */
    public ApiResponse decode(int status, String body, boolean raw) {
        if (raw) {
            return new ApiResponse(status, null, null, body);
        }
        if (status == 401) {
            throw new CliException(ExitCode.AUTH, message(body, status));
        }
        if (status == 403) {
            throw new CliException(ExitCode.FORBIDDEN, message(body, status));
        }
        if (status >= 500) {
            throw new CliException(ExitCode.NETWORK, message(body, status));
        }

        JsonNode root = read(body);
        if (root == null) {
            if (status >= 400) {
                throw new CliException(ExitCode.BUSINESS, "HTTP " + status);
            }
            return new ApiResponse(status == 0 ? 200 : status, null, null, body);
        }

        JsonNode payload = root;
        if (root.has("specversion") && root.get("data") != null && root.get("data").isObject()) {
            payload = root.get("data");
        }

        int code = status == 0 ? 200 : status;
        String message = text(payload, "message");
        JsonNode data = payload;
        if (payload.has("code") && payload.get("code").isNumber()) {
            code = payload.get("code").asInt();
            message = text(payload, "message");
            data = payload.get("data");
            if (code != 200) {
                throw businessError(code, message);
            }
        }

        if (status >= 400) {
            throw businessError(status, message != null ? message : "HTTP " + status);
        }
        return new ApiResponse(code, message, data, body);
    }

    private JsonNode read(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return mapper.readTree(body);
        } catch (RuntimeException e) {
            throw new CliException(ExitCode.BUSINESS, "响应不是合法 JSON", e);
        }
    }

    private CliException businessError(int code, String message) {
        int exitCode = code == 401 || code == 40101
                ? ExitCode.AUTH
                : code == 403 ? ExitCode.FORBIDDEN : ExitCode.BUSINESS;
        return new CliException(exitCode, message == null ? "业务错误：" + code : message);
    }

    private String message(String body, int status) {
        if (body != null && !body.isBlank()) {
            try {
                JsonNode root = mapper.readTree(body);
                String value = text(root, "message");
                if (value != null && !value.isBlank()) {
                    return value;
                }
                value = text(root, "error");
                if (value != null && !value.isBlank()) {
                    return value;
                }
            } catch (RuntimeException ignored) {
                // 非 JSON 错误体按状态码提示。
            }
        }
        return "HTTP " + status;
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field)) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }
}
