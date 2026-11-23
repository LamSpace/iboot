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
import java.util.concurrent.atomic.AtomicReference;

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

    private volatile boolean allow;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/data/iboot/authz/allow", exchange -> {
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

    private ServiceAuthorizationPolicy policyWithOpa() {
        return new ServiceAuthorizationPolicy(
                properties("http://127.0.0.1:" + server.getAddress().getPort()));
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
        return properties;
    }
}
