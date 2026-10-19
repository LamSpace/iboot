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

package com.iboot.admin.common.exception;

/**
 * 业务异常。
 *
 * <p>用于表达可预期的业务规则失败，例如数据不存在、唯一性冲突、状态不允许等。
 * 默认错误码为 {@code 500}；需要区分错误类型时可使用带 {@code code} 的构造器。
 *
 * <p>该异常是 {@link RuntimeException}，由 {@link GlobalExceptionHandler} 统一转换为
 * 标准响应。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see GlobalExceptionHandler
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 业务错误码，默认 500。
     */
    private final Integer code;

    /**
     * 业务错误消息。
     */
    private final String message;

    /**
     * 使用默认错误码 500 创建业务异常。
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.message = message;
        this.code = 500;
    }

    /**
     * 使用指定错误码创建业务异常。
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 使用默认错误码 500 和根因创建业务异常。
     *
     * @param message 错误消息
     * @param cause 根因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.code = 500;
    }

    /**
     * 使用指定错误码和根因创建业务异常。
     *
     * @param code 错误码
     * @param message 错误消息
     * @param cause 根因
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 错误码
     */
    @SuppressWarnings("all")
    public Integer getCode() {
        return this.code;
    }

    /**
     * 错误消息
     */
    @SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

}
