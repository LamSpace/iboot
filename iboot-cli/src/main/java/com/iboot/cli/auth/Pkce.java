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

package com.iboot.cli.auth;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 授权码流程使用的 PKCE 参数。
 *
 * <p>每次登录生成新的 verifier、challenge 与 state；verifier 不落盘，challenge 使用
 * S256，state 用于校验回调来源。
 *
 * @param verifier PKCE code verifier
 * @param challenge PKCE code challenge（S256）
 * @param state 授权请求与回调的关联值
 * @author iBoot Team
 * @since 1.0.0
 */
public record Pkce(String verifier, String challenge, String state) {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * 生成一组新的 PKCE 参数。
     *
     * @return PKCE 参数
     */
    public static Pkce generate() {
        byte[] verifierBytes = new byte[48];
        RANDOM.nextBytes(verifierBytes);
        String verifier = URL_ENCODER.encodeToString(verifierBytes);
        String challenge = s256(verifier);
        byte[] stateBytes = new byte[16];
        RANDOM.nextBytes(stateBytes);
        String state = URL_ENCODER.encodeToString(stateBytes);
        return new Pkce(verifier, challenge, state);
    }

    private static String s256(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return URL_ENCODER.encodeToString(digest.digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new CliException(ExitCode.BUSINESS, "当前 JDK 不支持 SHA-256", e);
        }
    }
}
