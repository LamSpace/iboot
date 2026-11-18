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

package com.iboot.cli;

import com.iboot.cli.auth.CredentialStore;
import com.iboot.cli.auth.TokenSet;
import com.iboot.cli.config.ConfigStore;
import com.iboot.cli.config.Context;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BackupCommandTest {

    @TempDir
    Path tempDir;

    private HttpServer server;

    private final AtomicInteger polls = new AtomicInteger();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void triggersAndWaitsUntilSuccess() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/monitor/backup/records", exchange -> {
            try {
                if ("POST".equals(exchange.getRequestMethod())) {
                    respond(exchange, "{\"code\":200,\"data\":{\"id\":7,\"status\":\"RUNNING\"}}");
                    return;
                }
                int count = polls.incrementAndGet();
                respond(exchange, count == 1
                        ? "{\"code\":200,\"data\":{\"id\":7,\"status\":\"RUNNING\"}}"
                        : "{\"code\":200,\"data\":{\"id\":7,\"status\":\"SUCCESS\"}}");
            } finally {
                exchange.close();
            }
        });
        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        ConfigStore configStore = new ConfigStore(tempDir.resolve("config.json"));
        configStore.put(new Context("test", baseUrl, baseUrl, "iboot-cli",
                "http://127.0.0.1:8765/callback"), true);
        CredentialStore credentialStore = new CredentialStore(
                tempDir.resolve("credentials.json"), tempDir.resolve("credentials.lock"));
        credentialStore.save("test", new TokenSet("test-token", null, null,
                System.currentTimeMillis() + 60_000, "Bearer"));
        CliApplication application = new CliApplication(configStore, credentialStore);
        CommandLine commandLine = Main.commandLine(new Main(application));

        int exitCode = commandLine.execute("backup", "trigger", "--wait", "--yes",
                "--json", "--interval", "1", "--timeout", "5");

        assertThat(exitCode).isZero();
        assertThat(polls).hasValue(2);
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
