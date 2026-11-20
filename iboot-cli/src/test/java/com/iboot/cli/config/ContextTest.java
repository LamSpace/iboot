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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextTest {

    @Test
    void defaultsToHttpsPortalOrigin() {
        Context context = Context.defaults();

        assertThat(context.name()).isEqualTo("dev");
        assertThat(context.baseUrl()).isEqualTo("https://localhost:5173");
        assertThat(context.issuer()).isEqualTo("https://localhost:5173");
        assertThat(context.redirectUri()).isEqualTo("http://127.0.0.1:8765/callback");
    }

    @Test
    void stripsTrailingSlash() {
        Context context = new Context("prod", "https://iboot.example.com/", null, null, null);

        assertThat(context.baseUrl()).isEqualTo("https://iboot.example.com");
        assertThat(context.issuer()).isEqualTo("https://iboot.example.com");
    }

    @Test
    void rejectsNonLoopbackHttpBaseUrl() {
        assertThatThrownBy(() -> new Context("prod", "http://iboot.example.com", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl")
                .hasMessageContaining("HTTPS");
    }

    @Test
    void allowsLoopbackHttpForTroubleshooting() {
        Context context = new Context("plain", "http://127.0.0.1:8088", null, null, null);

        assertThat(context.baseUrl()).isEqualTo("http://127.0.0.1:8088");
    }

    @Test
    void rejectsNonLoopbackHttpIssuer() {
        assertThatThrownBy(() -> new Context("prod", "https://iboot.example.com",
                "http://iboot.example.com", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    void allowsLoopbackHttpIssuer() {
        Context context = new Context("plain", "https://iboot.example.com",
                "http://localhost:5173", null, null);

        assertThat(context.issuer()).isEqualTo("http://localhost:5173");
    }

    @Test
    void rejectsInvalidUrl() {
        assertThatThrownBy(() -> new Context("bad", "not-a-url", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseUrl");
    }
}
