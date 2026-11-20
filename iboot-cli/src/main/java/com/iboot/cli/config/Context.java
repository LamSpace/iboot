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

import java.net.URI;
import java.util.Objects;

/**
 * 一个 CLI 运行上下文。
 *
 * <p>描述门户公开入口、OIDC 签发者、客户端标识与本地回调地址。凭据按 context 名称隔离，
 * 切换上下文不会复用其他环境的令牌。公开入口与签发者默认必须是 HTTPS；仅 loopback
 * 地址允许 HTTP，用于本地排障。
 *
 * @param name 上下文名称
 * @param baseUrl 门户公开入口基址（经门户反代到网关）
 * @param issuer OIDC 签发者；为空时回落到 {@code baseUrl}
 * @param clientId OAuth2 客户端标识
 * @param redirectUri loopback 回调地址
 * @author iBoot Team
 * @since 1.0.0
 */
public record Context(String name, String baseUrl, String issuer, String clientId, String redirectUri) {

    /**
     * 创建并校验上下文。
     *
     * @throws IllegalArgumentException 名称、地址为空，或地址不是 HTTPS/loopback HTTP 时抛出
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
        requireSecureOrigin(baseUrl, "baseUrl");
        requireSecureOrigin(issuer, "issuer");
        clientId = clientId == null || clientId.isBlank() ? "iboot-cli" : clientId;
        redirectUri = redirectUri == null || redirectUri.isBlank()
                ? "http://127.0.0.1:8765/callback"
                : redirectUri;
    }

    /**
     * 创建默认开发上下文。
     *
     * <p>默认指向宿主机开发的门户入口；Docker 全量部署使用单独的 context。
     *
     * @return 默认上下文
     */
    public static Context defaults() {
        return new Context("dev", "https://localhost:5173", "https://localhost:5173",
                "iboot-cli", "http://127.0.0.1:8765/callback");
    }

    private static void requireSecureOrigin(String value, String field) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(field + " 不是合法地址：" + value);
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException(field + " 缺少主机名：" + value);
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return;
        }
        if ("http".equalsIgnoreCase(scheme) && isLoopbackHost(host)) {
            return;
        }
        throw new IllegalArgumentException(field + " 必须使用 HTTPS（仅 loopback 地址允许 HTTP）：" + value);
    }

    private static boolean isLoopbackHost(String host) {
        String normalized = host.toLowerCase();
        return normalized.equals("localhost")
                || normalized.equals("127.0.0.1")
                || normalized.equals("::1")
                || normalized.equals("[::1]");
    }

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
