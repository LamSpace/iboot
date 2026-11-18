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
import com.iboot.cli.config.ConfigStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MainCommandTest {

    @TempDir
    Path tempDir;

    private CommandLine commandLine() {
        CliApplication application = new CliApplication(
                new ConfigStore(tempDir.resolve("config.json")),
                new CredentialStore(tempDir.resolve("credentials.json"),
                        tempDir.resolve("credentials.lock")));
        return Main.commandLine(new Main(application));
    }

    @Test
    void rootHelpSucceeds() {
        assertThat(commandLine().execute("--help")).isZero();
    }

    @Test
    void subcommandHelpSucceeds() {
        assertThat(commandLine().execute("user", "list", "--help")).isZero();
    }

    @Test
    void configListAndStatusSucceedWithoutLogin() {
        assertThat(commandLine().execute("config", "list")).isZero();
        assertThat(commandLine().execute("status")).isZero();
    }

    @Test
    void writeRequiresConfirmation() {
        assertThat(commandLine().execute("api", "DELETE", "/api/user/1"))
                .isEqualTo(ExitCode.CONFIRM);
    }

    @Test
    void dryRunDoesNotSendRequest() {
        assertThat(commandLine().execute("api", "DELETE", "/api/user/1", "--dry-run"))
                .isZero();
    }
}
