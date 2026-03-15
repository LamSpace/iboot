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

import com.iboot.admin.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * CloudEvents 事件工厂
 * <p>
 * 提供统一的 CloudEvent 事件创建方法
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class CloudEventFactory {

    /**
     * 默认事件源
     */
    private static final String DEFAULT_SOURCE = "/iboot";

    private CloudEventFactory() {
        // 防止实例化
    }

    /**
     * 创建成功事件的 CloudEvent
     *
     * @param type   事件类型
     * @param source 事件源
     * @param data   响应数据
     * @param <T>    数据类型
     * @return CloudEvent 实例
     */
    public static <T> CloudEvent createSuccessEvent(String type, String source, T data) {
        return createEvent(type, source, Result.success(data));
    }

    /**
     * 创建成功事件的 CloudEvent（带自定义消息）
     *
     * @param type    事件类型
     * @param source  事件源
     * @param message 成功消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return CloudEvent 实例
     */
    public static <T> CloudEvent createSuccessEvent(String type, String source, String message, T data) {
        return createEvent(type, source, Result.success(message, data));
    }

    /**
     * 创建错误事件的 CloudEvent
     *
     * @param type    事件类型
     * @param source  事件源
     * @param message 错误消息
     * @return CloudEvent 实例
     */
    public static CloudEvent createErrorEvent(String type, String source, String message) {
        return createEvent(type, source, Result.error(message));
    }

    /**
     * 创建错误事件的 CloudEvent（带自定义状态码）
     *
     * @param type    事件类型
     * @param source  事件源
     * @param code    错误码
     * @param message 错误消息
     * @return CloudEvent 实例
     */
    public static CloudEvent createErrorEvent(String type, String source, Integer code, String message) {
        return createEvent(type, source, Result.error(code, message));
    }

    /**
     * 从 Result 创建 CloudEvent
     *
     * @param result 响应结果
     * @param type   事件类型
     * @param source 事件源
     * @return CloudEvent 实例
     */
    public static CloudEvent fromResult(Result<?> result, String type, String source) {
        return createEvent(type, source, result);
    }

    /**
     * 从 Result 创建 CloudEvent（使用默认事件类型）
     *
     * @param result 响应结果
     * @param source 事件源
     * @return CloudEvent 实例
     */
    public static CloudEvent fromResult(Result<?> result, String source) {
        String type = determineEventType(result);
        return createEvent(type, source, result);
    }

    /**
     * 创建通用的成功响应 CloudEvent
     *
     * @param source 事件源
     * @param data   响应数据
     * @param <T>    数据类型
     * @return CloudEvent 实例
     */
    public static <T> CloudEvent createSuccessResponse(String source, T data) {
        return createSuccessEvent(CloudEventTypes.RESPONSE_SUCCESS, source, data);
    }

    /**
     * 创建通用的错误响应 CloudEvent
     *
     * @param source  事件源
     * @param message 错误消息
     * @return CloudEvent 实例
     */
    public static CloudEvent createErrorResponse(String source, String message) {
        return createErrorEvent(CloudEventTypes.RESPONSE_ERROR, source, message);
    }

    /**
     * 创建通用的错误响应 CloudEvent（带自定义状态码）
     *
     * @param source  事件源
     * @param code    错误码
     * @param message 错误消息
     * @return CloudEvent 实例
     */
    public static CloudEvent createErrorResponse(String source, Integer code, String message) {
        return createErrorEvent(CloudEventTypes.RESPONSE_ERROR, source, code, message);
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
     * 创建 CloudEvent 的核心方法
     *
     * @param type   事件类型
     * @param source 事件源
     * @param data   事件数据
     * @return CloudEvent 实例
     */
    private static CloudEvent createEvent(String type, String source, Object data) {
        URI sourceUri = source.startsWith("/") ? URI.create(source) : URI.create(DEFAULT_SOURCE + source);

        // 使用 ObjectMapper 将数据序列化为 JSON 字节
        try {
            ObjectMapper mapper = new ObjectMapper();
            byte[] jsonBytes = mapper.writeValueAsBytes(data);

            return CloudEventBuilder.v1()
                    .withId(generateEventId())
                    .withType(type)
                    .withSource(sourceUri)
                    .withTime(OffsetDateTime.now(ZoneId.systemDefault()))
                    .withDataContentType("application/json")
                    .withData(BytesCloudEventData.wrap(jsonBytes))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("创建 CloudEvent 失败", e);
        }
    }

    /**
     * 生成事件 ID
     * <p>
     * 使用 UUID 确保唯一性
     *
     * @return 事件 ID
     */
    private static String generateEventId() {
        return "ce-" + UUID.randomUUID();
    }

    /**
     * 将 CloudEvent 序列化为 JSON 字节数组
     *
     * @param event CloudEvent 实例
     * @return JSON 字节数组
     */
    public static byte[] toJsonBytes(CloudEvent event) {
        return EventFormatProvider.getInstance()
                .resolveFormat(JsonFormat.CONTENT_TYPE)
                .serialize(event);
    }

    /**
     * 从 JSON 字节数组反序列化为 CloudEvent
     *
     * @param bytes JSON 字节数组
     * @return CloudEvent 实例
     */
    public static CloudEvent fromJsonBytes(byte[] bytes) {
        return EventFormatProvider.getInstance()
                .resolveFormat(JsonFormat.CONTENT_TYPE)
                .deserialize(bytes);
    }
}
