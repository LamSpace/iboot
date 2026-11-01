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

package com.iboot.notify.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 配置。
 *
 * <p>只注册 SSE 跨实例广播所需的消息监听容器；会话读写用的 {@code redisTemplate}
 * 由共享内核提供，序列化方式与管理端一致。本服务不启用二级缓存，因此不定义缓存管理器。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see RedisTemplate
 */
@Configuration
public class RedisConfig {

    /**
     * 创建 Redis 消息监听容器。
     *
     * @param connectionFactory Redis 连接工厂
     * @return 消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            final RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
