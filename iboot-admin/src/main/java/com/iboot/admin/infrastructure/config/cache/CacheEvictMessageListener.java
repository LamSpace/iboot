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

package com.iboot.admin.infrastructure.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.nio.charset.StandardCharsets;

/**
 * 缓存失效消息监听器
 * <p>
 * 监听 Redis Pub/Sub 消息，当其他节点发布缓存失效消息时，
 * 清除本节点的 L1 本地缓存，保证分布式环境下的缓存一致性。
 *
 * @author iBoot
 */
@Slf4j
public class CacheEvictMessageListener implements MessageListener {

    private final TwoLevelCacheManager cacheManager;

    public CacheEvictMessageListener(TwoLevelCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
        log.debug("Received cache evict message: {}", messageBody);

        try {
            // 消息格式: cacheName:key
            int separatorIndex = messageBody.indexOf(':');
            if (separatorIndex > 0) {
                String cacheName = messageBody.substring(0, separatorIndex);
                String key = messageBody.substring(separatorIndex + 1);

                TwoLevelCache cache = cacheManager.getTwoLevelCache(cacheName);
                if (cache != null) {
                    cache.evictLocal(key);
                    log.debug("Evicted local cache: {}:{}", cacheName, key);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process cache evict message: {}", messageBody, e);
        }
    }

    /**
     * 注册监听器到 Redis 消息监听容器
     */
    public static void register(RedisMessageListenerContainer container,
                                TwoLevelCacheManager cacheManager) {
        CacheEvictMessageListener listener = new CacheEvictMessageListener(cacheManager);
        MessageListenerAdapter adapter = new MessageListenerAdapter(listener, "onMessage");
        adapter.afterPropertiesSet();
        container.addMessageListener(adapter, new ChannelTopic(TwoLevelCacheManager.CACHE_EVICT_CHANNEL));
        log.info("Registered cache evict message listener on channel: {}",
                TwoLevelCacheManager.CACHE_EVICT_CHANNEL);
    }
}
