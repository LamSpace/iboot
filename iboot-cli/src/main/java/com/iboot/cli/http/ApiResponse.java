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

import tools.jackson.databind.JsonNode;

/**
 * 已解包的业务响应。
 *
 * <p>{@code data} 是 CloudEvents 与 {@code Result} 解包后的业务数据；直接返回 DTO 的
 * 服务则原样放入。{@code rawBody} 保留原始响应，供 {@code --raw} 使用。
 *
 * @param code 业务码；直接 DTO 响应等于 HTTP 状态码
 * @param message 业务消息，可为 {@code null}
 * @param data 业务数据，可为 {@code null}
 * @param rawBody 原始响应体
 * @author iBoot Team
 * @since 1.0.0
 */
public record ApiResponse(int code, String message, JsonNode data, String rawBody) {

    /**
     * 判断业务是否成功。
     *
     * @return 业务码为 200 时返回 {@code true}
     */
    public boolean success() {
        return code == 200;
    }
}
