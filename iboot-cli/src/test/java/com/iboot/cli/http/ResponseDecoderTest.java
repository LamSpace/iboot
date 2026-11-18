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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseDecoderTest {

    private final ResponseDecoder decoder = new ResponseDecoder();

    @Test
    void unwrapsCloudEventAndResult() {
        String body = """
                {"specversion":"1.0","id":"1","type":"x","source":"y",
                 "datacontenttype":"application/json",
                 "data":{"code":200,"message":"ok","data":{"total":1}}}
                """;

        ApiResponse response = decoder.decode(200, body, false);

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.data().get("total").asInt()).isEqualTo(1);
    }

    @Test
    void preservesDirectDto() {
        ApiResponse response = decoder.decode(200, "{\"items\":[1,2]}", false);

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.data().get("items").size()).isEqualTo(2);
    }

    @Test
    void mapsBusinessCodeToBusinessExit() {
        assertThatThrownBy(() -> decoder.decode(200, "{\"code\":500,\"message\":\"boom\"}", false))
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.BUSINESS))
                .hasMessageContaining("boom");
    }

    @Test
    void mapsRetiredTokenToAuthExit() {
        assertThatThrownBy(() -> decoder.decode(200, "{\"code\":40101,\"message\":\"expired\"}", false))
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.AUTH));
    }

    @Test
    void mapsForbiddenBusinessCode() {
        assertThatThrownBy(() -> decoder.decode(200, "{\"code\":403,\"message\":\"denied\"}", false))
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.FORBIDDEN));
    }

    @Test
    void mapsHttpStatuses() {
        assertThatThrownBy(() -> decoder.decode(401, "{\"code\":401}", false))
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.AUTH));
        assertThatThrownBy(() -> decoder.decode(403, "{\"code\":403}", false))
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.FORBIDDEN));
        assertThatThrownBy(() -> decoder.decode(429, "{}", false))
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.BUSINESS));
        assertThatThrownBy(() -> decoder.decode(503, "{}", false))
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.NETWORK));
    }

    @Test
    void rawSkipsUnwrapping() {
        ApiResponse response = decoder.decode(200, "{\"specversion\":\"1.0\",\"data\":{}}", true);

        assertThat(response.data()).isNull();
        assertThat(response.rawBody()).contains("specversion");
    }
}
