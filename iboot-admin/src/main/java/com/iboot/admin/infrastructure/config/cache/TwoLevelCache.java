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

import com.iboot.admin.application.service.BusinessMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Callable;

/**
 * 二级缓存实现
 * <p>
 * L1: Caffeine 本地缓存（毫秒级响应）
 * L2: Redis 分布式缓存（跨节点共享）
 * <p>
 * 读取策略：L1 -> L2 -> DB（穿透写回）
 * 写入策略：同时写入 L1 + L2
 * 失效策略：同时清除 L1 + L2，并通过 Redis Pub/Sub 通知其他节点清除 L1
 *
 * @author iBoot
 */
@Slf4j
public class TwoLevelCache implements Cache {

    private final String name;
    private final Cache l1Cache;
    private final Cache l2Cache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String evictChannel;
    private BusinessMetricsService metricsService;

    public TwoLevelCache(String name, Cache l1Cache, Cache l2Cache,
                         RedisTemplate<String, Object> redisTemplate, String evictChannel) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
        this.redisTemplate = redisTemplate;
        this.evictChannel = evictChannel;
    }

    public void setMetricsService(BusinessMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        // 1. 先查 L1 本地缓存
        ValueWrapper wrapper = l1Cache.get(key);
        if (wrapper != null) {
            log.debug("L1 cache hit: {}:{}", name, key);
            if (metricsService != null) {
                metricsService.recordCacheHit();
            }
            return wrapper;
        }

        // 2. L1 未命中，查 L2 Redis 缓存
        wrapper = l2Cache.get(key);
        if (wrapper != null) {
            log.debug("L2 cache hit, backfill L1: {}:{}", name, key);
            if (metricsService != null) {
                metricsService.recordCacheHit();
            }
            // 回填 L1
            l1Cache.put(key, wrapper.get());
            return wrapper;
        }

        log.debug("Cache miss: {}:{}", name, key);
        if (metricsService != null) {
            metricsService.recordCacheMiss();
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) {
            return null;
        }
        Object value = wrapper.get();
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException(
                    "Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }

        // 缓存未命中，调用 valueLoader 获取值
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (value == null) {
            return;
        }
        log.debug("Put cache: {}:{}", name, key);
        l1Cache.put(key, value);
        l2Cache.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existingValue = get(key);
        if (existingValue == null) {
            put(key, value);
            return null;
        }
        return existingValue;
    }

    @Override
    public void evict(Object key) {
        log.debug("Evict cache: {}:{}", name, key);
        l1Cache.evict(key);
        l2Cache.evict(key);
        // 发布缓存失效消息，通知其他节点清除 L1
        publishEvictMessage(key.toString());
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean existed = get(key) != null;
        evict(key);
        return existed;
    }

    @Override
    public void clear() {
        log.debug("Clear cache: {}", name);
        l1Cache.clear();
        l2Cache.clear();
        // 发布全量清除消息
        publishEvictMessage("*");
    }

    @Override
    public boolean invalidate() {
        clear();
        return true;
    }

    /**
     * 仅清除本地 L1 缓存（供 Pub/Sub 监听器调用）
     */
    public void evictLocal(Object key) {
        if ("*".equals(key)) {
            l1Cache.clear();
        } else {
            l1Cache.evict(key);
        }
    }

    /**
     * 发布缓存失效消息
     */
    private void publishEvictMessage(String key) {
        if (redisTemplate != null && evictChannel != null) {
            String message = name + ":" + key;
            redisTemplate.convertAndSend(evictChannel, message);
            log.debug("Published evict message: {}", message);
        }
    }
}
