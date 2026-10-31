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

package com.iboot.kernel.security;

import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PublicJwk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 公钥集缓存的语义测试。
 *
 * <p>覆盖首次回源、未知 {@code kid} 触发刷新、回源失败保留缓存三种分支。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class JwkSetProviderTest {

    private static final String JWKS_URI = "http://auth.test/oauth2/jwks";

    private PublicKey publicKey;

    private String jwksJson;

    private RestClient.Builder clientBuilder;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = keyPair.getPublic();
        PublicJwk<?> jwk = Jwks.builder().key((RSAPublicKey) publicKey).id("k1").build();
        jwksJson = "{\"keys\":[" + Jwks.UNSAFE_JSON(jwk) + "]}";
        clientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(clientBuilder).build();
    }

    /**
     * 首次取公钥时回源并缓存。
     */
    @Test
    void fetchesKeyOnFirstUse() {
        server.expect(times(1), requestTo(JWKS_URI))
                .andRespond(withSuccess(jwksJson, MediaType.APPLICATION_JSON));

        JwkSetProvider provider = provider();

        assertThat(provider.resolve("k1")).isEqualTo(publicKey);
        assertThat(provider.size()).isEqualTo(1);
        server.verify();
    }

    /**
     * 未知的 kid 触发一次刷新，命中后再次解析不再回源。
     */
    @Test
    void refreshesOnUnknownKeyId() {
        server.expect(times(2), requestTo(JWKS_URI))
                .andRespond(withSuccess(jwksJson, MediaType.APPLICATION_JSON));

        JwkSetProvider provider = provider();

        assertThat(provider.resolve("k1")).isEqualTo(publicKey);
        assertThat(provider.resolve("k1")).isEqualTo(publicKey);
        assertThat(provider.resolve("k2")).isNull();
        server.verify();
    }

    /**
     * 回源失败时保留既有缓存，认证服务短暂不可用不应让在线用户掉线。
     */
    @Test
    void keepsCacheWhenRefreshFails() {
        server.expect(times(1), requestTo(JWKS_URI))
                .andRespond(withSuccess(jwksJson, MediaType.APPLICATION_JSON));
        server.expect(times(1), requestTo(JWKS_URI))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        JwkSetProvider provider = provider();
        assertThat(provider.resolve("k1")).isEqualTo(publicKey);

        provider.refresh();

        assertThat(provider.resolve("k1")).isEqualTo(publicKey);
        server.verify();
    }

    private JwkSetProvider provider() {
        return new JwkSetProvider(JWKS_URI, clientBuilder.build(), 0L);
    }
}
