package com.iboot.gateway;

import com.iboot.gateway.support.TraceIdResponseFilter;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * 网关服务端观测的存在性测试。
 *
 * <p>用 {@link TestObservationRegistry} 替换生产用的观测注册表：它基于
 * {@code SimpleObservationRegistry}，把观测记录在内存里供断言读取。断言只读内存记录，因此
 * 通过意味着「观测被创建了」，失败意味着「观测组件根本没执行」——两种结论不会混在一起。
 * 至于导出（观测创建了但追踪后端查不到），只能用追踪后端的查询脚本判断，不在本测试的范围内。
 *
 * <p>测试同时覆盖运维端点与转发请求：运维端点作为对照组，它能在追踪后端看到 span，所以它必须
 * 在这里也能被观测到；否则说明测试装置本身没生效，转发请求的失败结论就不成立。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.tracing.sampling.probability=1.0")
@Import(GatewayTracingTest.ObservationCapture.class)
class GatewayTracingTest {

    /** 服务端请求观测的固定名称，由 Spring Framework 的观测文档定义。 */
    private static final String SERVER_REQUEST_OBSERVATION = "http.server.requests";

    private static HttpServer adminStub;

    @Value("${local.server.port}")
    private int port;

    private WebTestClient client;

    private TestObservationRegistry observationRegistry;

    /**
     * 提供只记录不导出的观测注册表，并暴露给测试方法。
     *
     * <p>标记 {@link Primary} 以替换自动配置的注册表；自动配置的注册表会挂上 OpenTelemetry
     * 处理器，观测一旦创建就会尝试导出，反而看不出问题出在创建还是导出。
     *
     * @return 测试专用的注册表
     */
    @TestConfiguration
    static class ObservationCapture {

        @Bean
        @Primary
        TestObservationRegistry testObservationRegistry() {
            return TestObservationRegistry.create();
        }
    }

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

    @BeforeEach
    void clearCapturedObservations(@Autowired ObservationRegistry registry) {
        this.observationRegistry = (TestObservationRegistry) registry;
        this.observationRegistry.clear();
        this.client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    /**
     * 对照组：运维端点请求必须被观测到。
     *
     * <p>运维端点在追踪后端里能看到 span，若这里断言失败，说明测试装置没接管观测注册表，
     * 后续对转发请求的结论一并失效。
     */
    @Test
    void observesActuatorRequest() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo(SERVER_REQUEST_OBSERVATION);
    }

    /**
     * 经网关转发的请求必须产生一条服务端观测。
     *
     * <p>路径断言用高基数键 {@code http.url}：网关路由没有 Spring MVC 那样的路径模板，
     * 框架解析不出 {@code uri} 低基数键（实测为 {@code UNKNOWN}），路径只保留在高基数键上。
     */
    @Test
    void observesProxiedRequest() {
        client.get().uri("/api/user/list").exchange().expectStatus().isOk();

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasNumberOfObservationsWithNameEqualTo(SERVER_REQUEST_OBSERVATION, 1)
                .hasAnObservationWithAKeyValue("http.url", "/api/user/list");
    }

    /**
     * 响应头回显的 TraceID 必须是 32 位小写十六进制。
     *
     * <p>这是验证脚本唯一的标识来源：有了它才不必按时间窗检索追踪后端。
     */
    @Test
    void echoesTraceIdInResponseHeader() {
        client.get().uri("/api/user/list").exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches(TraceIdResponseFilter.TRACE_ID_HEADER, "^[0-9a-f]{32}$");
    }
}
