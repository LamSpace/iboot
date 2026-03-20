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

package com.iboot.admin.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 版本控制配置
 * 配置 Spring Framework 7.0+ 的 API 版本控制支持
 *
 * @author iBoot
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    /**
     * 配置 API 版本解析器
     * 使用 HeaderApiVersionResolver 通过 Accept-Version 请求头解析版本号
     * SSE 端点 (/api/push/**) 不需要版本号，因为 EventSource 不支持自定义请求头
     */
    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .useVersionResolver(new org.springframework.web.accept.HeaderApiVersionResolver("Accept-Version"))
            .setVersionRequired(false);  // 不强制要求版本号，允许没有版本号的请求（如 SSE 连接）
    }

}
