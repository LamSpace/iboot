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

package com.iboot.cli.config;

import java.util.Objects;

/**
 * 一个 CLI 运行上下文。
 *
 * <p>描述网关地址、OIDC 签发者、客户端标识与本地回调地址。凭据按 context 名称隔离，
 * 切换上下文不会复用其他环境的令牌。
 *
 * @param name 上下文名称
 * @param baseUrl 网关基址
 * @param issuer OIDC 签发者；为空时回落到 {@code baseUrl}
 * @param clientId OAuth2 客户端标识
 * @param redirectUri loopback 回调地址
 * @author iBoot Team
 * @since 1.0.0
 */
public record Context(String name, String baseUrl, String issuer, String clientId, String redirectUri) {

    /**
     * 创建并校验上下文。
     */
    public Context {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(baseUrl, "baseUrl");
        if (name.isBlank()) {
            throw new IllegalArgumentException("context 名称不能为空");
        }
        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl 不能为空");
        }
        baseUrl = stripTrailingSlash(baseUrl);
        issuer = issuer == null || issuer.isBlank() ? baseUrl : stripTrailingSlash(issuer);
        clientId = clientId == null || clientId.isBlank() ? "iboot-cli" : clientId;
        redirectUri = redirectUri == null || redirectUri.isBlank()
                ? "http://127.0.0.1:8765/callback"
                : redirectUri;
    }

    /**
     * 创建默认开发上下文。
     *
     * @return 默认上下文
     */
    public static Context defaults() {
        return new Context("dev", "http://localhost:8088", "http://localhost:8088",
                "iboot-cli", "http://127.0.0.1:8765/callback");
    }

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
