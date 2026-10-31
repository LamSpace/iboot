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

import com.iboot.kernel.security.JwtAccessDeniedHandler;
import com.iboot.kernel.security.JwtAuthenticationEntryPoint;
import com.iboot.kernel.security.JwtAuthenticationFilter;
import com.iboot.kernel.security.SecurityWhitelistProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.DispatcherType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/**
 * 授权服务器配置。
 *
 * <p>认证服务同时承担两种角色，因此这里定义三条过滤链：
 * <ol>
 *     <li>授权服务器端点（{@code /oauth2/**}、{@code /.well-known/**}、{@code /connect/**}）；</li>
 *     <li>登录页，需要服务端会话承载登录态，由共享 Redis 保存；</li>
 *     <li>其余接口，保持无状态并沿用内核的令牌过滤器。</li>
 * </ol>
 *
 * <p>客户端登记放在配置中（不建库表），授权记录放在内存——重启会使用户需要重新登录，
 * 但不会影响已签发令牌在资源服务上的验签。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties(AuthServerProperties.class)
public class AuthorizationServerConfig {

    /**
     * 授权服务器端点的过滤链。
     *
     * @param http HTTP 安全配置
     * @return 安全过滤链
     * @throws Exception 构建失败时抛出
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer configurer = new OAuth2AuthorizationServerConfigurer();
        http.securityMatcher(configurer.getEndpointsMatcher());
        OAuth2AuthorizationServerConfigurer applied = http.apply(configurer);
        applied.oidc(Customizer.withDefaults());

        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * 登录页的过滤链。
     *
     * <p>使用服务端会话承载登录态；会话由共享 Redis 保存，多实例部署时回调可落在任意实例。
     *
     * @param http HTTP 安全配置
     * @param sessionRevocationLogoutHandler 登出时撤销会话的处理器
     * @return 安全过滤链
     * @throws Exception 构建失败时抛出
     */
    @Bean
    @Order(2)
    public SecurityFilterChain loginFilterChain(HttpSecurity http,
                                                SessionRevocationLogoutHandler sessionRevocationLogoutHandler,
                                                AuthServerProperties properties)
            throws Exception {
        http.securityMatchers(matchers -> matchers.requestMatchers("/login", "/login/**"))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                // 直接打开认证服务登录页时没有授权请求可回跳，登录后送到门户，
                // 而不是落到本服务的根路径（那里没有页面）
                .formLogin(form -> form.loginPage("/login").permitAll()
                        .defaultSuccessUrl(properties.getPortalUrl() + "/dashboard", false))
                .logout(logout -> logout.addLogoutHandler(sessionRevocationLogoutHandler)
                        .logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    /**
     * 业务接口的过滤链，保持无状态并沿用内核的令牌过滤器。
     *
     * @param http HTTP 安全配置
     * @param jwtAuthenticationFilter 内核令牌过滤器
     * @param whitelistProperties 宿主白名单
     * @return 安全过滤链
     * @throws Exception 构建失败时抛出
     */
    @Bean
    @Order(3)
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http,
                                                         JwtAuthenticationFilter jwtAuthenticationFilter,
                                                         JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                                                         JwtAccessDeniedHandler jwtAccessDeniedHandler,
                                                         SecurityWhitelistProperties whitelistProperties)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 与内核默认链保持一致：未认证返回统一信封的 401，越权返回 403，
                // 否则会落到 Spring Boot 的 /error 白标页
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(whitelistProperties.getWhitelist().toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 门户客户端的登记信息。
     *
     * <p>公共客户端，强制 PKCE，不签发客户端密钥；启用刷新令牌轮换，便于多标签页与静默续期。
     *
     * @param properties 授权服务器配置
     * @return 客户端仓库
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(AuthServerProperties properties) {
        RegisteredClient portal = RegisteredClient.withId(properties.getClientId())
                .clientId(properties.getClientId())
                .clientName(properties.getClientName())
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUris(uris -> uris.addAll(properties.getRedirectUris()))
                .postLogoutRedirectUris(uris -> uris.addAll(properties.getPostLogoutRedirectUris()))
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("api")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(properties.getAccessTokenMinutes()))
                        .refreshTokenTimeToLive(Duration.ofMinutes(properties.getRefreshTokenMinutes()))
                        .reuseRefreshTokens(false)
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(portal);
    }

    /**
     * 授权记录存储。
     *
     * <p>记录本身放在内存中：认证服务重启后刷新令牌失效，用户需要重新登录；已签发的访问令牌
     * 在资源服务上的验签不受影响。外层包一层会话撤销：登出与令牌撤销会同步删除共享会话。
     *
     * @param redisTemplate 共享会话 Redis 模板
     * @return 授权记录存储
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(RedisTemplate<String, Object> redisTemplate) {
        return new SessionAwareAuthorizationService(new InMemoryOAuth2AuthorizationService(), redisTemplate);
    }

    /**
     * 授权同意存储。
     *
     * @return 授权同意存储
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService() {
        return new InMemoryOAuth2AuthorizationConsentService();
    }

    /**
     * 签名密钥源。
     *
     * @param properties 授权服务器配置
     * @return 公钥集来源
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(AuthServerProperties properties) {
        return new ImmutableJWKSet<>(new JWKSet(loadOrGenerateRsaKey(properties)));
    }

    /**
     * OIDC 用户信息端点使用的令牌解码器。
     *
     * @param jwkSource 公钥集来源
     * @return 令牌解码器
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * 授权服务器设置。
     *
     * @param properties 授权服务器配置
     * @return 授权服务器设置
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings(AuthServerProperties properties) {
        return AuthorizationServerSettings.builder().issuer(properties.getIssuer()).build();
    }

    /**
     * 加载或生成签名密钥。
     *
     * <p>配置了密钥文件时优先加载，不存在则生成并写回，使重启与多实例共享同一把密钥；
     * 未配置时生成临时密钥，仅适用于单实例开发环境。
     *
     * @param properties 授权服务器配置
     * @return RSA 签名密钥
     */
    private RSAKey loadOrGenerateRsaKey(AuthServerProperties properties) {
        String keyFile = properties.getJwkKeyFile();
        if (StringUtils.isBlank(keyFile)) {
            return generateRsaKey();
        }
        Path path = Path.of(keyFile);
        try {
            if (Files.exists(path)) {
                return RSAKey.parse(Files.readString(path));
            }
            RSAKey generated = generateRsaKey();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, generated.toJSONString());
            return generated;
        } catch (Exception e) {
            throw new IllegalStateException("加载或生成签名密钥失败：" + keyFile, e);
        }
    }

    /**
     * 生成一把 2048 位 RSA 签名密钥。
     *
     * @return RSA 签名密钥
     */
    private RSAKey generateRsaKey() {
        try {
            return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
        } catch (Exception e) {
            throw new IllegalStateException("生成签名密钥失败", e);
        }
    }
}
