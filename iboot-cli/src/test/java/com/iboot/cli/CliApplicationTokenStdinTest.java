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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 标准输入令牌注入的测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class CliApplicationTokenStdinTest {

    @TempDir
    Path tempDir;

    /**
     * 标准输入优先于本地凭据，且不写回凭据文件。
     *
     * @throws Exception 读取凭据文件失败时抛出
     */
    @Test
    void stdinOverridesCredentialsWithoutPersisting() throws Exception {
        Path credentials = tempDir.resolve("credentials.json");
        CredentialStore credentialStore = new CredentialStore(credentials,
                tempDir.resolve("credentials.lock"));
        credentialStore.save("dev", new TokenSet("credential-token", null, null,
                Long.MAX_VALUE, "Bearer"));
        CliApplication application = new CliApplication(
                new ConfigStore(tempDir.resolve("config.json")), credentialStore);
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("  stdin-token \n".getBytes(StandardCharsets.UTF_8)));
            application.useTokenFromStdin();
        } finally {
            System.setIn(original);
        }

        assertThat(application.hasTokenOverride()).isTrue();
        assertThat(application.tokens("dev").accessToken()).isEqualTo("stdin-token");
        assertThat(Files.readString(credentials)).doesNotContain("stdin-token");
    }

    /**
     * 标准输入为空时以用法错误结束。
     */
    @Test
    void emptyStdinFails() {
        CliApplication application = new CliApplication(
                new ConfigStore(tempDir.resolve("config.json")),
                new CredentialStore(tempDir.resolve("credentials.json"),
                        tempDir.resolve("credentials.lock")));
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("   \n".getBytes(StandardCharsets.UTF_8)));
            assertThatThrownBy(application::useTokenFromStdin)
                    .isInstanceOf(CliException.class)
                    .extracting(e -> ((CliException) e).exitCode())
                    .isEqualTo(ExitCode.USAGE);
        } finally {
            System.setIn(original);
        }
    }
}
