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

/**
 * 下游 admin 调用失败。
 *
 * <p>携带 HTTP 状态码，便于工具把"无权限"与"服务不可用"区分开反馈给模型。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class AdminApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    /**
     * 创建异常。
     *
     * @param statusCode HTTP 状态码，网络异常时为 0
     * @param message    说明
     */
    public AdminApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * 创建异常。
     *
     * @param statusCode HTTP 状态码
     * @param message    说明
     * @param cause      原始异常
     */
    public AdminApiException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * 获取 HTTP 状态码。
     *
     * @return 状态码，网络异常时为 0
     */
    public int getStatusCode() {
        return statusCode;
    }
}
