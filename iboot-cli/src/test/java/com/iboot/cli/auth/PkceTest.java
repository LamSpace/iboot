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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class PkceTest {

    @Test
    void generatesS256ChallengeAndState() throws Exception {
        Pkce pkce = Pkce.generate();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String expected = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(digest.digest(pkce.verifier().getBytes(StandardCharsets.US_ASCII)));

        assertThat(pkce.verifier()).isNotBlank();
        assertThat(pkce.challenge()).isEqualTo(expected);
        assertThat(pkce.state()).isNotBlank();
    }

    @Test
    void generatesDistinctValuesForEachLogin() {
        Pkce first = Pkce.generate();
        Pkce second = Pkce.generate();

        assertThat(first.verifier()).isNotEqualTo(second.verifier());
        assertThat(first.state()).isNotEqualTo(second.state());
    }
}
