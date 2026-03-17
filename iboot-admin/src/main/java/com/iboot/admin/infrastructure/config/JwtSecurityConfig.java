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

package com.iboot.admin.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 安全配置
 *
 * @author iBoot
 */
@Component
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
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "JwtSecurityConfig(expiration=" + this.getExpiration() + ", sessionTimeout=" + this.getSessionTimeout()
                + ")";
    }

}
