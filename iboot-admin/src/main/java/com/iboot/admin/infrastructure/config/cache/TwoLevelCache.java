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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Callable;

/**
 * 二级缓存实现。
 *
 * <p>L1 使用 Caffeine 本地缓存（毫秒级响应），L2 使用 Redis 分布式缓存（跨节点共享）。
 * 读取顺序为 L1 → L2 → 数据源；写入同时更新 L1 和 L2；失效时清除两层缓存，
 * 并通过 Redis Pub/Sub 通知其他节点清除 L1。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see TwoLevelCacheManager
 */
public class TwoLevelCache implements Cache {

    private static final Logger log = LoggerFactory.getLogger(TwoLevelCache.class);

    private final String name;

    private final Cache l1Cache;

    private final Cache l2Cache;

    private final RedisTemplate<String, Object> redisTemplate;

    private final String evictChannel;

    private BusinessMetricsService metricsService;

    /**
     * 创建二级缓存实例。
     *
     * @param name 缓存名称
     * @param l1Cache 本地缓存
     * @param l2Cache Redis 缓存
     * @param redisTemplate Redis 模板
     * @param evictChannel 失效消息通道
     */
    public TwoLevelCache(String name, Cache l1Cache, Cache l2Cache, RedisTemplate<String, Object> redisTemplate,
                         String evictChannel) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
        this.redisTemplate = redisTemplate;
        this.evictChannel = evictChannel;
    }

    /**
     * 注入业务指标服务。
     *
     * @param metricsService 业务指标服务
     */
    public void setMetricsService(BusinessMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    /**
     * 获取缓存名称。
     *
     * @return 缓存名称
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 获取底层缓存实现。
     *
     * @return 当前缓存实例本身
     */
    @Override
    public Object getNativeCache() {
        return this;
    }

    /**
     * 读取缓存值。
     *
     * <p>先查 L1，未命中再查 L2 并回填 L1。
     *
     * @param key 缓存键
     * @return 缓存值包装；未命中时返回 {@code null}
     */
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

    /**
     * 按类型读取缓存值。
     *
     * @param key 缓存键
     * @param type 期望类型
     * @param <T> 值类型
     * @return 缓存值；未命中时返回 {@code null}
     * @throws IllegalStateException 缓存值类型不匹配时抛出
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) {
            return null;
        }
        Object value = wrapper.get();
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    /**
     * 读取缓存值，未命中时加载并写入。
     *
     * @param key 缓存键
     * @param valueLoader 值加载器
     * @param <T> 值类型
     * @return 缓存值
     * @throws ValueRetrievalException 加载失败时抛出
     */
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

    /**
     * 写入缓存。
     *
     * <p>同时写入 L1 和 L2；值为 {@code null} 时忽略。
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    @Override
    public void put(Object key, Object value) {
        if (value == null) {
            return;
        }
        log.debug("Put cache: {}:{}", name, key);
        l1Cache.put(key, value);
        l2Cache.put(key, value);
    }

    /**
     * 仅当键不存在时写入缓存。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @return 已存在的值；写入成功时返回 {@code null}
     */
    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existingValue = get(key);
        if (existingValue == null) {
            put(key, value);
            return null;
        }
        return existingValue;
    }

    /**
     * 清除指定键。
     *
     * <p>同时清除 L1、L2，并广播失效消息。
     *
     * @param key 缓存键
     */
    @Override
    public void evict(Object key) {
        log.debug("Evict cache: {}:{}", name, key);
        l1Cache.evict(key);
        l2Cache.evict(key);
        // 发布缓存失效消息，通知其他节点清除 L1
        publishEvictMessage(key.toString());
    }

    /**
     * 清除指定键。
     *
     * @param key 缓存键
     * @return 键存在并已清除时返回 {@code true}
     */
    @Override
    public boolean evictIfPresent(Object key) {
        boolean existed = get(key) != null;
        evict(key);
        return existed;
    }

    /**
     * 清空当前缓存的所有键。
     *
     * <p>同时清空 L1、L2，并广播全量失效消息。
     */
    @Override
    public void clear() {
        log.debug("Clear cache: {}", name);
        l1Cache.clear();
        l2Cache.clear();
        // 发布全量清除消息
        publishEvictMessage("*");
    }

    /**
     * 清空缓存。
     *
     * @return 始终返回 {@code true}
     */
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
