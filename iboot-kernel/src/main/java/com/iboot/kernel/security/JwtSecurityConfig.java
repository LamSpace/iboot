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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 安全配置。
 *
 * <p>绑定 {@code jwt.*} 配置项，包括令牌过期时间、密钥等参数，
 * 供令牌生成和校验逻辑使用。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtSecurityConfig {

    /**
     * JWT Token 过期时间（秒），作为安全兜底
     */
    private long expiration = 1800L;

    /**
     * Session 超时时间（分钟）
     */
    private long sessionTimeout = 30L;

    /**
     * 认证服务的公钥集地址，资源服务器用它验签自包含令牌。
     */
    private String jwkSetUri = "http://localhost:8083/oauth2/jwks";

    /**
     * 是否继续接受旧的对称签名令牌。
     *
     * <p>迁移期双栈开关：开启时同时接受 HS256 与 RS256 令牌；关闭后旧令牌一律拒绝，
     * 并返回可识别的「令牌版本过期」错误码。
     */
    private boolean legacyEnabled = true;

    @SuppressWarnings("all")
    public JwtSecurityConfig() {
    }

    /**
     * JWT Token 过期时间（秒），作为安全兜底
     */
    @SuppressWarnings("all")
    public long getExpiration() {
        return this.expiration;
    }

    /**
     * JWT Token 过期时间（秒），作为安全兜底
     */
    @SuppressWarnings("all")
    public void setExpiration(final long expiration) {
        this.expiration = expiration;
    }

    /**
     * Session 超时时间（分钟）
     */
    @SuppressWarnings("all")
    public long getSessionTimeout() {
        return this.sessionTimeout;
    }

    /**
     * Session 超时时间（分钟）
     */
    @SuppressWarnings("all")
    public void setSessionTimeout(final long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    /**
     * 认证服务的公钥集地址。
     *
     * @return 公钥集地址
     */
    public String getJwkSetUri() {
        return this.jwkSetUri;
    }

    /**
     * 设置认证服务的公钥集地址。
     *
     * @param jwkSetUri 公钥集地址
     */
    public void setJwkSetUri(final String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    /**
     * 是否继续接受旧的对称签名令牌。
     *
     * @return 开启时返回 {@code true}
     */
    public boolean isLegacyEnabled() {
        return this.legacyEnabled;
    }

    /**
     * 设置是否继续接受旧的对称签名令牌。
     *
     * @param legacyEnabled 双栈开关
     */
    public void setLegacyEnabled(final boolean legacyEnabled) {
        this.legacyEnabled = legacyEnabled;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JwtSecurityConfig))
            return false;
        final JwtSecurityConfig other = (JwtSecurityConfig) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        if (this.getExpiration() != other.getExpiration())
            return false;
        if (this.getSessionTimeout() != other.getSessionTimeout())
            return false;
        if (this.isLegacyEnabled() != other.isLegacyEnabled())
            return false;
        if (!java.util.Objects.equals(this.getJwkSetUri(), other.getJwkSetUri()))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof JwtSecurityConfig;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $expiration = this.getExpiration();
        result = result * PRIME + (int) ($expiration >>> 32 ^ $expiration);
        final long $sessionTimeout = this.getSessionTimeout();
        result = result * PRIME + (int) ($sessionTimeout >>> 32 ^ $sessionTimeout);
        result = result * PRIME + (this.isLegacyEnabled() ? 79 : 97);
        result = result * PRIME + java.util.Objects.hashCode(this.getJwkSetUri());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "JwtSecurityConfig(expiration=" + this.getExpiration() + ", sessionTimeout=" + this.getSessionTimeout()
                + ", jwkSetUri=" + this.getJwkSetUri() + ", legacyEnabled=" + this.isLegacyEnabled() + ")";
    }

}
