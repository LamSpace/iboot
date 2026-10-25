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

package com.iboot.ai.infrastructure.persistence;

import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.config.RedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自定义 Redis 会话记忆仓储的集成测试。
 *
 * <p>验证窗口截断、TTL 与删除语义。
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisChatMemoryRepositoryTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.0.14"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;

    private RedisTemplate<String, Object> redisTemplate;

    private RedisChatMemoryRepository repository;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new RedisConfig().redisTemplate(connectionFactory);
        repository = new RedisChatMemoryRepository(redisTemplate, new AiProperties());
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void keepsOnlyWindowAndSetsTtl() {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(3)
                .build();

        memory.add("conversation-1", List.of(
                new UserMessage("first"),
                new AssistantMessage("second"),
                new UserMessage("third"),
                new AssistantMessage("fourth")));

        var messages = memory.get("conversation-1");
        assertThat(messages).hasSizeLessThanOrEqualTo(3);
        assertThat(messages).isNotEmpty();
        assertThat(messages.get(messages.size() - 1).getText()).isEqualTo("fourth");

        Long ttlHours = redisTemplate.getExpire(
                RedisChatMemoryRepository.MESSAGE_KEY_PREFIX + "conversation-1", TimeUnit.HOURS);
        assertThat(ttlHours).isGreaterThanOrEqualTo(23L);
    }

    @Test
    void clearsMessagesAndIndex() {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
        memory.add("conversation-2", new UserMessage("hello"));

        memory.clear("conversation-2");

        assertThat(memory.get("conversation-2")).isEmpty();
        assertThat(repository.findConversationIds()).doesNotContain("conversation-2");
    }
}
