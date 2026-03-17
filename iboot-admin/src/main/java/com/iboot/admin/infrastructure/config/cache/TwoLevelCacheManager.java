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
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 二级缓存管理器
 * <p>
 * 组合 Caffeine（L1）和 Redis（L2）实现二级缓存
 *
 * @author iBoot
 */
public class TwoLevelCacheManager implements CacheManager {

    /**
     * 缓存失效消息通道
     */
    public static final String CACHE_EVICT_CHANNEL = "cache:evict";

    private static final Logger log = LoggerFactory.getLogger(TwoLevelCacheManager.class);

    private final CacheManager l1CacheManager;

    private final CacheManager l2CacheManager;

    private final RedisTemplate<String, Object> redisTemplate;

    private final BusinessMetricsService metricsService;

    private final Map<String, TwoLevelCache> cacheMap = new ConcurrentHashMap<>();

    public TwoLevelCacheManager(CacheManager l1CacheManager, CacheManager l2CacheManager,
                                RedisTemplate<String, Object> redisTemplate, BusinessMetricsService metricsService) {
        this.l1CacheManager = l1CacheManager;
        this.l2CacheManager = l2CacheManager;
        this.redisTemplate = redisTemplate;
        this.metricsService = metricsService;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, cacheName -> {
            Cache l1Cache = l1CacheManager.getCache(cacheName);
            Cache l2Cache = l2CacheManager.getCache(cacheName);
            if (l1Cache == null || l2Cache == null) {
                log.warn("Failed to create TwoLevelCache for '{}', L1={}, L2={}", cacheName, l1Cache, l2Cache);
                return null;
            }
            log.info("Created TwoLevelCache: {}", cacheName);
            TwoLevelCache cache = new TwoLevelCache(cacheName, l1Cache, l2Cache, redisTemplate, CACHE_EVICT_CHANNEL);
            cache.setMetricsService(metricsService);
            return cache;
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        Set<String> names = new HashSet<>();
        names.addAll(l1CacheManager.getCacheNames());
        names.addAll(l2CacheManager.getCacheNames());
        return names;
    }

    /**
     * 获取二级缓存实例（供监听器使用）
     */
    public TwoLevelCache getTwoLevelCache(String name) {
        return cacheMap.get(name);
    }

}
