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

package com.iboot.admin.interfaces.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * CloudEvents 配置类
 * <p>
 * 配置 CloudEvents 响应体的序列化和 Content-Type
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class CloudEventConfig {

    /**
     * CloudEvents JSON 消息转换器
     * <p>
     * 为 CloudEventBody 类型设置特定的 Content-Type
     *
     * @return HttpMessageConverter
     */
    @Bean
    public JacksonJsonHttpMessageConverter cloudEventHttpMessageConverter() {
        JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        // 只设置 CloudEvents 特定的 Content-Type，避免影响其他 JSON 响应（如 springdoc）
        supportedMediaTypes.add(MediaType.valueOf("application/cloudevents+json"));
        converter.setSupportedMediaTypes(supportedMediaTypes);
        return converter;
    }

}
