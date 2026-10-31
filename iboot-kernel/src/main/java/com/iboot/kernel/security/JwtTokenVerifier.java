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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * 令牌校验器，同时支持自包含令牌与旧的对称签名令牌。
 *
 * <p>按令牌头部声明的算法分流：{@code RS}/{@code PS}/{@code ES} 走认证服务公钥验签，
 * {@code HS} 走共享密钥。双栈开关关闭后，对称签名令牌一律抛出
 * {@link TokenVersionRetiredException}，使前端可以区分「令牌版本过期」与「凭据错误」。
 *
 * <p>过期时间由共享存储中的会话控制（滑动续期），因此令牌自身过期不视为失败，
 * 仍返回声明交由调用方判断——这与迁移前的行为一致。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class JwtTokenVerifier {

    /**
     * 兼容旧令牌的共享密钥。
     */
    private final SecretKey legacyKey;

    /**
     * 认证服务公钥集。
     */
    private final JwkSetProvider jwkSetProvider;

    /**
     * 是否继续接受旧的对称签名令牌。
     */
    private final boolean legacyEnabled;

    /**
     * 创建校验器。
     *
     * @param legacySecret 旧令牌使用的共享密钥
     * @param jwkSetProvider 认证服务公钥集
     * @param legacyEnabled 是否继续接受旧令牌
     */
    public JwtTokenVerifier(String legacySecret, JwkSetProvider jwkSetProvider, boolean legacyEnabled) {
        this.legacyKey = Keys.hmacShaKeyFor(legacySecret.getBytes(StandardCharsets.UTF_8));
        this.jwkSetProvider = jwkSetProvider;
        this.legacyEnabled = legacyEnabled;
    }

    /**
     * 校验令牌并返回声明。
     *
     * @param token 令牌原文
     * @return 校验结果；签名非法或算法不受支持时返回 {@code null}
     * @throws TokenVersionRetiredException 令牌使用已停用的算法时抛出
     */
    public VerifiedToken verify(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .keyLocator(this::locateKey)
                    .build()
                    .parseSignedClaims(token);
            return new VerifiedToken(jws.getPayload(), isLegacy(jws.getHeader()));
        } catch (TokenVersionRetiredException e) {
            throw e;
        } catch (ExpiredJwtException e) {
            return new VerifiedToken(e.getClaims(), isLegacy(e.getHeader()));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 按令牌头部选择验签密钥。
     *
     * @param header 令牌头部
     * @return 验签密钥；无法确定时返回 {@code null}
     * @throws TokenVersionRetiredException 对称签名令牌已被停用时抛出
     */
    private Key locateKey(Header header) {
        String algorithm = header.getAlgorithm();
        if (algorithm == null) {
            return null;
        }
        if (algorithm.startsWith("HS")) {
            if (!legacyEnabled) {
                throw new TokenVersionRetiredException("旧的对称签名令牌已被停用");
            }
            return legacyKey;
        }
        if (algorithm.startsWith("RS") || algorithm.startsWith("PS") || algorithm.startsWith("ES")) {
            Object keyId = header.get("kid");
            return jwkSetProvider.resolve(keyId == null ? null : keyId.toString());
        }
        return null;
    }

    /**
     * 判断是否为旧的对称签名令牌。
     *
     * @param header 令牌头部
     * @return 使用 {@code HS} 算法时返回 {@code true}
     */
    private static boolean isLegacy(Header header) {
        String algorithm = header.getAlgorithm();
        return algorithm != null && algorithm.startsWith("HS");
    }

    /**
     * 令牌校验结果。
     *
     * @param claims 令牌声明
     * @param legacy 是否来自旧的对称签名令牌
     */
    public record VerifiedToken(Claims claims, boolean legacy) {
    }
}
