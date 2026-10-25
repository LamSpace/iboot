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

/**
 * 模型调用异常到前端错误码的映射。
 *
 * <p>错误码用于前端区分“可重试”与“配置错误”，同时作为指标标签，避免把不同原因混成一个失败率。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class ChatErrorMapper {

    /**
     * 对外错误。
     *
     * @param code      错误码
     * @param message   用户可见提示
     * @param retryable 是否可重试
     */
    public record AiError(String code, String message, boolean retryable) {
    }

    private ChatErrorMapper() {
    }

    /**
     * 将异常映射为对外错误。
     *
     * @param error 异常
     * @return 错误
     */
    public static AiError toError(Throwable error) {
        String message = error == null ? "" : String.valueOf(error.getMessage());
        String lower = message.toLowerCase();

        if (lower.contains("timed out") || lower.contains("timeout")
                || error instanceof java.util.concurrent.TimeoutException) {
            return new AiError("timeout", "模型响应超时，请稍后重试", true);
        }
        if (lower.contains("401") || lower.contains("unauthorized") || lower.contains("invalid_api_key")) {
            return new AiError("unauthorized", "模型认证配置错误，请联系管理员", false);
        }
        if (lower.contains("429") || lower.contains("rate limit") || lower.contains("throttl")) {
            return new AiError("rate_limited", "模型请求过于频繁，请稍后重试", true);
        }
        return new AiError("upstream_error", "模型暂时不可用，请稍后重试", true);
    }
}
