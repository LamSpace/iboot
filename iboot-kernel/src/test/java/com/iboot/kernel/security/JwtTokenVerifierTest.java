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

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 令牌校验器的双栈语义测试。
 *
 * <p>覆盖自包含令牌、旧令牌、双栈关闭后拒绝旧令牌、非法令牌四种分支。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class JwtTokenVerifierTest {

    private static final String SECRET = "iboot-admin-secret-key-for-jwt-token-generation-minimum-256-bits";

    private KeyPair keyPair;

    private JwkSetProvider jwkSetProvider;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        jwkSetProvider = mock(JwkSetProvider.class);
        when(jwkSetProvider.resolve("k1")).thenReturn(keyPair.getPublic());
    }

    /**
     * 自包含令牌按公钥验签，并标记为非旧令牌。
     */
    @Test
    void acceptsSelfContainedToken() {
        JwtTokenVerifier.VerifiedToken verified = verifier(true).verify(rsaToken());

        assertThat(verified).isNotNull();
        assertThat(verified.legacy()).isFalse();
        assertThat(verified.claims().getSubject()).isEqualTo("7");
        assertThat(verified.claims().getId()).isEqualTo("jti-1");
    }

    /**
     * 双栈开启时旧令牌仍可校验。
     */
    @Test
    void acceptsLegacyTokenWhenEnabled() {
        JwtTokenVerifier.VerifiedToken verified = verifier(true).verify(hmacToken());

        assertThat(verified).isNotNull();
        assertThat(verified.legacy()).isTrue();
    }

    /**
     * 双栈关闭后旧令牌抛出可识别的版本停用异常。
     */
    @Test
    void rejectsLegacyTokenWhenRetired() {
        JwtTokenVerifier verifier = verifier(false);

        assertThatThrownBy(() -> verifier.verify(hmacToken()))
                .isInstanceOf(TokenVersionRetiredException.class);
    }

    /**
     * 非法令牌返回 null，由过滤器交给后续安全组件处理。
     */
    @Test
    void returnsNullForMalformedToken() {
        assertThat(verifier(true).verify("not-a-jwt")).isNull();
    }

    private JwtTokenVerifier verifier(boolean legacyEnabled) {
        return new JwtTokenVerifier(SECRET, jwkSetProvider, legacyEnabled);
    }

    private String rsaToken() {
        return Jwts.builder()
                .id("jti-1")
                .subject("7")
                .header().keyId("k1").and()
                .claim("username", "tester")
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private String hmacToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("7")
                .claim("userId", 7L)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
