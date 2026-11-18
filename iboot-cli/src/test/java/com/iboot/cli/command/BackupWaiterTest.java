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

package com.iboot.cli.command;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackupWaiterTest {

    @Test
    void returnsWhenBackupSucceeds() {
        Queue<JsonNode> responses = new ArrayDeque<>(List.of(
                status("RUNNING"), status("RUNNING"), status("SUCCESS")));
        BackupWaiter waiter = new BackupWaiter(responses::poll,
                Duration.ZERO, Duration.ofSeconds(1), millis -> {
        });

        JsonNode result = waiter.awaitTerminal();

        assertThat(result.get("status").asString()).isEqualTo("SUCCESS");
    }

    @Test
    void throwsWhenBackupFails() {
        ObjectNode failed = JsonNodeFactory.instance.objectNode()
                .put("status", "FAILED")
                .put("errorMessage", "dump failed");
        Queue<JsonNode> responses = new ArrayDeque<>(List.of(status("RUNNING"), failed));
        BackupWaiter waiter = new BackupWaiter(responses::poll,
                Duration.ZERO, Duration.ofSeconds(1), millis -> {
        });

        assertThatThrownBy(waiter::awaitTerminal)
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.BUSINESS))
                .hasMessageContaining("dump failed");
    }

    @Test
    void throwsWhenTimeoutExpires() {
        BackupWaiter waiter = new BackupWaiter(() -> status("RUNNING"),
                Duration.ZERO, Duration.ZERO, millis -> {
        });

        assertThatThrownBy(waiter::awaitTerminal)
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.BUSINESS))
                .hasMessageContaining("超时");
    }

    private JsonNode status(String value) {
        return JsonNodeFactory.instance.objectNode().put("status", value);
    }
}
