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
 * Embedding 调用异常。
 *
 * <p>{@code code} 用于入库状态机的可辨识失败原因，例如 {@code DIMENSION_MISMATCH}、
 * {@code EMBEDDING_COUNT_MISMATCH}、{@code EMBEDDING_FAILED}、{@code EMBEDDING_QUOTA}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class EmbeddingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    /**
     * 创建异常。
     *
     * @param code    失败原因码
     * @param message 错误说明
     */
    public EmbeddingException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 创建异常。
     *
     * @param code    失败原因码
     * @param message 错误说明
     * @param cause   原始异常
     */
    public EmbeddingException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 失败原因码。
     *
     * @return 原因码
     */
    public String code() {
        return code;
    }
}
