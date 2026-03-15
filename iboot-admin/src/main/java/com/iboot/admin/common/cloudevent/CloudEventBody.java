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

package com.iboot.admin.common.cloudevent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iboot.admin.common.result.Result;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * CloudEvents 响应体包装器
 * <p>
 * 将 {@link Result} 包装为 CloudEvents 规范格式
 *
 * @param <T> 数据类型
 * @author iBoot Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudEventBody<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * CloudEvents 规范版本
     */
    @JsonProperty("specversion")
    private String specversion = "1.0";

    /**
     * 事件 ID，确保唯一性
     */
    @JsonProperty("id")
    private String id;

    /**
     * 事件类型
     */
    @JsonProperty("type")
    private String type;

    /**
     * 事件源
     */
    @JsonProperty("source")
    private String source;

    /**
     * 事件发生时间
     */
    @JsonProperty("time")
    private String time;

    /**
     * 数据内容类型
     */
    @JsonProperty("datacontenttype")
    private String datacontenttype = "application/json";

    /**
     * 实际的业务数据（原有的 Result 响应）
     */
    @JsonProperty("data")
    private Result<T> data;

    /**
     * 默认构造函数
     */
    public CloudEventBody() {
    }

    /**
     * 从 Result 构建 CloudEventBody
     *
     * @param type    事件类型
     * @param source  事件源
     * @param data    业务数据
     */
    public CloudEventBody(String type, String source, Result<T> data) {
        this.id = generateId();
        this.type = type;
        this.source = source;
        this.time = java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault())
                .toString();
        this.data = data;
    }

    /**
     * 创建成功响应的 CloudEventBody
     *
     * @param source 事件源
     * @param data   业务数据
     * @param <T>    数据类型
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> success(String source, T data) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(CloudEventTypes.RESPONSE_SUCCESS);
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(Result.success(data));
        return body;
    }

    /**
     * 创建成功响应的 CloudEventBody（带自定义消息）
     *
     * @param source  事件源
     * @param message 成功消息
     * @param data    业务数据
     * @param <T>     数据类型
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> success(String source, String message, T data) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(CloudEventTypes.RESPONSE_SUCCESS);
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(Result.success(message, data));
        return body;
    }

    /**
     * 创建错误响应的 CloudEventBody
     *
     * @param source  事件源
     * @param message 错误消息
     * @param <T>     数据类型
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> error(String source, String message) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(CloudEventTypes.RESPONSE_ERROR);
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(Result.error(message));
        return body;
    }

    /**
     * 创建错误响应的 CloudEventBody（带自定义状态码）
     *
     * @param source  事件源
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> error(String source, Integer code, String message) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(CloudEventTypes.RESPONSE_ERROR);
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(Result.error(code, message));
        return body;
    }

    /**
     * 从 Result 构建 CloudEventBody
     *
     * @param result 响应结果
     * @param source 事件源
     * @param <T>    数据类型
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> fromResult(Result<T> result, String source) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(determineEventType(result));
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(result);
        return body;
    }

    /**
     * 从 Result 构建 CloudEventBody（带指定事件类型）
     *
     * @param result 响应结果
     * @param type   事件类型
     * @param source 事件源
     * @param <T>    数据类型
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> fromResult(Result<T> result, String type, String source) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(type);
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(result);
        return body;
    }

    /**
     * 根据 Result 的状态码确定事件类型
     *
     * @param result 响应结果
     * @return 事件类型
     */
    private static String determineEventType(Result<?> result) {
        if (result != null && result.getCode() != null && result.getCode() == 200) {
            return CloudEventTypes.RESPONSE_SUCCESS;
        }
        return CloudEventTypes.RESPONSE_ERROR;
    }

    /**
     * 生成事件 ID
     *
     * @return 事件 ID
     */
    private static String generateId() {
        return "ce-" + java.util.UUID.randomUUID();
    }
}
