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

package com.iboot.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.sun.net.httpserver.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关路由级限流的集成测试。
 *
 * <p>用本地桩服务扮演通知服务与 AI 服务，验证被限流的路由返回统一的 429 JSON，
 * 且同一时刻其它路由不受影响。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayFlowControlTest {

    private static HttpServer notifyStub;

    private static HttpServer aiStub;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private List<GlobalFilter> globalFilters;

    private WebTestClient client;

    private WebTestClient webTestClient() {
        if (client == null) {
            client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        }
        return client;
    }

    private static HttpServer startStub(final String name) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = (name + "|" + exchange.getRequestURI().getPath())
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain;charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        return server;
    }

    @BeforeAll
    static void startStubs() throws IOException {
        notifyStub = startStub("notify");
        aiStub = startStub("ai");
    }

    @AfterAll
    static void stopStubs() {
        if (notifyStub != null) {
            notifyStub.stop(0);
        }
        if (aiStub != null) {
            aiStub.stop(0);
        }
    }

    @DynamicPropertySource
    static void routes(final DynamicPropertyRegistry registry) {
        registry.add("IBOOT_NOTIFY_URI", () -> "http://127.0.0.1:" + notifyStub.getAddress().getPort());
        registry.add("IBOOT_AI_URI", () -> "http://127.0.0.1:" + aiStub.getAddress().getPort());
    }

    @BeforeEach
    void loadFlowRule() {
        FlowRuleManager.loadRules(List.of(new FlowRule("notify")
                .setGrade(1)
                .setCount(1)));
        org.assertj.core.api.Assertions.assertThat(FlowRuleManager.getRules()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(globalFilters)
                .anyMatch(filter -> filter instanceof SentinelGatewayFilter);
    }

    @AfterEach
    void clearFlowRule() {
        FlowRuleManager.loadRules(List.of());
    }

    /**
     * 超限路由返回统一的 429 JSON，且其它路由仍正常。
     */
    @Test
    void limitsOnlyTheConfiguredRoute() {
        webTestClient().get().uri("/api/message/list")
                .exchange()
                .expectStatus().isOk();

        webTestClient().get().uri("/api/message/list")
                .exchange()
                .expectStatus().isEqualTo(429)
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.code").isEqualTo(429)
                .jsonPath("$.message").isEqualTo("请求过于频繁")
                .jsonPath("$.path").isEqualTo("/api/message/list");

        webTestClient().get().uri("/api/ai/ping")
                .exchange()
                .expectStatus().isOk();
    }
}
