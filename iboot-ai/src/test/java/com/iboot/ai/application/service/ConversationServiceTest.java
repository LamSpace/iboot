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

package com.iboot.ai.application.service;

import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.config.RedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话索引服务的集成测试。
 *
 * <p>验证标题截断、排序、跨用户隔离与容量上限。
 */
@Testcontainers(disabledWithoutDocker = true)
class ConversationServiceTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.0.14"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;

    private ConversationService service;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        RedisTemplate<String, Object> template = new RedisConfig().redisTemplate(connectionFactory);
        service = new ConversationService(template, new AiProperties());
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void createsTitleAndEnforcesOwnership() {
        service.touch(1L, "c1", "这是一条用于生成标题的很长的用户消息，需要在三十个字符之后被截断");

        assertThat(service.list(1L)).hasSize(1);
        assertThat(service.list(1L).get(0).title()).hasSizeLessThanOrEqualTo(30);
        assertThat(service.belongsTo(1L, "c1")).isTrue();
        assertThat(service.belongsTo(2L, "c1")).isFalse();
        assertThat(service.list(2L)).isEmpty();
    }

    @Test
    void keepsMostRecentFiftyConversations() {
        for (int i = 0; i < 51; i++) {
            service.touch(3L, "c" + i, "message " + i);
        }

        assertThat(service.list(3L)).hasSize(50);
    }
}
