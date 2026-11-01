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

package com.iboot.job.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 2 序列化配置。
 *
 * <p>Boot 4 的默认 JSON 实现是 Jackson 3（{@code tools.jackson}），本服务只保留一处 Jackson 2
 * 使用场景：备份记录中的逐表行数快照按 Jackson 2 语义序列化，与管理端写入的历史数据保持字节兼容。
 * 不加这个 Bean 时，应用启动会因缺少 {@code com.fasterxml.jackson.databind.ObjectMapper} 失败。
 *
 * <p>配置语义与管理端一致：忽略未知字段、注册 Java 时间模块，但不激活默认类型信息，
 * 避免影响 HTTP 请求与响应的序列化。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class Jackson2Config {

    /**
     * 创建 Jackson 2 的 {@link ObjectMapper}。
     *
     * @return 与管理端语义一致的映射器
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
