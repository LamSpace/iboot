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

package com.iboot.admin.interfaces.advisor;

import com.iboot.admin.common.cloudevent.CloudEventBody;
import com.iboot.admin.common.result.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * CloudEvents 响应体拦截器
 * <p>
 * 自动将 {@link Result} 格式响应包装为 {@link CloudEventBody} 格式
 * <p>
 * 拦截范围：/api/** 路径下的所有响应
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestControllerAdvice(basePackages = "com.iboot.admin.interfaces")
public class CloudEventResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 拦截所有 Result 类型的响应（CloudEventBody 除外）
        return Result.class.isAssignableFrom(returnType.getParameterType())
                && !CloudEventBody.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 如果 body 为 null，直接返回
        if (body == null) {
            return null;
        }

        // 如果已经是 CloudEventBody，不再包装
        if (body instanceof CloudEventBody) {
            return body;
        }

        // 获取请求路径作为事件源
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String requestURI = servletRequest.getRequestURI();

        // 只拦截 /api/** 路径下的请求
        if (!requestURI.startsWith("/api/")) {
            return body;
        }

        // 如果 body 是 Result 类型，包装为 CloudEventBody
        if (body instanceof Result) {
            Result<?> result = (Result<?>) body;

            // 确定事件类型
            String eventType = determineEventType(result);

            // 使用请求 URI 作为事件源
            return CloudEventBody.fromResult(result, eventType, requestURI);
        }

        // 其他类型直接返回（不包装）
        return body;
    }

    /**
     * 根据 Result 的状态码确定事件类型
     *
     * @param result 响应结果
     * @return 事件类型
     */
    private String determineEventType(Result<?> result) {
        if (result != null && Objects.equals(result.getCode(), 200)) {
            return com.iboot.admin.common.cloudevent.CloudEventTypes.RESPONSE_SUCCESS;
        }
        return com.iboot.admin.common.cloudevent.CloudEventTypes.RESPONSE_ERROR;
    }
}
