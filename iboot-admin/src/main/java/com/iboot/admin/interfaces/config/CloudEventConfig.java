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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
public class CloudEventConfig implements WebMvcConfigurer {

    /**
     * CloudEvents JSON 消息转换器
     * <p>
     * 为 CloudEventBody 类型设置特定的 Content-Type
     *
     * @param objectMapper Jackson ObjectMapper
     *
     * @return HttpMessageConverter
     */
    @Bean
    public MappingJackson2HttpMessageConverter cloudEventHttpMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        // 设置 CloudEvents 特定的 Content-Type
        supportedMediaTypes.add(MediaType.valueOf("application/cloudevents+json"));
        // 同时也支持普通 JSON
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        converter.setSupportedMediaTypes(supportedMediaTypes);
        return converter;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 将 CloudEvents 转换器添加到最前面，确保优先匹配
        ObjectMapper objectMapper = new ObjectMapper();
        MappingJackson2HttpMessageConverter converter = cloudEventHttpMessageConverter(objectMapper);
        converters.add(0, converter);
    }

}
