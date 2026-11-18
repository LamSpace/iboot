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

package com.iboot.cli.http;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import com.iboot.cli.config.Context;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsHeadersAndQueryThenUnwrapsResponse() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> version = new AtomicReference<>();
        AtomicReference<String> client = new AtomicReference<>();
        AtomicReference<String> query = new AtomicReference<>();
        start(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            version.set(exchange.getRequestHeaders().getFirst("Accept-Version"));
            client.set(exchange.getRequestHeaders().getFirst("X-Client"));
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] body = """
                    {"specversion":"1.0","id":"1","type":"x","source":"y",
                     "datacontenttype":"application/json",
                     "data":{"code":200,"message":"ok","data":{"username":"alice"}}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        });

        GatewayClient client0 = new GatewayClient(context(), () -> "token-1");
        ApiResponse response = client0.get("/api/user/list",
                Map.of("pageNum", "1", "username", "张 三"));

        assertThat(authorization.get()).isEqualTo("Bearer token-1");
        assertThat(version.get()).isEqualTo("1");
        assertThat(client.get()).isEqualTo("iboot-cli");
        assertThat(query.get()).contains("pageNum=1").contains("username=%E5%BC%A0%20%E4%B8%89");
        assertThat(response.data().get("username").asString()).isEqualTo("alice");
    }

    @Test
    void mapsUnauthorizedResponse() throws Exception {
        start(exchange -> {
            byte[] body = "{\"code\":401,\"message\":\"unauthorized\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
        });

        GatewayClient client = new GatewayClient(context(), () -> "token-1");

        assertThatThrownBy(() -> client.get("/api/user/list", Map.of()))
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.AUTH));
    }

    private Context context() {
        return new Context("test", "http://127.0.0.1:" + server.getAddress().getPort(),
                null, "iboot-cli", "http://127.0.0.1:8765/callback");
    }

    private void start(Handler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    @FunctionalInterface
    private interface Handler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
    }
}
