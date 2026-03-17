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

package com.iboot.admin.infrastructure.security;

import com.iboot.admin.common.constant.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 *
 * @author iBoot
 */
@Component
public class JwtTokenUtil {

    /**
     * 令牌密钥
     */
    @Value("${jwt.secret:iboot-admin-secret-key-for-jwt-token-generation-minimum-256-bits}")
    private String secret;

    /**
     * 令牌有效期（默认 30 分钟）
     */
    @Value("${jwt.expiration:1800}")
    private Long expiration;

    /**
     * 生成 Token
     *
     * @param claims 用户信息
     *
     * @return token
     */
    public String createToken(Map<String, Object> claims) {
        return createToken(claims, expiration);
    }

    /**
     * 生成 Token（指定过期时间）
     *
     * @param claims            用户信息
     * @param expirationSeconds 过期时间（秒）
     *
     * @return token
     */
    public String createToken(Map<String, Object> claims, long expirationSeconds) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder().claims(claims).issuedAt(now).expiration(expiryDate).signWith(getSecretKey()).compact();
    }

    /**
     * 从 Token 中获取数据声明
     *
     * @param token JWT令牌
     *
     * @return Claims 数据声明对象
     */
    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
    }

    /**
     * 从 Token 中获取用户 ID
     *
     * @param token JWT令牌
     *
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.get(Constants.USER_ID).toString());
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT令牌
     *
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 验证 Token 是否过期
     *
     * @param token JWT令牌
     *
     * @return 如果已过期则返回true，否则返回false
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证 Token
     *
     * @param token JWT令牌
     *
     * @return 如果有效则返回true，否则返回false
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 仅验证 Token 签名（忽略过期时间） 用于配合 Redis 滑动过期的 session 管理机制
     *
     * @param token JWT令牌
     *
     * @return 签名有效则返回true，否则返回false
     */
    public boolean validateTokenSignature(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            // 签名验证通过后才会抛出此异常，说明 Token 是本服务器签发的
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取密钥
     *
     * @return SecretKey 密钥对象
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

}
