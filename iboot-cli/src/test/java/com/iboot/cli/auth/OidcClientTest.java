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

package com.iboot.cli.auth;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import com.iboot.cli.config.Context;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OidcClientTest {

    private HttpServer server;

    private final OidcClient client = new OidcClient();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void discoversExchangesAndRefreshesTokens() throws Exception {
        AtomicReference<String> lastForm = new AtomicReference<>();
        start(exchange -> {
            if (exchange.getRequestURI().getPath().equals("/.well-known/openid-configuration")) {
                String base = baseUrl();
                respond(exchange, 200, """
                        {"issuer":"%s","authorization_endpoint":"%s/oauth2/authorize",
                         "token_endpoint":"%s/token","end_session_endpoint":"%s/logout"}
                        """.formatted(base, base, base, base));
                return;
            }
            lastForm.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"access_token":"access-1","refresh_token":"refresh-1",
                     "id_token":"id-1","token_type":"Bearer","expires_in":1800}
                    """);
        });

        Context context = context();
        OidcClient.Discovery discovery = client.discover(context);
        assertThat(discovery.tokenEndpoint()).endsWith("/token");

        TokenSet tokens = client.exchange(discovery, "code-1", "verifier-1",
                context.redirectUri(), context.clientId());
        assertThat(tokens.accessToken()).isEqualTo("access-1");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-1");
        assertThat(tokens.expiresAt()).isGreaterThan(System.currentTimeMillis());
        assertThat(lastForm.get()).contains("grant_type=authorization_code")
                .contains("code=code-1")
                .contains("code_verifier=verifier-1");

        TokenSet refreshed = client.refresh(discovery, "refresh-1", context.clientId());
        assertThat(refreshed.accessToken()).isEqualTo("access-1");
        assertThat(lastForm.get()).contains("grant_type=refresh_token")
                .contains("refresh_token=refresh-1");
    }

    @Test
    void mapsInvalidGrantToAuthExit() throws Exception {
        start(exchange -> {
            if (exchange.getRequestURI().getPath().equals("/.well-known/openid-configuration")) {
                String base = baseUrl();
                respond(exchange, 200, """
                        {"issuer":"%s","authorization_endpoint":"%s/oauth2/authorize",
                         "token_endpoint":"%s/token"}
                        """.formatted(base, base, base));
                return;
            }
            respond(exchange, 400, "{\"error\":\"invalid_grant\",\"error_description\":\"expired\"}");
        });

        OidcClient.Discovery discovery = client.discover(context());

        assertThatThrownBy(() -> client.refresh(discovery, "refresh-1", "iboot-cli"))
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.AUTH));
    }

    private Context context() {
        return new Context("test", baseUrl(), baseUrl(), "iboot-cli",
                "http://127.0.0.1:8765/callback");
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
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

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface Handler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
    }
}
