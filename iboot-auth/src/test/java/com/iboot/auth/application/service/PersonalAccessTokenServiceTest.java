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

package com.iboot.auth.application.service;

import com.iboot.auth.domain.model.AuthUser;
import com.iboot.auth.domain.model.PersonalAccessTokenCreated;
import com.iboot.auth.domain.model.PersonalAccessTokenRecord;
import com.iboot.auth.domain.model.PersonalAccessTokenSummary;
import com.iboot.auth.infrastructure.persistence.AuthDirectory;
import com.iboot.auth.infrastructure.persistence.PersonalAccessTokenRepository;
import com.iboot.auth.infrastructure.security.AuthServerProperties;
import com.iboot.auth.infrastructure.security.PersonalAccessTokenProperties;
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.web.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 个人访问令牌应用服务测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class PersonalAccessTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-18T00:00:00Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private PersonalAccessTokenRepository repository;

    @Mock
    private AuthDirectory authDirectory;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private PersonalAccessTokenService service;

    private PersonalAccessTokenProperties properties;

    @BeforeEach
    void setUp() {
        AuthServerProperties authProperties = new AuthServerProperties();
        properties = new PersonalAccessTokenProperties();
        service = new PersonalAccessTokenService(repository, authDirectory, jwtEncoder,
                redisTemplate, authProperties, properties, CLOCK);
    }

    /**
     * 签发应写入元数据与撤销记录，并返回一次明文。
     */
    @Test
    void createsTokenWithFixedExpiry() {
        AuthUser user = user();
        when(authDirectory.findUserById(7L)).thenReturn(Optional.of(user));
        when(repository.countActive(eq(7L), any(LocalDateTime.class))).thenReturn(0);
        when(authDirectory.findRoleCodesByRoleIds(List.of(2L))).thenReturn(List.of("ROLE_USER"));
        when(authDirectory.findPermissionsByRoleIds(List.of(2L))).thenReturn(Set.of("user:list"));
        when(jwtEncoder.encode(any())).thenReturn(
                Jwt.withTokenValue("jwt-token").header("alg", "RS256").claim("sub", "7").build());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        LocalDateTime expires = LocalDateTime.ofInstant(NOW.plus(Duration.ofDays(30)), ZoneOffset.UTC);
        when(repository.findByTokenId(anyString())).thenReturn(Optional.of(
                new PersonalAccessTokenRecord(42L, 7L, "ci", "jti", List.of(),
                        expires, false, null, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))));

        PersonalAccessTokenCreated created = service.create(7L, "  ci  ", 30L, List.of());

        assertThat(created.id()).isEqualTo(42L);
        assertThat(created.name()).isEqualTo("ci");
        assertThat(created.scopes()).isEmpty();
        assertThat(created.token()).isEqualTo("jwt-token");
        assertThat(created.expiresAt()).isEqualTo(expires);
        verify(valueOperations).set(startsWith(Constants.PAT_TOKEN_KEY), any(),
                eq(Duration.ofDays(30)));
        verify(repository).insert(eq(7L), eq("ci"), anyString(), isNull(), eq(expires));
    }

    /**
     * 指定 scope 时只授予 scope，去重后写入元数据与声明。
     */
    @Test
    void restrictsTokenToRequestedScopes() {
        AuthUser user = user();
        when(authDirectory.findUserById(7L)).thenReturn(Optional.of(user));
        when(repository.countActive(eq(7L), any(LocalDateTime.class))).thenReturn(0);
        when(authDirectory.findRoleCodesByRoleIds(List.of(2L))).thenReturn(List.of("ROLE_USER"));
        when(authDirectory.findPermissionsByRoleIds(List.of(2L)))
                .thenReturn(Set.of("user:list", "user:add"));
        when(jwtEncoder.encode(any())).thenReturn(
                Jwt.withTokenValue("jwt-token").header("alg", "RS256").claim("sub", "7").build());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        LocalDateTime expires = LocalDateTime.ofInstant(NOW.plus(Duration.ofDays(30)), ZoneOffset.UTC);
        when(repository.findByTokenId(anyString())).thenReturn(Optional.of(
                new PersonalAccessTokenRecord(42L, 7L, "ci", "jti", List.of("user:list"),
                        expires, false, null, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))));
        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        service.create(7L, "ci", 30L, List.of("user:list", "user:list"));

        verify(jwtEncoder).encode(captor.capture());
        Object permissionsClaim = captor.getValue().getClaims().getClaim(Constants.PERMISSIONS);
        Object scopesClaim = captor.getValue().getClaims().getClaim(Constants.PAT_SCOPES_CLAIM);
        assertThat(permissionsClaim).isEqualTo(List.of("user:list"));
        assertThat(scopesClaim).isEqualTo(List.of("user:list"));
        verify(repository).insert(eq(7L), eq("ci"), anyString(), eq("user:list"), eq(expires));
    }

    /**
     * scope 超出用户当前权限时应拒绝签发。
     */
    @Test
    void rejectsScopeNotGranted() {
        when(authDirectory.findUserById(7L)).thenReturn(Optional.of(user()));
        when(repository.countActive(eq(7L), any(LocalDateTime.class))).thenReturn(0);
        when(authDirectory.findPermissionsByRoleIds(List.of(2L))).thenReturn(Set.of("user:list"));

        assertThatThrownBy(() -> service.create(7L, "ci", 30L, List.of("user:delete")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("scope");
    }

    /**
     * scope 数量超过上限时应拒绝签发。
     */
    @Test
    void rejectsTooManyScopes() {
        Set<String> permissions = IntStream.range(0, 51)
                .mapToObj(index -> "perm:" + index)
                .collect(java.util.stream.Collectors.toSet());
        when(authDirectory.findUserById(7L)).thenReturn(Optional.of(user()));
        when(repository.countActive(eq(7L), any(LocalDateTime.class))).thenReturn(0);
        when(authDirectory.findPermissionsByRoleIds(List.of(2L))).thenReturn(permissions);

        assertThatThrownBy(() -> service.create(7L, "ci", 30L, List.copyOf(permissions)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("scope");
    }

    /**
     * 名称与有效期应在签发前校验。
     */
    @Test
    void rejectsInvalidNameAndExpiry() {
        assertThatThrownBy(() -> service.create(7L, "  ", 30L, List.of()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(7L, "ci", 91L, List.of()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(7L, "ci", 0L, List.of()))
                .isInstanceOf(BusinessException.class);
    }

    /**
     * 活动令牌达到上限时应拒绝创建。
     */
    @Test
    void rejectsWhenLimitReached() {
        when(authDirectory.findUserById(7L)).thenReturn(Optional.of(user()));
        when(repository.countActive(eq(7L), any(LocalDateTime.class))).thenReturn(10);

        assertThatThrownBy(() -> service.create(7L, "ci", 30L, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("上限");
    }

    /**
     * 列表应合并元数据与撤销记录，给出状态与最近使用时间。
     */
    @Test
    void listsStatusesAndLastUsedTime() {
        LocalDateTime future = LocalDateTime.ofInstant(NOW.plus(Duration.ofDays(1)), ZoneOffset.UTC);
        LocalDateTime past = LocalDateTime.ofInstant(NOW.minus(Duration.ofDays(1)), ZoneOffset.UTC);
        LocalDateTime created = LocalDateTime.ofInstant(NOW.minus(Duration.ofDays(2)), ZoneOffset.UTC);
        when(repository.findByUserId(7L)).thenReturn(List.of(
                new PersonalAccessTokenRecord(1L, 7L, "active", "jti-active", List.of("user:list"),
                        future, false, null, created),
                new PersonalAccessTokenRecord(2L, 7L, "revoked", "jti-revoked", List.of(),
                        future, true, past, created),
                new PersonalAccessTokenRecord(3L, 7L, "expired", "jti-expired", List.of(),
                        past, false, null, created)));
        when(redisTemplate.hasKey(Constants.PAT_TOKEN_KEY + "jti-active")).thenReturn(true);
        when(redisTemplate.hasKey(Constants.PAT_TOKEN_KEY + "jti-revoked")).thenReturn(false);
        when(redisTemplate.hasKey(Constants.PAT_TOKEN_KEY + "jti-expired")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(Constants.PAT_LAST_USED_KEY + "jti-active"))
                .thenReturn("2026-11-17T12:00:00Z");

        List<PersonalAccessTokenSummary> summaries = service.list(7L);

        assertThat(summaries).hasSize(3);
        assertThat(summaries.get(0).status()).isEqualTo(PersonalAccessTokenService.STATUS_ACTIVE);
        assertThat(summaries.get(0).lastUsedAt())
                .isEqualTo(LocalDateTime.ofInstant(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC));
        assertThat(summaries.get(1).status()).isEqualTo(PersonalAccessTokenService.STATUS_REVOKED);
        assertThat(summaries.get(2).status()).isEqualTo(PersonalAccessTokenService.STATUS_EXPIRED);
    }

    /**
     * 撤销应先删除共享存储记录，再标记元数据。
     */
    @Test
    void revokeDeletesSharedRecordAndMarksMetadata() {
        LocalDateTime expires = LocalDateTime.ofInstant(NOW.plus(Duration.ofDays(1)), ZoneOffset.UTC);
        when(repository.findByIdAndUserId(11L, 7L)).thenReturn(Optional.of(
                new PersonalAccessTokenRecord(11L, 7L, "ci", "jti-11", List.of(), expires, false, null,
                        LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))));

        service.revoke(7L, 11L);

        verify(redisTemplate).delete(Constants.PAT_TOKEN_KEY + "jti-11");
        verify(redisTemplate).delete(Constants.PAT_LAST_USED_KEY + "jti-11");
        verify(repository).revoke(11L, 7L, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
    }

    private AuthUser user() {
        return new AuthUser(7L, "tester", "$2a$10$hash", "Tester", null, 1L, 1, List.of(2L));
    }
}
