package com.iboot.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关路由与跨域的集成测试。
 *
 * <p>用两个本地桩服务分别扮演管理端与 AI 服务，验证：具体前缀优先、兜底前缀承接其余路径、
 * 跨域预检由网关直接应答而不转发到后端。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static HttpServer adminStub;

    private static HttpServer aiStub;

    @Value("${local.server.port}")
    private int port;

    private WebTestClient client;

    private WebTestClient webTestClient() {
        if (client == null) {
            client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        }
        return client;
    }

    private static HttpServer startStub(String name) throws IOException {
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
        adminStub = startStub("admin");
        aiStub = startStub("ai");
    }

    @AfterAll
    static void stopStubs() {
        if (adminStub != null) {
            adminStub.stop(0);
        }
        if (aiStub != null) {
            aiStub.stop(0);
        }
    }

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) {
        registry.add("IBOOT_ADMIN_URI", () -> "http://127.0.0.1:" + adminStub.getAddress().getPort());
        registry.add("IBOOT_AI_URI", () -> "http://127.0.0.1:" + aiStub.getAddress().getPort());
    }

    /**
     * 兜底前缀把普通接口转发到管理端。
     */
    @Test
    void routesToAdminByDefault() {
        webTestClient().get().uri("/api/user/list")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("admin|/api/user/list"));
    }

    /**
     * 具体前缀优先于兜底前缀。
     */
    @Test
    void routesAiPrefixBeforeFallback() {
        webTestClient().get().uri("/api/ai/ping")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("ai|/api/ai/ping"));
    }

    /**
     * 推送通道同样转发到管理端，且查询参数原样保留。
     */
    @Test
    void routesPushChannelToAdmin() {
        webTestClient().get().uri("/api/push/connect?t=token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("admin|/api/push/connect"));
    }

    /**
     * 跨域预检由网关应答，不转发到后端。
     */
    @Test
    void answersCorsPreflightWithoutForwarding() {
        webTestClient().method(HttpMethod.OPTIONS)
                .uri("/api/user/list")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
    }
}
