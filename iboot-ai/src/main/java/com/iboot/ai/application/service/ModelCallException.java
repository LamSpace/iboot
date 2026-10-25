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
 * 模型调用失败。
 *
 * <p>携带已映射的对外错误，控制器据此返回 503 与可辨识的错误码。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ModelCallException extends RuntimeException {

    private final ChatErrorMapper.AiError error;

    /**
     * 创建异常。
     *
     * @param error 对外错误
     * @param cause 原始异常
     */
    public ModelCallException(ChatErrorMapper.AiError error, Throwable cause) {
        super(error.message(), cause);
        this.error = error;
    }

    public ChatErrorMapper.AiError getError() {
        return error;
    }
}
