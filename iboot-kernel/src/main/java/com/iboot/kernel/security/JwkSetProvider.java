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

package com.iboot.kernel.security;

import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.security.PublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务公钥集的本地缓存。
 *
 * <p>资源服务器用公钥验签自包含令牌，不需要在每次请求时访问认证服务。缓存只在
 * {@code kid} 未命中时回源刷新，并按最小刷新间隔限流，避免伪造令牌的 {@code kid}
 * 把认证服务打成放大攻击的入口。
 *
 * <p>实例在进程内共享，缓存整体替换、读取不加锁；刷新方法本身是同步的。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class JwkSetProvider {

    private static final Logger log = LoggerFactory.getLogger(JwkSetProvider.class);

    /**
     * 两次回源之间的最小间隔，防止未知 {@code kid} 触发频繁刷新。
     */
    private static final long DEFAULT_MIN_REFRESH_INTERVAL_MILLIS = 5_000L;

    /**
     * 拉取公钥集的连接与读取超时。
     */
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(2);

    /**
     * 公钥集地址。
     */
    private final String jwkSetUri;

    /**
     * 拉取公钥集的客户端，已设置连接与读取超时。
     */
    private final RestClient restClient;

    /**
     * 两次回源之间的最小间隔（毫秒）。
     */
    private final long minRefreshIntervalMillis;

    /**
     * 按 {@code kid} 索引的公钥快照；整体替换，读取无需加锁。
     */
    private volatile Map<String, PublicKey> keys = Map.of();

    /**
     * 上次回源的时间戳（毫秒）。
     */
    private volatile long lastRefreshAt = 0L;

    /**
     * 使用默认超时与刷新间隔创建实例。
     *
     * @param jwkSetUri 公钥集地址
     */
    public JwkSetProvider(String jwkSetUri) {
        this(jwkSetUri, newRestClient(), DEFAULT_MIN_REFRESH_INTERVAL_MILLIS);
    }

    /**
     * 使用默认超时创建实例，并指定最小刷新间隔。
     *
     * @param jwkSetUri 公钥集地址
     * @param minRefreshIntervalMillis 两次回源之间的最小间隔（毫秒）
     */
    public JwkSetProvider(String jwkSetUri, long minRefreshIntervalMillis) {
        this(jwkSetUri, newRestClient(), minRefreshIntervalMillis);
    }

    /**
     * 使用指定的客户端创建实例，供测试与手工装配使用。
     *
     * @param jwkSetUri 公钥集地址
     * @param restClient 拉取公钥集的客户端
     * @param minRefreshIntervalMillis 两次回源之间的最小间隔（毫秒）
     */
    public JwkSetProvider(String jwkSetUri, RestClient restClient, long minRefreshIntervalMillis) {
        this.jwkSetUri = jwkSetUri;
        this.restClient = restClient;
        this.minRefreshIntervalMillis = minRefreshIntervalMillis;
    }

    /**
     * 按 {@code kid} 取公钥。
     *
     * <p>命中缓存直接返回；未命中时触发一次受频率限制的回源。未携带 {@code kid}
     * 且缓存中只有一把公钥时按该公钥处理。
     *
     * @param keyId 令牌头部的 {@code kid}，可为 {@code null}
     * @return 公钥；无法确定时返回 {@code null}
     */
    public PublicKey resolve(String keyId) {
        PublicKey cached = lookup(keyId);
        if (cached != null) {
            return cached;
        }
        refreshIfStale();
        return lookup(keyId);
    }

    /**
     * 立即回源刷新公钥集。
     *
     * <p>失败时保留既有缓存，只记录告警；这样认证服务短暂不可用不会让在线用户掉线。
     */
    public synchronized void refresh() {
        lastRefreshAt = System.currentTimeMillis();
        try {
            String body = restClient.get().uri(jwkSetUri).retrieve().body(String.class);
            JwkSet jwkSet = Jwks.setParser().build().parse(body);
            Map<String, PublicKey> parsed = new HashMap<>();
            for (Jwk<?> jwk : jwkSet.getKeys()) {
                if (jwk.toKey() instanceof PublicKey publicKey && jwk.getId() != null) {
                    parsed.put(jwk.getId(), publicKey);
                }
            }
            keys = Map.copyOf(parsed);
            log.debug("公钥集已刷新，共 {} 把", keys.size());
        } catch (Exception e) {
            log.warn("刷新认证服务公钥集失败：{}", e.getMessage());
        }
    }

    /**
     * 当前缓存的公钥数量。
     *
     * @return 公钥数量
     */
    public int size() {
        return keys.size();
    }

    /**
     * 仅在超过最小刷新间隔时回源。
     */
    private void refreshIfStale() {
        if (System.currentTimeMillis() - lastRefreshAt < minRefreshIntervalMillis) {
            return;
        }
        refresh();
    }

    /**
     * 从缓存快照中查找公钥。
     *
     * @param keyId 令牌头部的 {@code kid}，可为 {@code null}
     * @return 公钥；无法确定时返回 {@code null}
     */
    private PublicKey lookup(String keyId) {
        Map<String, PublicKey> snapshot = keys;
        if (keyId != null) {
            return snapshot.get(keyId);
        }
        return snapshot.size() == 1 ? snapshot.values().iterator().next() : null;
    }

    /**
     * 创建带超时的拉取客户端。
     *
     * @return 拉取公钥集的客户端
     */
    private static RestClient newRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(FETCH_TIMEOUT);
        factory.setReadTimeout(FETCH_TIMEOUT);
        return RestClient.builder().requestFactory(factory).build();
    }
}
