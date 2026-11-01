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

package com.iboot.admin.infrastructure.notification;

import com.iboot.admin.application.service.notification.NotificationCommand;
import com.iboot.admin.domain.notification.model.NotificationEventType;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link HttpNotificationPort} 的单元测试。
 *
 * <p>用 JDK 内置 HTTP 服务替代通知服务：验证请求头、成功时不计数、失败重试耗尽后计数且不抛出。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class HttpNotificationPortTest {

    private static final String TOKEN = "internal-secret";

    private HttpServer server;

    private SimpleMeterRegistry meterRegistry;

    private final AtomicInteger requests = new AtomicInteger();

    private final AtomicReference<String> tokenHeader = new AtomicReference<>();

    private int statusCode;

    @BeforeEach
    void setUp() throws IOException {
        meterRegistry = new SimpleMeterRegistry();
        statusCode = 200;
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/internal/notifications", exchange -> {
            requests.incrementAndGet();
            tokenHeader.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    /**
     * 验证成功登记时携带共享密钥且不增加失败计数。
     */
    @Test
    void sendsCommandWithTokenAndDoesNotCountFailures() {
        HttpNotificationPort port = port();

        port.enqueue(command());

        awaitRequests(1);
        assertThat(tokenHeader.get()).isEqualTo(TOKEN);
        assertThat(failureCount()).isZero();
    }

    /**
     * 验证重试耗尽后只记一次失败，且不向调用方抛出。
     */
    @Test
    void countsFailureAfterRetriesExhaustedWithoutThrowing() {
        statusCode = 500;
        HttpNotificationPort port = port();

        assertThatCode(() -> port.enqueue(command())).doesNotThrowAnyException();

        awaitFailureCount(1);
        assertThat(requests.get()).isEqualTo(3);
    }

    private HttpNotificationPort port() {
        return new HttpNotificationPort("http://localhost:" + server.getAddress().getPort(), TOKEN, meterRegistry);
    }

    private NotificationCommand command() {
        return new NotificationCommand("approval-todo-1", NotificationEventType.APPROVAL_TODO, "APPROVAL_TODO", 2L,
                Map.of("taskName", "年假"));
    }

    private double failureCount() {
        return meterRegistry.get("notification_trigger_failed").counter().count();
    }

    private void awaitRequests(final int expected) {
        await(() -> requests.get() >= expected, "请求未到达");
    }

    private void awaitFailureCount(final int expected) {
        await(() -> failureCount() >= expected, "失败计数未增加");
    }

    private void await(final java.util.function.BooleanSupplier condition, final String message) {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError(message);
    }
}
