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

/**
 * 一个 context 的令牌集合。
 *
 * <p>只保存认证服务返回的令牌与绝对到期时间；不保存密码、客户端密钥或授权码。
 * {@code expiresAt} 使用 epoch 毫秒，便于直接与本地时钟比较。
 *
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌，可为 {@code null}
 * @param idToken OIDC 身份令牌，可为 {@code null}
 * @param expiresAt 访问令牌到期时间（epoch 毫秒）
 * @param tokenType 令牌类型，通常为 {@code Bearer}
 * @author iBoot Team
 * @since 1.0.0
 */
public record TokenSet(String accessToken, String refreshToken, String idToken,
                       long expiresAt, String tokenType) {

    /**
     * 判断访问令牌是否需要在给定提前量内续期。
     *
     * @param minValidityMillis 最小剩余有效期（毫秒）
     * @return 需要续期时返回 {@code true}
     */
    public boolean needsRefresh(long minValidityMillis) {
        return expiresAt <= System.currentTimeMillis() + minValidityMillis;
    }

    /**
     * 判断是否持有刷新令牌。
     *
     * @return 存在且非空时返回 {@code true}
     */
    public boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.isBlank();
    }
}
