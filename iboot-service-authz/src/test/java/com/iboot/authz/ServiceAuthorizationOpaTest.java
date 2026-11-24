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

package com.iboot.authz;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OPA 授权决策测试。
 *
 * <p>用 JDK 内置 HTTP Server 模拟 OPA，验证允许、拒绝、请求体包含有序规则、异步路径以及
 * OPA 不可用时的失败关闭；不依赖真实 OPA 进程。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ServiceAuthorizationOpaTest {

    private HttpServer server;

    private final AtomicReference<String> requestBody = new AtomicReference<>("");

    private final AtomicInteger requests = new AtomicInteger();

    private volatile boolean allow;

    private volatile boolean failFirstRequest;

    @BeforeEach
    void startServer() throws Exception {
        requests.set(0);
        failFirstRequest = false;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/data/iboot/authz/allow", exchange -> {
            int requestNumber = requests.incrementAndGet();
            if (failFirstRequest && requestNumber == 1) {
                exchange.close();
                return;
            }
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = ("{\"result\":" + allow + "}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void opaAllowsGatewayRequest() throws Exception {
        allow = true;
        ServiceAuthorizationPolicy policy = policyWithOpa();
        assertFalse(policy.isDenied("/api/user/1", "iboot-gateway"));
        assertTrue(requestBody.get().contains("\"caller\":\"iboot-gateway\""));
        assertTrue(requestBody.get().contains("\"path\":\"/api/user/1\""));
        assertTrue(requestBody.get().contains("\"rules\":["));
        assertFalse(policy.isDeniedAsync("/api/user/1", "iboot-gateway").get(2, TimeUnit.SECONDS));
    }

    @Test
    void opaDeniesForeignService() {
        allow = false;
        ServiceAuthorizationPolicy policy = policyWithOpa();
        assertTrue(policy.isDenied("/api/user/1", "iboot-file"));
    }

    @Test
    void opaFailureIsFailClosed() {
        ServiceAuthorizationProperties properties = properties("http://127.0.0.1:1");
        ServiceAuthorizationPolicy policy = new ServiceAuthorizationPolicy(properties);
        assertTrue(policy.isDenied("/api/user/1", "iboot-gateway"));
        assertTrue(policy.isDeniedAsync("/api/user/1", "iboot-gateway").join());
    }

    @Test
    void opaRetriesTransientFailure() throws Exception {
        allow = true;
        failFirstRequest = true;
        ServiceAuthorizationPolicy policy = policyWithOpa(Duration.ofSeconds(2));
        assertFalse(policy.isDenied("/api/user/retry", "iboot-gateway"));
        assertEquals(2, requests.get());
    }

    @Test
    void opaCachesDefinitiveDecision() {
        allow = true;
        ServiceAuthorizationPolicy policy = policyWithCache(Duration.ofSeconds(5));
        assertFalse(policy.isDenied("/api/user/cache", "iboot-gateway"));
        assertFalse(policy.isDenied("/api/user/cache", "iboot-gateway"));
        assertEquals(1, requests.get());
    }

    @Test
    void opaCacheExpires() throws Exception {
        allow = true;
        ServiceAuthorizationPolicy policy = policyWithCache(Duration.ofMillis(100));
        assertFalse(policy.isDenied("/api/user/cache-ttl", "iboot-gateway"));
        Thread.sleep(250);
        assertFalse(policy.isDenied("/api/user/cache-ttl", "iboot-gateway"));
        assertEquals(2, requests.get());
    }

    private ServiceAuthorizationPolicy policyWithCache(final Duration ttl) {
        ServiceAuthorizationProperties properties =
                properties("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setOpaCacheTtl(ttl);
        return new ServiceAuthorizationPolicy(properties);
    }

    private ServiceAuthorizationPolicy policyWithOpa() {
        return new ServiceAuthorizationPolicy(
                properties("http://127.0.0.1:" + server.getAddress().getPort()));
    }

    private ServiceAuthorizationPolicy policyWithOpa(final Duration timeout) {
        ServiceAuthorizationProperties properties =
                properties("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setOpaTimeout(timeout);
        return new ServiceAuthorizationPolicy(properties);
    }

    private ServiceAuthorizationProperties properties(final String opaUrl) {
        ServiceAuthorizationRule rule = new ServiceAuthorizationRule();
        rule.setPaths(List.of("/api/**"));
        rule.setCallers(List.of("iboot-gateway"));
        ServiceAuthorizationProperties properties = new ServiceAuthorizationProperties();
        properties.setEnabled(true);
        properties.setRules(List.of(rule));
        properties.setOpaUrl(opaUrl);
        properties.setOpaTimeout(Duration.ofMillis(300));
        properties.setOpaCacheTtl(Duration.ZERO);
        properties.setOpaRetryAttempts(1);
        return properties;
    }
}
