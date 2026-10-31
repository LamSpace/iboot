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

package com.iboot.kernel.web;

import java.io.Serializable;

/**
 * 统一 API 响应结果。
 *
 * <p>接口成功响应统一包装为该结构：{@code code} 为业务状态码（成功固定为 200），
 * {@code message} 为可直接展示的消息，{@code data} 为业务数据，
 * {@code timestamp} 为响应生成时间。消息码通过 {@link I18nUtil} 按当前请求语言解析。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see PageResult
 * @see I18nUtil
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码，成功为 200。
     */
    private Integer code;

    /**
     * 响应消息，已完成国际化解析。
     */
    private String message;

    /**
     * 响应数据；无数据时为 {@code null}。
     */
    private T data;

    /**
     * 响应生成时间的时间戳，单位毫秒。
     */
    private Long timestamp;

    /**
     * 创建空响应，并记录当前时间戳。
     */
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建包含状态码和消息的响应。
     *
     * @param code 业务状态码
     * @param message 响应消息
     */
    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建包含状态码、消息和数据的响应。
     *
     * @param code 业务状态码
     * @param message 响应消息
     * @param data 响应数据
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建无数据的成功响应。
     *
     * @param <T> 数据类型
     * @return 状态码为 200 的成功响应，{@code data} 为 {@code null}
     */
    public static <T> Result<T> success() {
        return new Result<>(200, I18nUtil.getMessage("result.success"));
    }

    /**
     * 创建包含数据的成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 状态码为 200 的成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, I18nUtil.getMessage("result.success"), data);
    }

    /**
     * 按国际化消息码创建成功响应。
     *
     * @param code 国际化消息码，用于生成 {@code message}
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 状态码为 200 的成功响应
     */
    public static <T> Result<T> successWithCode(String code, T data) {
        return new Result<>(200, I18nUtil.getMessage(code), data);
    }

    /**
     * 使用自定义消息创建成功响应。
     *
     * @param message 响应消息，不做国际化解析
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 状态码为 200 的成功响应
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 创建无数据的失败响应。
     *
     * @param <T> 数据类型
     * @return 状态码为 500 的失败响应，{@code data} 为 {@code null}
     */
    public static <T> Result<T> error() {
        return new Result<>(500, I18nUtil.getMessage("result.error"));
    }

    /**
     * 使用自定义消息创建失败响应。
     *
     * @param message 错误消息，不做国际化解析
     * @param <T> 数据类型
     * @return 状态码为 500 的失败响应
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message);
    }

    /**
     * 按国际化消息码创建失败响应。
     *
     * @param code 国际化消息码，用于生成 {@code message}
     * @param <T> 数据类型
     * @return 状态码为 500 的失败响应
     */
    public static <T> Result<T> errorWithCode(String code) {
        return new Result<>(500, I18nUtil.getMessage(code));
    }

    /**
     * 使用自定义状态码和消息创建失败响应。
     *
     * @param code 业务状态码
     * @param message 错误消息，不做国际化解析
     * @param <T> 数据类型
     * @return 指定状态码的失败响应
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message);
    }

    /**
     * 响应码
     */
    @SuppressWarnings("all")
    public Integer getCode() {
        return this.code;
    }

    /**
     * 响应码
     */
    @SuppressWarnings("all")
    public void setCode(final Integer code) {
        this.code = code;
    }

    /**
     * 响应消息
     */
    @SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    /**
     * 响应消息
     */
    @SuppressWarnings("all")
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
     * 响应数据
     */
    @SuppressWarnings("all")
    public T getData() {
        return this.data;
    }

    /**
     * 响应数据
     */
    @SuppressWarnings("all")
    public void setData(final T data) {
        this.data = data;
    }

    /**
     * 时间戳
     */
    @SuppressWarnings("all")
    public Long getTimestamp() {
        return this.timestamp;
    }

    /**
     * 时间戳
     */
    @SuppressWarnings("all")
    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Result))
            return false;
        final Result<?> other = (Result<?>) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$code = this.getCode();
        final java.lang.Object other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code))
            return false;
        final java.lang.Object this$timestamp = this.getTimestamp();
        final java.lang.Object other$timestamp = other.getTimestamp();
        if (this$timestamp == null ? other$timestamp != null : !this$timestamp.equals(other$timestamp))
            return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message))
            return false;
        final java.lang.Object this$data = this.getData();
        final java.lang.Object other$data = other.getData();
        if (this$data == null ? other$data != null : !this$data.equals(other$data))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $code = this.getCode();
        result = result * PRIME + ($code == null ? 43 : $code.hashCode());
        final java.lang.Object $timestamp = this.getTimestamp();
        result = result * PRIME + ($timestamp == null ? 43 : $timestamp.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $data = this.getData();
        result = result * PRIME + ($data == null ? 43 : $data.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "Result(code=" + this.getCode() + ", message=" + this.getMessage() + ", data=" + this.getData()
                + ", timestamp=" + this.getTimestamp() + ")";
    }

}
