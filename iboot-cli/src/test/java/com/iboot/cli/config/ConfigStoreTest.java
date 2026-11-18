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

package com.iboot.cli.config;

import com.iboot.cli.CliException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigStoreTest {

    @TempDir
    Path tempDir;

    private ConfigStore store() {
        return new ConfigStore(tempDir.resolve("config.json"));
    }

    @Test
    void returnsDefaultContextWhenFileMissing() {
        CliConfig config = store().load();

        assertThat(config.current().name()).isEqualTo("dev");
        assertThat(config.current().baseUrl()).isEqualTo("http://localhost:8088");
        assertThat(config.current().clientId()).isEqualTo("iboot-cli");
        assertThat(Files.exists(tempDir.resolve("config.json"))).isFalse();
    }

    @Test
    void savesAndReloadsContexts() {
        ConfigStore store = store();
        store.put(new Context("prod", "https://iboot.example.com/", null, "iboot-cli",
                "http://127.0.0.1:8765/callback"), true);

        CliConfig reloaded = store().load();
        assertThat(reloaded.current().name()).isEqualTo("prod");
        assertThat(reloaded.current().baseUrl()).isEqualTo("https://iboot.example.com");
        assertThat(reloaded.current().issuer()).isEqualTo("https://iboot.example.com");
        assertThat(Files.exists(tempDir.resolve("config.json"))).isTrue();
    }

    @Test
    void switchesCurrentContext() {
        ConfigStore store = store();
        store.put(new Context("prod", "https://iboot.example.com", null, null, null), false);

        Context selected = store.use("prod");

        assertThat(selected.name()).isEqualTo("prod");
        assertThat(store().current().name()).isEqualTo("prod");
    }

    @Test
    void rejectsUnknownContext() {
        assertThatThrownBy(() -> store().use("missing"))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("context 不存在");
    }
}
