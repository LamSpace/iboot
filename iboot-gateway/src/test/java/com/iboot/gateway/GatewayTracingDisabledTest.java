package com.iboot.gateway;

import com.iboot.gateway.support.TraceIdResponseFilter;
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

/**
 * 追踪关闭时的网关行为。
 *
 * <p>用 {@code management.tracing.export.enabled=false} 关闭追踪后，网关不应再回显
 * {@link TraceIdResponseFilter#TRACE_ID_HEADER}——该标识在追踪后端查不到，回显只会误导排障；
 * 同时转发行为必须完全不变。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.tracing.export.enabled=false")
class GatewayTracingDisabledTest {

    private static HttpServer adminStub;

    @Value("${local.server.port}")
    private int port;

    @BeforeAll
    static void startStub() throws IOException {
        adminStub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        adminStub.createContext("/", exchange -> {
            byte[] body = "admin-ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        adminStub.start();
    }

    @AfterAll
    static void stopStub() {
        if (adminStub != null) {
            adminStub.stop(0);
        }
    }

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) {
        registry.add("IBOOT_ADMIN_URI", () -> "http://127.0.0.1:" + adminStub.getAddress().getPort());
        registry.add("IBOOT_AI_URI", () -> "http://127.0.0.1:" + adminStub.getAddress().getPort());
    }

    /**
     * 关闭追踪后请求照常转发，但不再回显 TraceID。
     */
    @Test
    void forwardsWithoutTraceHeader() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port).build();

        client.get().uri("/api/user/list")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(TraceIdResponseFilter.TRACE_ID_HEADER)
                .expectBody(String.class).isEqualTo("admin-ok");
    }
}
