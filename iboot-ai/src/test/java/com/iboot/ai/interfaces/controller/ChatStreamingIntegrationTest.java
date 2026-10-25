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
import io.micrometer.core.instrument.MeterRegistry;
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
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对话接口的端到端集成测试。
 *
 * <p>使用真实 HTTP、Testcontainers Redis 与假 OpenAI 服务，覆盖非流式、流式、历史与跨用户隔离。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatStreamingIntegrationTest {

    private static final String SECRET = "iboot-admin-secret-key-for-jwt-token-generation-minimum-256-bits";

    private static final FakeOpenAiServer FAKE = FakeOpenAiServer.start();

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.0.14"))
            .withExposedPorts(6379);

    @LocalServerPort
    private int port;

    @Autowired
    private LettuceConnectionFactory connectionFactory;

    @Autowired
    private MeterRegistry meterRegistry;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private RedisTemplate<String, Object> redisTemplate;

    private String userOneToken;

    private String userTwoToken;

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
        redisTemplate = new RedisConfig().redisTemplate(connectionFactory);
        FAKE.reset();
        userOneToken = tokenFor(1L, "user-one");
        userTwoToken = tokenFor(2L, "user-two");
        session(userOneToken, 1L, "user-one");
        session(userTwoToken, 2L, "user-two");
    }

    @AfterAll
    static void stopFake() {
        FAKE.stop();
    }

    @Test
    void nonStreamingReturnsContentAndUsage() throws Exception {
        HttpResponse<String> response = httpClient.send(
                post("/api/ai/chat", userOneToken, "{\"content\":\"你好\"}"),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        Map<?, ?> body = jsonMapper.readValue(response.body(), Map.class);
        assertThat(body.get("content")).isEqualTo("你好");
        assertThat(body.get("conversationId")).isNotNull();
        assertThat(((Map<?, ?>) body.get("usage")).get("promptTokens")).isEqualTo(10);
    }

    @Test
    void streamingEmitsMetaTokensAndDone() throws Exception {
        HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(
                post("/api/ai/chat/stream", userOneToken, "{\"content\":\"你好\"}"),
                HttpResponse.BodyHandlers.ofLines());

        List<String> lines = response.body().toList();
        String joined = String.join("\n", lines);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(joined).contains("event:meta");
        assertThat(countOccurrences(joined, "event:token")).isGreaterThanOrEqualTo(2);
        assertThat(joined).contains("event:done");
        assertThat(joined).doesNotContain("event:error");
        assertThat(joined).contains("\"promptTokens\":10");
        assertThat(meterRegistry.find("iboot.ai.first_token").timer()).isNotNull();
        assertThat(meterRegistry.find("iboot.ai.first_token").timer().count()).isPositive();
    }

    @Test
    void historyIsScopedToOwner() throws Exception {
        HttpResponse<String> created = httpClient.send(
                post("/api/ai/chat", userOneToken, "{\"content\":\"你好\"}"),
                HttpResponse.BodyHandlers.ofString());
        String conversationId = String.valueOf(jsonMapper.readValue(created.body(), Map.class).get("conversationId"));

        HttpResponse<String> list = httpClient.send(
                get("/api/ai/conversations", userOneToken),
                HttpResponse.BodyHandlers.ofString());
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains(conversationId);

        HttpResponse<String> messages = httpClient.send(
                get("/api/ai/conversations/" + conversationId + "/messages", userOneToken),
                HttpResponse.BodyHandlers.ofString());
        assertThat(messages.statusCode()).isEqualTo(200);
        assertThat(messages.body()).contains("\"role\":\"USER\"").contains("\"role\":\"ASSISTANT\"");

        HttpResponse<String> otherUser = httpClient.send(
                get("/api/ai/conversations/" + conversationId + "/messages", userTwoToken),
                HttpResponse.BodyHandlers.ofString());
        assertThat(otherUser.statusCode()).isEqualTo(404);
    }

    @Test
    void usageReflectsConsumedTokens() throws Exception {
        httpClient.send(post("/api/ai/chat", userOneToken, "{\"content\":\"你好\"}"),
                HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> usage = httpClient.send(
                get("/api/ai/usage", userOneToken),
                HttpResponse.BodyHandlers.ofString());

        assertThat(usage.statusCode()).isEqualTo(200);
        assertThat(usage.body()).contains("\"used\":12").contains("\"limit\":200000");
    }

    @Test
    void upstreamErrorBecomesErrorEvent() throws Exception {
        FAKE.failWith(401, "{\"error\":{\"message\":\"Incorrect API key\",\"code\":\"invalid_api_key\"}}");

        HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(
                post("/api/ai/chat/stream", userOneToken, "{\"content\":\"你好\"}"),
                HttpResponse.BodyHandlers.ofLines());

        String joined = String.join("\n", response.body().toList());
        assertThat(joined).contains("event:error");
        assertThat(joined).doesNotContain("event:done");
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private HttpRequest get(String path, String token) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
    }

    private HttpRequest post(String path, String token, String json) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    private String tokenFor(Long userId, String username) {
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key)
                .compact();
    }

    private void session(String token, Long userId, String username) {
        Map<String, Object> session = new HashMap<>();
        session.put("userId", userId);
        session.put("username", username);
        session.put("permissions", List.of("ai:chat"));
        redisTemplate.opsForValue().set("login_tokens:" + token, session, 30, TimeUnit.MINUTES);
    }
}
