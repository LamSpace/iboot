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
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的会话记忆仓储。
 *
 * <p>官方 Redis 仓储依赖 RediSearch 与 RedisJSON，而项目使用标准 Redis 7.0.14，因此这里只用
 * string 与 set 操作：每个会话存一个 JSON 数组，另用 set 维护会话 ID 索引。所有 key 均带 TTL。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Repository
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    /**
     * 会话消息 key 前缀。
     */
    public static final String MESSAGE_KEY_PREFIX = "iboot:ai:chat:";

    /**
     * 会话 ID 索引 key。
     */
    public static final String CONVERSATION_INDEX_KEY = "iboot:ai:chat:conversations";

    private static final String ROLE = "role";

    private static final String TEXT = "text";

    private final RedisTemplate<String, Object> redisTemplate;

    private final long ttlHours;

    /**
     * 创建仓储。
     *
     * @param redisTemplate 共享 Redis 模板
     * @param aiProperties  AI 配置
     */
    public RedisChatMemoryRepository(RedisTemplate<String, Object> redisTemplate, AiProperties aiProperties) {
        this.redisTemplate = redisTemplate;
        this.ttlHours = aiProperties.getChat().getConversationTtlHours();
    }

    @Override
    public List<String> findConversationIds() {
        var members = redisTemplate.opsForSet().members(CONVERSATION_INDEX_KEY);
        return members == null ? List.of() : members.stream().map(String::valueOf).toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Object value = redisTemplate.opsForValue().get(messageKey(conversationId));
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        List<Message> messages = new ArrayList<>(items.size());
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                messages.add(toMessage(String.valueOf(map.get(ROLE)), String.valueOf(map.get(TEXT))));
            }
        }
        return messages;
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        List<Map<String, String>> stored = new ArrayList<>(messages.size());
        for (Message message : messages) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put(ROLE, message.getMessageType().name());
            item.put(TEXT, message.getText());
            stored.add(item);
        }
        redisTemplate.opsForValue().set(messageKey(conversationId), stored, ttlHours, TimeUnit.HOURS);
        redisTemplate.opsForSet().add(CONVERSATION_INDEX_KEY, conversationId);
        redisTemplate.expire(CONVERSATION_INDEX_KEY, ttlHours, TimeUnit.HOURS);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(messageKey(conversationId));
        redisTemplate.opsForSet().remove(CONVERSATION_INDEX_KEY, conversationId);
    }

    private String messageKey(String conversationId) {
        return MESSAGE_KEY_PREFIX + conversationId;
    }

    private Message toMessage(String role, String text) {
        return switch (role) {
            case "ASSISTANT" -> new AssistantMessage(text);
            case "SYSTEM" -> new SystemMessage(text);
            default -> new UserMessage(text);
        };
    }
}
