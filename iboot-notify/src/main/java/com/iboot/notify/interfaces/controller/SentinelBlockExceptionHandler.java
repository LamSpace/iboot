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

package com.iboot.notify.interfaces.controller;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.iboot.kernel.web.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 通知服务入口流控的统一响应处理器。
 *
 * <p>Sentinel Web MVC 适配器在请求被限流时调用本处理器，输出项目统一的
 * {@code Result} JSON 信封，避免默认纯文本响应与调用方的 JSON 解析约定冲突。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class SentinelBlockExceptionHandler implements BlockExceptionHandler {

    private final ObjectMapper objectMapper;

    /**
     * 创建限流响应处理器。
     *
     * @param objectMapper JSON 序列化器
     */
    public SentinelBlockExceptionHandler(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 输出限流响应。
     *
     * @param request      当前请求
     * @param response     当前响应
     * @param resourceName 被限流的资源名
     * @param blockException 阻塞异常
     * @throws IOException 写出响应失败时抛出
     */
    @Override
    public void handle(final HttpServletRequest request, final HttpServletResponse response,
                       final String resourceName, final BlockException blockException) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), Result.error(429, "请求过于频繁"));
    }
}
