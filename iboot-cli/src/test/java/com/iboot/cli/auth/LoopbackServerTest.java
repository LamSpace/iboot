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
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoopbackServerTest {

    @Test
    void receivesAuthorizationCode() throws Exception {
        try (LoopbackServer server = new LoopbackServer(0, "state-1")) {
            CompletableFuture<String> received = CompletableFuture.supplyAsync(
                    () -> server.await(Duration.ofSeconds(2)));

            HttpResponse<String> response = callback(server, "code-1", "state-1");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(received.get(2, TimeUnit.SECONDS)).isEqualTo("code-1");
        }
    }

    @Test
    void rejectsMismatchedState() throws Exception {
        try (LoopbackServer server = new LoopbackServer(0, "state-1")) {
            callback(server, "code-1", "state-2");

            assertThatThrownBy(() -> server.await(Duration.ofSeconds(1)))
                    .isInstanceOfSatisfying(CliException.class,
                            error -> assertThat(error.exitCode()).isEqualTo(ExitCode.AUTH))
                    .hasMessageContaining("state");
        }
    }

    @Test
    void timesOutWithoutCallback() throws Exception {
        try (LoopbackServer server = new LoopbackServer(0, "state-1")) {
            assertThatThrownBy(() -> server.await(Duration.ofMillis(50)))
                    .isInstanceOfSatisfying(CliException.class,
                            error -> assertThat(error.exitCode()).isEqualTo(ExitCode.AUTH))
                    .hasMessageContaining("超时");
        }
    }

    private HttpResponse<String> callback(LoopbackServer server, String code, String state)
            throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + server.port()
                + "/callback?code=" + code + "&state=" + state);
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
