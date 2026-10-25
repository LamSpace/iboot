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

package com.iboot.ai.interfaces.controller;

import com.iboot.ai.infrastructure.config.RedisConfig;
import com.iboot.ai.support.FakeOpenAiServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 配额拦截的端到端测试。
 *
 * <p>把单用户配额设为 5，第一次调用消耗 12 个 token；第二次必须在调用模型前返回 429。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "iboot.ai.quota.user-daily-tokens=5")
@ActiveProfiles("test")
class QuotaEnforcementIntegrationTest {

    private static final String SECRET = "iboot-admin-secret-key-for-jwt-token-generation-minimum-256-bits";

    private static final FakeOpenAiServer FAKE = FakeOpenAiServer.start();

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.0.14"))
            .withExposedPorts(6379);

    @LocalServerPort
    private int port;

    @Autowired
    private LettuceConnectionFactory connectionFactory;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private String token;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.ai.openai.base-url", FAKE::baseUrl);
        registry.add("spring.ai.openai.chat.base-url", FAKE::baseUrl);
        registry.add("spring.ai.openai.embedding.base-url", FAKE::baseUrl);
        registry.add("spring.ai.openai.api-key", () -> "test-key");
        registry.add("spring.ai.openai.chat.api-key", () -> "test-key");
        registry.add("spring.ai.openai.embedding.api-key", () -> "test-key");
    }

    @BeforeEach
    void setUp() {
        connectionFactory.getConnection().serverCommands().flushAll();
        FAKE.reset();
        token = tokenFor(1L);
        Map<String, Object> session = new HashMap<>();
        session.put("userId", 1L);
        session.put("username", "tester");
        session.put("permissions", List.of("ai:chat"));
        new RedisConfig().redisTemplate(connectionFactory)
                .opsForValue().set("login_tokens:" + token, session, 30, TimeUnit.MINUTES);
    }

    @AfterAll
    static void stopFake() {
        FAKE.stop();
    }

    @Test
    void rejectsSecondCallWithoutInvokingModel() throws Exception {
        HttpResponse<String> first = httpClient.send(post(), HttpResponse.BodyHandlers.ofString());
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(FAKE.requests()).isEqualTo(1);

        HttpResponse<String> second = httpClient.send(post(), HttpResponse.BodyHandlers.ofString());
        assertThat(second.statusCode()).isEqualTo(429);
        assertThat(second.body()).contains("quota_exceeded");
        assertThat(FAKE.requests()).isEqualTo(1);
    }

    private HttpRequest post() {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/ai/chat"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"content\":\"你好\"}"))
                .build();
    }

    private String tokenFor(Long userId) {
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("tester")
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key)
                .compact();
    }
}
