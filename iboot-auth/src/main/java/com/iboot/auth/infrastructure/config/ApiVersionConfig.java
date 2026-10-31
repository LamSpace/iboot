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

package com.iboot.auth.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 版本控制配置。
 *
 * <p>启用 Spring Framework 7 的 API 版本控制，通过 {@code Accept-Version} 请求头解析版本号；
 * 控制器上的 {@code version = "1"} 依赖本配置提供的策略，缺失会导致启动失败。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    /**
     * 配置版本解析器：按请求头解析，不强制要求版本号。
     *
     * @param configurer API 版本配置器
     */
    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .useVersionResolver(new org.springframework.web.accept.HeaderApiVersionResolver("Accept-Version"))
                .setVersionRequired(false);
    }
}
