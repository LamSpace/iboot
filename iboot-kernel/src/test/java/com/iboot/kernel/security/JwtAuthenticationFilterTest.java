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
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 资源服务器过滤器的语义测试。
 *
 * <p>用模拟的 Redis 覆盖「放行 / 建立上下文 / 拒绝」三类分支；真实会话与滑动续期由
 * {@link JwtAuthenticationFilterRedisTest} 覆盖。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "iboot-admin-secret-key-for-jwt-token-generation-minimum-256-bits";

    private JwtTokenUtil jwtTokenUtil;

    private RedisTemplate<String, Object> redisTemplate;

    private ValueOperations<String, Object> valueOperations;

    private SecurityWhitelistProperties whitelistProperties;

    private JwkSetProvider jwkSetProvider;

    private KeyPair keyPair;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        jwtTokenUtil = new JwtTokenUtil(SECRET, 1800L);
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        whitelistProperties = new SecurityWhitelistProperties();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        jwkSetProvider = mock(JwkSetProvider.class);
        when(jwkSetProvider.resolve("k1")).thenReturn(keyPair.getPublic());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 白名单路径不读取会话，直接放行。
     */
    @Test
    void passesWhitelistedPathWithoutReadingSession() throws Exception {
        whitelistProperties.setWhitelist(List.of("/actuator/**"));
        FilterChain chain = mock(FilterChain.class);

        filter().doFilter(request("/actuator/health", null), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        org.mockito.Mockito.verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /**
     * 未携带令牌时放行，由后续安全组件决定是否拒绝。
     */
    @Test
    void passesRequestWithoutToken() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        filter().doFilter(request("/api/user/list", null), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        org.mockito.Mockito.verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    /**
     * 签名非法的令牌不建立认证上下文，也不报错。
     */
    @Test
    void ignoresTokenWithInvalidSignature() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        filter().doFilter(request("/api/user/list", "not-a-jwt"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /**
     * 会话已失效（Redis 中无记录）时不建立认证上下文。
     */
    @Test
    void ignoresTokenWithoutSession() throws Exception {
        when(valueOperations.get(anyString())).thenReturn(null);
        FilterChain chain = mock(FilterChain.class);

        filter().doFilter(request("/api/user/list", validToken()), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /**
     * 有效令牌与会话应建立认证上下文。
     */
    @Test
    void authenticatesWhenSessionExists() throws Exception {
        when(valueOperations.get(anyString()))
                .thenReturn(Map.of("userId", 7L, "username", "tester", "permissions", List.of("user:list")));

        filter().doFilter(request("/api/user/list", validToken()), new MockHttpServletResponse(),
                mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(7L);
    }

    /**
     * 读取会话时抛异常应返回 401，且响应体是统一信封。
     */
    @Test
    void returnsUnauthorizedEnvelopeWhenRedisFails() throws Exception {
        when(valueOperations.get(anyString())).thenThrow(new IllegalStateException("redis down"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request("/api/user/list", validToken()), response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":401").contains("\"message\"");
    }

    private JwtAuthenticationFilter filter() {
        return filter(true);
    }

    private JwtAuthenticationFilter filter(boolean legacyEnabled) {
        JwtTokenVerifier verifier = new JwtTokenVerifier(SECRET, jwkSetProvider, legacyEnabled);
        return new JwtAuthenticationFilter(verifier, redisTemplate, new JwtSecurityConfig(), whitelistProperties);
    }

    /**
     * 自包含令牌在会话未被撤销时应建立认证上下文。
     */
    @Test
    void authenticatesStatelessTokenWhenSessionExists() throws Exception {
        when(valueOperations.get(anyString()))
                .thenReturn(Map.of("userId", 7L, "username", "tester"));

        filter().doFilter(request("/api/user/list", rsaToken()), new MockHttpServletResponse(),
                mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(7L);
        assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("tester");
    }

    /**
     * 会话被撤销（共享存储中无记录）时，自包含令牌不建立认证上下文。
     */
    @Test
    void ignoresStatelessTokenWithoutSession() throws Exception {
        when(valueOperations.get(anyString())).thenReturn(null);

        filter().doFilter(request("/api/user/list", rsaToken()), new MockHttpServletResponse(),
                mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /**
     * 双栈关闭后旧令牌返回 401 与可识别的业务状态码。
     */
    @Test
    void returnsVersionRetiredCodeWhenLegacyDisabled() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter(false).doFilter(request("/api/user/list", validToken()), response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":" + Constants.TOKEN_VERSION_RETIRED);
    }

    private MockHttpServletRequest request(String path, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        return request;
    }

    private String validToken() {
        return jwtTokenUtil.createToken(Map.of("userId", 7L));
    }

    private String rsaToken() {
        return Jwts.builder()
                .id("jti-1")
                .subject("7")
                .header().keyId("k1").and()
                .claim("username", "tester")
                .claim(Constants.PERMISSIONS, List.of("user:list"))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }
}
