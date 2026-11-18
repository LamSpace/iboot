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

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 个人访问令牌的过滤器行为测试。
 *
 * <p>使用真实 Redis 验证 PAT 的固定有效期、撤销判定与在线名单隔离；Docker 不可用时跳过。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class JwtAuthenticationFilterPatTest {

    private static final String SECRET = "iboot-admin-secret-key-for-jwt-token-generation-minimum-256-bits";

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.0.14"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;

    private RedisTemplate<String, Object> redisTemplate;

    private JwtAuthenticationFilter filter;

    private SecretKey key;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.afterPropertiesSet();
        JwkSetProvider jwkSetProvider = new JwkSetProvider("http://localhost:1/oauth2/jwks");
        filter = new JwtAuthenticationFilter(new JwtTokenVerifier(SECRET, jwkSetProvider, true), redisTemplate,
                new JwtSecurityConfig(), new SecurityWhitelistProperties());
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        connectionFactory.destroy();
    }

    /**
     * 有效 PAT 建立认证上下文，并且不滑动自身 TTL、不改写在线映射。
     *
     * @throws Exception 过滤器执行失败时抛出
     */
    @Test
    void authenticatesPatWithoutSlidingSessionOrOnlineMapping() throws Exception {
        String jti = "pat-jti-1";
        String token = patToken(jti, Instant.now().plusSeconds(3600));
        Map<String, Object> pat = new HashMap<>();
        pat.put("userId", 7L);
        pat.put("name", "ci");
        redisTemplate.opsForValue().set(Constants.PAT_TOKEN_KEY + jti, pat, 120, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(Constants.ONLINE_TOKEN_KEY + 7L, "portal-session", 5, TimeUnit.MINUTES);

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(7L);
        assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("tester");
        assertThat(redisTemplate.getExpire(Constants.PAT_TOKEN_KEY + jti, TimeUnit.MINUTES))
                .isGreaterThanOrEqualTo(100L);
        assertThat(redisTemplate.getExpire(Constants.ONLINE_TOKEN_KEY + 7L, TimeUnit.MINUTES))
                .isLessThanOrEqualTo(6L);
        assertThat(redisTemplate.hasKey(Constants.PAT_LAST_USED_KEY + jti)).isTrue();
    }

    /**
     * PAT 不应创建在线映射。
     *
     * @throws Exception 过滤器执行失败时抛出
     */
    @Test
    void doesNotCreateOnlineMapping() throws Exception {
        String jti = "pat-jti-2";
        String token = patToken(jti, Instant.now().plusSeconds(3600));
        redisTemplate.opsForValue().set(Constants.PAT_TOKEN_KEY + jti, Map.of("userId", 7L), 60, TimeUnit.MINUTES);

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(redisTemplate.hasKey(Constants.ONLINE_TOKEN_KEY + 7L)).isFalse();
    }

    /**
     * 有效期内的 PAT 不因闲置而失效，也不会被重置为交互式会话的 30 分钟。
     *
     * @throws Exception 过滤器执行失败时抛出
     */
    @Test
    void keepsLongLivedPatWithoutRenewalWindow() throws Exception {
        String jti = "pat-jti-idle";
        String token = patToken(jti, Instant.now().plusSeconds(86_400));
        redisTemplate.opsForValue().set(Constants.PAT_TOKEN_KEY + jti, Map.of("userId", 7L), 5, TimeUnit.DAYS);

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(redisTemplate.getExpire(Constants.PAT_TOKEN_KEY + jti, TimeUnit.DAYS))
                .isGreaterThanOrEqualTo(4L);
    }

    /**
     * PAT 声明中的权限必须再按 scope 取交集。
     *
     * @throws Exception 过滤器执行失败时抛出
     */
    @Test
    void intersectsPermissionsWithScopes() throws Exception {
        String jti = "pat-jti-scope";
        String token = patToken(jti, Instant.now().plusSeconds(3600),
                List.of("user:list", "user:add"), List.of("user:list"));
        redisTemplate.opsForValue().set(Constants.PAT_TOKEN_KEY + jti, Map.of("userId", 7L), 60, TimeUnit.MINUTES);

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("user:list");
    }

    /**
     * 没有 scopes 声明的旧 PAT 保持全部声明的权限。
     *
     * @throws Exception 过滤器执行失败时抛出
     */
    @Test
    void keepsDeclaredPermissionsWithoutScopes() throws Exception {
        String jti = "pat-jti-no-scope";
        String token = patToken(jti, Instant.now().plusSeconds(3600),
                List.of("user:list", "user:add"), List.of());
        redisTemplate.opsForValue().set(Constants.PAT_TOKEN_KEY + jti, Map.of("userId", 7L), 60, TimeUnit.MINUTES);

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("user:list", "user:add");
    }

    /**
     * 撤销后（共享存储键不存在）不再建立认证上下文。
     *
     * @throws Exception 过滤器执行失败时抛出
     */
    @Test
    void rejectsRevokedPat() throws Exception {
        String token = patToken("pat-jti-revoked", Instant.now().plusSeconds(3600));

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /**
     * 过期 PAT 即使撤销记录仍在也必须拒绝。
     *
     * @throws Exception 过滤器执行失败时抛出
     */
    @Test
    void rejectsExpiredPat() throws Exception {
        String jti = "pat-jti-expired";
        String token = patToken(jti, Instant.now().minusSeconds(60));
        redisTemplate.opsForValue().set(Constants.PAT_TOKEN_KEY + jti, Map.of("userId", 7L), 60, TimeUnit.MINUTES);

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/list");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private String patToken(String jti, Instant expiration) {
        return patToken(jti, expiration, List.of("user:list"), List.of());
    }

    private String patToken(String jti, Instant expiration, List<String> permissions, List<String> scopes) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .id(jti)
                .subject("7")
                .claim("userId", 7L)
                .claim("username", "tester")
                .claim("permissions", permissions)
                .claim(Constants.PAT_CLAIM, true)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration));
        if (!scopes.isEmpty()) {
            builder.claim(Constants.PAT_SCOPES_CLAIM, scopes);
        }
        return builder.signWith(key).compact();
    }
}
