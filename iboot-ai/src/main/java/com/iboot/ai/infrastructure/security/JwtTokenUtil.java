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

package com.iboot.ai.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 验签工具。
 *
 * <p>只做验签与解析，不签发令牌。过期时间由 Redis 会话控制，因此签名有效但已过期的令牌
 * 仍返回 {@code true}，与 {@code iboot-admin} 的滑动过期语义保持一致。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class JwtTokenUtil {

    private final String secret;

    /**
     * 创建验签工具。
     *
     * @param secret 与 admin 共享的 HMAC 密钥
     */
    public JwtTokenUtil(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    /**
     * 仅校验令牌签名。
     *
     * @param token JWT
     * @return 签名有效返回 {@code true}
     */
    public boolean validateTokenSignature(String token) {
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析令牌声明。
     *
     * @param token JWT
     * @return 声明
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
