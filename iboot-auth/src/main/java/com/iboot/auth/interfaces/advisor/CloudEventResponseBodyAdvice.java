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

package com.iboot.auth.interfaces.advisor;

import com.iboot.auth.common.cloudevent.CloudEventBody;
import com.iboot.kernel.web.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * CloudEvents 响应体拦截器。
 *
 * <p>对 {@code /api/**} 下返回 {@link Result} 的接口，将响应体自动包装为
 * {@link CloudEventBody}，并把请求路径作为事件源。{@code basePackages} 必须指向本服务的
 * 控制器包，否则响应结构会少一层 {@code data}，前端与回归脚本都会解析失败。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see CloudEventBody
 */
@RestControllerAdvice(basePackages = "com.iboot.auth.interfaces")
public class CloudEventResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 判断是否需要对当前返回值应用响应包装。
     *
     * @param returnType 方法返回类型
     * @param converterType 选中的消息转换器类型
     * @return 返回类型为 {@link Result} 且不是 {@link CloudEventBody} 时返回 {@code true}
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        String returnTypeName = returnType.getParameterType().getName();
        if (returnTypeName.startsWith("io.swagger") || returnTypeName.startsWith("org.springdoc")) {
            return false;
        }
        return Result.class.isAssignableFrom(returnType.getParameterType())
                && !CloudEventBody.class.isAssignableFrom(returnType.getParameterType());
    }

    /**
     * 将 {@link Result} 响应包装为 {@link CloudEventBody}。
     *
     * <p>非 {@code /api/**} 请求、空响应和已包装的响应直接返回原值。
     *
     * @param body 原始响应体
     * @param returnType 方法返回类型
     * @param selectedContentType 选中的内容类型
     * @param selectedConverterType 选中的消息转换器类型
     * @param request 当前请求
     * @param response 当前响应
     * @return 包装后的 CloudEventBody，或原始响应体
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null || body instanceof CloudEventBody) {
            return body;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String requestURI = servletRequest.getRequestURI();
        if (!requestURI.startsWith("/api/")) {
            return body;
        }
        if (body instanceof Result<?> result) {
            return CloudEventBody.fromResult(result, requestURI);
        }
        return body;
    }
}
