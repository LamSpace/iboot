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

package com.iboot.ai.infrastructure.security;

import com.iboot.ai.infrastructure.config.RedisConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
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
 * 共享登录态过滤器的 Redis 集成测试。
 *
 * <p>使用真实 Redis 验证会话读取与滑动过期刷新；Docker 不可用时跳过。
 */
@Testcontainers(disabledWithoutDocker = true)
class JwtAuthenticationFilterRedisTest {

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
        redisTemplate = new RedisConfig().redisTemplate(connectionFactory);
        filter = new JwtAuthenticationFilter(new JwtTokenUtil(SECRET), redisTemplate, 30);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        connectionFactory.destroy();
    }

    @Test
    void authenticatesValidSessionAndRefreshesTtl() throws Exception {
        String token = token();
        String loginKey = JwtAuthenticationFilter.LOGIN_TOKEN_KEY + token;
        Map<String, Object> session = new HashMap<>();
        session.put("userId", 7L);
        session.put("username", "tester");
        session.put("permissions", List.of("ai:chat"));
        redisTemplate.opsForValue().set(loginKey, session, 5, TimeUnit.MINUTES);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(token), response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(7L);
        assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("tester");
        assertThat(redisTemplate.getExpire(loginKey, TimeUnit.MINUTES)).isGreaterThanOrEqualTo(29L);
    }

    @Test
    void rejectsLoggedOutSession() throws Exception {
        String token = token();

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(token), response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ai/ping");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private String token() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("tester")
                .claim("userId", 7L)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key)
                .compact();
    }
}
