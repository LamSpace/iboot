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

package com.iboot.auth.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 授权服务器配置的语义测试。
 *
 * <p>验证门户客户端为公共客户端且强制 PKCE，以及签名密钥与令牌解码器配套。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class AuthorizationServerConfigTest {

    private final AuthorizationServerConfig config = new AuthorizationServerConfig();

    private final AuthServerProperties properties = new AuthServerProperties();

    /**
     * 门户客户端必须是公共客户端，并强制 PKCE。
     */
    @Test
    void portalClientRequiresProofKey() {
        RegisteredClientRepository repository = config.registeredClientRepository(properties);
        RegisteredClient client = repository.findByClientId(properties.getClientId());

        assertThat(client).isNotNull();
        assertThat(client.getClientAuthenticationMethods())
                .containsExactly(ClientAuthenticationMethod.NONE);
        assertThat(client.getClientSettings().isRequireProofKey()).isTrue();
        assertThat(client.getAuthorizationGrantTypes())
                .contains(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN);
        assertThat(client.getRedirectUris()).containsExactlyElementsOf(properties.getRedirectUris());
        assertThat(client.getTokenSettings().isReuseRefreshTokens()).isFalse();
    }

    /**
     * 签发者取自配置。
     */
    @Test
    void issuerComesFromConfiguration() {
        AuthorizationServerSettings settings = config.authorizationServerSettings(properties);

        assertThat(settings.getIssuer()).isEqualTo(properties.getIssuer());
    }

    /**
     * 解码器应能校验由同一公钥集签发的令牌。
     */
    @Test
    void decoderVerifiesTokenSignedByJwkSource() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        JwtDecoder decoder = config.jwtDecoder(jwkSource);

        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                new JWTClaimsSet.Builder()
                        .subject("7")
                        .issuer(properties.getIssuer())
                        .issueTime(Date.from(Instant.now()))
                        .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                        .build());
        signedJwt.sign(new RSASSASigner(rsaKey));

        Jwt decoded = decoder.decode(signedJwt.serialize());

        assertThat(decoded.getSubject()).isEqualTo("7");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo(properties.getIssuer());
    }
}
