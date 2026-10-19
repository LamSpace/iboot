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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.nio.charset.StandardCharsets;

/**
 * 缓存失效消息监听器。
 *
 * <p>监听 Redis Pub/Sub 的缓存失效消息，当其他节点发布失效事件时清除本节点的
 * L1 本地缓存，保证分布式环境下的缓存一致性。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class CacheEvictMessageListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictMessageListener.class);

    private final TwoLevelCacheManager cacheManager;

    /**
     * 创建缓存失效监听器。
     *
     * @param cacheManager 二级缓存管理器
     */
    public CacheEvictMessageListener(TwoLevelCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 注册监听器到 Redis 消息监听容器
     */
    public static void register(RedisMessageListenerContainer container, TwoLevelCacheManager cacheManager) {
        CacheEvictMessageListener listener = new CacheEvictMessageListener(cacheManager);
        MessageListenerAdapter adapter = new MessageListenerAdapter(listener, "onMessage");
        adapter.afterPropertiesSet();
        container.addMessageListener(adapter, new ChannelTopic(TwoLevelCacheManager.CACHE_EVICT_CHANNEL));
        log.info("Registered cache evict message listener on channel: {}", TwoLevelCacheManager.CACHE_EVICT_CHANNEL);
    }

    /**
     * 处理缓存失效消息。
     *
     * <p>消息格式为 {@code cacheName:key}，收到后清除本节点对应的 L1 缓存；
     * {@code *} 表示清空全部 L1。
     *
     * @param message Redis 消息
     * @param pattern 订阅模式
     */
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

}
