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

import com.iboot.auth.application.service.CredentialAuthenticator;
import com.iboot.auth.domain.model.AuthUser;
import com.iboot.kernel.security.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortalTokenCustomizerTest {

    private RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> values;

    private CredentialAuthenticator authenticator;

    private AuthServerProperties properties;

    private PortalTokenCustomizer customizer;

    private PortalUserPrincipal principal;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        authenticator = mock(CredentialAuthenticator.class);
        when(authenticator.sessionTimeoutMinutes()).thenReturn(30L);
        properties = new AuthServerProperties();
        customizer = new PortalTokenCustomizer(redisTemplate, authenticator, properties);
        principal = new PortalUserPrincipal(
                new AuthUser(7L, "alice", "encoded", "Alice", null, 3L, 1, List.of(1L)),
                List.of("user:list"), List.of("admin"),
                "127.0.0.1", "curl", "Linux", LocalDateTime.now());
    }

    @Test
    void portalLoginEvictsPreviousSessionAndWritesOnlineMapping() {
        customizer.customize(context(properties.getClientId(),
                AuthorizationGrantType.AUTHORIZATION_CODE, "sid-portal"));

        verify(authenticator).evictPreviousSession(7L);
        verify(values).set(eq(Constants.LOGIN_TOKEN_KEY + "sid-portal"), any(),
                eq(30L), eq(TimeUnit.MINUTES));
        verify(values).set(eq(Constants.ONLINE_TOKEN_KEY + "7"), eq("sid-portal"),
                eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void cliLoginDoesNotEvictOrWriteOnlineMapping() {
        customizer.customize(context(properties.getCliClientId(),
                AuthorizationGrantType.AUTHORIZATION_CODE, "sid-cli"));

        verify(authenticator, never()).evictPreviousSession(anyLong());
        verify(values).set(eq(Constants.LOGIN_TOKEN_KEY + "sid-cli"), any(),
                eq(30L), eq(TimeUnit.MINUTES));
        verify(values, never()).set(eq(Constants.ONLINE_TOKEN_KEY + "7"), any(),
                anyLong(), any());
    }

    @Test
    void portalRefreshKeepsOnlineMappingWithoutEviction() {
        customizer.customize(context(properties.getClientId(),
                AuthorizationGrantType.REFRESH_TOKEN, "sid-portal"));

        verify(authenticator, never()).evictPreviousSession(anyLong());
        verify(values).set(eq(Constants.ONLINE_TOKEN_KEY + "7"), eq("sid-portal"),
                eq(30L), eq(TimeUnit.MINUTES));
    }

    private JwtEncodingContext context(String clientId, AuthorizationGrantType grantType,
                                       String sessionId) {
        JwtEncodingContext context = mock(JwtEncodingContext.class);
        RegisteredClient client = RegisteredClient.withId(clientId)
                .clientId(clientId)
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/callback")
                .scope("api")
                .build();
        OAuth2Authorization authorization = mock(OAuth2Authorization.class);
        when(authorization.getId()).thenReturn(sessionId);
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(context.getPrincipal()).thenReturn(authentication);
        when(context.getRegisteredClient()).thenReturn(client);
        when(context.getAuthorizationGrantType()).thenReturn(grantType);
        when(context.getAuthorization()).thenReturn(authorization);
        when(context.getClaims()).thenReturn(JwtClaimsSet.builder());
        return context;
    }
}
