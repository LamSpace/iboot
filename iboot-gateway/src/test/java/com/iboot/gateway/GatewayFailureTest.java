package com.iboot.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上游不可用时的降级行为测试。
 *
 * <p>管理端指向一个没有监听的端口，AI 服务正常，用于验证：故障只影响对应前缀、
 * 返回可区分于业务错误的状态码与响应体。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayFailureTest {

    /** 一个几乎不可能被占用的端口，用于制造连接拒绝。 */
    private static final int DEAD_PORT = 19999;

    private static HttpServer aiStub;

    @Value("${local.server.port}")
    private int port;

    @BeforeAll
    static void startStub() throws IOException {
        aiStub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        aiStub.createContext("/", exchange -> {
            byte[] body = "ai-ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        aiStub.start();
    }

    @AfterAll
    static void stopStub() {
        if (aiStub != null) {
            aiStub.stop(0);
        }
    }

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) {
        registry.add("IBOOT_ADMIN_URI", () -> "http://127.0.0.1:" + DEAD_PORT);
        registry.add("IBOOT_AI_URI", () -> "http://127.0.0.1:" + aiStub.getAddress().getPort());
    }

    /**
     * 上游不可用返回服务不可用状态码，响应体可区分于业务错误。
     */
    @Test
    void returnsServiceUnavailableWhenUpstreamRefuses() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port).build();

        client.get().uri("/api/user/list")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo(503)
                .jsonPath("$.message").isEqualTo("上游服务不可用");
    }

    /**
     * 单个服务故障不影响其它前缀的路由。
     */
    @Test
    void otherPrefixStillWorks() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port).build();

        client.get().uri("/api/ai/ping")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("ai-ok"));
    }
}
