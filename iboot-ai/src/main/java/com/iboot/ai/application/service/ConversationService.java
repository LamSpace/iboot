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
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户会话索引。
 *
 * <p>Spring AI 的会话记忆只负责消息，不提供“会话列表”。这里为每个用户维护一个有上限的
 * Redis Hash，保存会话 ID、标题与更新时间，用于 portal 的会话列表和跨用户访问校验。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class ConversationService {

    /**
     * 用户会话索引 key 前缀。
     */
    public static final String USER_KEY_PREFIX = "iboot:ai:conversations:";

    private static final int MAX_CONVERSATIONS = 50;

    private static final int TITLE_LENGTH = 30;

    private final RedisTemplate<String, Object> redisTemplate;

    private final long ttlHours;

    /**
     * 创建会话索引服务。
     *
     * @param redisTemplate 共享 Redis 模板
     * @param aiProperties  AI 配置
     */
    public ConversationService(RedisTemplate<String, Object> redisTemplate, AiProperties aiProperties) {
        this.redisTemplate = redisTemplate;
        this.ttlHours = aiProperties.getChat().getConversationTtlHours();
    }

    /**
     * 会话摘要。
     *
     * @param conversationId 会话 ID
     * @param title          会话标题
     * @param updatedAt      更新时间（epoch 毫秒）
     */
    public record ConversationSummary(String conversationId, String title, long updatedAt) {
    }

    /**
     * 新增或更新会话条目。
     *
     * @param userId         用户 ID
     * @param conversationId 会话 ID
     * @param firstMessage   首条用户消息
     */
    public void touch(Long userId, String conversationId, String firstMessage) {
        String key = userKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        Object existing = entries.get(conversationId);
        String title = existing instanceof Map<?, ?> map && map.get("title") != null
                ? String.valueOf(map.get("title"))
                : titleOf(firstMessage);

        Map<String, Object> value = new LinkedHashMap<>();
        value.put("title", title);
        value.put("updatedAt", Instant.now().toEpochMilli());
        redisTemplate.opsForHash().put(key, conversationId, value);
        redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);

        if (entries.size() >= MAX_CONVERSATIONS) {
            evictOldest(key);
        }
    }

    /**
     * 列出用户的会话，按更新时间倒序。
     *
     * @param userId 用户 ID
     * @return 会话摘要
     */
    public List<ConversationSummary> list(Long userId) {
        List<ConversationSummary> summaries = new ArrayList<>();
        redisTemplate.opsForHash().entries(userKey(userId)).forEach((id, raw) -> {
            if (raw instanceof Map<?, ?> map) {
                summaries.add(new ConversationSummary(
                        String.valueOf(id),
                        String.valueOf(map.get("title")),
                        Long.parseLong(String.valueOf(map.get("updatedAt")))));
            }
        });
        summaries.sort(Comparator.comparingLong(ConversationSummary::updatedAt).reversed());
        return summaries;
    }

    /**
     * 判断会话是否属于该用户。
     *
     * @param userId         用户 ID
     * @param conversationId 会话 ID
     * @return 是否属于
     */
    public boolean belongsTo(Long userId, String conversationId) {
        return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(userKey(userId), conversationId));
    }

    private void evictOldest(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        entries.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Map<?, ?>)
                .min(Comparator.comparingLong(entry ->
                        Long.parseLong(String.valueOf(((Map<?, ?>) entry.getValue()).get("updatedAt")))))
                .ifPresent(oldest -> redisTemplate.opsForHash().delete(key, oldest.getKey()));
    }

    private String titleOf(String firstMessage) {
        String normalized = StringUtils.normalizeSpace(StringUtils.defaultString(firstMessage));
        return StringUtils.abbreviate(normalized, TITLE_LENGTH);
    }

    private String userKey(Long userId) {
        return USER_KEY_PREFIX + userId;
    }
}
