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

package com.iboot.workflow.infrastructure.notification;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.iboot.workflow.application.service.notification.NotificationCommand;
import com.iboot.workflow.domain.notification.model.NotificationEventType;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * 通知登记 HTTP 适配器的治理行为测试。
 *
 * <p>用本地桩服务模拟通知服务，验证正常投递、重试耗尽与熔断阻塞三条路径。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class HttpNotificationPortTest {

    private static HttpServer server;

    private static final AtomicInteger REQUESTS = new AtomicInteger();

    private static final AtomicInteger RESPONSE_STATUS = new AtomicInteger(200);

    private SimpleMeterRegistry registry;

    private HttpNotificationPort port;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            REQUESTS.incrementAndGet();
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(RESPONSE_STATUS.get(), body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeEach
    void setUp() {
        REQUESTS.set(0);
        RESPONSE_STATUS.set(200);
        registry = new SimpleMeterRegistry();
        port = new HttpNotificationPort(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(), "token", registry);
        FlowRuleManager.loadRules(List.of());
    }

    @AfterEach
    void tearDown() {
        FlowRuleManager.loadRules(List.of());
    }

    /**
     * 正常投递只请求一次且不增加失败计数。
     */
    @Test
    void deliversOnceOnSuccess() {
        port.enqueue(command());

        await(() -> REQUESTS.get() == 1);
        assertThat(failedCount()).isZero();
    }

    /**
     * 重试耗尽后只记录一次失败，不向审批主流程抛出。
     */
    @Test
    void recordsFailureAfterRetriesExhausted() {
        RESPONSE_STATUS.set(500);

        port.enqueue(command());

        await(() -> failedCount() == 1.0);
        assertThat(REQUESTS.get()).isEqualTo(3);
    }

    /**
     * 熔断阻塞时不再向下游发起请求，并记录一次失败。
     */
    @Test
    void skipsDownstreamWhenBlockedByRule() {
        FlowRuleManager.loadRules(List.of(new FlowRule(HttpNotificationPort.RESOURCE).setCount(0)));

        port.enqueue(command());

        await(() -> failedCount() == 1.0);
        assertThat(REQUESTS.get()).isZero();
    }

    private NotificationCommand command() {
        return new NotificationCommand("event-1", NotificationEventType.APPROVAL_TODO,
                "APPROVAL_TODO", 1L, Map.of("title", "待办"));
    }

    private double failedCount() {
        return registry.get("notification_trigger_failed").counter().count();
    }

    private void await(final BooleanSupplier condition) {
        for (int i = 0; i < 200; i++) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("等待异步投递时被中断");
            }
        }
        fail("等待异步投递结果超时");
    }
}
